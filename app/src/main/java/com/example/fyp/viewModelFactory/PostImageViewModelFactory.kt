package com.example.fyp.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fyp.repository.PostImageRepository
import com.example.fyp.viewModel.PostImageViewModel

class PostImageViewModelFactory(
    private val repository: PostImageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostImageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostImageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
