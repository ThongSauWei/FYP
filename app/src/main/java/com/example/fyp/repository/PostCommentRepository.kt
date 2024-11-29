package com.example.fyp.repository

import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.data.PostComment

class PostCommentRepository(private val postCommentDao : PostCommentDAO) {

    fun addPostComment(postComment : PostComment) {
        postCommentDao.addPostComment(postComment)
    }

    suspend fun getPostComment(postID : String) : List<PostComment> {
        return postCommentDao.getPostComment(postID)
    }

    fun deletePostComment(postCommentID : String) {
        postCommentDao.deletePostComment(postCommentID)
    }
}