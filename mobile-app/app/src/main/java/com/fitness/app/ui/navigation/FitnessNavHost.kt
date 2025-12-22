package com.fitness.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun FitnessNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthNavigationViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    // Screens that show bottom navigation
    val bottomNavRoutes = listOf(
        Routes.Home.route,
        Routes.Reservations.route,
        Routes.Credits.route,
        Routes.Profile.route
    )

    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                ClientBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                    onNavigateToCredits = { navController.navigate(Routes.Credits.route) },
                    onNavigateToAdmin = { navController.navigate(Routes.AdminDashboard.route) }
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
                    }
                )
            }

            // Admin screens
            composable(Routes.AdminDashboard.route) {
                AdminDashboardScreen(
                    onNavigateToCalendar = { navController.navigate(Routes.AdminCalendar.route) },
                    onNavigateToClients = { navController.navigate(Routes.AdminClients.route) },
                    onNavigateBack = { navController.popBackStack() }
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
        }
    }
}
