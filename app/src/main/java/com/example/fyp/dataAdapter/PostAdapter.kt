package com.example.fyp.dataAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.R
import com.example.fyp.data.Post
import com.squareup.picasso.Picasso

class PostAdapter(
    private val postList: List<Post>,
    private val isProfileView: Boolean,
    private val onEditClicked: (Post) -> Unit,
    private val onDeleteClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPostTitle: TextView = itemView.findViewById(R.id.tvPostTitlePostHolder)
        val imgPost: ImageView = itemView.findViewById(R.id.imgPostPostHolder)
        val imgDropdownMenu: ImageView = itemView.findViewById(R.id.imgDropdownMenuPostItemProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postholder, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = postList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // Populate post data
        holder.tvPostTitle.text = post.postTitle
        Picasso.get().load(post.postType).into(holder.imgPost) // Assuming postType is image URL

        // Handle dropdown menu visibility
        holder.imgDropdownMenu.visibility = if (isProfileView) View.VISIBLE else View.GONE

        // Handle dropdown menu actions
        holder.imgDropdownMenu.setOnClickListener {
            val popupMenu = PopupMenu(holder.itemView.context, holder.imgDropdownMenu)
            popupMenu.menuInflater.inflate(R.menu.profile_post_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.edit_post -> {
                        onEditClicked(post) // Navigate to edit post
                        true
                    }
                    R.id.delete_post -> {
                        onDeleteClicked(post) // Trigger delete action
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }
}
