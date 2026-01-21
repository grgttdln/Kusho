package com.example.app.data.repository

import com.example.app.data.dao.ActivityDao
import com.example.app.data.entity.Activity
import kotlinx.coroutines.flow.Flow

class ActivityRepository(
    private val activityDao: ActivityDao
) {

    /**
     * Create a new activity for a user
     * @return Result with activity ID or error message
     */
    suspend fun addActivity(
        userId: Long,
        title: String,
        description: String? = null,
        coverImagePath: String? = null
    ): Result<Long> {
        return try {
            // Validate inputs
            if (title.isBlank()) {
                return Result.failure(Exception("Activity title cannot be empty"))
            }

            if (title.trim().length > 30) {
                return Result.failure(Exception("Activity title must be 30 characters or less"))
            }

            // Check if activity with same title already exists
            if (activityDao.activityTitleExistsForUser(userId, title)) {
                return Result.failure(Exception("Activity with this title already exists"))
            }

            val activity = Activity(
                userId = userId,
                title = title.trim(),
                description = description?.trim(),
                coverImagePath = coverImagePath
            )

            val activityId = activityDao.insertActivity(activity)
            Result.success(activityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all activities for a specific user (reactive - Flow)
     */
    fun getActivitiesForUser(userId: Long): Flow<List<Activity>> {
        return activityDao.getActivitiesByUserId(userId)
    }

    /**
     * Get all activities for a user (one-time fetch)
     */
    suspend fun getActivitiesForUserOnce(userId: Long): Result<List<Activity>> {
        return try {
            val activities = activityDao.getActivitiesByUserIdOnce(userId)
            Result.success(activities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific activity by ID
     */
    suspend fun getActivityById(activityId: Long): Result<Activity> {
        return try {
            val activity = activityDao.getActivityById(activityId)
            if (activity != null) {
                Result.success(activity)
            } else {
                Result.failure(Exception("Activity not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update activity details
     */
    suspend fun updateActivity(
        activityId: Long,
        title: String? = null,
        description: String? = null,
        coverImagePath: String? = null
    ): Result<Unit> {
        return try {
            if (title.isNullOrBlank() && description == null && coverImagePath == null) {
                return Result.failure(Exception("No updates provided"))
            }

            val activity = activityDao.getActivityById(activityId)
                ?: return Result.failure(Exception("Activity not found"))

            // Check for duplicate title if title is being updated
            if (!title.isNullOrBlank()) {
                if (title.trim().length > 30) {
                    return Result.failure(Exception("Activity title must be 30 characters or less"))
                }
                if (title.trim() != activity.title) {
                    if (activityDao.activityTitleExistsForUserExcluding(activity.userId, title.trim(), activityId)) {
                        return Result.failure(Exception("Activity with this title already exists"))
                    }
                }
            }

            val updatedActivity = activity.copy(
                title = title?.trim() ?: activity.title,
                description = description?.trim() ?: activity.description,
                coverImagePath = coverImagePath ?: activity.coverImagePath,
                updatedAt = System.currentTimeMillis()
            )

            activityDao.updateActivity(updatedActivity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an activity
     */
    suspend fun deleteActivity(activityId: Long): Result<Unit> {
        return try {
            val rowsDeleted = activityDao.deleteActivityById(activityId)
            if (rowsDeleted > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Activity not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get activity count for a user
     */
    suspend fun getActivityCount(userId: Long): Result<Int> = try {
        val count = activityDao.getActivityCountForUser(userId)
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
