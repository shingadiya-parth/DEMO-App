package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppContainer
import com.example.data.repository.AuthState
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTopBar
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.SignUpScreen
import com.example.ui.screens.earn.EarnScreen
import com.example.ui.screens.earn.EarnViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.play.PlayScreen
import com.example.ui.screens.play.PlayViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.rewards.RewardsScreen
import com.example.ui.screens.rewards.RewardsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.spin.SpinScreen
import com.example.ui.screens.spin.SpinViewModel
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.screens.wallet.WalletViewModel
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.PlayRewardsTheme

class MainActivity : ComponentActivity() {

    lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = (application as? PlayRewardsApplication)?.container
            ?: AppContainer(applicationContext)

        setContent {
            PlayRewardsTheme {
                PlayRewardsAppRoot(container = appContainer)
            }
        }
    }
}

@Composable
fun PlayRewardsAppRoot(container: AppContainer) {
    val authState by container.authRepository.authState.collectAsState()

    Crossfade(
        targetState = authState,
        label = "root_auth_crossfade"
    ) { state ->
        when (state) {
            is AuthState.Loading -> {
                SplashScreen()
            }
            is AuthState.Unauthenticated -> {
                AuthFlowContainer(container = container)
            }
            is AuthState.Authenticated -> {
                MainAppShell(
                    container = container,
                    authenticatedUserId = state.user.userId
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.Primary, AppColors.AccentPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = AppColors.GoldCoin,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "PlayRewards",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )

            Text(
                text = "Play Games • Earn Coins • Win Real Rewards",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            CircularProgressIndicator(
                color = AppColors.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun AuthFlowContainer(container: AppContainer) {
    var isSignUp by remember { mutableStateOf(false) }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(container.authRepository)
    )

    Crossfade(targetState = isSignUp, label = "auth_screen_switch") { showSignUp ->
        if (showSignUp) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { isSignUp = false },
                onSignUpSuccess = { /* Automatically transitions via authState */ }
            )
        } else {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { isSignUp = true },
                onLoginSuccess = { /* Automatically transitions via authState */ }
            )
        }
    }
}

@Composable
fun MainAppShell(
    container: AppContainer,
    authenticatedUserId: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val snackbarHostState = remember { SnackbarHostState() }
    var isLedgerSheetVisible by remember { mutableStateOf(false) }

    // Live observed balance from immutable database ledger
    val coinBalance by container.walletRepository.observeCalculatedBalance(
        authenticatedUserId
    ).collectAsState(initial = 0L)

    val currentUser by container.userRepository.observeCurrentUser().collectAsState(initial = null)

    val transactions by container.walletRepository.observeTransactions(
        authenticatedUserId
    ).collectAsState(initial = emptyList())

    val isTopBarVisible = currentRoute != Screen.Settings.route && 
                          currentRoute != Screen.Profile.route && 
                          currentRoute != Screen.Wallet.route &&
                          currentRoute != Screen.Home.route
    val isBottomBarVisible = currentRoute != Screen.Settings.route && 
                             currentRoute != Screen.Profile.route && 
                             currentRoute != Screen.Wallet.route

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight),
        topBar = {
            if (isTopBarVisible) {
                AppTopBar(
                    coinBalance = coinBalance,
                    userName = currentUser?.displayName ?: "Player",
                    onCoinClick = { navController.navigate(Screen.Wallet.route) },
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onNotificationClick = { isLedgerSheetVisible = true }
                )
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.BackgroundLight
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        container.userRepository,
                        container.walletRepository,
                        container.gameRepository,
                        container.earnRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateTo = { route -> navController.navigate(route) },
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Play.route) {
                val playViewModel: PlayViewModel = viewModel(
                    factory = PlayViewModel.Factory(
                        container.gameRepository,
                        container.gameEngine,
                        container.adMobService,
                        container.userRepository
                    )
                )
                PlayScreen(
                    viewModel = playViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Spin.route) {
                val spinViewModel: SpinViewModel = viewModel(
                    factory = SpinViewModel.Factory(
                        container.rewardEngine,
                        container.gameRepository,
                        container.adMobService,
                        container.userRepository
                    )
                )
                SpinScreen(
                    viewModel = spinViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Earn.route) {
                val earnViewModel: EarnViewModel = viewModel(
                    factory = EarnViewModel.Factory(
                        container.earnRepository,
                        container.userRepository
                    )
                )
                EarnScreen(
                    viewModel = earnViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Rewards.route) {
                val rewardsViewModel: RewardsViewModel = viewModel(
                    factory = RewardsViewModel.Factory(
                        container.redemptionRepository,
                        container.walletRepository,
                        container.userRepository
                    )
                )
                RewardsScreen(
                    viewModel = rewardsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Wallet.route) {
                val walletViewModel: WalletViewModel = viewModel(
                    factory = WalletViewModel.Factory(
                        container.walletRepository,
                        container.authRepository
                    )
                )
                WalletScreen(
                    viewModel = walletViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEarn = { navController.navigate(Screen.Earn.route) },
                    onNavigateToRewards = { navController.navigate(Screen.Rewards.route) },
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(
                        container.authRepository,
                        container.userRepository,
                        container.walletRepository
                    )
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.authRepository,
                        container.userRepository,
                        container.walletRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onLoggedOut = { /* Transitions via authState automatically */ },
                    snackbarHostState = snackbarHostState
                )
            }
        }

        // Ledger Audit Bottom Sheet Modal
        if (isLedgerSheetVisible) {
            TransactionHistoryDialog(
                transactions = transactions,
                currentBalance = coinBalance,
                onDismiss = { isLedgerSheetVisible = false }
            )
        }
    }
}
