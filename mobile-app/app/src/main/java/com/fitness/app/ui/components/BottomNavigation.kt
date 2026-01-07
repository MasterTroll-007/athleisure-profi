package com.fitness.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    object Pricing : BottomNavItem(
        route = "admin/pricing",
        titleRes = R.string.pricing,
        selectedIcon = Icons.Filled.Payments,
        unselectedIcon = Icons.Outlined.Payments
    )
    object Profile : BottomNavItem(
        route = "profile",
        titleRes = R.string.profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@Composable
fun ClientBottomNavigation(
    currentRoute: String?,
    isAdmin: Boolean = false,
    onNavigate: (String) -> Unit
) {
    // Admin sees Pricing instead of Credits
    val items = if (isAdmin) {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Reservations,
            BottomNavItem.Pricing,
            BottomNavItem.Profile
        )
    } else {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Reservations,
            BottomNavItem.Credits,
            BottomNavItem.Profile
        )
    }

    NavigationBar(
        modifier = Modifier.height(64.dp)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.titleRes),
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = null,
                selected = selected,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
