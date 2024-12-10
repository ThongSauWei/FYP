package com.example.fyp.dataAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.data.Chat
import com.google.firebase.storage.FirebaseStorage

class OuterChatAdapter(
    private var chatList: List<Chat>,
    private val currentUserID: String,
    private val onChatClicked: (Chat, String) -> Unit // Pass both Chat and friendUserID
) : RecyclerView.Adapter<OuterChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_holder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        // Determine the friend user ID dynamically
        val friendUserID = if (chat.initiatorUserID == currentUserID) chat.receiverUserID else chat.initiatorUserID
        holder.bind(chat, friendUserID)
        holder.itemView.setOnClickListener { onChatClicked(chat, friendUserID) }
    }

    override fun getItemCount(): Int = chatList.size

    fun updateChatList(newChatList: List<Chat>) {
        chatList = newChatList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProfile = itemView.findViewById<ImageView>(R.id.imgProfileChatHolder)
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameChatHolder)
        private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessageChatHolder)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTimeChatHolder)
        private val unreadBadge = itemView.findViewById<CardView>(R.id.cardViewUnreadBadgeChatHolder)
        private val unreadCount = itemView.findViewById<TextView>(R.id.tvUnreadCountChatHolder)

        fun bind(chat: Chat, friendUserID: String) {
            // Set friend user ID as the display name
            tvName.text = friendUserID // You may fetch the actual username here using a DAO or repository

            // Mock last message and time for now
            tvMessage.text = "Last message here" // Update with actual last message logic
            tvTime.text = "11:50 PM" // Update with actual timestamp

            // Handle unread badge visibility
            unreadBadge.visibility = View.GONE // Replace with actual unread logic

            // Load profile image (placeholder logic)
            val storageRef = FirebaseStorage.getInstance().reference.child("imageProfile/$friendUserID.png")
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(itemView.context).load(uri).into(imgProfile)
            }.addOnFailureListener {
                imgProfile.setImageResource(R.drawable.nullprofile)
            }
        }
    }
}
