package com.keeptrack.lasttime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keeptrack.lasttime.ui.home.HomeScreen
import com.keeptrack.lasttime.ui.theme.LastTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LastTimeTheme {
                HomeScreen()
            }
        }
    }
}
