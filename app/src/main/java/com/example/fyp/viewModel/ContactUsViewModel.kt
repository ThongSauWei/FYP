package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fyp.data.ContactUs
import com.example.fyp.repository.ContactUsRepository

class ContactUsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactUsRepository = ContactUsRepository()

    suspend fun addContactUs(contactUs: ContactUs) {
        repository.addContactUs(contactUs)
    }

    fun sendEmailToSupport(toEmail: String, subject: String, name: String, email: String, problemType: String, description: String, dateTime: String
    ) {
        repository.sendEmail(toEmail, subject, name, email, problemType, description, dateTime)
    }
}
