package ca.unb.attendify.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ca.unb.attendify.model.AttendanceSession

//Our data structure of the attendance sessions
@Dao
interface AttendanceSessionDao {

    @Query(
        "SELECT * FROM attendance_sessions " +
                "WHERE courseId = :courseId " +
                "ORDER BY sessionDateTimeMillis"
    )
    suspend fun getSessionsForCourse(courseId: String): List<AttendanceSession>

    @Query(
        "SELECT * FROM attendance_sessions " +
                "WHERE courseId = :courseId AND " +
                "sessionDateTimeMillis BETWEEN :fromMillis AND :toMillis " +
                "ORDER BY sessionDateTimeMillis"
    )
    suspend fun getSessionsForCourseBetween(
        courseId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<AttendanceSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: AttendanceSession)

    @Update
    suspend fun updateSession(session: AttendanceSession)

    @Delete
    suspend fun deleteSession(session: AttendanceSession)
}