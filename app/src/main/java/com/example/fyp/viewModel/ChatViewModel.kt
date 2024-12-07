package com.example.fyp.viewModel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.dao.ChatDAO
import com.example.fyp.data.Chat
import com.example.fyp.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel(application : Application) : AndroidViewModel(application) {
    private val chatRepository : ChatRepository

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

    suspend fun getChatByUser(userID : String) : List<Chat> {
        return chatRepository.getChatByUser(userID)
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
}