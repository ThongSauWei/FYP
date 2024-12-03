package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.FeedbackDAO
import com.example.fyp.data.Feedback
import com.example.fyp.repository.FeedbackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val feedbackRepository: FeedbackRepository

    init {
        val feedbackDAO = FeedbackDAO()
        feedbackRepository = FeedbackRepository(feedbackDAO)
    }

    fun addFeedback(feedback: Feedback) {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackRepository.addFeedback(feedback)
        }
    }

    fun updateFeedback(feedback: Feedback) {
        viewModelScope.launch(Dispatchers.IO) {
            feedbackRepository.updateFeedback(feedback)
        }
    }

    suspend fun getFeedbackByUserID(userID: String): Feedback? {
        return feedbackRepository.getFeedbackByUserID(userID)
    }

    suspend fun getAllFeedback(): List<Feedback> {
        return feedbackRepository.getAllFeedback()
    }
}
