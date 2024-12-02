package com.example.fyp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.FriendDAO
import com.example.fyp.data.Friend
import com.example.fyp.repository.FriendRepository
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    private val friendRepository: FriendRepository

    private val _friendList = MutableLiveData<List<Friend>>()
    val friendList: LiveData<List<Friend>> get() = _friendList

    private val userID = SaveSharedPreference.getUserID(application.applicationContext)

    init {
        val friendDao = FriendDAO()
        friendRepository = FriendRepository(friendDao)
        fetchFriendList()
    }

    fun addFriend(friend: Friend) {
        viewModelScope.launch(Dispatchers.IO) {
            if (friendRepository.getFriend(friend.requestUserID, friend.receiveUserID) == null) {
                friendRepository.addFriend(friend)
                fetchFriendList() // Refresh the friend list after adding
            }
        }
    }

    suspend fun getFriendList(userID: String): List<Friend> {
        return friendRepository.getFriendList(userID)
    }

    suspend fun getFriend(userID_1: String, userID_2: String): Friend? {
        return friendRepository.getFriend(userID_1, userID_2)
    }

    fun updateFriend(friend: Friend) {
        viewModelScope.launch(Dispatchers.IO) {
            friendRepository.updateFriend(friend)
        }
    }

    fun deleteFriend(friendID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            friendRepository.deleteFriend(friendID)
            fetchFriendList()
        }
    }

    private fun fetchFriendList() {
        viewModelScope.launch {
            val newList = friendRepository.getFriendList(userID)
            _friendList.postValue(newList)
        }
    }

    fun observeFriendStatus(userID1: String, userID2: String, callback: (Friend?) -> Unit) {
        friendRepository.observeFriendStatus(userID1, userID2, callback)
    }

    suspend fun checkFriendDeleted(friendID: String): Boolean {
        return withContext(Dispatchers.IO) {
            val snapshot = friendRepository.getFriendByID(friendID) // Add this in FriendRepository
            snapshot == null // If null, it was successfully deleted
        }
    }
}
