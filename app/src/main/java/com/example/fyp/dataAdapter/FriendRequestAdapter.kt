package com.example.fyp.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.FriendRequest
import com.example.fyp.R
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.dao.FriendDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Announcement
import com.example.fyp.data.Friend
import com.example.fyp.data.Post
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
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FriendRequestAdapter(
    private val items: List<ListItem>,
    private val activity: AppCompatActivity,
    private val postImageDAO: PostImageDAO,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_REQUEST = 1

    private val userViewModel: UserViewModel = ViewModelProvider(activity).get(UserViewModel::class.java)
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.FriendItem -> TYPE_REQUEST
            is ListItem.AnnouncementItem -> TYPE_REQUEST  // Add case for AnnouncementItem
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_request_holder, parent, false)
            RequestViewHolder(view, postImageDAO, activity) // Pass activity here
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.FriendItem -> (holder as RequestViewHolder).bind(item)
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)

        fun bind(header: ListItem.Header) {
            tvDay.text = header.date
        }
    }

    class RequestViewHolder(itemView: View, private val postImageDAO: PostImageDAO, private val activity: AppCompatActivity) : RecyclerView.ViewHolder(itemView) {
        private val tvRequest: TextView = itemView.findViewById(R.id.textView4)
        private val tvTime: TextView = itemView.findViewById(R.id.textView5)
        private val btnConfirm: AppCompatButton = itemView.findViewById(R.id.btnConfirm)
        private val btnDelete: AppCompatButton = itemView.findViewById(R.id.btnDel)
        private val profileImage: ImageView = itemView.findViewById(R.id.imageView11)

        private val friendDAO = FriendDAO()

        private lateinit var storageRef: StorageReference
        private lateinit var userAnnRef: DatabaseReference
        private val userViewModel: UserViewModel = ViewModelProvider(itemView.context as AppCompatActivity).get(UserViewModel::class.java)

        init {
            storageRef = FirebaseStorage.getInstance().getReference()
            userAnnRef = FirebaseDatabase.getInstance().getReference("UserAnnouncement")
        }

        fun bind(item: ListItem.FriendItem) {
            val friend = item.friend

            // Fetch user details using the requestUserID
            GlobalScope.launch(Dispatchers.Main) {
                val user = userViewModel.getUserByID(friend.requestUserID)

                user?.let {
                    tvRequest.text = "${it.username} requested to follow you"

                    // Load the profile image
                    val ref = storageRef.child("imageProfile").child("${it.userID}.png")
                    ref.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Glide.with(profileImage).load(task.result.toString()).into(profileImage)
                        }
                    }

                    // Confirm friend request
                    btnConfirm.setOnClickListener {
                        GlobalScope.launch(Dispatchers.IO) {
                            friend.status = "Friend" // Update status to "Friend"
                            friendDAO.updateFriend(friend)

                            // Set post to null when adding announcement for a friend request (no postID needed)
                            addAnnouncementForAction(null, friend.requestUserID,"Friend Request") {
                                // Refresh the fragment or activity here
                                val fragment = FriendRequest()
                                val transaction = activity.supportFragmentManager.beginTransaction()
                                transaction.replace(R.id.fragmentContainerView, fragment)
                                transaction.addToBackStack(null)
                                transaction.commit()
                                Log.d("FriendRequestAdapter", "Friendship confirmed and updated.")
                            }
                        }
                    }

                    // Delete (Reject) friend request
                    btnDelete.setOnClickListener {
                        GlobalScope.launch(Dispatchers.IO) {
                            friend.status = "Rejected" // Update status to "Rejected"
                            friendDAO.updateFriend(friend)

                            // Refresh the fragment or activity here
                            val fragment = FriendRequest()
                            val transaction = activity.supportFragmentManager.beginTransaction()
                            transaction.replace(R.id.fragmentContainerView, fragment)
                            transaction.addToBackStack(null)
                            transaction.commit()

                            // Optionally, update UI or show a message that the request is rejected
                            Log.d("FriendRequestAdapter", "Friend request rejected and updated.")
                        }
                    }
                }
            }

            // Set the time text
            tvTime.text = calculateRelativeTime(friend.timeStamp)
        }

        private fun addAnnouncementForAction(
            post: Post?,
            userID: String,
            actionType: String,
            onSuccess: () -> Unit
        ) {
            val currentUserID = getCurrentUserID()

            val announcement = Announcement(
                announcementID = "", // To be generated by AnnouncementDAO
                announcementType = actionType,
                announcementDate = getCurrentTimestamp(),
                announcementStatus = 1
            )

            // Allow postID to be null if there's no post (for friend request)
            val postID: String? = post?.postID


            AnnoucementDAO().addAnnouncement(
                announcement,
                userID = userID, // Now passing a non-nullable userID
                senderUserID = currentUserID,
                postID = postID // Passing nullable postID
            ) { success, exception ->
                if (success) {
                    Log.d("PostAdapter", "$actionType announcement added successfully.")
                    onSuccess()
                } else {
                    Log.e("PostAdapter", "Failed to add $actionType announcement: ${exception?.message}")
                }
            }
        }

        private fun getCurrentUserID(): String {
            return SaveSharedPreference.getUserID(itemView.context) // Use itemView.context here
        }

        fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
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
