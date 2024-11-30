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
import com.google.firebase.storage.FirebaseStorage

data class RestrictedUser(val name: String, val userID: String, var isSelected: Boolean)


class RestrictedUserAdapter(
    private var userList: MutableList<RestrictedUser>, // List of users
    private val onItemClick: (RestrictedUser) -> Unit // Item click handler
) : RecyclerView.Adapter<RestrictedUserAdapter.RestrictedUserHolder>() {

    class RestrictedUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvNameFriend)
        val actionButton: ImageView = itemView.findViewById(R.id.btnActionFriendHolder)
        val selectedCardView: CardView = itemView.findViewById(R.id.selectedFriendCardView) // CardView for selection
        val imageProfileUser: ImageView = itemView.findViewById(R.id.imageView11)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestrictedUserHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_list, parent, false)
        return RestrictedUserHolder(itemView)
    }

    override fun onBindViewHolder(holder: RestrictedUserHolder, position: Int) {
        val user = userList[position]

        // Bind data to the view
        holder.nameTextView.text = user.name
        holder.selectedCardView.visibility = if (user.isSelected) View.VISIBLE else View.GONE

        // Glide usage inside onBindViewHolder
        val ref = FirebaseStorage.getInstance().getReference("imageProfile/${user.userID}.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Glide.with(holder.itemView.context)
                    .load(task.result)
                    .placeholder(R.drawable.baseline_people_24) // Placeholder image
                    .into(holder.imageProfileUser) // Assuming actionButton is the ImageView
            } else {
                holder.imageProfileUser.setImageResource(R.drawable.baseline_people_24) // Fallback image
            }
        }


        // Toggle selection on item click
        holder.itemView.setOnClickListener {
            user.isSelected = !user.isSelected // Toggle the selection
            holder.selectedCardView.visibility = if (user.isSelected) View.VISIBLE else View.GONE
            onItemClick(user) // Call onItemClick handler
        }
    }

    override fun getItemCount(): Int = userList.size

    // Function to get the list of selected users
    fun getSelectedUsers(): List<RestrictedUser> {
        return userList.filter { it.isSelected }
    }

    fun updateData(newUserList: List<RestrictedUser>) {
        userList.clear()
        userList.addAll(newUserList)
        notifyDataSetChanged()
    }

}


