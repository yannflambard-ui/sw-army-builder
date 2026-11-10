package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Journal append-only des mutations de partie 2 joueurs (prérequis sync).
 * Chaque changement d'état sortant est écrit ici avant envoi. À la reconnexion,
 * les entrées non acquittées (acked = false) sont rejouées dans l'ordre de `seq`.
 */
@Entity(tableName = "play_journal")
data class PlayJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: String,
    val seq: Long,
    val kind: String,        // "state" | "game" | "round" | "turn" | "roundcard"
    val payload: String,     // JSON du payload PlaySyncPayload
    val createdAt: Long,
    val acked: Boolean = false
)
