package com.example.fyp.dao

import com.example.fyp.data.PostViewHistory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PostViewHistoryDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostViewHistory")
    private val counterRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Counters/PostViewHistory")

    // Check if a view history record exists for a given postID and userID
    fun checkIfViewHistoryExists(postID: String, userID: String, onComplete: (Boolean, PostViewHistory?) -> Unit) {
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var exists = false
                var existingHistory: PostViewHistory? = null
                for (viewSnapshot in snapshot.children) {
                    val viewHistory = viewSnapshot.getValue(PostViewHistory::class.java)
                    if (viewHistory?.userID == userID) {
                        exists = true
                        existingHistory = viewHistory
                        break
                    }
                }
                onComplete(exists, existingHistory)
            }

            override fun onCancelled(error: DatabaseError) {
                onComplete(false, null)  // In case of error, assume no history
            }
        })
    }

    // Add or update view history
    fun addOrUpdateViewHistory(viewHistory: PostViewHistory, onComplete: (Exception?) -> Unit) {
        if (viewHistory.viewID.isNotEmpty()) {
            // If viewID exists, update the timestamp
            dbRef.child(viewHistory.viewID).setValue(viewHistory).addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception)
            }
        } else {
            // Create new view history with sequential ID
            counterRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentCounter = currentData.getValue(Int::class.java) ?: 999
                    val newCounter = currentCounter + 1
                    currentData.value = newCounter
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed && currentData != null) {
                        val newViewID = "V${currentData.value}"
                        viewHistory.viewID = newViewID
                        dbRef.child(newViewID).setValue(viewHistory).addOnCompleteListener { task ->
                            onComplete(if (task.isSuccessful) null else task.exception)
                        }
                    } else {
                        onComplete(error?.toException())
                    }
                }
            })
        }
    }

    // Get post view history for a user
    suspend fun getPostHistoryForUser(userID: String): List<PostViewHistory> {
        return withContext(Dispatchers.IO) {
            // Use suspendCoroutine to handle the Firebase callback asynchronously
            suspendCoroutine { continuation ->
                val postHistoryList = mutableListOf<PostViewHistory>()
                dbRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (data in snapshot.children) {
                            val postViewHistory = data.getValue(PostViewHistory::class.java)
                            if (postViewHistory != null) {
                                postHistoryList.add(postViewHistory)
                            }
                        }
                        continuation.resume(postHistoryList) // Return the postHistoryList
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException()) // Handle error properly
                    }
                })
            }
        }
    }

}
