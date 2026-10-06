package com.joecode.brokemon.data.model

/**
 * The user's own card ("catch yourself first"). Lives in DataStore as JSON,
 * not in the bros table, so it never counts as a catch or shows up in the dex.
 */
data class Trainer(
    val name: String,
    val look: BroLook = BroLook(),
    val type1: String = BroType.MAIN_CHARACTER.name,
    val motto: String = "",
    val frame: String = TrainerFrame.BASIC.name,
    val createdAt: Long = 0,
) {
    val type: BroType get() = BroType.from(type1) ?: BroType.MAIN_CHARACTER
    val resolvedFrame: TrainerFrame get() = TrainerFrame.entries.firstOrNull { it.name == frame } ?: TrainerFrame.BASIC

    /** Gson skips Kotlin defaults, so anything missing from older JSON comes back null. */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    fun sanitized(): Trainer = copy(
        name = (name ?: "Trainer").take(MAX_NAME),
        look = look ?: BroLook(),
        type1 = type1 ?: BroType.MAIN_CHARACTER.name,
        motto = (motto ?: "").take(Bro.MAX_FLAVOR),
        frame = frame ?: TrainerFrame.BASIC.name,
    )

    companion object {
        const val MAX_NAME = 16
    }
}

/** Card frames for the Trainer Card. Every frame but BASIC is a Journal reward. */
enum class TrainerFrame(val label: String, val colors: List<Long>, val holo: Boolean = false) {
    BASIC("Classic", listOf(0xFFD7263D, 0xFF8C1427)),
    BRONZE("Bronze", listOf(0xFFE0A06A, 0xFF8A5A2B, 0xFFE0A06A)),
    SILVER("Silver", listOf(0xFFF2F4F8, 0xFF8E96A6, 0xFFF2F4F8)),
    GOLD("Gold holo", listOf(0xFFFFE07A, 0xFFFF9E3B, 0xFFFFF3B0, 0xFFC99A1E), holo = true),
}
