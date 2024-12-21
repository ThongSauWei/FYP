package com.example.fyp.dataAdapter

import android.app.Dialog
import android.content.Context
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
        this.chatLineList = chatLines.sortedBy { chatLine ->
            try {
                // 将日期字符串解析为时间戳进行排序
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                inputFormat.parse(chatLine.dateTime)?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE // 如果解析失败，将放置到最后
            }
        }
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
                    Glide.with(imgMedia.context)
                        .load(chatLine.mediaURL)
                        .placeholder(R.drawable.tarumt_logo)
                        .error(R.drawable.no_internet)
                        .into(imgMedia)

                    imgMedia.setOnClickListener {
                        showDialog(itemView.context, "image", chatLine.mediaURL!!)
                    }
                }
                "video" -> {
                    videoContainer.visibility = View.VISIBLE
                    videoView.setVideoPath(chatLine.mediaURL)
                    btnPlayVideo.visibility = View.VISIBLE

                    btnPlayVideo.setOnClickListener {
                        videoView.start()
                        btnPlayVideo.visibility = View.GONE
                    }

                    videoView.setOnCompletionListener {
                        btnPlayVideo.visibility = View.VISIBLE
                    }

                    videoContainer.setOnClickListener {
                        showDialog(itemView.context, "video", chatLine.mediaURL!!)
                    }
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
                    Glide.with(imgMedia.context)
                        .load(chatLine.mediaURL)
                        .placeholder(R.drawable.tarumt_logo)
                        .error(R.drawable.no_internet)
                        .into(imgMedia)

                    imgMedia.setOnClickListener {
                        showDialog(itemView.context, "image", chatLine.mediaURL!!)
                    }
                }
                "video" -> {
                    videoContainer.visibility = View.VISIBLE
                    videoView.setVideoPath(chatLine.mediaURL)
                    btnPlayVideo.visibility = View.VISIBLE

                    btnPlayVideo.setOnClickListener {
                        videoView.start()
                        btnPlayVideo.visibility = View.GONE
                    }

                    videoView.setOnCompletionListener {
                        btnPlayVideo.visibility = View.VISIBLE
                    }

                    videoContainer.setOnClickListener {
                        showDialog(itemView.context, "video", chatLine.mediaURL!!)
                    }
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
    private fun showDialog(context: Context, mediaType: String, mediaURL: String) {
        val dialog = Dialog(context)

        when (mediaType) {
            "image" -> {
                dialog.setContentView(R.layout.dialog_image_view)
                val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
                val closeButton = dialog.findViewById<ImageButton>(R.id.closeFullImageButton)

                if (imageView != null && closeButton != null) {
                    imageView.visibility = View.VISIBLE
                    Glide.with(context).load(mediaURL).into(imageView)
                    closeButton.setOnClickListener { dialog.dismiss() }
                } else {
                    Toast.makeText(context, "Image dialog components not found!", Toast.LENGTH_SHORT).show()
                }
            }

            "video" -> {
                dialog.setContentView(R.layout.dialog_video_view)
                val videoView = dialog.findViewById<VideoView>(R.id.dialogVideoView)
                val closeButton = dialog.findViewById<ImageButton>(R.id.closeFullVideoButton)

                if (videoView != null && closeButton != null) {
                    videoView.visibility = View.VISIBLE
                    videoView.setVideoURI(Uri.parse(mediaURL))
                    videoView.setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.start() // Start playback
                    }
                    videoView.setOnErrorListener { _, what, extra ->
                        Toast.makeText(context, "Failed to play video: $what, $extra", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        true
                    }

                    closeButton.setOnClickListener { dialog.dismiss() }
                } else {
                    Toast.makeText(context, "Video dialog components not found!", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
                Toast.makeText(context, "Unsupported media type!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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
