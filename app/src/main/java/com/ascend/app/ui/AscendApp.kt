package com.ascend.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.ui.theme.AscendColors

// TODO(nav wiring task): replaced with the full NavHost once all screens exist.
@Composable
fun AscendApp(repository: AscendRepository) {
    Box(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Text("ASCEND", color = AscendColors.TextPrimary)
    }
}
