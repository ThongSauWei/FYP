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
import com.example.fyp.data.Post
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryAdapter(private var posts: MutableList<Post>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvPostTitleHistoryHolder)
        private val date: TextView = itemView.findViewById(R.id.todayDateHistoryHolder)
        private val image: ImageView = itemView.findViewById(R.id.imgPostHistoryHolder)

        fun bind(post: Post) {
            title.text = post.postTitle
            date.text = post.postDateTime
            // Load image if needed
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_holder, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}

