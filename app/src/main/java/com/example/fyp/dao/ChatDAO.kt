package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.Chat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChatDAO {
    private val dbRef : DatabaseReference = FirebaseDatabase.getInstance().getReference("Chat")

    private var nextID = 1000

    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }

    fun addChat(chat : Chat) {
        "C$nextID".also { chat.chatID = it }
        nextID++

        dbRef.child(chat.chatID).setValue(chat)
            .addOnCompleteListener{

            }
            .addOnFailureListener{

            }
    }

    suspend fun getChat(userID_1: String, userID_2: String): Chat? {
        Log.d("ChatDAO", "Checking if chat exists between $userID_1 and $userID_2")
        var chat: Chat? = null

        val snapshot = dbRef.get().await()
        if (snapshot.exists()) {
            for (chatSnapshot in snapshot.children) {
                val initiatorUserID = chatSnapshot.child("initiatorUserID").getValue(String::class.java)
                val receiverUserID = chatSnapshot.child("receiverUserID").getValue(String::class.java)
                if ((initiatorUserID == userID_1 && receiverUserID == userID_2) ||
                    (initiatorUserID == userID_2 && receiverUserID == userID_1)
                ) {
                    chat = chatSnapshot.getValue(Chat::class.java)
                    if (chat != null) {
                        Log.d("ChatDAO", "Chat found: ${chat.chatID}")
                    }
                    break
                }
            }
        } else {
            Log.d("ChatDAO", "No chat found in the database.")
        }

        return chat
    }


    suspend fun getChatByID(chatID : String) : Chat? = suspendCoroutine { continuation ->

        dbRef.orderByChild("chatID").equalTo(chatID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (chatSnapshot in snapshot.children) {
                            val chat = chatSnapshot.getValue(Chat::class.java)
                            continuation.resume(chat)
                            return
                        }
                    }

                    continuation.resume(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }

            })
    }

    suspend fun getChatByUser(userID : String) : List<Chat> = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine { continuation ->

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = ArrayList<Chat>()
                    if (snapshot.exists()) {
                        for (chatSnapshot in snapshot.children) {
                            val chat = chatSnapshot.getValue(Chat::class.java)
                            chatList.add(chat!!)
                        }

                        val iterator = chatList.iterator()
                        while (iterator.hasNext()) {
                            val chat = iterator.next()
                            if (chat.initiatorUserID != userID && chat.receiverUserID != userID) {
                                iterator.remove()
                            }
                        }

                        continuation.resume(chatList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    fun updateLastSeen(chat : Chat) {
        dbRef.child(chat.chatID).setValue(chat)
            .addOnCompleteListener {

            }.addOnFailureListener {

            }
    }

    fun deleteChat(chatID : String) {
        dbRef.child(chatID).removeValue()
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    private suspend fun getNextID() : Int {
        var chatID = 1000
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (chatSnapshot in snapshot.children) {
                val lastChatID = chatSnapshot.key!!
                chatID = lastChatID.substring(1).toInt() + 1
            }
        }

        return chatID
    }

    fun updateChat(chat: Chat) {
        dbRef.child(chat.chatID).setValue(chat)
    }

    fun searchChats(query: String, callback: (List<Chat>) -> Unit) {
        dbRef.orderByChild("userID").startAt(query).endAt("$query\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val filteredChats = mutableListOf<Chat>()
                    for (chatSnapshot in snapshot.children) {
                        val chat = chatSnapshot.getValue(Chat::class.java)
                        filteredChats.add(chat!!)
                    }
                    callback(filteredChats)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForChatUpdates(userID: String, onUpdate: (List<Chat>) -> Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedChats = mutableListOf<Chat>();
                for (chatSnapshot in snapshot.children) {
                    val chat = chatSnapshot.getValue(Chat::class.java);
                    if (chat != null && (chat.initiatorUserID == userID || chat.receiverUserID == userID)) {
                        updatedChats.add(chat);
                    }
                }
                onUpdate(updatedChats);
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if necessary
            }
        });
    }

    // Update the last seen timestamp for a chat
    fun updateLastSeen(chatID: String, userID: String, timestamp: String) {
        dbRef.child(chatID).child(if (userID == "initiator") "initiatorLastSeen" else "receiverLastSeen").setValue(timestamp);
    }
}