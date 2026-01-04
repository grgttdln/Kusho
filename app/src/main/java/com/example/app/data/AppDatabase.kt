package com.example.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app.data.dao.ClassDao
import com.example.app.data.dao.EnrollmentDao
import com.example.app.data.dao.StudentDao
import com.example.app.data.dao.UserDao
import com.example.app.data.dao.WordDao
import com.example.app.data.entity.Class
import com.example.app.data.entity.Enrollment
import com.example.app.data.entity.Student
import com.example.app.data.entity.User
import com.example.app.data.entity.Word

/**
 * Room Database for the Kusho application.
 *
 * This is a singleton database that provides access to all DAOs.
 * The database is created lazily when first accessed.
 */
@Database(
    entities = [User::class, Word::class, Student::class, Class::class, Enrollment::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun wordDao(): WordDao
    abstract fun studentDao(): StudentDao
    abstract fun classDao(): ClassDao
    abstract fun enrollmentDao(): EnrollmentDao

    companion object {
        private const val DATABASE_NAME = "kusho_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get the singleton instance of the database.
         * Uses double-checked locking for thread safety.
         *
         * @param context Application context
         * @return The singleton AppDatabase instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Removed fallbackToDestructiveMigration to preserve data across app restarts
                // If you need to change the schema, increment the version number and add proper migrations
                .build()
        }
    }
}

