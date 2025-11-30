package com.example.vibeai

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object MoodBoardRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Listener so we can stop it when not needed
    private var listenerRegistration: ListenerRegistration? = null

    // This list is what your HomeScreen & others will observe
    val boards: SnapshotStateList<MoodBoard> = mutableStateListOf()

    /**
     * Start listening to the current user's mood boards in Firestore.
     * Call this after login (we'll hook it into HomeActivity next).
     */
    fun startListening(userId: String) {
        // Remove old listener if any
        listenerRegistration?.remove()

        listenerRegistration = db.collection("users")
            .document(userId)
            .collection("boards")
            .orderBy("lastUpdatedTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // For now just ignore the error; you can log it if you like
                    return@addSnapshotListener
                }

                val newList = snapshot?.documents
                    ?.mapNotNull { doc -> docToMoodBoard(doc) }
                    ?: emptyList()

                boards.clear()
                boards.addAll(newList)
            }
    }

    /**
     * Stop listening (e.g. when user logs out).
     */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        boards.clear()
    }

    /**
     * Save a new mood board for this user into Firestore.
     * The list will update automatically because of the snapshot listener.
     */
    fun addBoard(userId: String, board: MoodBoard, onResult: (Boolean) -> Unit = {}) {
        val data = moodBoardToMap(board)

        db.collection("users")
            .document(userId)
            .collection("boards")
            .add(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Toggle favourite for this board in Firestore.
     * We find the document by the numeric 'id' field.
     */
    fun toggleFavorite(userId: String, board: MoodBoard) {
        val newValue = !board.isFavorite

        db.collection("users")
            .document(userId)
            .collection("boards")
            .whereEqualTo("id", board.id)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                val doc = query.documents.firstOrNull() ?: return@addOnSuccessListener
                doc.reference.update("isFavorite", newValue)
            }
    }

    /**
     * Delete a mood board from Firestore.
     */
    fun deleteBoard(userId: String, board: MoodBoard) {
        db.collection("users")
            .document(userId)
            .collection("boards")
            .whereEqualTo("id", board.id)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                val doc = query.documents.firstOrNull() ?: return@addOnSuccessListener
                doc.reference.delete()
            }
    }

    // --------- INTERNAL MAPPERS (MoodBoard <-> Firestore map) ---------

    private fun moodBoardToMap(board: MoodBoard): Map<String, Any> {
        return mapOf(
            // We use your existing Int id field (you generate it in CreateMoodScreen)
            "id" to board.id,
            "title" to board.title,
            "description" to board.description,
            "moodTag" to board.moodTag,
            "imageCount" to board.imageCount,
            "lastUpdated" to board.lastUpdated,
            // for ordering by time in Firestore
            "lastUpdatedTimestamp" to System.currentTimeMillis(),
            "isFavorite" to board.isFavorite,
            // Colors can’t be stored as Color, so we store them as Ints
            "colorInts" to board.dominantColors.map { it.value.toLong() }
        )
    }

    private fun docToMoodBoard(doc: DocumentSnapshot): MoodBoard? {
        val id = doc.getLong("id")?.toInt() ?: return null

        val title = doc.getString("title") ?: ""
        val description = doc.getString("description") ?: ""
        val moodTag = doc.getString("moodTag") ?: ""
        val imageCount = doc.getLong("imageCount")?.toInt() ?: 0
        val lastUpdated = doc.getString("lastUpdated") ?: ""
        val isFavorite = doc.getBoolean("isFavorite") ?: false

        val colorIntsAny = doc.get("colorInts") as? List<*>
        val colors = colorIntsAny
            ?.mapNotNull { it as? Number }
            ?.map { num -> Color(num.toInt()) }
            ?: emptyList()

        return MoodBoard(
            id = id,
            title = title,
            description = description,
            moodTag = moodTag,
            imageCount = imageCount,
            dominantColors = colors,
            lastUpdated = lastUpdated,
            isFavorite = isFavorite
        )
    }
}
