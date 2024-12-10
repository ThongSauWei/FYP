package com.example.fyp.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostDAO
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.repository.PostCategoryRepository
import com.example.fyp.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostCategoryViewModel(application : Application) : AndroidViewModel(application) {
    val postCategoryRepository : PostCategoryRepository

    init {
        val postCategoryDAO = PostCategoryDAO()
        postCategoryRepository = PostCategoryRepository(postCategoryDAO)
    }

    fun addCategories(postID: String, categories: List<String>, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            postCategoryRepository.addCategories(postID, categories, userID) { success, exception ->
                onComplete(success, exception)
            }
        }
    }

    suspend fun getPostByCategory(category: String): List<Post> {
        Log.d("PostCategoryViewModel", "Fetching posts for category: $category")
        val posts = postCategoryRepository.getPostByCategory(category)
        Log.d("PostCategoryViewModel", "Fetched ${posts.size} posts for category: $category")
        return posts
    }

    fun deleteCategoriesByPostID(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryRepository.deleteCategoriesByPostID(postID, onComplete)
    }

    suspend fun getCategoriesByPostID(postID: String): List<PostCategory> {
        return postCategoryRepository.getCategoriesByPostID(postID)
    }

    fun updateCategories(postID: String, categories: List<String>, userID: String) {
        viewModelScope.launch {
            postCategoryRepository.updateCategories(postID, categories, userID) { success ->
                if (!success) {
                    // Handle failure (e.g., log error or notify user)
                }
            }
        }
    }

    fun addCategory(postID: String, category: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryRepository.addCategory(postID, category, userID, onComplete)
    }

    fun deleteCategoryByPostAndUser(postID: String, category: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryRepository.deleteCategoryByPostAndUser(postID, category, userID, onComplete)
    }
}