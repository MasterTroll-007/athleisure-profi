package com.fitness.app.ui.navigation

sealed class Routes(val route: String) {
    // Auth
    object Login : Routes("login")
    object Register : Routes("register")
    object VerifyEmail : Routes("verify_email/{email}") {
        fun createRoute(email: String) = "verify_email/$email"
    }

    // Client
    object Home : Routes("home")
    object Reservations : Routes("reservations")
    object NewReservation : Routes("reservations/new")
    object Credits : Routes("credits")
    object BuyCredits : Routes("credits/buy")
    object Plans : Routes("plans")
    object MyPlans : Routes("plans/my")
    object Profile : Routes("profile")
    object ChangePassword : Routes("profile/password")

    // Admin
    object AdminDashboard : Routes("admin")
    object AdminCalendar : Routes("admin/calendar")
    object AdminTemplates : Routes("admin/templates")
    object AdminClients : Routes("admin/clients")
    object AdminClientDetail : Routes("admin/clients/{id}") {
        fun createRoute(id: String) = "admin/clients/$id"
    }
    object AdminPlans : Routes("admin/plans")
    object AdminPricing : Routes("admin/pricing")
    object AdminPayments : Routes("admin/payments")
}
