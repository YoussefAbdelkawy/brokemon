package com.joecode.brokemon.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
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

@Composable
fun BrokemonApp(
    showOnboarding: Boolean,
    onOnboardingDone: () -> Unit,
    openBroId: Long? = null,
    onOpenHandled: () -> Unit = {},
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
    val back: () -> Unit = { nav.popBackStack() }
    val duration = 280

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
            HomeScreen(
                onCatch = { nav.navigate(Routes.CATCH_BRO) },
                onBroClick = { nav.navigate(Routes.broDetail(it)) },
                onSquads = { nav.navigate(Routes.SQUADS) },
                onTrade = { nav.navigate(Routes.TRADE) },
                onCheckOnBro = { nav.navigate(Routes.CHECK_ON_BRO) },
                onWrapped = { nav.navigate(Routes.WRAPPED) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
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
