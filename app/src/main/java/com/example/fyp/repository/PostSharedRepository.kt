package com.example.fyp.repository

import com.example.fyp.dao.PostSharedDAO

class PostSharedRepository(private val postSharedDAO: PostSharedDAO) {

    fun addSharedPost(
        postID: String,
        userID: String,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        postSharedDAO.addSharedPost(postID, userID, onComplete)
    }
}
