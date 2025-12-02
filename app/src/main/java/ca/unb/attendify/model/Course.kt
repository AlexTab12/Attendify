package ca.unb.attendify.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This class represents a course the student is actively tracking
 */
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey
    val courseId: String,                 // Unique ID (e.g., UUID)
    val courseCode: String,              // e.g., "CS 2063"
    val courseName: String,              // e.g., "App Development"
    val requiredAttendanceThreshold: Int // e.g., 80 (%)
)