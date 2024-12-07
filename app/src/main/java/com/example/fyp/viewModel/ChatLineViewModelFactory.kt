package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fyp.repository.ChatLineRepository

class ChatLineViewModelFactory(
    private val application: Application,
    private val repository: ChatLineRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatLineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatLineViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
