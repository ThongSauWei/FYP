package com.example.fyp.repository

import com.example.fyp.dao.PostDAO
import com.example.fyp.data.Post

class PostRepository(private val postDao : PostDAO) {

    fun addPost(post: Post, onComplete: (String?, Exception?) -> Unit) {
        postDao.addPost(post) { postID, exception ->
            if (exception == null) {
                // Post was successfully added
                onComplete(postID, null)
            } else {
                // Handle error
                onComplete(null, exception)
            }
        }
    }

}