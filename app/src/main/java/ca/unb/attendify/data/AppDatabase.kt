package ca.unb.attendify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ca.unb.attendify.model.AttendanceSession
import ca.unb.attendify.model.Course

//this is to set up our database locally
@Database(
    entities = [Course::class, AttendanceSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun attendanceSessionDao(): AttendanceSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendify.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}