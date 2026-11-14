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
        appCtx = ctx.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            report(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private var appCtx: Context? = null
    private const val FILE = "last_crash.txt"

    /** Texte du dernier crash (affiché au redémarrage pour copie). */
    fun lastCrash(ctx: Context): String? =
        ctx.getFileStreamPath(FILE).takeIf { it.exists() }?.readText()

    fun clearCrash(ctx: Context) { ctx.getFileStreamPath(FILE).delete() }

    private fun report(error: Throwable) {
        val msg = error.message?.take(120) ?: error.javaClass.simpleName
        val full = "Crash: $msg\n${error.stackTraceToString()}"
        Log.e(TAG, full)
        // Écriture locale : survit au crash, relue au prochain lancement
        appCtx?.let { c -> try { c.openFileOutput(FILE, Context.MODE_PRIVATE).use { it.write(full.toByteArray()) } } catch (_: Exception) {} }
    }

    /**
     * Journal local d'un événement de diagnostic (ex: "Catalogue vide").
     * Anciennement posté en issue GitHub via un token embarqué — retiré pour sécurité.
     */
    fun reportEvent(title: String, body: String) {
        Log.w(TAG, "$title — $body")
    }
}
