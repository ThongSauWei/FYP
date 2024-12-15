package com.example.fyp.dataAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.dao.ChatDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Chat
import com.example.fyp.data.UserAnnouncement
import com.example.fyp.dialog.DeleteAnnDialog
import com.example.fyp.models.ListItem
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class AnnoucementAdapter(private val items: List<ListItem>, private val activity: AppCompatActivity, private val postImageDAO: PostImageDAO) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ANNOUNCEMENT = 1
    private val TYPE_FRIEND = 2

    private lateinit var storageRef: StorageReference
    private lateinit var userAnnRef: DatabaseReference
//    private val postImageDAO: PostImageDAO
    private val userViewModel: UserViewModel = ViewModelProvider(activity).get(UserViewModel::class.java)

    init {
        // Initialize Firebase references
        storageRef = FirebaseStorage.getInstance().getReference()
        userAnnRef = FirebaseDatabase.getInstance().getReference("UserAnnouncement")
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.AnnouncementItem -> TYPE_ANNOUNCEMENT
            is ListItem.FriendItem -> TYPE_FRIEND // Handle FriendItem
            else -> throw IllegalArgumentException("Unknown item type: $item") // Optional else case
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.annoucement_holder, parent, false)
            // Pass postImageDAO to AnnouncementViewHolder
            AnnouncementViewHolder(view, postImageDAO)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.AnnouncementItem -> (holder as AnnouncementViewHolder).bind(item)
            else -> throw IllegalArgumentException("Unknown item type at position $position")
//            is ListItem.FriendItem -> (holder as FriendViewHolder).bind(item)  // This should work now
        }
    }


    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)

        fun bind(header: ListItem.Header) {
            tvDay.text = header.date
        }
    }

    class AnnouncementViewHolder(itemView: View, private val postImageDAO: PostImageDAO) : RecyclerView.ViewHolder(itemView) {
        private val tvTypeTitle: TextView = itemView.findViewById(R.id.tvTypeTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val profileImage: ImageView = itemView.findViewById(R.id.imageView11)
        private val imagePost: ImageView = itemView.findViewById(R.id.imagePost)
        private val imageDel: ImageView = itemView.findViewById(R.id.imageDel)

        private lateinit var storageRef: StorageReference
        private lateinit var userAnnRef: DatabaseReference
        private val userViewModel: UserViewModel = ViewModelProvider(itemView.context as AppCompatActivity).get(UserViewModel::class.java)

        init {
            storageRef = FirebaseStorage.getInstance().getReference()
            userAnnRef = FirebaseDatabase.getInstance().getReference("UserAnnouncement")
        }

        fun bind(item: ListItem.AnnouncementItem) {
            val announcement = item.announcement

            // Fetch the userID based on the announcementID from UserAnnouncement
            userAnnRef.orderByChild("announcementID").equalTo(announcement.announcementID)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // If the userAnnouncement exists, get the userID
                        val userAnnouncement = snapshot.children.firstOrNull()?.getValue(UserAnnouncement::class.java)
                        val senderUserID = userAnnouncement?.senderUserID  // The user who sent the request
                        val receiverUserID = userAnnouncement?.userID

                        if (senderUserID != null) {
                            // Use the userID to fetch the user details
                            GlobalScope.launch(Dispatchers.Main) {
                                val user = userViewModel.getUserByID(senderUserID)

                                // Set the user data in the view (username, profile image, etc.)
                                user?.let {
                                    if (announcement.announcementType == "Friend Request") {
                                        // Display message for Friend Request type
                                        tvTypeTitle.text = "${it.username} accepted your friend request"

                                        // Hide imagePost for "Friend Request"
                                        imagePost.visibility = View.GONE

                                        // **Add chat creation here**
                                        if (receiverUserID != null) {
                                            GlobalScope.launch(Dispatchers.IO) {
                                                try {
                                                    val chatDAO = ChatDAO()
                                                    val existingChat = chatDAO.getChat(receiverUserID, senderUserID)

                                                    if (existingChat == null) {
                                                        // Create a new chat if not already exists
                                                        val newChatID = "C${System.currentTimeMillis()}"
                                                        val newChat = Chat(
                                                            chatID = newChatID,
                                                            initiatorUserID = receiverUserID,
                                                            receiverUserID = senderUserID,
                                                            initiatorLastSeen = "",
                                                            receiverLastSeen = ""
                                                        )
                                                        chatDAO.addChat(newChat)
                                                        Log.d("AnnouncementAdapter", "Chat created with ID: $newChatID")
                                                    } else {
                                                        Log.d("AnnouncementAdapter", "Chat already exists with ID: ${existingChat.chatID}")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AnnouncementAdapter", "Error creating chat: ${e.message}")
                                                }
                                            }
                                        }
                                    } else {
                                        // Display generic message for other types of announcements
                                        tvTypeTitle.text = "${it.username} ${announcement.announcementType.toLowerCase()} your post"

                                        // Make sure imagePost is visible for other types
                                        imagePost.visibility = View.VISIBLE
                                    }

                                    // Load the profile image
                                    val ref = storageRef.child("imageProfile").child("${it.userID}.png")
                                    ref.downloadUrl.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Glide.with(profileImage).load(task.result.toString()).into(profileImage)
                                        }
                                    }
                                } ?: run {
                                    tvTypeTitle.text = "Unknown user liked your post"  // In case user data is not available
                                }
                            }

                            imageDel.setOnClickListener {
                                val deleteDialog = DeleteAnnDialog()
                                deleteDialog.announcementID = item.announcement.announcementID
                                deleteDialog.show((itemView.context as AppCompatActivity).supportFragmentManager, "DeleteAnnDialog")
                            }

                            // Get the postID from UserAnnouncement
                            val postID = userAnnouncement.postID

                            if (!postID.isNullOrEmpty()) {
                                // Fetch images for the given postID using PostImageDAO
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        val images = postImageDAO.getImagesByPostID(postID)
                                        val imageUrls = images.map { it.postImage }

                                        if (images.isNotEmpty()) {
                                            // Get the first image's URL
                                            val firstImageUrl = imageUrls.first()
                                            Glide.with(imagePost).load(firstImageUrl).into(imagePost)  // Load image into ImageView
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AnnouncementAdapter", "Error fetching post images: ${e.message}")
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("AnnouncementAdapter", "Error fetching UserAnnouncement: ${error.message}")
                    }
                })

            // Set the time text
            tvTime.text = calculateRelativeTime(announcement.announcementDate)
        }



        private fun calculateRelativeTime(postDateTime: String): String {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Adjusted to parse date and time
            return try {
                val postDate = inputDateFormat.parse(postDateTime)
                val currentDate = Date()
                val diffInMillis = currentDate.time - postDate.time

                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                when {
                    minutes < 60 -> "$minutes mins ago"
                    hours < 24 -> "$hours hours ago"
                    days < 30 -> "$days days ago"
                    days < 365 -> "${days / 30} months ago"
                    else -> "${days / 365} years ago"
                }
            } catch (e: Exception) {
                Log.e("AnnouncementAdapter", "Error parsing date: $postDateTime", e)
                "Unknown time"
            }
        }
    }

}


