package com.legionforge.app.ui.screens
import com.legionforge.app.ui.theme.LegionColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.WikiSectionEntity
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

/** Wiki des règles officielles (Armada + Legion) : recherche par mot-clé sur la
 *  page de garde, affichage de chaque section. Le texte reprend les PDF officiels
 *  Atomic Mass Games. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(
    onBack: () -> Unit,
    vm: ArmyBuilderViewModel
) {
    var query by remember { mutableStateOf("") }
    val sections by vm.wikiSections.collectAsState()
    val results by vm.wikiSearchResults.collectAsState()
    val searching by vm.wikiSearching.collectAsState()
    var selected by remember { mutableStateOf<WikiSectionEntity?>(null) }

    LaunchedEffect(Unit) { vm.loadAllWiki() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("WIKI DES RÈGLES", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LegionColors.Background)
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(LegionColors.Background).padding(pad).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Barre de recherche par mot-clé
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.searchWiki(it) },
                placeholder = { Text("Mot-clé : boucliers, activation, attaque, vitesse…", color = LegionColors.TextTertiary, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WikiGameChip("LEGION", GameSystem.LEGION_V2, Modifier.weight(1f), vm)
                WikiGameChip("ARMADA", GameSystem.ARMADA_V15, Modifier.weight(1f), vm)
            }
            when {
                searching -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LegionColors.Accent, strokeWidth = 2.dp) }
                query.isNotBlank() && results.isEmpty() -> Text("Aucune section pour « $query »", color = LegionColors.TextTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                query.isNotBlank() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(results) { section -> WikiSectionRow(section, onClick = { selected = section }) }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(sections) { section -> WikiSectionRow(section, onClick = { selected = section }) }
                }
            }
        }
    }

    // Popup de section : affiche le texte officiel de la règle.
    selected?.let { section ->
        RulePopup(section = section, onClose = { selected = null })
    }
}

@Composable
private fun WikiGameChip(label: String, system: GameSystem, modifier: Modifier = Modifier, vm: ArmyBuilderViewModel) {
    var active by remember(system) { mutableStateOf(false) }
    FilterChip(
        selected = active,
        onClick = {
            active = !active
            if (active) vm.loadWiki(system) else vm.loadAllWiki()
        },
        modifier = modifier,
        label = { Text(label, fontSize = 12.sp) }
    )
}

private fun localizedTitle(section: WikiSectionEntity): String =
    com.legionforge.app.data.i18n.I18n.wikiTitle(section.id) ?: section.title

private fun localizedContent(section: WikiSectionEntity): String =
    com.legionforge.app.data.i18n.I18n.wikiContent(section.id) ?: section.content

@Composable
private fun WikiSectionRow(section: WikiSectionEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SurfaceRaised)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(localizedTitle(section), color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(if (section.gameSystem == GameSystem.LEGION_V2.name) "LEGION" else "ARMADA", color = LegionColors.Accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Text(localizedContent(section).replace('\n', ' ').take(90) + "…", color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Popup de règle : fenêtre superposée (même style que les filtres du catalogue)
 *  affichant le texte officiel d'une section du wiki. */
@Composable
fun RulePopup(section: WikiSectionEntity, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
            shape = RoundedCornerShape(18.dp),
            color = LegionColors.SurfaceRaised
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(localizedTitle(section), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (section.gameSystem == GameSystem.LEGION_V2.name) "LEGION V2" else "ARMADA V1.5", color = LegionColors.Accent, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("✕", color = Color.White, fontSize = 18.sp) }
                }
                HorizontalDivider(color = LegionColors.Border)
                Text(
                    localizedContent(section),
                    color = Color(0xFFD5DCE6),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/** Texte de règles avec mots-clés du wiki cliquables : clic sur un mot-clé
 *  (ex: "boucliers", "activation", "attaque") -> onRuleClick(section) pour
 *  afficher le point de règle officiel dans une popup superposée. */
// Icônes de mots-clés Armada (kw_*.webp) injectées inline AVANT le mot-clé,
// comme sur les vraies cartes. Map mot-clé (minuscule) -> asset.
private val KEYWORD_ICON_ASSETS = mapOf(
    "adept" to "icons/kw_adept.webp",
    "ai" to "icons/kw_ai.webp",
    "assault" to "icons/kw_assault.webp",
    "bomber" to "icons/kw_bomber.webp",
    "cloak" to "icons/kw_cloak.webp",
    "counter" to "icons/kw_counter.webp",
    "dodge" to "icons/kw_dodge.webp",
    "escort" to "icons/kw_escort.webp",
    "grit" to "icons/kw_grit.webp",
    "heavy" to "icons/kw_heavy.webp",
    "intel" to "icons/kw_intel.webp",
    "relay" to "icons/kw_relay.webp",
    "rogue" to "icons/kw_rogue.webp",
    "scout" to "icons/kw_scout.webp",
    "screen" to "icons/kw_screen.webp",
    "snipe" to "icons/kw_snipe.webp",
    "strategic" to "icons/kw_strategic.webp",
    "swarm" to "icons/kw_swarm.webp"
)

@Composable
fun ClickableRulesText(
    text: String,
    wikiSections: List<WikiSectionEntity>,
    onRuleClick: (WikiSectionEntity) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LegionColors.TextSecondary,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = Int.MAX_VALUE
) {
    // Construire une map mot-clé (minuscule) -> section pour la détection.
    val kwToSection = remember(wikiSections) {
        buildMap {
            wikiSections.forEach { s ->
                s.keywordList().forEach { kw ->
                    if (kw.length >= 3) put(kw.lowercase(), s)
                }
                // le titre lui-même est un mot-clé
                if (s.title.length >= 3) put(s.title.lowercase(), s)
            }
        }
    }
    // Charger les icônes de mots-clés présents dans le texte (ImageBitmap, pas AsyncImage).
    val context = LocalContext.current
    val iconBitmaps = remember(text) {
        val present = KEYWORD_ICON_ASSETS.keys.filter { text.lowercase().contains(it) }
        present.associateWith { kw ->
            try {
                context.assets.open(KEYWORD_ICON_ASSETS.getValue(kw)).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (_: Exception) { null }
        }.filterValues { it != null }
    }
    val inlineContent = remember(iconBitmaps) {
        buildMap {
            iconBitmaps.forEach { (kw, bmp) ->
                put(kw, InlineTextContent(Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)) {
                    Image(bitmap = bmp!!, contentDescription = null, modifier = Modifier.size(12.dp), contentScale = ContentScale.Fit)
                })
            }
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotated = buildAnnotatedString {
        // Découper le texte en tokens (mots + ponctuation) pour détecter les mots-clés.
        val regex = Regex("""[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ'\-]*""")
        var last = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val word = m.value
            val lower = word.lowercase()
            val section = kwToSection[lower]
            // Icône inline AVANT le mot-clé (règle user : comme sur les cartes).
            if (iconBitmaps.containsKey(lower)) {
                appendInlineContent(lower, word)
            }
            if (section != null) {
                pushStringAnnotation(tag = "rule", annotation = section.id)
                withStyle(SpanStyle(color = LegionColors.Accent, fontWeight = FontWeight.Bold)) {
                    append(word)
                }
                pop()
            } else {
                append(word)
            }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
    Text(
        text = annotated,
        style = style.copy(color = color),
        maxLines = maxLines,
        inlineContent = inlineContent,
        softWrap = true,
        overflow = TextOverflow.Clip,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures { pos ->
                val lr = layoutResult ?: return@detectTapGestures
                val offset = lr.getOffsetForPosition(pos)
                annotated.getStringAnnotations(tag = "rule", start = offset, end = offset)
                    .firstOrNull()?.let { ann ->
                        kwToSection.values.firstOrNull { it.id == ann.item }?.let(onRuleClick)
                    }
            }
        }
    )
}
