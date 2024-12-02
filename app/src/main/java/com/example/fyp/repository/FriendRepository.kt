package com.example.fyp.repository

import com.example.fyp.dao.FriendDAO
import com.example.fyp.data.Friend

class FriendRepository(private val friendDao : FriendDAO) {

    suspend fun addFriend(friend : Friend) {
        friendDao.addFriend(friend)
    }

    suspend fun getFriendList(userID : String) : List<Friend> {
        return friendDao.getFriendList(userID)
    }

    suspend fun getFriend(userID_1 : String, userID_2 : String) : Friend? {
        return friendDao.getFriend(userID_1, userID_2)
    }

    fun updateFriend(friend : Friend) {
        friendDao.updateFriend(friend)
    }

    fun deleteFriend(friendID : String) {
        friendDao.deleteFriend(friendID)
    }

    fun observeFriendStatus(userID1: String, userID2: String, callback: (Friend?) -> Unit) {
        friendDao.observeFriendStatus(userID1, userID2, callback)
    }

    suspend fun getFriendByID(friendID: String): Friend? {
        return friendDao.getFriendByID(friendID)
    }

    fun observeFriendList(userID: String, callback: (List<Friend>) -> Unit) {
        friendDao.observeFriendList(userID, callback)
    }

}