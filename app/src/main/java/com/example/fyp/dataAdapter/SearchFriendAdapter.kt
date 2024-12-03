package com.example.fyp.dataAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.FriendProfile
import com.example.fyp.R
import com.example.fyp.data.Friend
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchFriendAdapter(
    private val friendViewModel: FriendViewModel,
    private val profileViewModel: ProfileViewModel,
    private val currentUserID: String,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<SearchFriendAdapter.ViewHolder>() {

    private var userList = mutableListOf<User>()
    private var profileList = mutableListOf<Profile>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfileFriendAddHolder)
        val tvName: TextView = itemView.findViewById(R.id.tvNameFriendAddHolder)
        val tvCourse: TextView = itemView.findViewById(R.id.tvCourseFriendAddHolder)
        val btnAdd: LinearLayout = itemView.findViewById(R.id.AddFriendLinearLayout)
        val btnRequested: LinearLayout = itemView.findViewById(R.id.RequestedFriendLinearLayout)
        val btnAlreadyFriend: LinearLayout = itemView.findViewById(R.id.AlreadyFriendLinearLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.add_friend_holder, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        val profile = profileList.find { it.userID == user.userID }

        holder.tvName.text = user.username
        holder.tvCourse.text = profile?.userCourse ?: "Unknown Course"

        // Load Profile Image
        val storageRef = FirebaseStorage.getInstance().reference.child("imageProfile/${user.userID}.png")
        storageRef.downloadUrl.addOnSuccessListener {
            Glide.with(holder.imgProfile.context).load(it).into(holder.imgProfile)
        }.addOnFailureListener {
            holder.imgProfile.setImageResource(R.drawable.nullprofile)
        }

        // Navigate to Profile
        holder.imgProfile.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
            val fragment = FriendProfile()
            val bundle = Bundle()
            bundle.putString("userID", user.userID) // Passing userID
            fragment.arguments = bundle
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }


        // Button visibility based on friend status
        CoroutineScope(Dispatchers.Main).launch {
            val friend = friendViewModel.getFriend(currentUserID, user.userID)
            when (friend?.status) {
                "Pending" -> {
                    holder.btnAdd.visibility = View.GONE
                    holder.btnRequested.visibility = View.VISIBLE
                    holder.btnAlreadyFriend.visibility = View.GONE
                }
                "Friend" -> {
                    holder.btnAdd.visibility = View.GONE
                    holder.btnRequested.visibility = View.GONE
                    holder.btnAlreadyFriend.visibility = View.VISIBLE
                }
                else -> {
                    holder.btnAdd.visibility = View.VISIBLE
                    holder.btnRequested.visibility = View.GONE
                    holder.btnAlreadyFriend.visibility = View.GONE
                }
            }
        }

        // Add Friend
        holder.btnAdd.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val friendRequest = Friend(
                    friendID = "",
                    receiveUserID = user.userID,
                    requestUserID = currentUserID,
                    status = "Pending",
                    timeStamp = System.currentTimeMillis().toString()
                )
                friendViewModel.addFriend(friendRequest)
                CoroutineScope(Dispatchers.Main).launch {
                    holder.btnAdd.visibility = View.GONE
                    holder.btnRequested.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int = userList.size

    fun setSuggestedFriends(allUsers: List<User>, allProfiles: List<Profile>, friends: List<Friend>) {
        val nonFriends = allUsers.filter { user ->
            friends.none { friend ->
                (friend.receiveUserID == user.userID || friend.requestUserID == user.userID) &&
                        friend.status == "Friend"
            } && user.userID != currentUserID
        }
        userList = nonFriends.shuffled().take(10).toMutableList()
        profileList = allProfiles.filter { profile -> userList.any { it.userID == profile.userID } }.toMutableList()
        notifyDataSetChanged()
    }

    fun setSearchResults(searchResults: List<User>, allProfiles: List<Profile>) {
        userList = searchResults.toMutableList()
        profileList = allProfiles.filter { profile -> userList.any { it.userID == profile.userID } }.toMutableList()
        notifyDataSetChanged()
    }
}
