package com.legionforge.app.data.i18n

import android.content.Context
import com.google.gson.Gson
import java.util.Locale

/** Couche de localisation à l'AFFICHAGE uniquement.
 *
 *  La base de données (catalog_cards, wiki_sections) reste 100% EN canonique :
 *  c'est elle qui alimente les filtres et la recherche plein texte. Ici on ne fait
 *  que traduire à l'affichage, avec fallback vers l'anglais si la traduction manque.
 *
 *  - Noms de cartes : gérés par `CardDefinition.names` + `displayName()` (déjà branché).
 *  - Mots-clés : dict `keywords_i18n.json` (EN -> {fr,de,es}).
 *  - Wiki : dict `wiki_i18n.json` (sectionId -> {fr,de,es} -> {title,content}).
 */
object I18n {
    private val gson = Gson()
    private var keywords: Map<String, Map<String, String>> = emptyMap()
    private var wiki: Map<String, Map<String, WikiLocalized>> = emptyMap()
    private var upgrades: Map<String, Map<String, String>> = emptyMap()

    /** À appeler une fois au démarrage (MainActivity.onCreate). */
    fun load(context: Context) {
        keywords = try {
            val json = context.assets.open("keywords_i18n.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, KeywordsI18nDocument::class.java)?.keywords ?: emptyMap()
        } catch (_: Exception) { emptyMap() }

        upgrades = try {
            val json = context.assets.open("armada_upgrades_i18n.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, UpgradesI18nDocument::class.java)?.upgrades?.mapValues { it.value } ?: emptyMap()
        } catch (_: Exception) { emptyMap() }

        wiki = try {
            val json = context.assets.open("wiki_i18n.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, WikiI18nDocument::class.java)?.sections ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    /** Traduit un mot-clé de jeu (ex "Relentless") pour la locale courante. */
    fun keyword(en: String, locale: String = Locale.getDefault().language): String =
        keywords[en]?.get(locale) ?: en

    /** Texte FR d'une amélioration Armada (par id de carte), ou [en] si absent. */
    fun upgradeText(cardId: String, en: String, locale: String = Locale.getDefault().language): String =
        if (locale == "en") en else upgrades[cardId]?.get(locale) ?: en

    /** Titre localisé d'une section wiki, ou null si absent (fallback EN). */
    fun wikiTitle(sectionId: String, locale: String = Locale.getDefault().language): String? =
        wiki[sectionId]?.get(locale)?.title

    /** Contenu localisé d'une section wiki, ou null si absent (fallback EN). */
    fun wikiContent(sectionId: String, locale: String = Locale.getDefault().language): String? =
        wiki[sectionId]?.get(locale)?.content
}

data class KeywordsI18nDocument(
    val schemaVersion: Int = 1,
    val keywords: Map<String, Map<String, String>> = emptyMap()
)

data class UpgradesI18nDocument(
    val schemaVersion: Int = 1,
    val upgrades: Map<String, Map<String, String>> = emptyMap()
)

data class WikiI18nDocument(
    val schemaVersion: Int = 1,
    val sections: Map<String, Map<String, WikiLocalized>> = emptyMap()
)

data class WikiLocalized(
    val title: String,
    val content: String
)
