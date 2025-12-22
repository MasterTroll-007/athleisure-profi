package com.fitness.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitness.app.ui.screens.auth.LoginScreen
import com.fitness.app.ui.screens.auth.RegisterScreen
import com.fitness.app.ui.screens.auth.VerifyEmailScreen
import com.fitness.app.ui.screens.home.HomeScreen
import com.fitness.app.ui.screens.reservations.ReservationsScreen
import com.fitness.app.ui.screens.reservations.NewReservationScreen
import com.fitness.app.ui.screens.credits.CreditsScreen
import com.fitness.app.ui.screens.credits.BuyCreditsScreen
import com.fitness.app.ui.screens.plans.PlansScreen
import com.fitness.app.ui.screens.plans.MyPlansScreen
import com.fitness.app.ui.screens.profile.ProfileScreen
import com.fitness.app.ui.screens.profile.ChangePasswordScreen
import com.fitness.app.ui.screens.admin.AdminDashboardScreen
import com.fitness.app.ui.screens.admin.AdminCalendarScreen
import com.fitness.app.ui.screens.admin.AdminTemplatesScreen
import com.fitness.app.ui.screens.admin.AdminClientsScreen
import com.fitness.app.ui.screens.admin.AdminClientDetailScreen
import com.fitness.app.ui.screens.admin.AdminPlansScreen
import com.fitness.app.ui.screens.admin.AdminPricingScreen
import com.fitness.app.ui.screens.admin.AdminPaymentsScreen

@Composable
fun FitnessNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthNavigationViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onNavigateToReservations = { navController.navigate(Routes.NewReservation.route) },
                onNavigateToCredits = { navController.navigate(Routes.BuyCredits.route) },
                onNavigateToAdmin = { navController.navigate(Routes.AdminDashboard.route) },
                navController = navController
            )
        }

        composable(Routes.Reservations.route) {
            ReservationsScreen(
                onNavigateToNew = { navController.navigate(Routes.NewReservation.route) },
                navController = navController
            )
        }

        composable(Routes.NewReservation.route) {
            NewReservationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Credits.route) {
            CreditsScreen(
                onNavigateToBuy = { navController.navigate(Routes.BuyCredits.route) },
                navController = navController
            )
        }

        composable(Routes.BuyCredits.route) {
            BuyCreditsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Plans.route) {
            PlansScreen(
                onNavigateToMyPlans = { navController.navigate(Routes.MyPlans.route) },
                navController = navController
            )
        }

        composable(Routes.MyPlans.route) {
            MyPlansScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Profile.route) {
            ProfileScreen(
                onNavigateToChangePassword = { navController.navigate(Routes.ChangePassword.route) },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable(Routes.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Admin screens
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }

        composable(Routes.AdminCalendar.route) {
            AdminCalendarScreen(navController = navController)
        }

        composable(Routes.AdminTemplates.route) {
            AdminTemplatesScreen(navController = navController)
        }

        composable(Routes.AdminClients.route) {
            AdminClientsScreen(
                onNavigateToDetail = { id -> navController.navigate(Routes.AdminClientDetail.createRoute(id)) },
                navController = navController
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

        composable(Routes.AdminPlans.route) {
            AdminPlansScreen(navController = navController)
        }

        composable(Routes.AdminPricing.route) {
            AdminPricingScreen(navController = navController)
        }

        composable(Routes.AdminPayments.route) {
            AdminPaymentsScreen(navController = navController)
        }
    }
}
