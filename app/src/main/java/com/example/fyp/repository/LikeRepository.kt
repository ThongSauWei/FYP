package com.example.fyp.repository

import com.example.fyp.dao.LikeDAO
import com.example.fyp.data.Like

class LikeRepository(private val likeDAO: LikeDAO) {

    suspend fun getLikeCountByPostID(postID: String): Int {
        return likeDAO.getLikeCountByPostID(postID)
    }

    suspend fun saveLike(like: Like) {
        likeDAO.saveLike(like)
    }

    suspend fun getLikeByUserIDAndPostID(userID: String, postID: String): Like? {
        return likeDAO.getLikeByUserIDAndPostID(userID, postID)
    }

    suspend fun updateLikeStatus(like: Like) {
        likeDAO.updateLikeStatus(like)
    }
}
