package com.example.fyp.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.UserDAO
import com.example.fyp.data.User
import com.example.fyp.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(application : Application) : AndroidViewModel(application) {
    private val userRepository : UserRepository
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        val userDao = UserDAO()
        userRepository = UserRepository(userDao)
    }

    fun addUser(user : User) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.addUser(user)
        }
    }



    fun updateUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateUser(user)
        }
    }

    suspend fun getUserByID(userID : String) : User? {

        return userRepository.getUserByID(userID)
    }

    suspend fun getUserByLogin(userEmail : String, userPassword : String) : User? {

        return userRepository.getUserByLogin(userEmail, userPassword)
    }

    fun deleteUser(userID : String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteUser(userID)
        }
    }

    suspend fun getUserByEmail(userEmail: String): User? {
        return userRepository.getUserByEmail(userEmail)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Log.d("PasswordReset", "Password reset email sent successfully to $email")
        } catch (e: Exception) {
            Log.e("PasswordReset", "Error sending password reset email: ${e.message}")
            throw Exception("Failed to send password reset email")
        }
    }



    suspend fun searchUser(searchText : String) : List<User> {
        return userRepository.searchUser(searchText)
    }

    fun hashPassword(password: String): String {
        return userRepository.hashPassword(password)
    }

    suspend fun isEmailRegistered(email: String): Boolean {
        return userRepository.isEmailRegistered(email)
    }

    fun saveToken(email: String, token: String, expirationTime: Long, callback: (Boolean) -> Unit) {
        userRepository.saveToken(email, token, expirationTime, callback)
    }

    fun validateToken(email: String, token: String, callback: (Boolean) -> Unit) {
        userRepository.validateToken(email, token, callback)
    }

    fun deleteToken(email: String, callback: (Boolean) -> Unit) {
        userRepository.deleteToken(email, callback)
    }

    suspend fun searchUsers(searchText: String): List<User> {
        return userRepository.searchUser(searchText)
    }

    suspend fun getAllUsers(): List<User> {
        return userRepository.getAllUsers()
    }

}
