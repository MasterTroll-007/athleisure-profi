package com.fitness.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.fitness.app.R

sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Reservations : BottomNavItem(
        route = "reservations",
        titleRes = R.string.reservations,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )
    object Credits : BottomNavItem(
        route = "credits",
        titleRes = R.string.credits,
        selectedIcon = Icons.Filled.CreditCard,
        unselectedIcon = Icons.Outlined.CreditCard
    )
    object Profile : BottomNavItem(
        route = "profile",
        titleRes = R.string.profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
    object Admin : BottomNavItem(
        route = "admin",
        titleRes = R.string.admin,
        selectedIcon = Icons.Filled.AdminPanelSettings,
        unselectedIcon = Icons.Outlined.AdminPanelSettings
    )
}

@Composable
fun ClientBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    isAdmin: Boolean = false
) {
    val items = buildList {
        add(BottomNavItem.Home)
        add(BottomNavItem.Reservations)
        add(BottomNavItem.Credits)
        add(BottomNavItem.Profile)
        if (isAdmin) add(BottomNavItem.Admin)
    }

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route ||
                (item == BottomNavItem.Admin && currentRoute?.startsWith("admin") == true)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(item.titleRes)) },
                selected = selected,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
