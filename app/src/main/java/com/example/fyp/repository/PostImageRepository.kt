package com.example.fyp.repository

import android.net.Uri
import com.example.fyp.dao.PostImageDAO

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
}
