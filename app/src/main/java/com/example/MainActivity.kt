package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LudoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    private val ludoViewModel: LudoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                    var isGuestOrCustomLoggedIn by remember { mutableStateOf(false) }

                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            currentUser = auth.currentUser
                            if (auth.currentUser != null) {
                                isGuestOrCustomLoggedIn = true
                                ludoViewModel.onUserAuthenticated()
                            }
                        }
                        Firebase.auth.addAuthStateListener(listener)
                        onDispose {
                            Firebase.auth.removeAuthStateListener(listener)
                        }
                    }

                    if (currentUser == null && !isGuestOrCustomLoggedIn) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                                isGuestOrCustomLoggedIn = true
                                ludoViewModel.onUserAuthenticated()
                            },
                            onGuestOrCustomLogin = { username, avatarId ->
                                ludoViewModel.updateProfile(username, avatarId)
                                isGuestOrCustomLoggedIn = true
                            }
                        )
                    } else {
                        // Set up modern Compose Navigation Controller
                        val navController = rememberNavController()

                        DisposableEffect(navController) {
                            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                                val route = destination.route
                                if (route != null && route != "splash") {
                                    ludoViewModel.updateScreenState(route)
                                }
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose {
                                navController.removeOnDestinationChangedListener(listener)
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "splash"
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onNavigateToLobby = {
                                        val targetRoute = when (ludoViewModel.lastScreenState) {
                                            "game" -> if (ludoViewModel.players.isNotEmpty()) "game" else "lobby"
                                            "spin_wheel" -> "spin_wheel"
                                            "leaderboard" -> "leaderboard"
                                            "store" -> "store"
                                            else -> "lobby"
                                        }
                                        navController.navigate(targetRoute) {
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
}
