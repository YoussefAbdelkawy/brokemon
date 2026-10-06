package com.joecode.brokemon.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CATCH_BRO = "catch"
    const val BRO_DETAIL = "bro/{broId}"
    const val SHARE_BRO = "bro/{broId}/share"
    const val ROOM = "bro/{broId}/room"
    const val SQUADS = "squads"
    const val SQUAD_DETAIL = "squad/{squadId}"
    const val TRADE = "trade"
    const val TRADE_SCAN = "trade/scan"
    const val CHECK_ON_BRO = "check-on-bro"
    const val WRAPPED = "wrapped"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
    const val TRAINER = "trainer"
    const val TRAINER_EDIT = "trainer/edit"
    const val JOURNAL = "journal"
    const val WILD = "wild"
    const val BATTLE_HUB = "battle"
    const val BATTLE_PLAY = "battle/play/{kind}?tournamentId={tournamentId}&round={round}&index={index}"
    const val BATTLE_REPLAY = "battle/replay/{replayId}"
    const val BATTLE_RECORDS = "battle/records"
    const val TOURNAMENTS = "tournaments"
    const val TOURNAMENT = "tournament/{tournamentId}"

    fun battle(kind: String, tournamentId: Long = 0, round: Int = -1, index: Int = -1) =
        "battle/play/$kind?tournamentId=$tournamentId&round=$round&index=$index"
    fun replay(id: Long) = "battle/replay/$id"
    fun tournament(id: Long) = "tournament/$id"

    const val ARG_BRO_ID = "broId"
    const val ARG_SQUAD_ID = "squadId"

    fun broDetail(id: Long) = "bro/$id"
    fun shareBro(id: Long) = "bro/$id/share"
    fun room(id: Long) = "bro/$id/room"
    fun squadDetail(id: Long) = "squad/$id"
}
