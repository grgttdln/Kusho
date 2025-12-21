package com.example.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app.data.dao.UserDao
import com.example.app.data.dao.WordDao
import com.example.app.data.entity.User
import com.example.app.data.entity.Word

/**
 * Room Database for the Kusho application.
 *
 * This is a singleton database that provides access to the UserDao and WordDao.
 * The database is created lazily when first accessed.
 */
@Database(
    entities = [User::class, Word::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun wordDao(): WordDao

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
                // Enable destructive migration for development
                // In production, you should use proper migrations
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

