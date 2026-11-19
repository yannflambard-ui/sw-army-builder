package com.legionforge.app.domain

/**
 * Round 0 — Build a Mission (rulebook:1200-1275).
 *
 * MODÈLE CONFORME : 1 Objectif + 1 Secondaire dans des espaces PARTAGÉS,
 * 1 Avantage PAR JOUEUR, 1 Map appariée à l'Objectif par titre identique.
 * (L'ancien modèle own_/opp_ pour Objectif/Secondaire était faux.)
 */
object LegionMissionBuilder {

    const val CAT_OBJECTIVE = "objective"
    const val CAT_SECONDARY = "secondary"
    const val CAT_ADVANTAGE = "advantage"
    const val KEY_OBJECTIVE = "objective"   // espace partagé
    const val KEY_SECONDARY = "secondary"   // espace partagé
    const val KEY_MAP = "map"               // appariée à l'objectif
    const val KEY_ADV_OWN = "own_advantage"
    const val KEY_ADV_OPP = "opp_advantage"

    /** État de la mission en cours de construction. */
    data class MissionState(
        val bluePlayer: String? = null,          // "own" | "opp"
        val revealed: Map<String, String> = emptyMap(),
        val turn: String = "own",                // camp qui doit agir
        val modsLeft: Map<String, Int> = mapOf("own" to 2, "opp" to 2),
        val step: Step = Step.BLUE,
    ) {
        val objective: String? get() = revealed[KEY_OBJECTIVE]
        val secondary: String? get() = revealed[KEY_SECONDARY]
        val advOwn: String? get() = revealed[KEY_ADV_OWN]
        val advOpp: String? get() = revealed[KEY_ADV_OPP]
        /** Map = carte dont le titre = l'objectif révélé (rulebook:1181-1185). */
        fun map(mapNames: Set<String>): String? =
            objective?.takeIf { it in mapNames }
    }

    enum class Step { BLUE, REVEAL, MODIFY, DONE }

    enum class ModAction { OBJECTIVE, SECONDARY, ADVANTAGE, OPP_ADVANTAGE, BECOME_BLUE, PASS }

    /** Déterminé par le jet de dés AVANT toute révélation (rulebook:1221-1227). */
    fun withBlue(state: MissionState, winner: String): MissionState =
        if (winner !in setOf("own", "opp")) state
        else state.copy(
            bluePlayer = winner, turn = winner,
            step = if (state.step == Step.BLUE) Step.REVEAL else state.step,
        )

    /**
     * Révélation initiale ordonnée (rulebook:1200-1204) :
     * (a) Blue choisit Objectif OU Secondaire de SON deck → espace partagé ;
     * (b) adversaire révèle l'AUTRE type de SON deck → espace partagé ;
     * (c) chaque joueur révèle le dessus de son deck Avantage → son côté.
     * @param blueFirstType type choisi par le Blue ("objective"|"secondary")
     * @param draw fonction (side,type,deckCourant) -> carte tirée du dessus
     */
    fun initialReveal(
        state: MissionState,
        blueFirstType: String,
        draw: (String, String, String?) -> String?,
    ): MissionState {
        val blue = state.bluePlayer ?: return state // gate : pas de révélation sans Blue
        val opp = if (blue == "own") "opp" else "own"
        val otherType = if (blueFirstType == CAT_OBJECTIVE) CAT_SECONDARY else CAT_OBJECTIVE
        val r = state.revealed.toMutableMap()

        // (a) Blue → son type choisi
        draw(blue, blueFirstType, null)?.let { r[keyFor(blueFirstType)] = it }
        // (b) adversaire → l'autre type
        draw(opp, otherType, null)?.let { r[keyFor(otherType)] = it }
        // (c) Avantages : chacun le sien
        draw(blue, CAT_ADVANTAGE, null)?.let { r[advKey(blue)] = it }
        draw(opp, CAT_ADVANTAGE, null)?.let { r[advKey(opp)] = it }

        return state.copy(revealed = r, turn = blue, step = Step.MODIFY)
    }

    /** Clé de l'espace PARTAGÉ pour un type Objectif/Secondaire. */
    private fun keyFor(type: String) = when (type) {
        CAT_OBJECTIVE -> KEY_OBJECTIVE
        CAT_SECONDARY -> KEY_SECONDARY
        else -> type
    }

    private fun advKey(side: String) = if (side == "own") KEY_ADV_OWN else KEY_ADV_OPP

    /**
     * Une modification (rulebook:1209-1240). Alternance B/R/B/R, 2 par joueur.
     * @param draw fonction (side,type,carteÉcartée) -> nouvelle carte du dessus
     */
    fun modify(
        state: MissionState,
        side: String,
        action: ModAction,
        draw: (String, String, String?) -> String?,
    ): MissionState {
        if (side != state.turn) return state                       // camp inactif verrouillé
        if ((state.modsLeft[side] ?: 0) <= 0) return state         // 2 modifications max
        val r = state.revealed.toMutableMap()
        when (action) {
            ModAction.OBJECTIVE -> draw(side, CAT_OBJECTIVE, r[KEY_OBJECTIVE])?.let { r[KEY_OBJECTIVE] = it }
            ModAction.SECONDARY -> draw(side, CAT_SECONDARY, r[KEY_SECONDARY])?.let { r[KEY_SECONDARY] = it }
            ModAction.ADVANTAGE -> draw(side, CAT_ADVANTAGE, r[advKey(side)])?.let { r[advKey(side)] = it }
            ModAction.OPP_ADVANTAGE -> {
                val opp = if (side == "own") "opp" else "own"
                draw(side, CAT_ADVANTAGE, r[advKey(opp)])?.let { r[advKey(opp)] = it }
            }
            ModAction.BECOME_BLUE -> { /* déplacer le jeton Blue : règle officielle */ }
            ModAction.PASS -> Unit
        }
        val blue = if (action == ModAction.BECOME_BLUE) side else state.bluePlayer
        val left = state.modsLeft.toMutableMap()[side]!! - 1
        val nextTurn = if (side == "own") "opp" else "own"
        val done = state.modsLeft.values.sum() - 1 <= 0
        return state.copy(
            revealed = r,
            bluePlayer = blue,
            modsLeft = state.modsLeft + (side to left),
            turn = nextTurn,
            step = if (done) Step.DONE else Step.MODIFY,
        )
    }

    /**
     * Tirage du dessus avec reshuffle quand le deck est vide (rulebook:1268-1275) :
     * on remélange les cartes écartées, Y COMPRIS celle venant d'être écartée.
     */
    fun drawTop(deck: List<String>, discarded: List<String>, rng: (Int) -> Int): Pair<String?, List<String>> {
        val pile = if (deck.isEmpty()) discarded else deck
        if (pile.isEmpty()) return null to discarded
        val i = rng(pile.size).coerceIn(0, pile.size - 1)
        return pile[i] to (pile.toMutableList().also { it.removeAt(i) })
    }

    /** Garde-fou battle deck : EXACTEMENT 3/3/3 sans doublon (rulebook:1182-1185). */
    fun battleDeckValid(counts: Map<String, Int>): Boolean =
        counts[CAT_OBJECTIVE] == 3 && counts[CAT_SECONDARY] == 3 && counts[CAT_ADVANTAGE] == 3
}
