package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.Chat
import com.example.fyp.data.Friend
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Friend")
    private val chatDAO = ChatDAO() // Initialize ChatDAO

    private var nextID = 1000

    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }


    suspend fun addFriend(friend: Friend) {
        Log.d("AddFriend", "Checking if the friend exists between ${friend.requestUserID} and ${friend.receiveUserID}")

        // If the friend relationship doesn't exist, we proceed to add
        if (getFriend(friend.requestUserID, friend.receiveUserID) == null) {
            friend.friendID = "F$nextID"
            nextID++

            Log.d("AddFriend", "Adding friend with ID: ${friend.friendID}")

            dbRef.child(friend.friendID).setValue(friend)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AddFriend", "Friend added successfully")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AddFriend", "Failed to add friend: ${exception.message}")
                }.await()
        } else {
            Log.d("AddFriend", "Friendship already exists.")
        }
    }


    suspend fun getFriendList(userID: String): List<Friend> = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<List<Friend>>()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendList = mutableListOf<Friend>()
                if (snapshot.exists()) {
                    for (friendSnapshot in snapshot.children) {
                        val friend = friendSnapshot.getValue(Friend::class.java)
                        if (friend != null && friend.status == "Friend" &&
                            (friend.requestUserID == userID || friend.receiveUserID == userID)
                        ) {
                            friendList.add(friend)
                        }
                    }
                }
                deferred.complete(friendList)
            }

            override fun onCancelled(error: DatabaseError) {
                deferred.completeExceptionally(error.toException())
            }
        })

        return@withContext deferred.await()
    }


    fun updateFriend(friend: Friend) {
        dbRef.child(friend.friendID).setValue(friend)
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    fun deleteFriend(friendID: String) {
        dbRef.child(friendID).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("DeleteFriend", "Successfully deleted friend with ID: $friendID")
                } else {
                    Log.e("DeleteFriend", "Failed to delete friend: ${task.exception?.message}")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteFriend", "Error deleting friend: ${exception.message}")
            }
    }


    suspend fun getFriend(userID1: String, userID2: String): Friend? {
        val snapshot = dbRef.get().await()
        for (friendSnapshot in snapshot.children) {
            val friend = friendSnapshot.getValue(Friend::class.java)
            if (friend != null &&
                ((friend.requestUserID == userID1 && friend.receiveUserID == userID2) ||
                        (friend.requestUserID == userID2 && friend.receiveUserID == userID1))
            ) {
                return friend
            }
        }
        return null
    }

    private suspend fun getNextID(): Int {
        var friendID = 1000
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (friendSnapshot in snapshot.children) {
                val lastFriendID = friendSnapshot.key!!
                friendID = lastFriendID.substring(1).toInt() + 1
            }
        }

        return friendID
    }

    suspend fun getPendingFriendRequests(receiveUserID: String): List<Friend> = withContext(Dispatchers.IO) {
        val pendingRequests = mutableListOf<Friend>()
        val snapshot = dbRef.orderByChild("receiveUserID").equalTo(receiveUserID).get().await()
        if (snapshot.exists()) {
            for (friendSnapshot in snapshot.children) {
                val friend = friendSnapshot.getValue(Friend::class.java)
                if (friend != null && friend.status == "Pending") {
                    pendingRequests.add(friend)
                }
            }
        }
        return@withContext pendingRequests
    }

    fun observeFriendStatus(userID1: String, userID2: String, callback: (Friend?) -> Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var friend: Friend? = null
                for (friendSnapshot in snapshot.children) {
                    val requestUserID = friendSnapshot.child("requestUserID").getValue(String::class.java)
                    val receiveUserID = friendSnapshot.child("receiveUserID").getValue(String::class.java)

                    if ((requestUserID == userID1 && receiveUserID == userID2) ||
                        (requestUserID == userID2 && receiveUserID == userID1)
                    ) {
                        friend = friendSnapshot.getValue(Friend::class.java)

                        // Trigger chat creation when status changes to "Friend"
                        if (friend?.status == "Friend") {
                            createChatIfNotExists(userID1, userID2)
                        }

                        break
                    }
                }
                callback(friend)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendDAO", "Error observing friend status: ${error.message}")
            }
        })
    }


    suspend fun getFriendByID(friendID: String): Friend? {
        val snapshot = dbRef.child(friendID).get().await()
        return if (snapshot.exists()) snapshot.getValue(Friend::class.java) else null
    }

    private fun createChatIfNotExists(userID1: String, userID2: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val existingChat = chatDAO.getChat(userID1, userID2)
            if (existingChat == null) {
                val newChatID = "C${System.currentTimeMillis()}"
                val newChat = Chat(
                    chatID = newChatID,
                    initiatorUserID = userID1,
                    receiverUserID = userID2,
                    initiatorLastSeen = "",
                    receiverLastSeen = ""
                )
                chatDAO.addChat(newChat)
                Log.d("Chat", "Created new chat with ID: $newChatID")
            } else {
                Log.d("Chat", "Chat already exists between $userID1 and $userID2")
            }
        }
    }

}