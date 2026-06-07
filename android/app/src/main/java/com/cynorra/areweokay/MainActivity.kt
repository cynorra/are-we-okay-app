package com.cynorra.areweokay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.cynorra.areweokay.data.DefaultDataRepository
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.theme.AreWeOkayTheme

class MainActivity : ComponentActivity() {
    private lateinit var dataRepository: DefaultDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dataRepository = DefaultDataRepository(applicationContext)

        setContent {
            AreWeOkayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalDataRepository provides dataRepository) {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
