package com.example.fyp.dataAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.FriendProfile
import com.example.fyp.InnerChat
import com.example.fyp.R
import com.example.fyp.data.Friend
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.dialog.DeleteFriendDialog
import com.google.firebase.storage.FirebaseStorage

class FriendAdapter(
    private var userList: List<User>,
    private var profileList: List<Profile>,
    private var friendList: List<Friend>,
    private val fragmentManager: FragmentManager,
    private val friendViewModel: com.example.fyp.viewModel.FriendViewModel
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfileFriendHolder)
        val tvName: TextView = itemView.findViewById(R.id.tvNameFriendHolder)
        val tvCourse: TextView = itemView.findViewById(R.id.tvCourseFriendHolder)
        val btnMessage: ImageView = itemView.findViewById(R.id.btnMessageFriendHolder)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelActionFriendHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friend_holder, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = userList[position]
        val profile = profileList[position]
        val friend = friendList[position]

        // Set user profile image
        val storageRef = FirebaseStorage.getInstance().reference.child("imageProfile/${user.userID}.png")
        storageRef.downloadUrl.addOnSuccessListener {
            Glide.with(holder.imgProfile.context).load(it).into(holder.imgProfile)
        }.addOnFailureListener {
            holder.imgProfile.setImageResource(R.drawable.nullprofile) // Default profile image
        }

        // Set user name and course
        holder.tvName.text = user.username
        holder.tvCourse.text = profile.userCourse

        // Navigate to FriendProfile when profile image is clicked
        holder.imgProfile.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
            val fragment = FriendProfile()
            val bundle = Bundle()
            bundle.putString("friendUserID", user.userID)
            fragment.arguments = bundle
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Navigate to InnerChat when message button is clicked
        holder.btnMessage.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
            val fragment = InnerChat()
            val bundle = Bundle()
            bundle.putString("friendUserID", user.userID)
            fragment.arguments = bundle
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // Prompt delete dialog when delete button is clicked
        holder.btnDelete.setOnClickListener {
            val deleteDialog = DeleteFriendDialog().apply {
                friendID = friend.friendID
                username = user.username
                viewModel = friendViewModel
            }
            deleteDialog.show(fragmentManager, "DeleteFriendDialog")
        }
    }
}
