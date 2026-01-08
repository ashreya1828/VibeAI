package com.example.vibeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.vibeai.ui.theme.VibeAITheme
import com.google.firebase.auth.FirebaseAuth

/**
 * UPDATED CreateMoodActivity + CreateMoodScreen:
 * ✅ Works with your NEW MoodBoard model:
 *    - id: String (Firestore doc id created automatically)
 *    - dominantColors: List<String> (HEX)
 *    - updatedAt: Long (handled by repository)
 *    - removed lastUpdated
 *
 * Works with UPDATED repository signature:
 *    addBoard(userId, board, onSuccess, onError)
 */
class CreateMoodActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Safety: if opened without login
        if (currentUser == null) {
            finish()
            return
        }

        val userId = currentUser.uid

        setContent {
            VibeAITheme {
                CreateMoodScreen(
                    onBack = { finish() },
                    onSave = { newBoard ->
                        MoodBoardRepository.addBoard(
                            userId = userId,
                            board = newBoard,
                            onSuccess = { finish() },
                            onError = { /* optionally show Snackbar/Toast */ }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMoodScreen(
    onBack: () -> Unit = {},
    onSave: (MoodBoard) -> Unit = {}
) {
    var prompt by remember { mutableStateOf("") }
    var selectedVibe by remember { mutableStateOf("Calm") }
    var hasPreview by remember { mutableStateOf(false) }

    // Preview palette (UI-only)
    val previewColors: List<Color> = remember(selectedVibe) {
        when (selectedVibe) {
            "Calm" -> listOf(Color(0xFFB3E5FC), Color(0xFF80CBC4), Color(0xFFE3F2FD))
            "Bright" -> listOf(Color(0xFFFFC107), Color(0xFFFF5722), Color(0xFFFFEB3B))
            "Dark" -> listOf(Color(0xFF263238), Color(0xFF455A64), Color(0xFF1A237E))
            "Minimal" -> listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0xFFB0BEC5))
            else -> listOf(Color(0xFFB3E5FC), Color(0xFF80CBC4), Color(0xFFE3F2FD))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Mood") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Describe the vibe you want to create. Example: “Tropical getaway” or “Cozy winter evening”.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Prompt input
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe your mood / idea") },
                placeholder = { Text("Tropical getaway, Cyberpunk city...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
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

            // Generate preview
            Button(
                onClick = { hasPreview = prompt.isNotBlank() },
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
                            text = prompt.ifBlank { "Untitled Mood" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(text = "Vibe: $selectedVibe", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Preview palette is generated from the selected vibe. Later you can replace this with real AI colours/images.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            previewColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = color, shape = MaterialTheme.shapes.small)
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
                    val title = prompt.ifBlank { "Untitled Mood" }
                    val colorsHex = previewColors.map { it.toHexString() }

                    val moodBoard = MoodBoard(
                        id = "", // Firestore will generate document id
                        title = title,
                        description = "A mood board for: $title",
                        moodTag = selectedVibe,
                        imageCount = 0,
                        dominantColors = colorsHex, // ✅ List<String> (HEX)
                        updatedAt = 0L,            // repo sets updatedAt on save
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
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Converts Compose Color to HEX string (#AARRGGBB)
 * Matches repository/model storage requirement: List<String>
 */
private fun Color.toHexString(): String {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

@Preview(showBackground = true)
@Composable
fun CreateMoodPreview() {
    VibeAITheme {
        CreateMoodScreen()
    }
}
