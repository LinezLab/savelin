package com.pinfold.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pinfold.app.data.model.Collection
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name")
    fun observeAll(): Flow<List<Collection>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collection: Collection): Long

    @Update
    suspend fun update(collection: Collection)

    @Delete
    suspend fun delete(collection: Collection)
}
