package com.legionforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.style.TextAlign
import com.legionforge.app.data.repository.CollectionRepository
import com.legionforge.app.ui.nav.LegionForgeNavHost
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.legionforge.app.util.CrashReporter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
CrashReporter.init(applicationContext)
        val lastCrashText = CrashReporter.lastCrash(applicationContext)
        CollectionRepository.init(applicationContext)
        com.legionforge.app.data.i18n.I18n.load(applicationContext)
        // Réapplique la langue sauvegardée par l'utilisateur (Settings) à chaque ouverture.
        val savedLang = getSharedPreferences("legionforge", MODE_PRIVATE).getString("locale", "en") ?: "en"
        com.legionforge.app.ui.screens.setAppLocale(applicationContext, savedLang)

        setContent {
            LegionForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (lastCrashText != null) {
                        CrashScreen(lastCrashText, onClear = {
                            CrashReporter.clearCrash(applicationContext)
                            recreate()
                        })
                    } else {
                        SafeApp()
                    }
                }
            }
        }
    }
}

/** Écran affiché après un plantage : texte complet + bouton Copier (presse-papiers, aucun réseau). */
@Composable
private fun CrashScreen(text: String, onClear: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Crash détecté", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF87171))
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(text)) }) { Text("Copier") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onClear) { Text("Effacer et continuer") }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(text, fontSize = 11.sp, color = Color(0xFFE5E7EB))
        }
    }
}

@Composable
private fun SafeApp() {
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            // Test basic init — will throw if DB/catalog is broken
            // ViewModel init is lazy, so we let it load naturally
        } catch (e: Exception) {
            hasError = true
            errorMsg = e.message ?: e.javaClass.simpleName
        }
    }

    if (hasError) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠", fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp))
            Spacer(Modifier.height(16.dp))
            Text("Une erreur est survenue au lancement.", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(errorMsg, color = Color.Gray, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { hasError = false }) { Text("Réessayer") }
        }
    } else {
        LegionForgeNavHost()
    }
}
