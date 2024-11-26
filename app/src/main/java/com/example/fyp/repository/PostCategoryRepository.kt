package com.example.fyp.repository

import com.example.fyp.dao.PostCategoryDAO

class PostCategoryRepository(private val postCategoryDAO: PostCategoryDAO) {

    fun addCategories(postID: String, categories: List<String>, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.addCategories(postID, categories, userID) { success, exception ->
            onComplete(success, exception)
        }
    }
}
