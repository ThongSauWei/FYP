package com.example.fyp.dao

import com.example.fyp.data.Feedback
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FeedbackDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Feedback")

    private var nextID = 1000

    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }

    // Add feedback with auto-generated feedback ID
    suspend fun addFeedback(feedback: Feedback) = withContext(Dispatchers.IO) {
        if (feedback.feedbackID.isEmpty()) {
            feedback.feedbackID = "FB$nextID"
            nextID++
        }
        dbRef.child(feedback.feedbackID).setValue(feedback).await()
    }

    suspend fun updateFeedback(feedback: Feedback) = withContext(Dispatchers.IO) {
        dbRef.child(feedback.feedbackID).setValue(feedback).await()
    }

    private suspend fun getNextID(): Int {
        var id = 1000
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()
        if (snapshot.exists()) {
            for (feedbackSnapshot in snapshot.children) {
                val lastFeedbackID = feedbackSnapshot.key?.substring(2)?.toIntOrNull() // Safely parse as int
                if (lastFeedbackID != null && lastFeedbackID >= id) {
                    id = lastFeedbackID + 1
                }
            }
        }
        return id
    }

    // Get all feedback
    suspend fun getAllFeedback(): List<Feedback> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val feedbackList = mutableListOf<Feedback>()
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (feedbackSnapshot in snapshot.children) {
                            val feedback = feedbackSnapshot.getValue(Feedback::class.java)
                            if (feedback != null) {
                                feedbackList.add(feedback)
                            }
                        }
                    }
                    continuation.resume(feedbackList)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    // Get Feedback by UserID
    suspend fun getFeedbackByUserID(userID: String): Feedback? = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            dbRef.orderByChild("userID").equalTo(userID)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (feedbackSnapshot in snapshot.children) {
                                val feedback = feedbackSnapshot.getValue(Feedback::class.java)
                                if (feedback != null) {
                                    continuation.resume(feedback)
                                    return
                                }
                            }
                        }
                        continuation.resume(null)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
        }
    }
}
