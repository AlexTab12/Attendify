package ca.unb.attendify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.attendify.model.CourseAttendanceSummary
import ca.unb.attendify.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import ca.unb.attendify.model.AttendanceSession
import ca.unb.attendify.model.Course
import java.util.Calendar
import kotlinx.coroutines.delay

//This is the attendance view model class

data class DashboardUiState(
    val isLoading: Boolean = false,
    val courseSummaries: List<CourseAttendanceSummary> = emptyList(),
    val errorMessage: String? = null
)

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val course: Course? = null,
    val sessions: List<AttendanceSession> = emptyList(),
    val summary: CourseAttendanceSummary? = null,
    val errorMessage: String? = null
)

/**
 * This is the main viewModel, it talks to the repository and exposes UI state to compose to app
 */
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository =
        AttendanceRepository.getInstance(application)

    private val _dashboardState = MutableStateFlow(DashboardUiState(isLoading = true))
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _detailState = MutableStateFlow(CourseDetailUiState())
    val detailState: StateFlow<CourseDetailUiState> = _detailState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            try {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                val summaries = repository.getCourseSummaries()
                _dashboardState.value = DashboardUiState(
                    isLoading = false,
                    courseSummaries = summaries,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    //The optional manual check in logic, works only on a per day basis
    fun manualCheckInForToday(courseId: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val endOfDay = startOfDay + 24L * 60L * 60L * 1000L - 1L

                val todaySessions = repository.getSessionsForCourseBetween(
                    courseId,
                    startOfDay,
                    endOfDay
                )

                if (todaySessions.any { it.isAttended }) {
                    val msg = "Already checked in for today"
                    _dashboardState.value = _dashboardState.value.copy(errorMessage = msg)
                    _detailState.value = _detailState.value.copy(errorMessage = msg)

                    viewModelScope.launch {
                        delay(2500)
                        _dashboardState.value = _dashboardState.value.copy(errorMessage = null)
                        _detailState.value = _detailState.value.copy(errorMessage = null)
                    }

                    return@launch
                }

                val session = AttendanceSession(
                    sessionId = java.util.UUID.randomUUID().toString(),
                    courseId = courseId,
                    sessionDateTimeMillis = now,
                    isAttended = true
                )

                repository.upsertSession(session)

                // a way to refresh screen if needed for any user
                refreshDashboard()
                loadCourseDetail(courseId)

                // to make sure there is a way to clear any un needed errors(we had some issue with them not going away properly)
                _detailState.value = _detailState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to check in"
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = msg
                )
                _detailState.value = _detailState.value.copy(
                    errorMessage = msg
                )
            }
        }
    }

    fun addSessionWithDayOffset(
        courseId: String,
        daysOffset: Int,
        isAttended: Boolean
    ) {
        viewModelScope.launch {
            try {

                val base = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // A way to move accrodingly to the calendar to ensure the days aren't off in the app
                base.add(Calendar.DAY_OF_YEAR, daysOffset)
                val startOfDay = base.timeInMillis
                val endOfDay = startOfDay + 24L * 60L * 60L * 1000L - 1L

                // Check for already existing sessions with same data
                val existing = repository.getSessionsForCourseBetween(
                    courseId,
                    startOfDay,
                    endOfDay
                )

                if (existing.isNotEmpty()) {
                    val msg = "Session already exists for that date"
                    _dashboardState.value = _dashboardState.value.copy(errorMessage = msg)
                    _detailState.value = _detailState.value.copy(errorMessage = msg)

                    // clear errors in 2.5s, we had issues with them not going away
                    viewModelScope.launch {
                        delay(2500)
                        _dashboardState.value = _dashboardState.value.copy(errorMessage = null)
                        _detailState.value = _detailState.value.copy(errorMessage = null)
                    }

                    return@launch
                }

                // Default time of storing courses to be at noon to avoid issues
                val ts = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                    set(Calendar.HOUR_OF_DAY, 12)
                }.timeInMillis

                repository.createSession(
                    courseId = courseId,
                    timestampMillis = ts,
                    isAttended = isAttended
                )

                refreshDashboard()
                loadCourseDetail(courseId)

                _detailState.value = _detailState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to add session"
                _dashboardState.value = _dashboardState.value.copy(errorMessage = msg)
                _detailState.value = _detailState.value.copy(errorMessage = msg)
            }
        }
    }

    fun addMissedSessionYesterday(courseId: String) {
        addSessionWithDayOffset(
            courseId = courseId,
            daysOffset = -1,
            isAttended = false
        )
    }

    fun addFutureSessionTomorrow(courseId: String) {
        addSessionWithDayOffset(
            courseId = courseId,
            daysOffset = +1,
            isAttended = false
        )
    }

    //QR handeling logic
    fun handleScannedQr(raw: String) {
        viewModelScope.launch {
            try {
                val text = raw.trim()

                //We have it accepting both formats such as "COURSE:CS 2063" or just "CS 2063"
                val courseCode = if (text.startsWith("COURSE:", ignoreCase = true)) {
                    text.substringAfter(":", "").trim()
                } else {
                    text
                }

                if (courseCode.isBlank()) {
                    setTransientError("Invalid QR code")
                    return@launch
                }

                val course = repository.getCourseByCode(courseCode)
                if (course == null) {
                    setTransientError("No course for QR: $courseCode")
                    return@launch
                }

                // Double check guarding
                manualCheckInForToday(course.courseId)
            } catch (e: Exception) {
                setTransientError(e.message ?: "Failed to handle QR scan")
            }
        }
    }

    private fun setTransientError(message: String) {
        _dashboardState.value = _dashboardState.value.copy(errorMessage = message)
        _detailState.value = _detailState.value.copy(errorMessage = message)

        viewModelScope.launch {
            delay(2500)
            _dashboardState.value = _dashboardState.value.copy(errorMessage = null)
            _detailState.value = _detailState.value.copy(errorMessage = null)
        }
    }


    //loading course detail logic
    fun loadCourseDetail(courseId: String) {
        viewModelScope.launch {
            try {
                _detailState.value = CourseDetailUiState(isLoading = true)

                val course = repository.getCourseById(courseId)
                    ?: throw IllegalArgumentException("Course not found")

                val sessions = repository.getSessionsForCourse(courseId)
                val summary = ca.unb.attendify.model.calculateCourseAttendanceSummary(
                    course,
                    sessions
                )

                _detailState.value = CourseDetailUiState(
                    isLoading = false,
                    course = course,
                    sessions = sessions,
                    summary = summary,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _detailState.value = CourseDetailUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load course"
                )
            }
        }
    }

    fun addCourse(
        courseCode: String,
        courseName: String,
        requiredThreshold: Int
    ) {
        if (courseCode.isBlank() || courseName.isBlank()) return

        viewModelScope.launch {
            try {
                repository.createCourse(
                    courseCode = courseCode,
                    courseName = courseName,
                    requiredThreshold = requiredThreshold
                )
                refreshDashboard()
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = e.message ?: "Failed to add course"
                )
            }
        }
    }
}

