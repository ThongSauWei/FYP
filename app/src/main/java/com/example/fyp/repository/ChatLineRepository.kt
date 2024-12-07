package com.example.fyp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fyp.dao.ChatLineDAO
import com.example.fyp.data.ChatLine

class ChatLineRepository (private val chatLineDao : ChatLineDAO) {

    private val chatLineList = MutableLiveData<List<ChatLine>>()

    fun addChatLine(chatLine : ChatLine) {
        chatLineDao.addChatLine(chatLine)
    }

    fun getChatLines(chatID: String, callback: (List<ChatLine>) -> Unit) {
        chatLineDao.getChatLines(chatID) { chatLines ->
            callback(chatLines)
        }
    }

    suspend fun getChatLine(chatID : String) : List<ChatLine> {
        return chatLineDao.getChatLine(chatID)
    }

    suspend fun getLastChat(chatID : String) : ChatLine? {
        return chatLineDao.getLastChat(chatID)
    }

    fun deleteChatLine(chatLineID : String) {
        chatLineDao.deleteChatLine(chatLineID)
    }
}