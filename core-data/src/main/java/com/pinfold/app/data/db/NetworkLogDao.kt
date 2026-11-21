package com.pinfold.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pinfold.app.data.model.NetworkLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkLogDao {
    @Query("SELECT * FROM network_log ORDER BY time DESC LIMIT 200")
    fun observeRecent(): Flow<List<NetworkLogEntry>>

    @Insert
    suspend fun insert(entry: NetworkLogEntry)

    @Query("DELETE FROM network_log WHERE time < :cutoff")
    suspend fun prune(cutoff: Long)
}
