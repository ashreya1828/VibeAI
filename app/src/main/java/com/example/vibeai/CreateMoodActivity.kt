package com.example.vibeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.vibeai.ui.theme.VibeAITheme

class CreateMoodActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeAITheme {
                CreateMoodScreen(
                    onSave = { newBoard ->
                        // Add new board to shared list
                        MoodBoardRepository.boards.add(newBoard)
                        // Go back to Home
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun CreateMoodScreen(
    onSave: (MoodBoard) -> Unit = {}
) {
    var prompt by remember { mutableStateOf("") }
    var selectedVibe by remember { mutableStateOf("Calm") }
    var hasPreview by remember { mutableStateOf(false) }

    // Fake palette based on vibe
    val previewColors = remember(selectedVibe) {
        when (selectedVibe) {
            "Calm" -> listOf(
                Color(0xFFB3E5FC),
                Color(0xFF80CBC4),
                Color(0xFFE3F2FD)
            )
            "Bright" -> listOf(
                Color(0xFFFFC107),
                Color(0xFFFF5722),
                Color(0xFFFFEB3B)
            )
            "Dark" -> listOf(
                Color(0xFF263238),
                Color(0xFF455A64),
                Color(0xFF1A237E)
            )
            "Minimal" -> listOf(
                Color(0xFFFFFFFF),
                Color(0xFFCFD8DC),
                Color(0xFFB0BEC5)
            )
            else -> listOf(
                Color(0xFFB3E5FC),
                Color(0xFF80CBC4),
                Color(0xFFE3F2FD)
            )
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Title
            Text(
                text = "Create Mood",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Describe the vibe you want to create. For example: \"Tropical getaway\" or \"Cozy winter evening\".",
                style = MaterialTheme.typography.bodyMedium
            )

            // Prompt input
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe your mood / idea") },
                placeholder = { Text("Tropical getaway, Cyberpunk city...") },
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
            )

            // Vibe chips
            Text(text = "Vibe type", fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Calm", "Bright", "Dark", "Minimal").forEach { vibe ->
                    FilterChipSimple(
                        text = vibe,
                        selected = selectedVibe == vibe,
                        onClick = { selectedVibe = vibe }
                    )
                }
            }

            // Generate preview button
            Button(
                onClick = {
                    hasPreview = prompt.isNotBlank()
                },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate preview")
            }

            // Preview card
            if (hasPreview) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (prompt.isBlank()) "Untitled Mood" else prompt,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Vibe: $selectedVibe",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "This is a preview of your mood board. In a later sprint, this will pull real images and colours using AI & APIs.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            previewColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = color,
                                            shape = MaterialTheme.shapes.small
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    val nextId = (MoodBoardRepository.boards.maxOfOrNull { it.id } ?: 0) + 1
                    val moodBoard = MoodBoard(
                        id = nextId,
                        title = if (prompt.isBlank()) "Untitled Mood" else prompt,
                        description = "A mood board for: $prompt",
                        moodTag = selectedVibe,
                        imageCount = 0, // later you'll update when real images are added
                        dominantColors = previewColors,
                        lastUpdated = "Just now",
                        isFavorite = false
                    )
                    onSave(moodBoard)
                },
                enabled = hasPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Mood Board")
            }
        }
    }
}

@Composable
fun FilterChipSimple(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateMoodPreview() {
    VibeAITheme {
        CreateMoodScreen()
    }
}
