package com.example.vibeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibeai.ui.theme.VibeAITheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeAITheme {
                HomeScreen()
            }
        }
    }
}

// ---------- DATA MODEL ----------

data class MoodBoard(
    val id: Int,
    val title: String,
    val description: String,
    val moodTag: String,
    val imageCount: Int,
    val dominantColors: List<Color>,
    val lastUpdated: String,
    val isFavorite: Boolean
)

// ---------- HOME SCREEN ----------

@Composable
fun HomeScreen() {
    // Sample mood boards (in real app this will come from Room DB)
    var moodBoards by remember { mutableStateOf(sampleMoodBoards()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }

    val filteredBoards = moodBoards.filter { board ->
        val matchesSearch = searchQuery.isBlank() ||
                board.title.contains(searchQuery, ignoreCase = true) ||
                board.moodTag.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            HomeFilter.ALL -> true
            HomeFilter.FAVOURITES -> board.isFavorite
            HomeFilter.RECENT -> true // For now, we treat all as recent; later you can sort by date
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            HomeTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Navigate to "Create Mood" screen
                }
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("Search mood boards") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter row
            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mood boards list
            if (filteredBoards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No mood boards yet.\nTap + to create your first one!",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBoards, key = { it.id }) { board ->
                        MoodBoardCard(
                            board = board,
                            onClick = {
                                // TODO: Open mood board detail / editor
                            },
                            onToggleFavorite = {
                                moodBoards = moodBoards.map { existing ->
                                    if (existing.id == board.id) {
                                        existing.copy(isFavorite = !existing.isFavorite)
                                    } else existing
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------- FILTER SUPPORT ----------

enum class HomeFilter {
    ALL, FAVOURITES, RECENT
}

@Composable
fun FilterRow(
    selectedFilter: HomeFilter,
    onFilterSelected: (HomeFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            text = "All",
            selected = selectedFilter == HomeFilter.ALL,
            onClick = { onFilterSelected(HomeFilter.ALL) }
        )
        FilterChip(
            text = "Favourites",
            selected = selectedFilter == HomeFilter.FAVOURITES,
            onClick = { onFilterSelected(HomeFilter.FAVOURITES) }
        )
        FilterChip(
            text = "Recent",
            selected = selectedFilter == HomeFilter.RECENT,
            onClick = { onFilterSelected(HomeFilter.RECENT) }
        )
    }
}

@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- TOP BAR ----------

@Composable
fun HomeTopBar() {
    val gradientColors = listOf(
        Color(0xFF7F00FF),
        Color(0xFF3F51B5)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Brush.horizontalGradient(gradientColors)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "VibeAI Mood Boards",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ---------- CARD FOR MOOD BOARD ----------

@Composable
fun MoodBoardCard(
    board: MoodBoard,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = board.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = { onToggleFavorite() }
                ) {
                    Text(
                        text = if (board.isFavorite) "★" else "☆",
                        fontSize = 18.sp
                    )
                }
            }

            Text(
                text = board.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mood tag and image count
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { /* could filter by tag in future */ },
                    label = { Text(board.moodTag) }
                )

                Text(
                    text = "${board.imageCount} images",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Colour palette row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                board.dominantColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Last updated: ${board.lastUpdated}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ---------- SAMPLE DATA ----------

fun sampleMoodBoards(): List<MoodBoard> = listOf(
    MoodBoard(
        id = 1,
        title = "Tropical Getaway",
        description = "Bright, warm, and sunny beach-inspired mood board with palm trees and sunsets.",
        moodTag = "Tropical",
        imageCount = 8,
        dominantColors = listOf(
            Color(0xFFFFC107),
            Color(0xFFFF5722),
            Color(0xFF4CAF50)
        ),
        lastUpdated = "Today",
        isFavorite = true
    ),
    MoodBoard(
        id = 2,
        title = "Minimal Workspace",
        description = "Clean, neutral workspace with soft whites, woods, and calm tones.",
        moodTag = "Minimal",
        imageCount = 5,
        dominantColors = listOf(
            Color(0xFFB0BEC5),
            Color(0xFFFFFFFF),
            Color(0xFF795548)
        ),
        lastUpdated = "Yesterday",
        isFavorite = false
    ),
    MoodBoard(
        id = 3,
        title = "Cyberpunk City",
        description = "Neon-filled futuristic streets with deep purples, blues, and pinks.",
        moodTag = "Cyberpunk",
        imageCount = 10,
        dominantColors = listOf(
            Color(0xFF9C27B0),
            Color(0xFF2196F3),
            Color(0xFFE91E63)
        ),
        lastUpdated = "3 days ago",
        isFavorite = true
    )
)

// ---------- PREVIEW ----------

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    VibeAITheme {
        HomeScreen()
    }
}
