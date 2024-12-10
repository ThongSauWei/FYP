package com.example.fyp.repository

import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory

class PostCategoryRepository(private val postCategoryDAO: PostCategoryDAO) {

    fun addCategories(postID: String, categories: List<String>, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.addCategories(postID, categories, userID) { success, exception ->
            onComplete(success, exception)
        }
    }

    suspend fun getPostByCategory(category: String): List<Post> {
        return postCategoryDAO.getPostByCategory(category)
    }

    fun deleteCategoriesByPostID(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.deleteCategoriesByPostID(postID, onComplete)
    }

    suspend fun getCategoriesByPostID(postID: String): List<PostCategory> {
        return postCategoryDAO.getCategoriesByPostID(postID)
    }

    fun updateCategories(postID: String, categories: List<String>, userID: String, callback: (Boolean) -> Unit) {
        postCategoryDAO.updateCategories(postID, categories, userID, callback)
    }

    fun addCategory(postID: String, category: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.addCategory(postID, category, userID, onComplete)
    }

    fun deleteCategoryByPostAndUser(postID: String, category: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postCategoryDAO.deleteCategoryByPostAndUser(postID, category, userID, onComplete)
    }

}
