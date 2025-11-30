package com.example.vibeai

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
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
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentUser = auth.currentUser

        // If user is not logged in → go back to Login
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userId = currentUser.uid

        // Start listening to this user's mood boards in Firestore
        MoodBoardRepository.startListening(userId)

        setContent {
            VibeAITheme {
                HomeScreen(
                    userEmail = currentUser.email,
                    userId = userId,
                    onCreateMoodClick = {
                        startActivity(Intent(this, CreateMoodActivity::class.java))
                    },
                    onLogoutClick = {
                        // Stop Firestore listener, sign out, go to Login
                        MoodBoardRepository.stopListening()
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safety: stop listening when Activity is destroyed
        MoodBoardRepository.stopListening()
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
fun HomeScreen(
    userEmail: String? = null,
    userId: String? = null,
    onCreateMoodClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    // All boards that Firestore is keeping in sync
    val moodBoards = MoodBoardRepository.boards

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }

    val filteredBoards = moodBoards.filter { board ->
        val matchesSearch = searchQuery.isBlank() ||
                board.title.contains(searchQuery, ignoreCase = true) ||
                board.moodTag.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            HomeFilter.ALL -> true
            HomeFilter.FAVOURITES -> board.isFavorite
            HomeFilter.RECENT -> true // later you can sort/filter by date
        }

        matchesSearch && matchesFilter
    }

    val totalBoards = moodBoards.size
    val favouriteCount = moodBoards.count { it.isFavorite }
    val latestBoard = moodBoards.maxByOrNull { it.id } // simple way to get "most recent"

    Scaffold(
        topBar = {
            HomeTopBar(
                userEmail = userEmail,
                onLogoutClick = onLogoutClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMoodClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create mood board"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Small stats row – live data from Firestore
            Text(
                text = "You have $totalBoards boards · $favouriteCount favourites",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Inspiration card now uses *real* boards when available
            InspirationCard(latestBoard = latestBoard)

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
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
                        text = "No mood boards yet.\nTap the + button to create your first one!",
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
                                // later: open mood board detail / editor
                            },
                            onToggleFavorite = {
                                // Update favourite flag in Firestore (UI will refresh via listener)
                                if (userId != null) {
                                    MoodBoardRepository.toggleFavorite(userId, board)
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
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- TOP BAR ----------

@Composable
fun HomeTopBar(
    userEmail: String?,
    onLogoutClick: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF7F00FF),
        Color(0xFF3F51B5)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors))
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "VibeAI Mood Boards",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                userEmail?.let {
                    Text(
                        text = "Signed in as $it",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Log out",
                    tint = Color.White
                )
            }
        }
    }
}

// ---------- INSPIRATION CARD (DYNAMIC) ----------

@Composable
fun InspirationCard(latestBoard: MoodBoard?) {
    val gradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF8E2DE2),
            Color(0xFF4A00E0)
        )
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Inspiration",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (latestBoard != null) {
                    Text(
                        text = "Revisit \"${latestBoard.title}\" or create a new board with a similar vibe (${latestBoard.moodTag}).",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Start by creating your first mood board. Think of a theme or feeling you want to explore today.",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
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
            modifier = Modifier.padding(16.dp)
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

                TextButton(onClick = { onToggleFavorite() }) {
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

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { /* later: filter by this tag */ },
                    label = { Text(board.moodTag) }
                )

                Text(
                    text = "${board.imageCount} images",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

// ---------- PREVIEW ----------

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    VibeAITheme {
        HomeScreen(
            userEmail = "demo@vibeai.com",
            userId = null // no Firestore calls in preview
        )
    }
}
