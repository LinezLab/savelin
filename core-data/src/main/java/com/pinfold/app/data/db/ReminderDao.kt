package com.pinfold.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pinfold.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAt")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE status = :status AND triggerAt <= :now ORDER BY triggerAt")
    suspend fun due(status: String, now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE bookmarkId = :bookmarkId")
    suspend fun forBookmark(bookmarkId: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun findById(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
