package ca.unb.attendify.model

import kotlin.math.roundToInt

/**
 * This class is for attendance calculations that show in the dashbard for display
 */
data class CourseAttendanceSummary(
    val course: Course,
    val attendedCount: Int,
    val totalSessions: Int,
    val attendancePercentage: Int,
    val isBelowThreshold: Boolean
)

/**
 * Calculation for attendance for a single class
 * And future sessions are ignored for the current precentage
 */
fun calculateCourseAttendanceSummary(
    course: Course,
    allSessions: List<AttendanceSession>
): CourseAttendanceSummary {
    val now = System.currentTimeMillis()

    val courseSessions = allSessions.filter {
        it.courseId == course.courseId && it.sessionDateTimeMillis <= now
    }

    val attendedCount = courseSessions.count { it.isAttended }
    val totalSessions = courseSessions.size

    val percentage = if (totalSessions == 0) {
        0
    } else {
        ((attendedCount * 100.0) / totalSessions.toDouble()).roundToInt()
    }

    val isBelowThreshold = percentage < course.requiredAttendanceThreshold

    return CourseAttendanceSummary(
        course = course,
        attendedCount = attendedCount,
        totalSessions = totalSessions,
        attendancePercentage = percentage,
        isBelowThreshold = isBelowThreshold
    )
}