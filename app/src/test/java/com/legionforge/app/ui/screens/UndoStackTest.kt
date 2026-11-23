package com.legionforge.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    @Test
    fun undo_restaure_etat_avant() {
        val s = UndoStack<Int>()
        s.push("me", 1)
        s.push("me", 2)
        assertEquals(2, s.undo("me"))
        assertEquals(1, s.undo("me"))
        assertFalse(s.canUndo)
    }

    @Test
    fun ring_buffer_borne_a_10() {
        val s = UndoStack<Int>(capacity = 3)
        repeat(5) { s.push("me", it) }
        assertEquals(3, s.size)
        assertEquals(4, s.undo("me"))
        assertEquals(3, s.undo("me"))
        assertEquals(2, s.undo("me"))
        assertNull(s.undo("me"))
    }

    @Test
    fun garde_2j_refuse_annuler_action_adverse() {
        val s = UndoStack<Int>()
        s.push("me", 1)
        s.push("opp", 2)   // action adverse : bloque l'undo
        assertNull(s.undo("me"))
        assertEquals(2, s.undo("opp")) // l'adversaire peut annuler la sienne
        assertEquals(1, s.undo("me"))  // puis le joueur récupère la sienne
    }

    @Test
    fun clear_vide_la_pile() {
        val s = UndoStack<String>()
        s.push("me", "a")
        s.clear()
        assertFalse(s.canUndo)
        assertNull(s.undo("me"))
    }
}
