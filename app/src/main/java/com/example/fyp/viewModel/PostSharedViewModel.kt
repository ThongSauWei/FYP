package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.PostDAO
import com.example.fyp.dao.PostSharedDAO
import com.example.fyp.data.Post
import com.example.fyp.repository.PostRepository
import com.example.fyp.repository.PostSharedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostSharedViewModel(application : Application) : AndroidViewModel(application) {
    val postSharedRepository : PostSharedRepository

    init {
        val postSharedDAO = PostSharedDAO()
        postSharedRepository = PostSharedRepository(postSharedDAO)
    }

    fun addSharedPost(
        postID: String,
        userID: String,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            postSharedRepository.addSharedPost(postID, userID, onComplete)
        }
    }



}