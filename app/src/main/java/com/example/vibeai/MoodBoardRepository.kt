package com.example.vibeai

import androidx.compose.runtime.mutableStateListOf

object MoodBoardRepository {
    // Shared list of mood boards seen by Home & CreateMood
    val boards = mutableStateListOf<MoodBoard>().apply {
        addAll(sampleMoodBoards()) // start with your sample data
    }
}
