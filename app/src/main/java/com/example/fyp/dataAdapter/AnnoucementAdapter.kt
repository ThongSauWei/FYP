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
import com.example.fyp.dao.PostImageDAO
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
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.AnnouncementItem -> TYPE_ANNOUNCEMENT
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
                        val userID = snapshot.children.firstOrNull()?.getValue(UserAnnouncement::class.java)?.senderUserID
                        if (userID != null) {
                            // Use the userID to fetch the user details
                            GlobalScope.launch(Dispatchers.Main) {
                                val user = userViewModel.getUserByID(userID)

                                // Set the user data in the view (username, profile image, etc.)
                                user?.let {
                                    tvTypeTitle.text = "${it.username} ${announcement.announcementType.toLowerCase()} your post"  // Set the announcement type

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
                            val postID = snapshot.children.firstOrNull()?.getValue(UserAnnouncement::class.java)?.postID

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

            // Only display certain types of announcements (hide "Friend Request")
            if (announcement.announcementType == "Friend Request") {
                itemView.visibility = View.GONE // Hide the item if the type is "Friend Request"
            } else {
                itemView.visibility = View.VISIBLE // Show the item otherwise

                // Set the time text
                tvTime.text = calculateRelativeTime(announcement.announcementDate)
            }
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


