package com.wault.app.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wault.app.R
import com.wault.app.ui.screens.conversation.ConversationScreen
import com.wault.app.ui.screens.conversation.ConversationViewModel
import com.wault.app.ui.screens.inbox.InboxScreen
import com.wault.app.ui.screens.inbox.InboxViewModel
import com.wault.app.ui.screens.pairing.PairingScreen
import com.wault.app.ui.screens.pairing.PairingViewModel
import com.wault.app.ui.screens.settings.SettingsScreen
import com.wault.app.ui.screens.settings.SettingsViewModel
import com.wault.app.ui.screens.vault.VaultScreen
import com.wault.app.ui.screens.vault.VaultViewModel

private const val NAV_TWEEN_MS = 220

private data class TabSpec(
    val route: String,
    val matchRoute: String,
    val labelRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

private val TABS = listOf(
    TabSpec(
        route = WaultDestinations.VAULT,
        matchRoute = WaultDestinations.VAULT,
        labelRes = R.string.tab_vault,
        icon = Icons.Outlined.Shield,
        iconSelected = Icons.Filled.Shield,
    ),
    TabSpec(
        route = WaultDestinations.INBOX,
        matchRoute = WaultDestinations.INBOX_PATTERN,
        labelRes = R.string.tab_inbox,
        icon = Icons.Outlined.ChatBubble,
        iconSelected = Icons.Filled.ChatBubble,
    ),
    TabSpec(
        route = WaultDestinations.SETTINGS,
        matchRoute = WaultDestinations.SETTINGS,
        labelRes = R.string.tab_settings,
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings,
    ),
)

/**
 * Single-activity nav graph: Vault / Inbox / Settings as bottom tabs, with
 * Conversation and Pairing pushed full-screen (no bottom bar). Transitions
 * are quiet 220 ms fades on tabs and shared-axis-ish slides for pushes.
 */
@Composable
fun WaultNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TABS.any { it.matchRoute == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                WaultBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navigateToTab(navController, route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WaultDestinations.VAULT,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { fadeIn(tween(NAV_TWEEN_MS)) },
            exitTransition = { fadeOut(tween(NAV_TWEEN_MS)) },
            popEnterTransition = { fadeIn(tween(NAV_TWEEN_MS)) },
            popExitTransition = { fadeOut(tween(NAV_TWEEN_MS)) },
        ) {
            composable(WaultDestinations.VAULT) {
                val viewModel: VaultViewModel = hiltViewModel()
                VaultScreen(
                    viewModel = viewModel,
                    onOpenAccount = { accountId ->
                        navController.navigate(WaultDestinations.inbox(accountId))
                    },
                    onPairAccount = { accountId ->
                        navController.navigate(WaultDestinations.pairing(accountId))
                    },
                )
            }
            composable(
                route = WaultDestinations.INBOX_PATTERN,
                arguments = listOf(navArgument("filter") { defaultValue = "all" }),
            ) {
                val viewModel: InboxViewModel = hiltViewModel()
                InboxScreen(
                    viewModel = viewModel,
                    onOpenChat = { chatId ->
                        navController.navigate(WaultDestinations.conversation(chatId))
                    },
                )
            }
            composable(WaultDestinations.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                route = WaultDestinations.CONVERSATION_PATTERN,
                enterTransition = {
                    slideInHorizontally(tween(NAV_TWEEN_MS)) { it / 10 } + fadeIn(tween(NAV_TWEEN_MS))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(NAV_TWEEN_MS)) { it / 10 } + fadeOut(tween(NAV_TWEEN_MS))
                },
            ) {
                val viewModel: ConversationViewModel = hiltViewModel()
                ConversationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = WaultDestinations.PAIRING_PATTERN,
                enterTransition = {
                    slideInHorizontally(tween(NAV_TWEEN_MS)) { it / 10 } + fadeIn(tween(NAV_TWEEN_MS))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(NAV_TWEEN_MS)) { it / 10 } + fadeOut(tween(NAV_TWEEN_MS))
                },
            ) {
                val viewModel: PairingViewModel = hiltViewModel()
                PairingScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun WaultBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TABS.forEach { tab ->
            val selected = currentRoute == tab.matchRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.iconSelected else tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
