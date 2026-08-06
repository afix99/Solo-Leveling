package com.ascend.app.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ascend.app.AscendApplication
import com.ascend.app.ui.theme.AscendTheme

/**
 * Hosts the Focus Session timer in its own Activity so notifications can be
 * suppressed and the screen can go full-bleed for the duration of the
 * session, independent of back-stack/tab state on the main screen.
 * Launch with [EXTRA_HABIT_ID].
 */
class FocusSessionActivity : ComponentActivity() {
    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val repository = (application as AscendApplication).repository
        setContent {
            AscendTheme {
                FocusSessionScreen(
                    habitId = habitId,
                    repository = repository,
                    onFinish = { finish() },
                )
            }
        }
    }
}
