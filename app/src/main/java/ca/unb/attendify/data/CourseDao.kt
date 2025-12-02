package ca.unb.attendify.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ca.unb.attendify.model.Course

//Our courses data structure
@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY courseCode")
    suspend fun getAllCourses(): List<Course>

    @Query("SELECT * FROM courses WHERE courseId = :id LIMIT 1")
    suspend fun getCourseById(id: String): Course?

    @Query("SELECT * FROM courses WHERE courseCode = :courseCode LIMIT 1")
    suspend fun getCourseByCode(courseCode: String): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)
}