package com.legionforge.app.util

import android.content.Context
import android.util.Log

/**
 * Intercepte les exceptions non capturées et les journal localement.
 * Plus AUCUN appel réseau, AUCUN token GitHub embarqué dans l'APK (sécurité / vie privée).
 */
object CrashReporter {
    private const val TAG = "SWArmyBuilder"

    fun init(ctx: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            report(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun report(error: Throwable) {
        val msg = error.message?.take(120) ?: error.javaClass.simpleName
        Log.e(TAG, "Crash: $msg\n${error.stackTraceToString()}")
    }

    /**
     * Journal local d'un événement de diagnostic (ex: "Catalogue vide").
     * Anciennement posté en issue GitHub via un token embarqué — retiré pour sécurité.
     */
    fun reportEvent(title: String, body: String) {
        Log.w(TAG, "$title — $body")
    }
}
