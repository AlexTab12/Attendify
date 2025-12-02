package ca.unb.attendify
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.unb.attendify.model.CourseAttendanceSummary
import ca.unb.attendify.ui.theme.AttendifyTheme
import ca.unb.attendify.viewmodel.AttendanceViewModel
import ca.unb.attendify.viewmodel.AttendanceViewModelFactory
import ca.unb.attendify.viewmodel.DashboardUiState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.platform.LocalContext
import ca.unb.attendify.notifications.ThresholdNotifier
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FabPosition

//The main activity class where we connect everything here
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttendifyTheme {
                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }

                val viewModel: AttendanceViewModel = viewModel(
                    factory = AttendanceViewModelFactory(application)
                )

                val dashboardState by viewModel.dashboardState.collectAsState()
                val detailState by viewModel.detailState.collectAsState()

                var selectedCourseId by remember { mutableStateOf<String?>(null) }

                val qrLauncher = rememberLauncherForActivityResult(
                    contract = ScanContract()
                ) { result: ScanIntentResult ->
                    val contents = result.contents
                    if (contents != null) {
                        viewModel.handleScannedQr(contents)
                    }
                }


                if (selectedCourseId == null) {
                    AttendifyDashboardScreen(
                            uiState = dashboardState,
                            onRefresh = { viewModel.refreshDashboard() },

                        onScanQr = {
                            val options = ScanOptions().apply {
                                setPrompt("Scan course QR code")
                                setBeepEnabled(false)
                                setOrientationLocked(true)
                            }
                            qrLauncher.launch(options)
                        },

                            onAddCourse = { code, name, threshold ->
                                viewModel.addCourse(code, name, threshold)
                            },

                            onManualCheckIn = { courseId ->
                                viewModel.manualCheckInForToday(courseId)
                            },

                            onCourseSelected = { courseId ->
                                selectedCourseId = courseId
                                viewModel.loadCourseDetail(courseId)
                            }
                        )
                } else {
                    CourseDetailScreen(
                        uiState = detailState,
                        onBack = { selectedCourseId = null },
                        onManualCheckIn = {
                            viewModel.manualCheckInForToday(selectedCourseId!!)
                        },
                        onAddMissedYesterday = {
                            viewModel.addMissedSessionYesterday(selectedCourseId!!)
                        },
                        onAddFutureTomorrow = {
                            viewModel.addFutureSessionTomorrow(selectedCourseId!!)
                        }
                    )
                }
            }
        }
    }
}

//The dashboard set up in the main screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendifyDashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onScanQr: () -> Unit,
    onAddCourse: (String, String, Int) -> Unit,
    onManualCheckIn: (String) -> Unit,
    onCourseSelected: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var courseIdPendingCheckIn by remember { mutableStateOf<String?>(null) }
    var courseTitlePendingCheckIn by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(uiState.courseSummaries) {
        uiState.courseSummaries.forEach { summary ->
            val percentage = summary.attendancePercentage
            val required = summary.course.requiredAttendanceThreshold

            if (summary.totalSessions > 0 && percentage < required) {
                ThresholdNotifier.notifyBelowThreshold(
                    context = context,
                    courseCode = summary.course.courseCode,
                    percentage = percentage,
                    required = required
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Attendify",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = { },
                actions = {
                    IconButton(onClick = onScanQr) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan QR code"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add course"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                //the default state for a user who just opened the app for the first time
                uiState.courseSummaries.isEmpty() -> {
                    Text(
                        text = "No courses yet. Tap + to add one.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.courseSummaries) { summary ->
                            CourseCard(
                                summary = summary,
                                onClick = { onCourseSelected(summary.course.courseId) }
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    if (courseIdPendingCheckIn != null) {
        AlertDialog(
            onDismissRequest = { courseIdPendingCheckIn = null },
            title = { Text("Manual check-in") },
            text = {
                Text(
                    "Mark today as attended for\n$courseTitlePendingCheckIn ?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onManualCheckIn(courseIdPendingCheckIn!!)
                    courseIdPendingCheckIn = null
                }) {
                    Text("Check in")
                }
            },
            dismissButton = {
                TextButton(onClick = { courseIdPendingCheckIn = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddCourseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, threshold ->
                onAddCourse(code, name, threshold)
                showAddDialog = false
            }
        )
    }
}

//The set up for each course card
@Composable
fun CourseCard(summary: CourseAttendanceSummary, onClick: () -> Unit = {}) {
    val course = summary.course
    val percentage = summary.attendancePercentage
    val belowThreshold = summary.isBelowThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${course.courseCode} • ${course.courseName}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Required: ${course.requiredAttendanceThreshold}%",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Attendance: $percentage%",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (belowThreshold)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${summary.attendedCount}/${summary.totalSessions} sessions",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

//The adding a course set up
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course code (e.g. CS 2063)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it.filter { ch -> ch.isDigit() }.take(3)
                    },
                    label = { Text("Required attendance %") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val threshold = thresholdText.toIntOrNull() ?: 80
                onConfirm(code, name, threshold.coerceIn(0, 100))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
//The detail screen set up if you click a course to expand it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    uiState: ca.unb.attendify.viewmodel.CourseDetailUiState,
    onBack: () -> Unit,
    onManualCheckIn: () -> Unit,
    onAddMissedYesterday: () -> Unit,
    onAddFutureTomorrow: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.course?.courseCode ?: "Course Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                uiState.course == null -> {
                    Text(
                        text = uiState.errorMessage ?: "Course not found.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    val course = uiState.course
                    val summary = uiState.summary

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "${course.courseCode} • ${course.courseName}",
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (summary != null) {
                            Text(
                                text = "Attendance: ${summary.attendancePercentage}%",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${summary.attendedCount}/${summary.totalSessions} sessions",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // This is our main check in button
                        Button(
                            onClick = onManualCheckIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                "Mark Today as Attended",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // All the other buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAddMissedYesterday,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Missed (Yesterday)")
                            }

                            OutlinedButton(
                                onClick = onAddFutureTomorrow,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Future (Tomorrow)")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Class Sessions",
                            style = MaterialTheme.typography.titleMedium
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(uiState.sessions) { session ->
                                SessionRow(session)
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
//This is the session rows in the detailed analytic view for missed/attended/future classes
@Composable
fun SessionRow(session: ca.unb.attendify.model.AttendanceSession) {
    val dateText = java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm",
        java.util.Locale.getDefault()
    ).format(java.util.Date(session.sessionDateTimeMillis))

    val label = when {
        session.sessionDateTimeMillis > System.currentTimeMillis() -> "Future"
        session.isAttended -> "Attended"
        else -> "Missed"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateText)
            Text(label)
        }
    }
}
