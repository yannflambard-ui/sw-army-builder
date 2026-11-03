package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.legionforge.app.data.model.GameSessionEntity

@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions WHERE listId = :listId LIMIT 1")
    suspend fun getByList(listId: String): GameSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE listId = :listId")
    suspend fun delete(listId: String)
}
