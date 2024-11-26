package com.example.fyp.repository

import com.example.fyp.dao.UserDAO
import com.example.fyp.data.User
import java.security.MessageDigest

class UserRepository(private val userDAO: UserDAO) {
    
    //hash password
    suspend fun addUser(user : User) {
        // hash password here
        val hashedPassword = hashPassword(user.password) //hash the password before save to db
        user.password = hashedPassword
        userDAO.addUser(user)
    }

    suspend fun updateUser(user: User){
        userDAO.updateUser(user)
    }

    fun deleteUser(userID: String) {
        userDAO.deleteUser(userID)
    }

    suspend fun searchUser(searchText : String) : List<User> {
        return userDAO.searchUser(searchText)
    }

    suspend fun getUserByID(userID: String): User? {
        return userDAO.getUserByID(userID)
    }

    suspend fun getUserByEmail(userEmail: String): User? {
        return userDAO.getUserByEmail(userEmail)
    }

    suspend fun getUserByLogin(userEmail: String, userPassword: String): User? {
        // hash password here
        val hashedPassword = hashPassword(userPassword)
        return userDAO.getUserByLogin(userEmail, hashedPassword)
    }

    suspend fun isEmailRegistered(email: String): Boolean {
        return userDAO.isEmailRegistered(email)
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256") //user256 algo
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}