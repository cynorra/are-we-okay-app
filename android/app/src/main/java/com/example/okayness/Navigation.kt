package com.example.okayness

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.okayness.data.LocalDataRepository
import com.example.okayness.ui.auth.AuthScreen
import com.example.okayness.ui.home.HomeScreen

@Composable
fun MainNavigation() {
    val dataRepository = LocalDataRepository.current
    val currentUser by dataRepository.currentUser.collectAsState()
    
    val startDestination = if (currentUser == null) Auth else Home
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Auth> {
                AuthScreen(
                    onAuthSuccess = {
                        backStack.add(Home)
                        backStack.remove(Auth)
                    }
                )
            }
            entry<Home> {
                HomeScreen(
                    onSignOut = {
                        backStack.add(Auth)
                        backStack.remove(Home)
                    }
                )
            }
        }
    )
}
