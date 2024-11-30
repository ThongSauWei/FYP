package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.PostComment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PostCommentDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostComment")

    private var nextID = 100

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

    fun addPostComment(postComment: PostComment) {
        dbRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastPostCommentSnapshot = snapshot.children.lastOrNull()
                val lastPostCommentID = lastPostCommentSnapshot?.key?.substring(2)?.toIntOrNull() ?: 0
                val nextID = lastPostCommentID + 1
                postComment.postCommentID = "PC$nextID"
                dbRef.child(postComment.postCommentID).setValue(postComment)
                    .addOnCompleteListener {
                        Log.d("PostCommentDAO", "Comment added successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PostCommentDAO", "Failed to add comment.", e)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostCommentDAO", "Database operation cancelled.", error.toException())
            }
        })
    }

    suspend fun getPostComment(postID: String): List<PostComment> = suspendCoroutine { continuation ->
        val commentList = mutableListOf<PostComment>()
        val query = dbRef.orderByChild("postID").equalTo(postID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val comment = snapshot.getValue(PostComment::class.java)
                    comment?.let { commentList.add(it) }
                }
                continuation.resume(commentList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                continuation.resumeWithException(databaseError.toException())
            }
        })
    }

    fun deletePostComment(postCommentID : String) {
        dbRef.child(postCommentID).removeValue()
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
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

