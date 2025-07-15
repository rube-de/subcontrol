package com.subcontrol.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.subcontrol.R
import com.subcontrol.ui.navigation.Screen
import com.subcontrol.ui.navigation.bottomNavigationScreens

/**
 * Bottom navigation bar for the SubControl app.
 * 
 * This composable provides navigation between main app sections
 * with Material Design 3 styling and proper state management.
 */
@Composable
fun SubControlBottomNavigation(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavigationScreens.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.getIcon(),
                        contentDescription = stringResource(screen.getTitleRes())
                    )
                },
                label = {
                    Text(
                        text = stringResource(screen.getTitleRes()),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when reselecting
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Extension function to get the icon for each navigation screen.
 */
private fun Screen.getIcon(): ImageVector {
    return when (this) {
        Screen.Subscriptions -> Icons.Default.Subscriptions
        Screen.Categories -> Icons.Default.Category
        Screen.Budgets -> Icons.Default.MonetizationOn
        Screen.Settings -> Icons.Default.Settings
        else -> Icons.Default.Subscriptions // Default fallback
    }
}

/**
 * Extension function to get the title resource for each navigation screen.
 */
private fun Screen.getTitleRes(): Int {
    return when (this) {
        Screen.Subscriptions -> R.string.nav_subscriptions
        Screen.Categories -> R.string.nav_categories
        Screen.Budgets -> R.string.nav_budgets
        Screen.Settings -> R.string.nav_settings
        else -> R.string.nav_subscriptions // Default fallback
    }
}