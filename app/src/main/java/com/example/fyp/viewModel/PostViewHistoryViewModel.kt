package com.example.fyp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.data.PostViewHistory
import com.example.fyp.repository.PostViewHistoryRepository
import kotlinx.coroutines.launch

class PostViewHistoryViewModel(private val postViewHistoryRepository: PostViewHistoryRepository) : ViewModel() {

    fun addOrUpdateViewHistory(postID: String, userID: String) {
        viewModelScope.launch {
            try {
                postViewHistoryRepository.addOrUpdateViewHistory(postID, userID)
            } catch (e: Exception) {
                Log.e("PostViewHistoryViewModel", "Error adding/updating view history", e)
            }
        }
    }
}


