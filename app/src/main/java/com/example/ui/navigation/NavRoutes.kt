package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Play : Screen(
        route = "play",
        title = "Play",
        selectedIcon = Icons.Filled.Gamepad,
        unselectedIcon = Icons.Outlined.Gamepad
    )

    data object Spin : Screen(
        route = "spin",
        title = "Spin",
        selectedIcon = Icons.Filled.Refresh,
        unselectedIcon = Icons.Outlined.Refresh
    )

    data object Scratch : Screen(
        route = "scratch",
        title = "Scratch",
        selectedIcon = Icons.Filled.CardGiftcard,
        unselectedIcon = Icons.Outlined.CardGiftcard
    )

    data object Puzzle : Screen(
        route = "puzzle",
        title = "Puzzle",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    )

    data object CoinToss : Screen(
        route = "coin_toss",
        title = "Coin Toss",
        selectedIcon = Icons.Filled.Refresh,
        unselectedIcon = Icons.Outlined.Refresh
    )

    data object TicTacToe : Screen(
        route = "tictactoe",
        title = "Tic-Tac-Toe",
        selectedIcon = Icons.Filled.Gamepad,
        unselectedIcon = Icons.Outlined.Gamepad
    )

    data object BubblePop : Screen(
        route = "bubble_pop",
        title = "Bubble Pop",
        selectedIcon = Icons.Filled.Gamepad,
        unselectedIcon = Icons.Outlined.Gamepad
    )

    data object Earn : Screen(
        route = "earn",
        title = "Earn",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp
    )

    data object Rewards : Screen(
        route = "rewards",
        title = "Rewards",
        selectedIcon = Icons.Filled.CardGiftcard,
        unselectedIcon = Icons.Outlined.CardGiftcard
    )

    data object Wallet : Screen(
        route = "wallet",
        title = "Wallet",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )

    data object Profile : Screen(
        route = "profile",
        title = "My Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object Notifications : Screen(
        route = "notifications",
        title = "Notifications",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    )

    data object Activity : Screen(
        route = "activity",
        title = "Activity History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Settings : Screen(
        route = "settings",
        title = "Profile & Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object ReferEarn : Screen(
        route = "refer_earn",
        title = "Refer & Earn",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        // Exactly 5 main bottom-navigation tabs
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Play, Spin, Earn, Rewards)
    }
}
