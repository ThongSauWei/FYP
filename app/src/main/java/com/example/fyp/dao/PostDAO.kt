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

    suspend fun getPostByUser(userID: String): List<Post> = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
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

    fun deletePost(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        // Remove the post
        dbRef.child(postID).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null) // Post deleted successfully
            } else {
                onComplete(false, task.exception) // Deletion failed
            }
        }
    }
    fun deletePostWithAssociations(
        postID: String,
        postImageDAO: PostImageDAO,
        postCategoryDAO: PostCategoryDAO,
        postCommentDAO: PostCommentDAO,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        dbRef.child(postID).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Delete associated data
                postImageDAO.deleteImagesByPostID(postID) { imageSuccess, _ ->
                    postCategoryDAO.deleteCategoriesByPostID(postID) { categorySuccess, _ ->
                        postCommentDAO.deleteCommentsByPostID(postID) { commentSuccess, _ ->
                            if (imageSuccess && categorySuccess && commentSuccess) {
                                onComplete(true, null) // All deletions successful
                            } else {
                                onComplete(false, Exception("Failed to delete some associations"))
                            }
                        }
                    }
                }
            } else {
                onComplete(false, task.exception)
            }
        }
    }


}
