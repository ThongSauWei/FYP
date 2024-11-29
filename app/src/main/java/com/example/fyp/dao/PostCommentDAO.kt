package com.example.fyp.dao

import com.example.fyp.data.PostComment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostCommentDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostComment")

    suspend fun getCommentCountByPostID(postID: String): Int = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    fun deleteCommentsByPostID(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        dbRef.orderByChild("postID").equalTo(postID).get().addOnSuccessListener { snapshot ->
            val tasks = snapshot.children.map { it.ref.removeValue() }
            Tasks.whenAllComplete(tasks).addOnCompleteListener { task ->
                if (task.isSuccessful) onComplete(true, null)
                else onComplete(false, task.exception)
            }
        }
    }

    fun deleteComment(postCommentID: String) {
        dbRef.child(postCommentID).removeValue()
    }

    suspend fun getCommentsByPostID(postID: String): List<PostComment> = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<PostComment>()
                if (snapshot.exists()) {
                    for (commentSnapshot in snapshot.children) {
                        commentSnapshot.getValue(PostComment::class.java)?.let { comments.add(it) }
                    }
                }
                continuation.resume(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }


}

