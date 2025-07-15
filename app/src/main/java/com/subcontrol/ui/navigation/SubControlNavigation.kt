package com.subcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.subcontrol.ui.screens.budget.BudgetEditScreen
import com.subcontrol.ui.screens.budget.BudgetScreen
import com.subcontrol.ui.screens.category.CategoryEditScreen
import com.subcontrol.ui.screens.category.CategoryScreen
import com.subcontrol.ui.screens.backup.BackupScreen
import com.subcontrol.ui.screens.settings.SettingsScreen
import com.subcontrol.ui.screens.subscription.SubscriptionEditScreen
import com.subcontrol.ui.screens.subscription.SubscriptionListScreen

/**
 * Main navigation component for SubControl app.
 * 
 * This composable handles navigation between different screens
 * using Jetpack Compose Navigation.
 */
@Composable
fun SubControlNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Subscriptions.route,
        modifier = modifier
    ) {
        composable(Screen.Subscriptions.route) {
            SubscriptionListScreen(
                onNavigateToAddSubscription = {
                    navController.navigate(Screen.AddSubscription.route)
                },
                onNavigateToEditSubscription = { subscriptionId ->
                    navController.navigate(Screen.EditSubscription.createRoute(subscriptionId))
                }
            )
        }
        
        composable(Screen.Categories.route) {
            CategoryScreen(
                onNavigateToAddCategory = {
                    navController.navigate(Screen.AddCategory.route)
                },
                onNavigateToEditCategory = { categoryId ->
                    navController.navigate(Screen.EditCategory.createRoute(categoryId))
                }
            )
        }
        
        composable(Screen.Budgets.route) {
            BudgetScreen(
                onNavigateToAddBudget = {
                    navController.navigate(Screen.AddBudget.route)
                },
                onNavigateToEditBudget = { budgetId ->
                    navController.navigate(Screen.EditBudget.createRoute(budgetId))
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                }
            )
        }
        
        composable(Screen.Backup.route) {
            BackupScreen()
        }
        
        composable(Screen.AddSubscription.route) {
            SubscriptionEditScreen(
                subscriptionId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditSubscription.route,
            arguments = Screen.EditSubscription.arguments
        ) { backStackEntry ->
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
            SubscriptionEditScreen(
                subscriptionId = subscriptionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddCategory.route) {
            CategoryEditScreen(
                categoryId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditCategory.route,
            arguments = Screen.EditCategory.arguments
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            CategoryEditScreen(
                categoryId = categoryId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddBudget.route) {
            BudgetEditScreen(
                budgetId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditBudget.route,
            arguments = Screen.EditBudget.arguments
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId") ?: ""
            BudgetEditScreen(
                budgetId = budgetId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}