package com.example.vibeai

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object MoodBoardRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    // Compose will recompose when this list updates
    val boards = mutableStateListOf<MoodBoard>()

    private fun userBoardsRef(userId: String) =
        db.collection("users").document(userId).collection("moodBoards")

    fun startListening(userId: String) {
        stopListening()

        listener = userBoardsRef(userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.map { doc ->
                    MoodBoard(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        moodTag = doc.getString("moodTag") ?: "",
                        imageCount = (doc.getLong("imageCount") ?: 0L).toInt(),
                        dominantColors = (doc.get("dominantColors") as? List<*>)?.mapNotNull { it as? String }
                            ?: emptyList(),
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        isFavorite = doc.getBoolean("isFavorite") ?: false
                    )
                }

                boards.clear()
                boards.addAll(list)
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
        boards.clear()
    }

    fun toggleFavorite(userId: String, board: MoodBoard) {
        if (board.id.isBlank()) return
        userBoardsRef(userId).document(board.id)
            .update(
                mapOf(
                    "isFavorite" to !board.isFavorite,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
    }

    // Optional: if you want delete later
    fun deleteMoodBoard(userId: String, boardId: String) {
        if (boardId.isBlank()) return
        userBoardsRef(userId).document(boardId).delete()
    }

    fun addBoard(
        userId: String,
        board: MoodBoard,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        userBoardsRef(userId)
            .add(
                mapOf(
                    "title" to board.title,
                    "description" to board.description,
                    "moodTag" to board.moodTag,
                    "imageCount" to board.imageCount,
                    "dominantColors" to board.dominantColors,
                    "updatedAt" to System.currentTimeMillis(),
                    "isFavorite" to board.isFavorite
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}