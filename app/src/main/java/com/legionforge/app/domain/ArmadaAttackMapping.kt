package com.legionforge.app.domain

/** Les dés absents valent zéro ; les valeurs négatives sont rejetées. */
object ArmadaAttackMapping {
    /** shipStats des escadrons : [bleu, rouge, noir]. */
    fun squadronDice(values: List<Int>): ArmadaRangeDice = ArmadaRangeDice(
        blue = values.getOrElse(0) { 0 },
        red = values.getOrElse(1) { 0 },
        black = values.getOrElse(2) { 0 },
    )

    /** shipStats.attack des vaisseaux : [rouge, bleu, noir]. */
    fun shipDice(values: List<Int>): ArmadaRangeDice = ArmadaRangeDice(
        red = values.getOrElse(0) { 0 },
        blue = values.getOrElse(1) { 0 },
        black = values.getOrElse(2) { 0 },
    )

    /** Sélectionne la batterie de la zone déclarée, sans déduire ni valider l'arc.
     * Retourne null si cette zone est absente, jamais la batterie d'une autre zone.
     * Les clés sont celles du JSON : front, right, left, rear, right_aux, left_aux.
     */
    fun shipBattery(attack: Map<String, List<Int>>, sector: ShipSector): ArmadaRangeDice? =
        attack[sector.key]?.let(::shipDice)

    /** Cap en degrés dans [0, 360[, proue en haut et sens horaire positif. */
    fun normalizeHeading(headingDeg: Double): Double {
        require(headingDeg.isFinite())
        val remainder = headingDeg % 360.0
        val normalized = if (remainder < 0.0) remainder + 360.0 else remainder
        return if (normalized == 0.0 || normalized >= 360.0) 0.0 else normalized
    }
}

/** Zone déclarée par le joueur, y compris les zones auxiliaires des vaisseaux énormes. */
enum class ShipSector(val key: String) {
    FRONT("front"), RIGHT("right"), LEFT("left"), REAR("rear"),
    RIGHT_AUX("right_aux"), LEFT_AUX("left_aux"),
}
