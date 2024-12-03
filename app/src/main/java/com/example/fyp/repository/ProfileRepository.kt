package com.example.fyp.repository

import com.example.fyp.dao.ProfileDAO
import com.example.fyp.data.Profile

class ProfileRepository(private val profileDao: ProfileDAO) {

    fun addProfile(profile: Profile) {
        profileDao.addProfile(profile)
    }

    suspend fun getProfile(userID: String): Profile? {
        return profileDao.getProfile(userID)
    }

    suspend fun getUserListByCourse(course: String): List<String> {
        return profileDao.getUserListByCourse(course)
    }

    suspend fun getRemainingUsers(userIDList: List<String>): List<String> {
        return profileDao.getRemainingUsers(userIDList)
    }

    fun deleteProfile(userID: String) {
        profileDao.deleteProfile(userID)
    }

    fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile)
    }

    // Correctly access the DAO instance here
    suspend fun getAllProfiles(): List<Profile> {
        return profileDao.getAllProfiles()
    }
}
