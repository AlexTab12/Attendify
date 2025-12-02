package ca.unb.attendify.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * This class represents 1 class meeting for any course
 */
@Entity(
    tableName = "attendance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class AttendanceSession(
    @PrimaryKey
    val sessionId: String,
    val courseId: String,
    val sessionDateTimeMillis: Long,// We decided to have the time in epoch millis
    val isAttended: Boolean
)