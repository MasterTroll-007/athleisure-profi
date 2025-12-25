package com.fitness.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitness.app.ui.components.ClientBottomNavigation
import com.fitness.app.ui.screens.auth.LoginScreen
import com.fitness.app.ui.screens.auth.RegisterScreen
import com.fitness.app.ui.screens.auth.VerifyEmailScreen
import com.fitness.app.ui.screens.home.HomeScreen
import com.fitness.app.ui.screens.reservations.ReservationsScreen
import com.fitness.app.ui.screens.credits.CreditsScreen
import com.fitness.app.ui.screens.plans.PlansScreen
import com.fitness.app.ui.screens.profile.ProfileScreen
import com.fitness.app.ui.screens.admin.AdminDashboardScreen
import com.fitness.app.ui.screens.admin.AdminCalendarScreen
import com.fitness.app.ui.screens.admin.AdminClientsScreen
import com.fitness.app.ui.screens.admin.AdminClientDetailScreen
import com.fitness.app.ui.screens.admin.AdminTemplatesScreen
import com.fitness.app.ui.screens.admin.AdminPlansScreen
import com.fitness.app.ui.screens.admin.AdminPricingScreen
import com.fitness.app.ui.screens.admin.AdminPaymentsScreen
import com.fitness.app.data.local.PreferencesManager

@Composable
fun FitnessNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthNavigationViewModel = hiltViewModel(),
    preferencesManager: PreferencesManager
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    var hasInitialized by remember { mutableStateOf(false) }
    val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Login.route

    // Navigate to login when user is logged out (tokens cleared)
    LaunchedEffect(isLoggedIn) {
        if (hasInitialized && !isLoggedIn) {
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
        hasInitialized = true
    }

    // Handle forced logout event (when token refresh fails)
    LaunchedEffect(Unit) {
        authViewModel.logoutEvent.collect {
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    // Screens that show bottom navigation
    val bottomNavRoutes = listOf(
        Routes.Home.route,
        Routes.Reservations.route,
        Routes.Credits.route,
        Routes.Profile.route,
        Routes.AdminDashboard.route,
        Routes.AdminCalendar.route,
        Routes.AdminTemplates.route,
        Routes.AdminClients.route,
        Routes.AdminPlans.route,
        Routes.AdminPricing.route,
        Routes.AdminPayments.route
    )

    val showBottomNav = currentRoute in bottomNavRoutes || currentRoute?.startsWith("admin/clients/") == true

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                ClientBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Routes.Home.route) {
                            // Navigate to Home and clear backstack
                            navController.navigate(route) {
                                popUpTo(Routes.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(Routes.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    isAdmin = isAdmin
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (showBottomNav) Modifier.padding(paddingValues) else Modifier
        ) {
            // Auth screens
            composable(Routes.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToVerifyEmail = { email ->
                        navController.navigate(Routes.VerifyEmail.createRoute(email))
                    }
                )
            }

            composable(
                route = Routes.VerifyEmail.route,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                VerifyEmailScreen(
                    email = email,
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            // Client screens
            composable(Routes.Home.route) {
                HomeScreen(
                    onNavigateToReservations = { navController.navigate(Routes.Reservations.route) },
                    onNavigateToCredits = { navController.navigate(Routes.Credits.route) }
                )
            }

            composable(Routes.Reservations.route) {
                ReservationsScreen()
            }

            composable(Routes.Credits.route) {
                CreditsScreen(
                    onOpenPaymentUrl = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            composable(Routes.Plans.route) {
                PlansScreen(
                    onDownloadPlan = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    preferencesManager = preferencesManager
                )
            }

            // Admin screens
            composable(Routes.AdminDashboard.route) {
                AdminDashboardScreen(
                    onNavigateToCalendar = { navController.navigate(Routes.AdminCalendar.route) },
                    onNavigateToClients = { navController.navigate(Routes.AdminClients.route) },
                    onNavigateToTemplates = { navController.navigate(Routes.AdminTemplates.route) },
                    onNavigateToPlans = { navController.navigate(Routes.AdminPlans.route) },
                    onNavigateToPricing = { navController.navigate(Routes.AdminPricing.route) },
                    onNavigateToPayments = { navController.navigate(Routes.AdminPayments.route) }
                )
            }

            composable(Routes.AdminCalendar.route) {
                AdminCalendarScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AdminClients.route) {
                AdminClientsScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Routes.AdminClientDetail.createRoute(id))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.AdminClientDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                AdminClientDetailScreen(
                    clientId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AdminTemplates.route) {
                AdminTemplatesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AdminPlans.route) {
                AdminPlansScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AdminPricing.route) {
                AdminPricingScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AdminPayments.route) {
                AdminPaymentsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
