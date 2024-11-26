package com.example.fyp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.repository.PostImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostImageViewModel(private val repository: PostImageRepository) : ViewModel() {

    fun uploadImages(
        postID: String,
        uris: List<Uri>,
        userID: String,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.uploadImages(postID, uris, userID) { success, exception ->
                onComplete(success, exception)
            }
        }
    }
}
