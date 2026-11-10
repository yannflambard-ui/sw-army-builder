package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.legionforge.app.data.model.PlayJournalEntity

@Dao
interface PlayJournalDao {
    @Insert
    suspend fun append(entry: PlayJournalEntity): Long

    @Query("SELECT * FROM play_journal WHERE listId = :listId AND acked = 0 ORDER BY seq ASC")
    suspend fun pending(listId: String): List<PlayJournalEntity>

    @Query("UPDATE play_journal SET acked = 1 WHERE listId = :listId AND seq <= :upToSeq")
    suspend fun ack(listId: String, upToSeq: Long)

    @Query("DELETE FROM play_journal WHERE listId = :listId")
    suspend fun clear(listId: String)
}
