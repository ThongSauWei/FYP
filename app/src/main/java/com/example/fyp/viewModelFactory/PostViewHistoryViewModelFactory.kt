package com.example.fyp.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fyp.repository.PostViewHistoryRepository
import com.example.fyp.viewModel.PostViewHistoryViewModel

class PostViewHistoryViewModelFactory(
    private val postViewHistoryRepository: PostViewHistoryRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewHistoryViewModel::class.java)) {
            return PostViewHistoryViewModel(postViewHistoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
