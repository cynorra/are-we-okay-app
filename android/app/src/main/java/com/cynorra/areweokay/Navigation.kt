package com.cynorra.areweokay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.cynorra.areweokay.components.BannerAd
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.ui.auth.AuthScreen
import com.cynorra.areweokay.ui.home.HomeScreen

@Composable
fun MainNavigation() {
    val dataRepository = LocalDataRepository.current
    val currentUser by dataRepository.currentUser.collectAsState()
    
    val startDestination = if (currentUser == null) Auth else Home
    val backStack = rememberNavBackStack(startDestination)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
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
        
        // Reklamı uygulamanın her ekranında en alt kısımda gösteriyoruz
        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}
