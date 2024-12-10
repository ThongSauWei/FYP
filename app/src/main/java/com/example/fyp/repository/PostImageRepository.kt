package com.example.fyp.repository

import android.net.Uri
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.PostImage

class PostImageRepository(private val postImageDAO: PostImageDAO) {

    fun uploadImages(
        postID: String,
        uris: List<Uri>,
        userID: String,
        callback: (Boolean, Exception?) -> Unit
    ) {
        postImageDAO.uploadImages(postID, uris, userID) { success, exception ->
            callback(success, exception)
        }
    }

    suspend fun getImagesByPostID(postID: String): List<PostImage> {
        return postImageDAO.getImagesByPostID(postID)
    }

    fun deleteImagesByPostID(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postImageDAO.deleteImagesByPostID(postID, onComplete)
    }

    fun deleteImageByID(imageID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postImageDAO.deleteImageByID(imageID, onComplete)
    }

}
