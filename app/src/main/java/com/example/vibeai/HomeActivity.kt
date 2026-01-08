package com.example.vibeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibeai.ui.theme.VibeAITheme
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        MoodBoardRepository.stopListening()
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onOpenBoard = { boardId ->
                        startActivity(
                            Intent(this, MoodBoardDetailActivity::class.java)
                                .putExtra("boardId", boardId)
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MoodBoardRepository.stopListening()
    }
}

// ---------- HOME SCREEN ----------

enum class HomeFilter { ALL, FAVOURITES, RECENT }

@Composable
fun HomeScreen(
    userEmail: String? = null,
    userId: String? = null,
    onCreateMoodClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onOpenBoard: (String) -> Unit = {}
) {
    val moodBoards = MoodBoardRepository.boards

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val baseList = when (selectedFilter) {
        HomeFilter.ALL -> moodBoards
        HomeFilter.FAVOURITES -> moodBoards.filter { it.isFavorite }
        HomeFilter.RECENT -> moodBoards.sortedByDescending { it.updatedAt }
    }

    val filteredBoards = baseList.filter { board ->
        val matchesSearch =
            searchQuery.isBlank() ||
                    board.title.contains(searchQuery, ignoreCase = true) ||
                    board.moodTag.contains(searchQuery, ignoreCase = true)

        val matchesTag = selectedTag == null || board.moodTag.equals(selectedTag, ignoreCase = true)

        matchesSearch && matchesTag
    }

    val totalBoards = moodBoards.size
    val favouriteCount = moodBoards.count { it.isFavorite }
    val latestBoard = moodBoards.maxByOrNull { it.updatedAt }

    Scaffold(
        topBar = {
            HomeTopBar(userEmail = userEmail, onLogoutClick = onLogoutClick)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMoodClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create mood board")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "You have $totalBoards boards · $favouriteCount favourites",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            InspirationCard(latestBoard = latestBoard)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search mood boards") },
                singleLine = true
            )

            if (selectedTag != null) {
                Spacer(Modifier.height(10.dp))
                AssistChip(
                    onClick = { selectedTag = null },
                    label = { Text("Tag: $selectedTag (tap to clear)") }
                )
            }

            Spacer(Modifier.height(12.dp))

            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(Modifier.height(16.dp))

            if (filteredBoards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No mood boards found.\nTap the + button to create one!",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredBoards, key = { it.id }) { board ->
                        MoodBoardCard(
                            board = board,
                            onClick = { onOpenBoard(board.id) },
                            onToggleFavorite = {
                                if (userId != null) MoodBoardRepository.toggleFavorite(userId, board)
                            },
                            onTagClick = { tag ->
                                selectedTag = tag
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------- FILTER ROW ----------

@Composable
fun FilterRow(
    selectedFilter: HomeFilter,
    onFilterSelected: (HomeFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip("All", selectedFilter == HomeFilter.ALL) { onFilterSelected(HomeFilter.ALL) }
        FilterChip("Favourites", selectedFilter == HomeFilter.FAVOURITES) { onFilterSelected(HomeFilter.FAVOURITES) }
        FilterChip("Recent", selectedFilter == HomeFilter.RECENT) { onFilterSelected(HomeFilter.RECENT) }
    }
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
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

// ---------- TOP BAR ----------

@Composable
fun HomeTopBar(userEmail: String?, onLogoutClick: () -> Unit) {
    val gradientColors = listOf(Color(0xFF7F00FF), Color(0xFF3F51B5))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors))
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
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

// ---------- INSPIRATION CARD ----------

@Composable
fun InspirationCard(latestBoard: MoodBoard?) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))

    Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
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

// ---------- MOOD BOARD CARD ----------

@Composable
fun MoodBoardCard(
    board: MoodBoard,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTagClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = board.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(onClick = onToggleFavorite) {
                    Text(text = if (board.isFavorite) "★" else "☆", fontSize = 18.sp)
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
                    onClick = { onTagClick(board.moodTag) },
                    label = { Text(board.moodTag) }
                )

                Text(
                    text = "${board.imageCount} images",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                board.dominantColors.take(6).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(hexToColor(hex))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Updated: ${formatTime(board.updatedAt)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ---------- HELPERS ----------

private fun hexToColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000 or value).toInt())
            8 -> Color(value.toInt())
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}

private fun formatTime(epoch: Long): String {
    if (epoch <= 0L) return "—"
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(epoch))
}

// ---------- PREVIEW ----------

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    VibeAITheme {
        HomeScreen(
            userEmail = "demo@vibeai.com",
            userId = null,
            onOpenBoard = {}
        )
    }
}
