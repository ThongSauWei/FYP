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

    suspend fun getLastMessageContent(chatID: String): String? {
        return chatLineDao.getLastMessageContent(chatID)
    }

    suspend fun getLastMessageTime(chatID: String): String? {
        return chatLineDao.getLastMessageTime(chatID)
    }

    suspend fun getUnreadMessageCount(chatID: String, currentUserID: String): Int {
        return chatLineDao.getUnreadMessageCount(chatID, currentUserID)
    }
}