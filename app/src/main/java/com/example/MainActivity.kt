package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LudoViewModel

class MainActivity : ComponentActivity() {
    private val ludoViewModel: LudoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Set up modern Compose Navigation Controller
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToLobby = {
                                    navController.navigate("lobby") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("lobby") {
                            LobbyScreen(
                                viewModel = ludoViewModel,
                                onNavigateToGame = { navController.navigate("game") },
                                onNavigateToSpin = { navController.navigate("spin_wheel") },
                                onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                                onNavigateToStore = { navController.navigate("store") }
                            )
                        }
                        
                        composable("game") {
                            GameScreen(
                                viewModel = ludoViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("spin_wheel") {
                            SpinWheelScreen(
                                viewModel = ludoViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("leaderboard") {
                            LeaderboardScreen(
                                viewModel = ludoViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("store") {
                            StoreScreen(
                                viewModel = ludoViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
