package com.example.fyp.dataAdapter

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.fyp.MainActivity
import com.example.fyp.Profile
import com.example.fyp.R
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostSharedDAO
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class PostAdapter(
    private var posts: List<Post>,
    private var userViewModel: UserViewModel,
    private val postImageDAO: PostImageDAO,
    private val postCategoryDAO: PostCategoryDAO,
    private val likeDAO: LikeDAO,
    private val postCommentDAO: PostCommentDAO,
    private val isProfileMode: Boolean = false // Indicates if the adapter is used in Profile mode
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val storageRef = FirebaseStorage.getInstance().getReference()

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamePostHolder: TextView = itemView.findViewById(R.id.tvNamePostHolder)
        val tvDateTimePostHolder: TextView = itemView.findViewById(R.id.tvDateTimePostHolder)
        val profileUserImage: ImageView = itemView.findViewById(R.id.profileUserImage)
        val tvPostTitlePostHolder: TextView = itemView.findViewById(R.id.tvPostTitlePostHolder)
        val numLovePostHolder: TextView = itemView.findViewById(R.id.numLovePostHolder)
        val numCommentPostHolder: TextView = itemView.findViewById(R.id.numCommentPostHolder)
        val viewPagerPostImages: ViewPager2 = itemView.findViewById(R.id.viewPagerPostImages)
        val indicatorContainer: LinearLayout = itemView.findViewById(R.id.indicatorContainer)
        val cardViewTypeHolder: LinearLayout = itemView.findViewById(R.id.cardViewTypeHolder)
        val imgDropdownMenu: ImageView = itemView.findViewById(R.id.imgDropdownMenuPostItemProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postholder, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set post date and title
        holder.tvPostTitlePostHolder.text = post.postTitle

        // Show dropdown menu only in Profile mode
        if (isProfileMode) {
            holder.imgDropdownMenu.visibility = View.VISIBLE
            holder.imgDropdownMenu.setOnClickListener {
                showDropdownMenu(holder.imgDropdownMenu, post)
            }
        } else {
            holder.imgDropdownMenu.visibility = View.GONE
        }

        // Calculate and display relative time for postDateTime
        val relativeTime = calculateRelativeTime(post.postDateTime)
        holder.tvDateTimePostHolder.text = relativeTime

        // Fetch and display user details
        GlobalScope.launch(Dispatchers.Main) {
            val user = userViewModel.getUserByID(post.userID)
            holder.tvNamePostHolder.text = user?.username ?: "Unknown"

            user?.let {
                val ref = storageRef.child("imageProfile").child("${user.userID}.png")
                ref.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Glide.with(holder.profileUserImage).load(it.result.toString()).into(holder.profileUserImage)
                    }
                }
            }
        }

        // Fetch and display images for the post
        CoroutineScope(Dispatchers.Main).launch {
            val images = postImageDAO.getImagesByPostID(post.postID)
            val imageUrls = images.map { it.postImage }

            holder.viewPagerPostImages.adapter = ImageSliderAdapter(imageUrls)
            setupIndicators(holder.indicatorContainer, imageUrls.size, holder.viewPagerPostImages)
        }

        // Fetch and display like count
        CoroutineScope(Dispatchers.Main).launch {
            val likeCount = likeDAO.getLikeCountByPostID(post.postID)
            holder.numLovePostHolder.text = likeCount.toString()
        }

        // Fetch and display comment count
        CoroutineScope(Dispatchers.Main).launch {
            val commentCount = postCommentDAO.getCommentCountByPostID(post.postID)
            holder.numCommentPostHolder.text = commentCount.toString()
        }

        // Fetch and display categories for the post
        CoroutineScope(Dispatchers.Main).launch {
            val categories = postCategoryDAO.getCategoriesByPostID(post.postID)
            populateCategories(holder, categories)
        }
    }

    // Show dropdown menu
    private fun showDropdownMenu(view: View, post: Post) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.menuInflater.inflate(R.menu.profile_post_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit_post -> {
                    // Handle edit post
                    editPost(post)
                    true
                }
                R.id.delete_post -> {
                    // Handle delete post
                    showDeleteConfirmation(view.context, post) // Use view.context here
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun editPost(post: Post) {

    }


    private fun deletePost(context: Context, post: Post) {
        val postID = post.postID

        // Initialize the required DAOs and ViewModel
        val postViewModel = PostViewModel(context.applicationContext as Application)
        val postImageDAO = PostImageDAO(
            FirebaseStorage.getInstance().reference,
            FirebaseDatabase.getInstance().reference
        )
        val postCategoryDAO = PostCategoryDAO()
        val postCommentDAO = PostCommentDAO()
        val likeDAO = LikeDAO() // To delete likes
        val postSharedDAO = PostSharedDAO() // To delete shares

        // Perform the deletion
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete Post Images
                val postImages = postImageDAO.getImagesByPostID(postID)
                for (image in postImages) {
                    postImageDAO.deleteImage(image.postImageID)
                    // Optionally delete image file from Firebase Storage
                    val imageRef = FirebaseStorage.getInstance().reference.child("${image.postImageID}.jpg")
                    imageRef.delete()
                }

                // Delete Post Categories
                val categories = postCategoryDAO.getCategoriesByPostID(postID)
                for (category in categories) {
                    postCategoryDAO.deleteCategory(category.postCategoryID)
                }

                // Delete Comments
                val comments = postCommentDAO.getCommentsByPostID(postID)
                for (comment in comments) {
                    postCommentDAO.deleteComment(comment.postCommentID)
                }

                // Delete Likes
                likeDAO.deleteLikesByPostID(postID)

                // Delete Shares
                postSharedDAO.deleteSharesByPostID(postID)

                postViewModel.deletePost(postID) { success, exception ->
                    if (success) {
                        // Handle successful deletion
                        Toast.makeText(context, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate back to the Profile fragment
                        val activity = context as? MainActivity
                        activity?.supportFragmentManager?.beginTransaction()?.apply {
                            replace(R.id.fragmentContainerView, Profile())
                            addToBackStack(null)
                            commit()
                        }
                    } else {
                        // Handle failure
                        Toast.makeText(context, "Failed to delete post: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                /*// Notify success on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate back to the profile
                    val activity = context as? MainActivity
                    activity?.supportFragmentManager?.beginTransaction()?.apply {
                        replace(R.id.fragmentContainerView, Profile())
                        addToBackStack(null)
                        commit()
                    }
                }*/
            } catch (e: Exception) {
                // Notify failure on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showDeleteConfirmation(context: Context, post: Post) {
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                deletePost(context, post) // Pass the context to the deletePost method
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun populateCategories(holder: PostViewHolder, categories: List<PostCategory>) {
        holder.cardViewTypeHolder.removeAllViews()

        categories.forEach { category ->
            val cardView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.category_card_view, holder.cardViewTypeHolder, false) as CardView
            val textView = cardView.findViewById<TextView>(R.id.textViewType1)
            textView.text = category.category
            holder.cardViewTypeHolder.addView(cardView)
        }
    }

    private fun calculateRelativeTime(postDateTime: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val postDate = dateFormat.parse(postDateTime)
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
            "Unknown time"
        }
    }

    private fun setupIndicators(indicatorContainer: LinearLayout, size: Int, viewPager: ViewPager2) {
        if (size <= 1) {
            indicatorContainer.visibility = View.GONE
            return
        }
        indicatorContainer.visibility = View.VISIBLE
        indicatorContainer.removeAllViews()
        val indicators = Array(size) { ImageView(indicatorContainer.context) }

        indicators.forEachIndexed { index, imageView ->
            imageView.setImageResource(if (index == 0) R.drawable.option1 else R.drawable.option2)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            imageView.layoutParams = params
            indicatorContainer.addView(imageView)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                indicators.forEachIndexed { index, imageView ->
                    imageView.setImageResource(if (index == position) R.drawable.option1 else R.drawable.option2)
                }
            }
        })
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
