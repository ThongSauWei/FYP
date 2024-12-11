package com.example.fyp.dataAdapter

import android.app.Application
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.dao.ChatLineDAO
import com.example.fyp.dao.UserDAO
import com.example.fyp.data.Chat
import com.example.fyp.viewModel.ChatViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class OuterChatAdapter(
    private var chatList: List<Chat>,
    private val currentUserID: String, // Pass currentUserID to the Adapter
    private val onChatClicked: (Chat, String) -> Unit
) : RecyclerView.Adapter<OuterChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_holder, parent, false)
        return ViewHolder(view, currentUserID) // Pass currentUserID to ViewHolder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        val friendUserID =
            if (chat.initiatorUserID == currentUserID) chat.receiverUserID else chat.initiatorUserID
        holder.bind(chat, friendUserID)
        holder.itemView.setOnClickListener {
            onChatClicked(chat, friendUserID)
            holder.markMessagesAsRead(chat.chatID)
        }
    }

    override fun getItemCount(): Int = chatList.size

    fun updateChatList(newChatList: List<Chat>) {
        chatList = newChatList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View, private val currentUserID: String) :
        RecyclerView.ViewHolder(itemView) {
        private val imgProfile = itemView.findViewById<ImageView>(R.id.imgProfileChatHolder)
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameChatHolder)
        private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessageChatHolder)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTimeChatHolder)
        private val unreadBadge = itemView.findViewById<CardView>(R.id.cardViewUnreadBadgeChatHolder)
        private val unreadCount = itemView.findViewById<TextView>(R.id.tvUnreadCountChatHolder)
        private val userDAO = UserDAO()
        private val chatLineDAO = ChatLineDAO()

        fun bind(chat: Chat, friendUserID: String) {
            CoroutineScope(Dispatchers.IO).launch {
                // Fetch and set the username
                val user = userDAO.getUserByID(friendUserID)
                withContext(Dispatchers.Main) {
                    tvName.text = user?.username ?: "Unknown User"
                }

                // Fetch the latest message and time
                val lastMessage = chatLineDAO.getLastMessageContent(chat.chatID) ?: ""
                val lastTime = chatLineDAO.getLastMessageTime(chat.chatID) ?: ""
                withContext(Dispatchers.Main) {
                    tvMessage.text = lastMessage
                    tvTime.text = formatDateTime(lastTime)
                }

                // Fetch unread message count
                val unreadCountValue = chatLineDAO.getUnreadMessageCount(chat.chatID, currentUserID)
                withContext(Dispatchers.Main) {
                    if (unreadCountValue > 0) {
                        unreadBadge.visibility = View.VISIBLE
                        unreadCount.text = unreadCountValue.toString() // 显示未读数量
                    } else {
                        unreadBadge.visibility = View.GONE // 隐藏红点
                    }
                }

                // Load profile image
                val storageRef = FirebaseStorage.getInstance().reference.child("imageProfile/$friendUserID.png")
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(itemView.context).load(uri).into(imgProfile)
                }.addOnFailureListener {
                    imgProfile.setImageResource(R.drawable.nullprofile)
                }
            }
        }

        fun markMessagesAsRead(chatID: String) {
            CoroutineScope(Dispatchers.IO).launch {
                chatLineDAO.markAllMessagesAsRead(chatID, currentUserID)
                withContext(Dispatchers.Main) {
                    unreadBadge.visibility = View.GONE
                }
            }
        }

        private fun formatDateTime(dateTime: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return try {
                val date = inputFormat.parse(dateTime)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateTime
            }
        }
    }

        // Converts time to 12-hour format with AM/PM
        private fun formatDateTime(dateTime: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return try {
                val date = inputFormat.parse(dateTime)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateTime // Fallback to raw dateTime in case of parsing issues
            }
        }
    }

