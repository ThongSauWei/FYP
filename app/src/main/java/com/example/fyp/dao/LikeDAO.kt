package com.example.fyp.dao

import com.example.fyp.data.Like
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LikeDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Like")
    private val counterRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("LikeCounter")

    suspend fun getLikeCountByPostID(postID: String): Int = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var likeCount = 0

                // Iterate over the snapshot and count likes with status == 1
                for (dataSnapshot in snapshot.children) {
                    val like = dataSnapshot.getValue(Like::class.java)
                    if (like?.status == 1) {
                        likeCount++
                    }
                }

                continuation.resume(likeCount)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }


    suspend fun saveLike(like: Like) = suspendCancellableCoroutine<Unit> { continuation ->
        // Generate the sequential likeID starting from 1000
        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.getValue(Int::class.java) ?: 0
                val newCount = currentCount + 1
                currentData.value = newCount
                like.likeID = "L${1000 + newCount}" // Generate the sequential ID starting from L1000
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    // Save the Like object with the generated likeID
                    dbRef.child(like.likeID).setValue(like).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
                        }
                    }
                } else {
                    continuation.resumeWithException(error?.toException() ?: Exception("Transaction failed"))
                }
            }
        })
    }

    // Fetch the like by userID and postID
    suspend fun getLikeByUserIDAndPostID(userID: String, postID: String): Like? = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var like: Like? = null
                for (data in snapshot.children) {
                    val tempLike = data.getValue(Like::class.java)
                    if (tempLike?.postID == postID) {
                        like = tempLike
                        break
                    }
                }
                continuation.resume(like)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    // Update like status
    suspend fun updateLikeStatus(like: Like) = suspendCancellableCoroutine<Unit> { continuation ->
        dbRef.child(like.likeID).setValue(like).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Error updating like status"))
            }
        }
    }
}


