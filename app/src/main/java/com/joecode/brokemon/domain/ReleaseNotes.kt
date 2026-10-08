package com.joecode.brokemon.domain

/** One release's "What's new" popup. Keep entries short and friendly. */
data class ReleaseNote(val versionCode: Int, val versionName: String, val headline: String, val bullets: List<String>)

object ReleaseNotes {
    val all: List<ReleaseNote> = listOf(
        ReleaseNote(
            4, "1.3.0", "Brodex glow-up",
            listOf(
                "New bottom bar: Brodex, Battle, Trainer and a big CATCH button.",
                "Catching is now 5 quick steps, or a Quick Catch with just a name.",
                "Meet Dexy, your pocket guide. Tap them for tips and jokes.",
                "Daily Pack: one free sticker, frame, backdrop or hat a day. No streaks.",
                "Cards come alive: blinking bros, type particles, shiny Rare, Epic and Legendary frames.",
                "Trainer Room, group photos, and swipe between cards on a bro's page.",
                "Hold a card for quick actions. Search forgives typos.",
                "Sounds, music and haptics, each with a switch in Settings.",
            ),
        ),
    )

    /** Notes for exactly this version, or null when there is nothing to show. */
    fun forVersion(code: Int): ReleaseNote? = all.firstOrNull { it.versionCode == code }

    /**
     * Whether to show What's new at startup. Fresh installs never see it (they get the intro instead);
     * upgraders see it once per version.
     */
    fun shouldShow(onboardingDone: Boolean, lastSeen: Int?, current: Int): Boolean =
        onboardingDone && (lastSeen ?: 0) < current && forVersion(current) != null
}
