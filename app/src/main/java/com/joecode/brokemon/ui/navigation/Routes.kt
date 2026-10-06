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
    const val CHECK_ON_BRO = "check-on-bro"
    const val WRAPPED = "wrapped"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
    const val TRAINER = "trainer"
    const val TRAINER_EDIT = "trainer/edit"
    const val JOURNAL = "journal"
    const val WILD = "wild"

    const val ARG_BRO_ID = "broId"
    const val ARG_SQUAD_ID = "squadId"

    fun broDetail(id: Long) = "bro/$id"
    fun shareBro(id: Long) = "bro/$id/share"
    fun room(id: Long) = "bro/$id/room"
    fun squadDetail(id: Long) = "squad/$id"
}
