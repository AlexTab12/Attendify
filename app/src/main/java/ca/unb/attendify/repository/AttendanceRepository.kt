package ca.unb.attendify.repository

import android.content.Context
import ca.unb.attendify.data.AppDatabase
import ca.unb.attendify.model.AttendanceSession
import ca.unb.attendify.model.Course
import ca.unb.attendify.model.CourseAttendanceSummary
import ca.unb.attendify.model.calculateCourseAttendanceSummary
import java.util.UUID

/**
 * This class is to have a single source of truth for loading or saving attendance data.
 */
class AttendanceRepository private constructor(
    private val db: AppDatabase
) {

    private val courseDao = db.courseDao()
    private val sessionDao = db.attendanceSessionDao()

    // ---- COURSE OPERATIONS ----

    suspend fun getAllCourses(): List<Course> = courseDao.getAllCourses()

    suspend fun getCourseById(id: String): Course? = courseDao.getCourseById(id)

    suspend fun createCourse(
        courseCode: String,
        courseName: String,
        requiredThreshold: Int
    ): Course {
        val course = Course(
            courseId = UUID.randomUUID().toString(),
            courseCode = courseCode.trim(),
            courseName = courseName.trim(),
            requiredAttendanceThreshold = requiredThreshold
        )
        courseDao.upsertCourse(course)
        return course
    }

    suspend fun updateCourse(course: Course) {
        courseDao.upsertCourse(course)
    }

    suspend fun deleteCourse(course: Course) {
        courseDao.deleteCourse(course)
    }

    // The operations for our session logic

    suspend fun getSessionsForCourse(courseId: String): List<AttendanceSession> {
        return sessionDao.getSessionsForCourse(courseId)
    }

    suspend fun upsertSession(session: AttendanceSession) {
        sessionDao.upsertSession(session)
    }

    suspend fun deleteSession(session: AttendanceSession) {
        sessionDao.deleteSession(session)
    }

    /**
     * Returns all courses with the calculated attendance summary
     */
    suspend fun getCourseSummaries(): List<CourseAttendanceSummary> {
        val courses = courseDao.getAllCourses()
        val result = mutableListOf<CourseAttendanceSummary>()
        for (course in courses) {
            val sessions = sessionDao.getSessionsForCourse(course.courseId)
            result += calculateCourseAttendanceSummary(course, sessions)
        }
        return result
    }

    suspend fun getSessionsForCourseBetween(
        courseId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<AttendanceSession> {
        return sessionDao.getSessionsForCourseBetween(courseId, fromMillis, toMillis)
    }

    suspend fun createSession(
        courseId: String,
        timestampMillis: Long,
        isAttended: Boolean
    ): AttendanceSession {
        val session = AttendanceSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            courseId = courseId,
            sessionDateTimeMillis = timestampMillis,
            isAttended = isAttended
        )
        sessionDao.upsertSession(session)
        return session
    }

    suspend fun getCourseByCode(courseCode: String): Course? {
        return courseDao.getCourseByCode(courseCode)
    }

    companion object {
        @Volatile
        private var INSTANCE: AttendanceRepository? = null

        fun getInstance(context: Context): AttendanceRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = AttendanceRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}