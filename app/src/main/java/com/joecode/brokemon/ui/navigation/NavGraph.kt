package com.joecode.brokemon.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.joecode.brokemon.ui.room.RoomScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joecode.brokemon.ui.catchbro.CatchBroScreen
import com.joecode.brokemon.ui.detail.BroDetailScreen
import com.joecode.brokemon.ui.engage.CheckOnBroScreen
import com.joecode.brokemon.ui.engage.WrappedScreen
import com.joecode.brokemon.ui.home.HomeScreen
import com.joecode.brokemon.ui.onboarding.OnboardingScreen
import com.joecode.brokemon.ui.settings.LicensesScreen
import com.joecode.brokemon.ui.settings.PrivacyPolicyScreen
import com.joecode.brokemon.ui.settings.SettingsScreen
import com.joecode.brokemon.ui.squads.SquadDetailScreen
import com.joecode.brokemon.ui.squads.SquadsScreen
import com.joecode.brokemon.ui.trade.ShareBroScreen
import com.joecode.brokemon.ui.trade.TradeScreen
import com.joecode.brokemon.domain.Quest
import com.joecode.brokemon.ui.trainer.JournalScreen
import com.joecode.brokemon.ui.trainer.TrainerEditScreen
import com.joecode.brokemon.ui.trainer.TrainerScreen
import com.joecode.brokemon.ui.wild.WildBroScreen
import com.joecode.brokemon.ui.battle.BattleHubScreen
import com.joecode.brokemon.ui.battle.BattleRecordsScreen
import com.joecode.brokemon.ui.battle.BattleScreen
import com.joecode.brokemon.ui.battle.TournamentScreen
import com.joecode.brokemon.ui.battle.TournamentsScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BrokemonApp(
    showOnboarding: Boolean,
    onOnboardingDone: () -> Unit,
    openBroId: Long? = null,
    onOpenHandled: () -> Unit = {},
    /** A route to open from an app shortcut (catch, scan). */
    openRoute: String? = null,
    onRouteHandled: () -> Unit = {},
) {
    val nav = rememberNavController()

    // Deep link from the widget or a notification: jump straight to that card.
    LaunchedEffect(openBroId) {
        if (openBroId == null) return@LaunchedEffect
        if (nav.currentDestination?.route != Routes.ONBOARDING) {
            nav.navigate(Routes.broDetail(openBroId)) { launchSingleTop = true }
        }
        onOpenHandled()
    }
    LaunchedEffect(openRoute) {
        if (openRoute == null) return@LaunchedEffect
        if (nav.currentDestination?.route != Routes.ONBOARDING) {
            nav.navigate(openRoute) { launchSingleTop = true }
        }
        onRouteHandled()
    }
    val back: () -> Unit = { nav.popBackStack() }
    val duration = 280

    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
    NavHost(
        navController = nav,
        startDestination = if (showOnboarding) Routes.ONBOARDING else Routes.HOME,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(duration)) + fadeIn(tween(duration))
        },
        exitTransition = { fadeOut(tween(duration / 2)) },
        popEnterTransition = { fadeIn(tween(duration)) },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(duration)) + fadeOut(tween(duration))
        },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                onOnboardingDone()
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            CompositionLocalProvider(LocalNavAnimatedScope provides this) {
            HomeScreen(
                onCatch = { nav.navigate(Routes.CATCH_BRO) },
                onBroClick = { nav.navigate(Routes.broDetail(it)) },
                onSquads = { nav.navigate(Routes.SQUADS) },
                onTrade = { nav.navigate(Routes.TRADE) },
                onCheckOnBro = { nav.navigate(Routes.CHECK_ON_BRO) },
                onWrapped = { nav.navigate(Routes.WRAPPED) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onEnterRoom = { nav.navigate(Routes.room(it)) },
                onTrainer = { nav.navigate(Routes.TRAINER) },
                onJournal = { nav.navigate(Routes.JOURNAL) },
                onWild = { nav.navigate(Routes.WILD) { launchSingleTop = true } },
                onBattle = { nav.navigate(Routes.BATTLE_HUB) },
            )
            }
        }
        composable(Routes.BATTLE_HUB) {
            BattleHubScreen(
                onBack = back,
                onPlay = { nav.navigate(Routes.battle(it.name)) },
                onTournaments = { nav.navigate(Routes.TOURNAMENTS) },
                onRecords = { nav.navigate(Routes.BATTLE_RECORDS) },
            )
        }
        composable(
            Routes.BATTLE_PLAY,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("tournamentId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("round") { type = NavType.IntType; defaultValue = -1 },
                navArgument("index") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { BattleScreen(onBack = back) }
        composable(
            Routes.BATTLE_REPLAY,
            arguments = listOf(
                navArgument("replayId") { type = NavType.LongType },
                navArgument("kind") { type = NavType.StringType; defaultValue = "REPLAY" },
            ),
        ) { BattleScreen(onBack = back) }
        composable(Routes.BATTLE_RECORDS) {
            BattleRecordsScreen(onBack = back, onReplay = { nav.navigate(Routes.replay(it)) })
        }
        composable(Routes.TOURNAMENTS) {
            TournamentsScreen(onBack = back, onOpen = { nav.navigate(Routes.tournament(it)) })
        }
        composable(
            Routes.TOURNAMENT,
            arguments = listOf(navArgument("tournamentId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("tournamentId") ?: 0L
            TournamentScreen(
                onBack = back,
                onPlayHere = { m -> nav.navigate(Routes.battle("LOCAL", id, m.round, m.index)) },
                onPlayNearby = { m -> nav.navigate(Routes.battle("HOST", id, m.round, m.index)) },
            )
        }
        composable(Routes.TRAINER) {
            TrainerScreen(
                onBack = back,
                onEdit = { nav.navigate(Routes.TRAINER_EDIT) },
                onJournal = { nav.navigate(Routes.JOURNAL) },
            )
        }
        composable(Routes.TRAINER_EDIT) { TrainerEditScreen(onBack = back) }
        composable(Routes.JOURNAL) {
            JournalScreen(
                onBack = back,
                onGo = { quest ->
                    when (quest) {
                        Quest.MAKE_TRAINER_CARD -> nav.navigate(Routes.TRAINER_EDIT)
                        Quest.CATCH_FIRST_BRO -> nav.navigate(Routes.CATCH_BRO)
                        Quest.TRADE_QR -> nav.navigate(Routes.TRADE)
                        Quest.CHECK_IN -> nav.navigate(Routes.CHECK_ON_BRO)
                        // Facts and memories live on a card: back to the dex to pick one.
                        Quest.ADD_FACT, Quest.LOG_MEMORY -> nav.popBackStack(Routes.HOME, inclusive = false)
                    }
                },
            )
        }
        composable(
            Routes.WILD,
            enterTransition = { fadeIn(tween(120)) },
            popExitTransition = { fadeOut(tween(200)) },
        ) {
            WildBroScreen(
                onBack = back,
                onOpenBro = { nav.navigate(Routes.broDetail(it)) { popUpTo(Routes.HOME) } },
                onCatch = { nav.navigate(Routes.CATCH_BRO) { popUpTo(Routes.HOME) } },
            )
        }
        composable(
            Routes.ROOM,
            arguments = listOf(navArgument(Routes.ARG_BRO_ID) { type = NavType.LongType }),
            // The shared card bounds carry this transition; keep the screen fades simple.
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(250)) },
        ) { entry ->
            CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                RoomScreen(
                    broId = entry.arguments?.getLong(Routes.ARG_BRO_ID) ?: 0L,
                    onBack = back,
                    onOpenCard = { nav.navigate(Routes.broDetail(it)) },
                )
            }
        }
        composable(Routes.CATCH_BRO) {
            CatchBroScreen(
                onBack = back,
                onViewBro = { id ->
                    nav.navigate(Routes.broDetail(id)) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(
            Routes.BRO_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_BRO_ID) { type = NavType.LongType }),
        ) {
            BroDetailScreen(
                onBack = back,
                onShare = { nav.navigate(Routes.shareBro(it)) },
                onVisitRoom = { nav.navigate(Routes.room(it)) },
            )
        }
        composable(
            Routes.SHARE_BRO,
            arguments = listOf(navArgument(Routes.ARG_BRO_ID) { type = NavType.LongType }),
        ) { entry ->
            ShareBroScreen(broId = entry.arguments?.getLong(Routes.ARG_BRO_ID) ?: 0L, onBack = back)
        }
        composable(Routes.SQUADS) {
            SquadsScreen(onBack = back, onSquadClick = { nav.navigate(Routes.squadDetail(it)) })
        }
        composable(
            Routes.SQUAD_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SQUAD_ID) { type = NavType.LongType }),
        ) {
            SquadDetailScreen(onBack = back, onBroClick = { nav.navigate(Routes.broDetail(it)) })
        }
        composable(Routes.TRADE) {
            TradeScreen(
                onBack = back,
                onShareBro = { nav.navigate(Routes.shareBro(it)) },
                onOpenBro = { nav.navigate(Routes.broDetail(it)) { popUpTo(Routes.HOME) } },
            )
        }
        composable(Routes.TRADE_SCAN) {
            TradeScreen(
                onBack = back,
                onShareBro = { nav.navigate(Routes.shareBro(it)) },
                onOpenBro = { nav.navigate(Routes.broDetail(it)) { popUpTo(Routes.HOME) } },
                autoScan = true,
            )
        }
        composable(Routes.CHECK_ON_BRO) {
            CheckOnBroScreen(onBack = back, onOpenBro = { nav.navigate(Routes.broDetail(it)) })
        }
        composable(Routes.WRAPPED) {
            WrappedScreen(onBack = back)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onPrivacy = { nav.navigate(Routes.PRIVACY) },
                onLicenses = { nav.navigate(Routes.LICENSES) },
            )
        }
        composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = back) }
        composable(Routes.LICENSES) { LicensesScreen(onBack = back) }
    }
    }
    }
}
