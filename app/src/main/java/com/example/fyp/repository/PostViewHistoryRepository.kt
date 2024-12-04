package com.example.fyp.repository

import android.util.Log
import com.example.fyp.dao.PostViewHistoryDAO
import com.example.fyp.data.PostViewHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostViewHistoryRepository(private val postViewHistoryDAO: PostViewHistoryDAO) {

    suspend fun addOrUpdateViewHistory(postID: String, userID: String) {
        val viewHistory = PostViewHistory(
            viewID = "",  // This will be set later
            postID = postID,
            userID = userID,
            timestamp = getCurrentTimestamp()
        )

        postViewHistoryDAO.checkIfViewHistoryExists(postID, userID) { exists, existingHistory ->
            if (exists) {
                // Update the existing view history timestamp
                viewHistory.viewID = existingHistory?.viewID ?: ""
                postViewHistoryDAO.addOrUpdateViewHistory(viewHistory) { error ->
                    if (error != null) {
                        Log.e("PostViewHistoryRepository", "Error updating view history", error)
                    }
                }
            } else {
                // Add a new view history
                postViewHistoryDAO.addOrUpdateViewHistory(viewHistory) { error ->
                    if (error != null) {
                        Log.e("PostViewHistoryRepository", "Error adding view history", error)
                    }
                }
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}


