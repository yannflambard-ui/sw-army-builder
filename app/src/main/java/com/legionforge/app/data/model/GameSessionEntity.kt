package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Partie sauvegardée (solo). Une ligne par liste : le snapshot JSON du mode play.
 * `snapshot` = sérialisation Gson de [PlaySessionSnapshot].
 */
@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val listId: String,
    val gameSystem: String,
    val snapshot: String,
    val updatedAt: Long
)
