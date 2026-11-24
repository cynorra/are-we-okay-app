package com.cynorra.areweokay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cynorra.areweokay.components.AdGate
import com.cynorra.areweokay.ui.consent.ConsentScreen
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
                        var consented by remember { mutableStateOf(dataRepository.local.hasValidConsent) }
                        val toAuth = {
                            backStack.add(Auth)
                            backStack.remove(Home)
                        }
                        if (!consented) {
                            ConsentScreen(
                                onAccepted = { consented = true },
                                onDeclined = {
                                    dataRepository.signOut()
                                    toAuth()
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                // Check-in chosen before login is only saved once consent exists.
                                dataRepository.local.pendingMood?.let { key ->
                                    dataRepository.local.pendingMood = null
                                    val lv = when (key) { "good" -> 4; "bad" -> 1; else -> 3 }
                                    dataRepository.createCheckin(lv, emptyList(), "", true)
                                }
                            }
                            HomeScreen(onSignOut = { toAuth() })
                        }
                    }
                }
            )
        }
        
        // Reklamı uygulamanın her ekranında en alt kısımda gösteriyoruz
        // No ads on sensitive screens (consent, crisis, journal, breathing).
        if (!AdGate.adsHidden) BannerAd(modifier = Modifier.fillMaxWidth())
    }
}
