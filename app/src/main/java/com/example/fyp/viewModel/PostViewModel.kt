package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fyp.dao.PostDAO
import com.example.fyp.data.Post
import com.example.fyp.repository.PostRepository

class PostViewModel(application : Application) : AndroidViewModel(application) {
    val postRepository : PostRepository

    init {
        val postDao = PostDAO()
        postRepository = PostRepository(postDao)
    }

    fun addPost(post: Post, onPostCreated: (String?, Exception?) -> Unit) {
        postRepository.addPost(post) { postID, exception ->
            if (postID != null) {
                // Handle success
                onPostCreated(postID, null)
            } else {
                // Handle failure
                onPostCreated(null, exception)
            }
        }
    }

    // Method to get posts by a list of user IDs (for fetching posts of friends)
    suspend fun getPostsByUserIDs(userIDs: List<String>): List<Post> {
        val posts = mutableListOf<Post>()
        for (userID in userIDs) {
            posts.addAll(postRepository.getPostByUser(userID))
        }
        return posts
    }

    suspend fun getPostByID(postID: String) : Post? {
        return postRepository.getPostByID(postID)
    }

    suspend fun getAllPosts(): List<Post> {
        return postRepository.getAllPosts()
    }




}