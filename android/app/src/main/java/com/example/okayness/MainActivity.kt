package com.example.okayness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.example.okayness.data.DefaultDataRepository
import com.example.okayness.data.LocalDataRepository
import com.example.okayness.theme.OkaynessTheme

class MainActivity : ComponentActivity() {
    private lateinit var dataRepository: DefaultDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dataRepository = DefaultDataRepository(applicationContext)

        setContent {
            OkaynessTheme {
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
