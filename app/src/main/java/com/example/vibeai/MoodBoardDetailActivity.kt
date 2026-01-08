package com.example.vibeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vibeai.ui.theme.VibeAITheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodBoardDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val boardId = intent.getStringExtra("boardId") ?: ""

        setContent {
            VibeAITheme {
                MoodBoardDetailScreen(boardId = boardId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodBoardDetailScreen(boardId: String) {
    val board = remember(MoodBoardRepository.boards, boardId) {
        MoodBoardRepository.boards.firstOrNull { it.id == boardId }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(board?.title ?: "Mood Board Details") })
        }
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {

            Text(
                text = "Tag: ${board?.moodTag ?: "—"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = board?.description ?: "No description available.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Images: ${board?.imageCount ?: 0}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Updated: ${formatTimeLocal(board?.updatedAt ?: 0L)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatTimeLocal(epoch: Long): String {
    if (epoch <= 0L) return "—"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(epoch))
}
