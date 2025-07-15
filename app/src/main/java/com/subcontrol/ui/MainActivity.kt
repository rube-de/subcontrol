package com.subcontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.subcontrol.ui.components.SubControlBottomNavigation
import com.subcontrol.ui.navigation.SubControlNavigation
import com.subcontrol.ui.theme.SubControlTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the SubControl app.
 * 
 * This activity serves as the single entry point for the app
 * and hosts the Compose UI with navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android experience
        enableEdgeToEdge()
        
        setContent {
            SubControlApp()
        }
    }
}

/**
 * Main app composable that sets up the theme and navigation structure.
 */
@Composable
private fun SubControlApp() {
    SubControlTheme {
        val navController = rememberNavController()
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                SubControlBottomNavigation(navController = navController)
            }
        ) { innerPadding ->
            SubControlNavigation(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}