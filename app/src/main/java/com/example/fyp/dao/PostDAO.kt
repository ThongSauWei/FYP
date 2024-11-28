package com.example.fyp.dao

import com.example.fyp.data.Post
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Post")

    fun addPost(post: Post, onComplete: (String?, Exception?) -> Unit) {
        SaveSharedPreference.getNextID(dbRef, "P") { newPostID ->
            post.postID = newPostID
            dbRef.child(post.postID).setValue(post).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(post.postID, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
        }
    }

    suspend fun getAllPost(): List<Post> = suspendCancellableCoroutine { continuation ->
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()
                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        postSnapshot.getValue(Post::class.java)?.let { posts.add(it) }
                    }
                }
                continuation.resume(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    suspend fun getPostByID(postID: String): Post? = suspendCancellableCoroutine { continuation ->
        dbRef.child(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.getValue(Post::class.java)
                continuation.resume(post)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }
}
