package com.example.fyp.repository

import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.data.Post

class PostCategoryRepository(private val postCategoryDAO: PostCategoryDAO) {

    fun addCategories(postID: String, categories: List<String>, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.addCategories(postID, categories, userID) { success, exception ->
            onComplete(success, exception)
        }
    }

    suspend fun getPostByCategory(category: String): List<Post> {
        return postCategoryDAO.getPostByCategory(category)
    }

}
