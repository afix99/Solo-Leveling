package com.ascend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ascend.app.ui.AscendApp
import com.ascend.app.ui.theme.AscendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as AscendApplication).repository
        setContent {
            AscendTheme {
                AscendApp(repository = repository)
            }
        }
    }
}
