package com.subcontrol.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Sealed class representing all navigation destinations in the app.
 * 
 * This provides type-safe navigation with compile-time checking
 * and ensures consistency across the app.
 */
sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    
    /**
     * Subscriptions list screen - main entry point.
     */
    object Subscriptions : Screen("subscriptions")
    
    /**
     * Categories management screen.
     */
    object Categories : Screen("categories")
    
    /**
     * Budgets management screen.
     */
    object Budgets : Screen("budgets")
    
    /**
     * Settings screen.
     */
    object Settings : Screen("settings")
    
    /**
     * Add new subscription screen.
     */
    object AddSubscription : Screen("add_subscription")
    
    /**
     * Edit existing subscription screen.
     */
    object EditSubscription : Screen(
        route = "edit_subscription/{subscriptionId}",
        arguments = listOf(
            navArgument("subscriptionId") {
                type = NavType.StringType
                nullable = false
            }
        )
    ) {
        fun createRoute(subscriptionId: String): String {
            return "edit_subscription/$subscriptionId"
        }
    }
    
    /**
     * Add new category screen.
     */
    object AddCategory : Screen("add_category")
    
    /**
     * Edit existing category screen.
     */
    object EditCategory : Screen(
        route = "edit_category/{categoryId}",
        arguments = listOf(
            navArgument("categoryId") {
                type = NavType.StringType
                nullable = false
            }
        )
    ) {
        fun createRoute(categoryId: String): String {
            return "edit_category/$categoryId"
        }
    }
    
    /**
     * Add new budget screen.
     */
    object AddBudget : Screen("add_budget")
    
    /**
     * Edit existing budget screen.
     */
    object EditBudget : Screen(
        route = "edit_budget/{budgetId}",
        arguments = listOf(
            navArgument("budgetId") {
                type = NavType.StringType
                nullable = false
            }
        )
    ) {
        fun createRoute(budgetId: String): String {
            return "edit_budget/$budgetId"
        }
    }
}

/**
 * Bottom navigation destinations.
 * These screens appear in the bottom navigation bar.
 */
val bottomNavigationScreens = listOf(
    Screen.Subscriptions,
    Screen.Categories,
    Screen.Budgets,
    Screen.Settings
)