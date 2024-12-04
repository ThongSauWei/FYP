package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Post
import com.example.fyp.repository.PostRepository
import kotlinx.coroutines.launch

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

    suspend fun searchPost(searchText : String) : List<Post> {
        return postRepository.searchPost(searchText)
    }

    suspend fun getAllPosts(): List<Post> {
        return postRepository.getAllPosts()
    }

    suspend fun getPostByUser(userID : String) : List<Post> {
        return postRepository.getPostByUser(userID)
    }

    fun deletePost(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postRepository.deletePost(postID, onComplete)
    }
    fun deletePostWithAssociations(
        postID: String,
        postImageDAO: PostImageDAO,
        postCategoryDAO: PostCategoryDAO,
        postCommentDAO: PostCommentDAO,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        postRepository.deletePostWithAssociations(postID, postImageDAO, postCategoryDAO, postCommentDAO, onComplete)
    }

    suspend fun checkIfUserHasAccessToPost(userID: String, postID: String): Boolean {
        return postRepository.checkIfUserHasAccessToPost(userID, postID)
    }


}