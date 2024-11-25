package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.ProfileDAO
import com.example.fyp.data.Profile
import com.example.fyp.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(application : Application) : AndroidViewModel(application) {
    private val profileRepository : ProfileRepository

    private var userIDList = ArrayList<String>()

    init {
        val profileDao = ProfileDAO()
        profileRepository = ProfileRepository(profileDao)
    }

    fun addProfile(profile : Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            profileRepository.addProfile(profile)
        }
    }

    suspend fun getProfile(userID : String) : Profile? {
        return profileRepository.getProfile(userID)
    }

    suspend fun getUserListByCourse(course : String) : List<String> {
        userIDList = ArrayList(profileRepository.getUserListByCourse(course))

        return if (userIDList.size < 10) {
            profileRepository.getRemainingUsers(userIDList)
        } else {
            userIDList
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch(Dispatchers.IO) {
            profileRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(userID : String) {
        viewModelScope.launch(Dispatchers.IO) {
            profileRepository.deleteProfile(userID)
        }
    }
}