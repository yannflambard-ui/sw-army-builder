package com.legionforge.app.ui.screens
import com.legionforge.app.ui.theme.LegionColors

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.R
import com.legionforge.app.data.repository.CollectionRepository
import java.util.Locale

private data class LangOption(val code: String, val flag: String, val label: String)

private val languages = listOf(
    LangOption("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
    LangOption("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Fran\u00E7ais"),
    LangOption("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    LangOption("es", "\uD83C\uDDEA\uD83C\uDDF8", "Espa\u00F1ol")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onCollection: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("legionforge", Context.MODE_PRIVATE)
    var currentLang by remember { mutableStateOf(prefs.getString("locale", "en") ?: "en") }
    val activity = context as? android.app.Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.language),
                color = LegionColors.Accent,
                style = MaterialTheme.typography.titleMedium)

            languages.forEach { lang ->
                val selected = lang.code == currentLang
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentLang = lang.code
                            prefs.edit().putString("locale", lang.code).apply()
                            setAppLocale(context, lang.code)
                            activity?.recreate()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFFFFB800).copy(alpha = 0.2f) else Color(0xFF192331)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flag, fontSize = 28.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(lang.label, color = Color.White, fontSize = 18.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (selected) {
                            Text("\u2713", color = Color(0xFFFFB800), fontSize = 22.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.my_collection),
                color = LegionColors.Accent,
                style = MaterialTheme.typography.titleMedium)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCollection),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF192331))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.my_collection_desc),
                        color = Color.White, fontSize = 16.sp)
                    Text("\u276F", color = LegionColors.Accent, fontSize = 18.sp)
                }
            }

            // Case à cocher : n'afficher dans le catalogue que le matériel possédé.
            val collectionOnly by CollectionRepository.collectionOnly.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { CollectionRepository.setCollectionOnly(!collectionOnly) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF192331))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = collectionOnly,
                        onCheckedChange = { CollectionRepository.setCollectionOnly(it) },
                        colors = CheckboxDefaults.colors(checkedColor = LegionColors.Accent, checkmarkColor = Color.Black)
                    )
                    Text(stringResource(R.string.collection_only_catalog),
                        color = Color.White, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.more_options),
                color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun setAppLocale(context: Context, langCode: String) {
    val locale = Locale(langCode)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
