package com.example.fyp.dataAdapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.data.ChatLine

class InnerChatAdapter(
    private val currentUserID: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var chatLineList: List<ChatLine> = listOf()

    companion object {
        const val VIEW_TYPE_SEND = 1
        const val VIEW_TYPE_RECEIVE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatLineList[position].senderID == currentUserID) VIEW_TYPE_SEND else VIEW_TYPE_RECEIVE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SEND) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.send_chat_holder, parent, false)
            SendViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.receive_chat_holder, parent, false)
            ReceiveViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatLine = chatLineList[position]
        if (holder is SendViewHolder) {
            holder.bind(chatLine)
        } else if (holder is ReceiveViewHolder) {
            holder.bind(chatLine)
        }
    }

    override fun getItemCount(): Int = chatLineList.size

    fun setChatLines(chatLines: List<ChatLine>) {
        this.chatLineList = chatLines
        notifyDataSetChanged()
    }

    // ViewHolder for sent messages
    inner class SendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvChatSendChatHolder)
        private val imgMedia: ImageView = itemView.findViewById(R.id.imgChatSendChatHolder)
        private val videoContainer: FrameLayout = itemView.findViewById(R.id.videoChatSendChatHolder)
        private val videoView: VideoView = itemView.findViewById(R.id.videoViewChatSend)
        private val btnPlayVideo: ImageView = itemView.findViewById(R.id.btnPlayVideo)
        private val tvFile: TextView = itemView.findViewById(R.id.tvFileLinkSend)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeSendChatHolder)

        fun bind(chatLine: ChatLine) {
            resetVisibility()

            when (chatLine.mediaType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = chatLine.content
                }
                "image" -> {
                    imgMedia.visibility = View.VISIBLE
                    Glide.with(imgMedia.context).load(chatLine.mediaURL).into(imgMedia)
                }
                "video" -> {
                    videoContainer.visibility = View.VISIBLE
                    videoView.setVideoPath(chatLine.mediaURL)
                    btnPlayVideo.setOnClickListener {
                        videoView.start()
                        btnPlayVideo.visibility = View.GONE
                    }
                    videoView.setOnCompletionListener { btnPlayVideo.visibility = View.VISIBLE }
                }
                "file" -> {
                    tvFile.visibility = View.VISIBLE
                    tvFile.text = "Download File"
                    tvFile.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(chatLine.mediaURL)
                        }
                        tvFile.context.startActivity(intent)
                    }
                }
            }

            tvTime.text = formatDateTime(chatLine.dateTime)
        }

        private fun resetVisibility() {
            tvMessage.visibility = View.GONE
            imgMedia.visibility = View.GONE
            videoContainer.visibility = View.GONE
            tvFile.visibility = View.GONE
        }
    }

    // ViewHolder for received messages
    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvChatReceiveChatHolder)
        private val imgMedia: ImageView = itemView.findViewById(R.id.imgChatReceiveChatHolder)
        private val videoContainer: FrameLayout = itemView.findViewById(R.id.videoChatReceiveChatHolder)
        private val videoView: VideoView = itemView.findViewById(R.id.videoViewChatReceive)
        private val btnPlayVideo: ImageView = itemView.findViewById(R.id.btnPlayVideo)
        private val tvFile: TextView = itemView.findViewById(R.id.tvFileLinkReceive)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeReceiveChatHolder)

        fun bind(chatLine: ChatLine) {
            resetVisibility()

            when (chatLine.mediaType) {
                "text" -> {
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = chatLine.content
                }
                "image" -> {
                    imgMedia.visibility = View.VISIBLE
                    Glide.with(imgMedia.context).load(chatLine.mediaURL).into(imgMedia)
                }
                "video" -> {
                    videoContainer.visibility = View.VISIBLE
                    videoView.setVideoPath(chatLine.mediaURL)
                    btnPlayVideo.setOnClickListener {
                        videoView.start()
                        btnPlayVideo.visibility = View.GONE
                    }
                    videoView.setOnCompletionListener { btnPlayVideo.visibility = View.VISIBLE }
                }
                "file" -> {
                    tvFile.visibility = View.VISIBLE
                    tvFile.text = "Download File"
                    tvFile.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(chatLine.mediaURL)
                        }
                        tvFile.context.startActivity(intent)
                    }
                }
            }

            tvTime.text = formatDateTime(chatLine.dateTime)
        }

        private fun resetVisibility() {
            tvMessage.visibility = View.GONE
            imgMedia.visibility = View.GONE
            videoContainer.visibility = View.GONE
            tvFile.visibility = View.GONE
        }
    }

    // Converts time to 12-hour format with AM/PM
    private fun formatDateTime(dateTime: String): String {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateTime
        }
    }
}
