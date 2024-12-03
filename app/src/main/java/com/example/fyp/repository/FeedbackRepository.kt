package com.example.fyp.repository

import com.example.fyp.dao.FeedbackDAO
import com.example.fyp.data.Feedback

class FeedbackRepository(private val feedbackDAO: FeedbackDAO) {
    suspend fun addFeedback(feedback: Feedback) {
        feedbackDAO.addFeedback(feedback)
    }

    suspend fun updateFeedback(feedback: Feedback) {
        feedbackDAO.updateFeedback(feedback)
    }

    suspend fun getFeedbackByUserID(userID: String): Feedback? {
        return feedbackDAO.getFeedbackByUserID(userID)
    }

    suspend fun getAllFeedback(): List<Feedback> {
        return feedbackDAO.getAllFeedback()
    }
}
