package com.example.fyp.viewModel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.ChatDAO
import com.example.fyp.data.Chat
import com.example.fyp.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatViewModel(application : Application) : AndroidViewModel(application) {
    private val chatRepository : ChatRepository

    private val _chatList = MutableLiveData<List<Chat>>()
    val chatList: LiveData<List<Chat>> get() = _chatList

    init {
        val chatDao = ChatDAO()
        chatRepository = ChatRepository(chatDao)
    }

    fun addChat(chat : Chat) {
        viewModelScope.launch (Dispatchers.IO) {
            chatRepository.addChat(chat)
        }
    }

    suspend fun getChat(userID_1 : String, userID_2 : String) : Chat? {
        return chatRepository.getChat(userID_1, userID_2)
    }

    suspend fun getChatByID(chatID : String) : Chat? {
        return chatRepository.getChatByID(chatID)
    }

    // Fetch chats by user and post to LiveData
    fun fetchChatsByUser(userID: String) {
        viewModelScope.launch {
            val chats = chatRepository.getChatByUser(userID)
            _chatList.postValue(chats)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateLastSeen(chatID : String, userID : String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.updateLastSeen(chatID, userID)
        }
    }

    fun updateChat(chat: Chat) {
        chatRepository.updateChat(chat)
    }

    fun deleteChat(chatID : String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.deleteChat(chatID)
        }
    }

    fun searchChats(query: String): LiveData<List<Chat>> {
        val liveData = MutableLiveData<List<Chat>>()
        chatRepository.searchChats(query) { filteredChats ->
            liveData.postValue(filteredChats)
        }
        return liveData
    }

    fun listenForChatUpdates(userID: String) {
        chatRepository.listenForChatUpdates(userID) { updatedChats ->
            _chatList.postValue(updatedChats);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun markAsRead(chatID: String, userID: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.updateLastSeen(chatID, userID, timestamp);
        }
    }
}