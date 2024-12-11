package com.example.fyp.dao

import com.example.fyp.data.Post
import com.example.fyp.data.PostShared
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PostDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Post")
    private val postSharedRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostShared")

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

    suspend fun searchPost(searchText : String) : List<Post> = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine { continuation ->

            val postList = ArrayList<Post>()

            dbRef.orderByChild("postTitle")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (postSnapshot in snapshot.children) {
                                val postTitle = postSnapshot.child("postTitle").getValue(String::class.java)!!
                                if (postTitle.startsWith(searchText, true)) {
                                    val post = postSnapshot.getValue(Post::class.java)
                                    postList.add(post!!)
                                }
                            }
                        }

                        continuation.resume(postList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }

                })
        }
    }

    suspend fun getAllPost(): List<Post> = suspendCancellableCoroutine { continuation ->
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        postSnapshot.getValue(Post::class.java)?.let { post ->
                            posts.add(post)
                        }
                    }
                }

                // Sort posts by date in descending order
                val sortedPosts = posts.sortedByDescending { post ->
                    try {
                        dateFormat.parse(post.postDateTime)?.time ?: Long.MIN_VALUE
                    } catch (e: Exception) {
                        Long.MIN_VALUE
                    }
                }

                continuation.resume(sortedPosts)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }




    suspend fun getPostsForUser(currentUserID: String): List<Post> {
        val allPosts = getAllPost() // Assuming `getAllPost()` gives all posts

        val visiblePosts = mutableListOf<Post>()

        for (post in allPosts) {
            when (post.postType) {
                "Public" -> {
                    // Public posts are visible to everyone
                    visiblePosts.add(post)
                }
                "Private" -> {
                    // Private posts are visible only to the user who created it
                    if (post.userID == currentUserID) {
                        visiblePosts.add(post)
                    }
                }
                "Restricted" -> {
                    // Restricted posts require checking the PostShared table
                    val hasAccess = checkIfUserHasAccessToPost(currentUserID, post.postID)
                    if (hasAccess) {
                        visiblePosts.add(post)
                    }
                }
            }
        }

        return visiblePosts
    }

    suspend fun checkIfUserHasAccessToPost(userID: String, postID: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            postSharedRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasAccess = false
                    if (snapshot.exists()) {
                        for (sharedSnapshot in snapshot.children) {
                            val postShared = sharedSnapshot.getValue(PostShared::class.java)
                            if (postShared?.userID == userID) {
                                hasAccess = true
                                break
                            }
                        }
                    }
                    continuation.resume(hasAccess)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
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

    suspend fun updatePostActiveStatus(postID: String, newStatus: Int) {
        dbRef.child(postID).child("active").setValue(newStatus)
    }

    fun updatePost(post: Post, onComplete: (Boolean, Exception?) -> Unit) {
        dbRef.child(post.postID).setValue(post)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception)
            }
    }

}
