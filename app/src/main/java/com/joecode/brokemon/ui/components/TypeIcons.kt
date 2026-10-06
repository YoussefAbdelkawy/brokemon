package com.joecode.brokemon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.ui.graphics.vector.ImageVector
import com.joecode.brokemon.data.model.BroType

/** An icon per type, so types don't rely on color alone (accessibility). */
val BroType?.icon: ImageVector
    get() = when (this) {
        BroType.ROAD_RAGER -> Icons.Filled.DirectionsCar
        BroType.BAD_DRIVER -> Icons.Filled.CarCrash
        BroType.YAPPER -> Icons.Filled.RecordVoiceOver
        BroType.GHOST -> Icons.Filled.VisibilityOff
        BroType.GYM_RAT -> Icons.Filled.FitnessCenter
        BroType.FOODIE -> Icons.Filled.Restaurant
        BroType.GAMER -> Icons.Filled.SportsEsports
        BroType.NERD -> Icons.Filled.Science
        BroType.SPORTS_FAN -> Icons.Filled.SportsSoccer
        BroType.PARTY_ANIMAL -> Icons.Filled.Celebration
        BroType.CHILL_GUY -> Icons.Filled.SelfImprovement
        BroType.CHAOS_AGENT -> Icons.Filled.Bolt
        BroType.MAIN_CHARACTER -> Icons.Filled.Star
        BroType.ALWAYS_LATE -> Icons.Filled.Schedule
        BroType.CRYPTO_BRO -> Icons.Filled.CurrencyBitcoin
        BroType.OUTDOORSY -> Icons.Filled.Terrain
        BroType.WINGMAN -> Icons.Filled.Favorite
        null -> Icons.Filled.WavingHand
    }
