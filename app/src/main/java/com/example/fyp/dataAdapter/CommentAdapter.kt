package com.example.fyp.dataAdapter

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.data.PostComment
import com.example.fyp.dialog.DeleteCommentDialog
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
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

class CommentAdapter(
    private val context: Context,
    private var comments: List<PostComment>,
    private val coroutineScope: CoroutineScope,
    private val deleteCommentListener: (PostComment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentHolder>() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var storageRef: StorageReference
    private lateinit var postViewModel: PostViewModel

    class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCommentCommentHolder: TextView = itemView.findViewById(R.id.tvCommentCommentHolder)
        val imgProfileCommentHolder: ImageView = itemView.findViewById(R.id.imgProfileCommentHolder)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvCommentDate: TextView = itemView.findViewById(R.id.tvCommentDate)
        val deleteComment: TextView = itemView.findViewById(R.id.delete_comment)
    }

    fun setViewModel(userViewModel: UserViewModel) {
        this.userViewModel = userViewModel
        storageRef = FirebaseStorage.getInstance().reference
        notifyDataSetChanged()
    }

    fun updateComments(newComments: List<PostComment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_holder, parent, false)

        return CommentHolder(itemView)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    override fun onBindViewHolder(holder: CommentHolder, position: Int) {
        val comment = comments[position]
        coroutineScope.launch {
            holder.tvCommentCommentHolder.text = comment.pcContent
            holder.tvCommentDate.text = calculateRelativeTime(comment.pcDateTime)

            val user = userViewModel.getUserByID(comment.userID)
            holder.tvUsername.text = user?.username ?: "Unknown"

            user?.let {
                val ref = storageRef.child("imageProfile").child("${it.userID}.png")
                ref.downloadUrl.addOnCompleteListener { task ->
                    Glide.with(holder.imgProfileCommentHolder)
                        .load(task.result)
                        .into(holder.imgProfileCommentHolder)
                }
            }

//            holder.deleteComment.setOnClickListener {
//                deleteCommentListener(comment)
//            }

//            holder.deleteComment.setOnClickListener {
//                val deleteCommentDialog = DeleteCommentDialog(comment) { confirmedComment ->
//                    deleteCommentListener(confirmedComment)
//                }
//                deleteCommentDialog.show((holder.itemView.context as FragmentActivity).supportFragmentManager, "DeleteCommentDialog")
//            }

            // Fetch currentUserID
            val currentUserID = getCurrentUserID()

            val postViewModel = PostViewModel(context.applicationContext as Application)

            // Fetch the post for the current comment
            val post = postViewModel.getPostByID(comment.postID)

            // Show or hide the delete button based on the user's authorization
            if (post != null && post.userID == currentUserID) {
                holder.deleteComment.visibility = View.VISIBLE
            } else {
                holder.deleteComment.visibility = View.GONE
            }

            // Handle delete button click
            holder.deleteComment.setOnClickListener {
                val deleteCommentDialog = DeleteCommentDialog(comment) { confirmedComment ->
                    deleteCommentListener(confirmedComment)
                }
                deleteCommentDialog.show((holder.itemView.context as FragmentActivity).supportFragmentManager, "DeleteCommentDialog")
            }

        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(context) // Use the passed context
    }

    private fun calculateRelativeTime(dateTime: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val time = sdf.parse(dateTime)?.time ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val diff = now - time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
    }
}

