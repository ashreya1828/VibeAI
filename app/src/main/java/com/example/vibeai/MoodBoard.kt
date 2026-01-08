package com.example.vibeai

/**
 * Firestore-friendly model:
 * - dominantColors stored as HEX strings (e.g. "#FFAA00")
 * - updatedAt stored as epoch millis (Long)
 * - id is Firestore document id
 */
data class MoodBoard(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val moodTag: String = "",
    val imageCount: Int = 0,
    val dominantColors: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val isFavorite: Boolean = false
)
