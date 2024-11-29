package com.example.fyp.dataAdapter

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils.replace
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.fyp.Detail
import com.example.fyp.MainActivity
import com.example.fyp.Profile
import com.example.fyp.R
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostSharedDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Friend
import com.example.fyp.data.Like
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.data.Save
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class PostAdapter(
    private var posts: List<Post>,
    private val userViewModel: UserViewModel,
    private val postImageDAO: PostImageDAO,
    private val postCategoryDAO: PostCategoryDAO,
    private val likeDAO: LikeDAO,
    private val postCommentDAO: PostCommentDAO,
    private val saveDAO: SaveDAO,
    private val context: Context,
    private val postViewModel: PostViewModel,
    private val friendViewModel: FriendViewModel,
    private val isProfileMode: Boolean = false // Indicates if the adapter is used in
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private lateinit var storageRef : StorageReference

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
        val lovePostHolder: ImageView = itemView.findViewById(R.id.lovePostHolder)
        val bookmarkPostHolder: ImageView = itemView.findViewById(R.id.bookmarkPostHolder)
        val sharePostHolder: ImageView = itemView.findViewById(R.id.sharePostHolder)
        val commentPostHolder: ImageView = itemView.findViewById(R.id.commentPostHolder)
        val cardFollow: CardView = itemView.findViewById(R.id.cardFollow)
        val tvFollow: TextView = itemView.findViewById(R.id.tvFollow)
        val cvProfilePostHolder: CardView = itemView.findViewById(R.id.cvProfilePostHolder)
        val cardMain: CardView = itemView.findViewById(R.id.cardMain)
        val imgDropdownMenu: ImageView = itemView.findViewById(R.id.imgDropdownMenuPostItemProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postholder, parent, false)

        storageRef = FirebaseStorage.getInstance().getReference()

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        val currentUserID = getCurrentUserID()

        // Show dropdown menu only in Profile mode (put OnBindView)
        if (isProfileMode) {
            holder.imgDropdownMenu.visibility = View.VISIBLE
            holder.imgDropdownMenu.setOnClickListener {
                showDropdownMenu(holder.imgDropdownMenu, post)
            }
        } else {
            holder.imgDropdownMenu.visibility = View.GONE
        }

        // By default, hide the cardFollow button
        holder.cardFollow.visibility = View.GONE

        // Show `cardFollow` button only if it's not the current user's post
        if (currentUserID != post.userID) {
            CoroutineScope(Dispatchers.Main).launch {
                val friend = friendViewModel.getFriend(currentUserID, post.userID)
                Log.d("PostAdapter", "Friend Status Check: currentUserID=$currentUserID, postUserID=${post.userID}, friend=$friend")

                when {
                    friend == null -> {
                        // No friendship exists
                        holder.cardFollow.visibility = View.VISIBLE
                        holder.tvFollow.text = "Follow"
                        Log.d("PostAdapter", "Friend not found. Showing 'Follow'.")
                    }
                    friend.status == "Pending" -> {
                        // Friend request is pending
                        holder.cardFollow.visibility = View.VISIBLE
                        holder.tvFollow.text = "Requested"
                        Log.d("PostAdapter", "Friend request is pending. Showing 'Requested'.")
                    }
                    friend.status == "Friend" -> {
                        // Already friends
                        holder.cardFollow.visibility = View.GONE
                        Log.d("PostAdapter", "Already friends. Hiding cardFollow.")
                    }
                }
            }

            // Handle follow/unfollow click
            holder.cardFollow.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val friend = friendViewModel.getFriend(currentUserID, post.userID)
                        if (friend == null) {
                            // Send friend request
                            val newFriend = Friend(
                                friendID = UUID.randomUUID().toString(),
                                requestUserID = currentUserID,
                                receiveUserID = post.userID,
                                status = "Pending",
                                timeStamp = getCurrentTimestamp()
                            )
                            friendViewModel.addFriend(newFriend)
                            holder.tvFollow.text = "Requested"
                            Toast.makeText(context, "Friend request sent.", Toast.LENGTH_SHORT).show()
                            Log.d("PostAdapter", "Friend request sent: $newFriend")
                        } else if (friend.status == "Pending") {
                            // Cancel friend request
                            friendViewModel.deleteFriend(friend.friendID)
                            holder.tvFollow.text = "Follow"
                            Toast.makeText(context, "Friend request canceled.", Toast.LENGTH_SHORT).show()
                            Log.d("PostAdapter", "Friend request canceled: friendID=${friend.friendID}")
                        }
                    } catch (e: Exception) {
                        Log.e("PostAdapter", "Error handling follow action: ${e.message}", e)
                        Toast.makeText(context, "Error handling follow action.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d("PostAdapter", "Post belongs to current user. Hiding cardFollow.")
        }

        // Set post date and title
        holder.tvPostTitlePostHolder.text = post.postTitle

        // Calculate and display relative time for postDateTime
        val relativeTime = calculateRelativeTime(post.postDateTime)
        holder.tvDateTimePostHolder.text = relativeTime

        GlobalScope.launch(Dispatchers.Main) {
            val user = userViewModel.getUserByID(post.userID)
            holder.tvNamePostHolder.text = user?.username ?: "Unknown"

            user?.let {
                val ref = storageRef.child("imageProfile").child("${user?.userID}.png")
                ref.downloadUrl
                    .addOnCompleteListener {
                        Glide.with(holder.profileUserImage).load(it.result.toString()).into(holder.profileUserImage)
                    }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            val images = postImageDAO.getImagesByPostID(post.postID)
            val imageUrls = images.map { it.postImage }
            Log.d("PostAdapter", "Fetched Images for PostID: ${post.postID}: $imageUrls")

            val adapter = holder.viewPagerPostImages.adapter as? ImageSliderAdapter
            if (adapter != null) {
                adapter.updateImages(imageUrls)
            } else {
                holder.viewPagerPostImages.adapter = ImageSliderAdapter(imageUrls)
            }

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

        // Check if the current user has liked the post and update the like button accordingly
        GlobalScope.launch(Dispatchers.Main) {
            val existingLike = likeDAO.getLikeByUserIDAndPostID(currentUserID, post.postID)
            if (existingLike != null && existingLike.status == 1) {
                holder.lovePostHolder.setImageResource(R.drawable.baseline_favorite_24)
            } else {
                holder.lovePostHolder.setImageResource(R.drawable.love_border)
            }
        }

        // Check if the current user has bookmarked the post and update the bookmark button accordingly
        GlobalScope.launch(Dispatchers.Main) {
            val existingSave = saveDAO.getSaveByUserIDAndPostID(currentUserID, post.postID)
            if (existingSave != null && existingSave.status == 1) {
                holder.bookmarkPostHolder.setImageResource(R.drawable.bookmark_full)
            } else {
                holder.bookmarkPostHolder.setImageResource(R.drawable.bookmark_border)
            }
        }

        // Add click listener for lovePostHolder
        holder.lovePostHolder.setOnClickListener {
            // Launch a coroutine when handling the like click
            GlobalScope.launch(Dispatchers.Main) {
                handleLikeClick(post, holder)
            }
        }

        // Add click listener for bookmarkPostHolder
        holder.bookmarkPostHolder.setOnClickListener {
            // Launch a coroutine when handling the bookmark click
            GlobalScope.launch(Dispatchers.Main) {
                handleBookmarkClick(post, holder)
            }
        }

        //share
        holder.sharePostHolder.setOnClickListener {
            val postDetails = "Check out this amazing post: ${post.postTitle}\n\nShared from TARUMT Campus App!"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain" // Define the MIME type
                putExtra(Intent.EXTRA_TEXT, postDetails) // Content to share
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share post via"))
        }

        //comment
        holder.cvProfilePostHolder.setOnClickListener {
            val fragment = Profile()
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        holder.tvNamePostHolder.setOnClickListener {
            val fragment = Profile()
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        // commentPostHolder click listener
        holder.commentPostHolder.setOnClickListener {
            val fragment = Detail()
            val bundle = Bundle()
            bundle.putString("POST_ID", post.postID)  // Pass the postID to the fragment
            fragment.arguments = bundle

            // Start the fragment transaction
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        holder.cardMain.setOnClickListener {
            val fragment = Detail()
            val bundle = Bundle()
            bundle.putString("POST_ID", post.postID)  // Pass the postID to the fragment
            fragment.arguments = bundle

            // Start the fragment transaction
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

    }

    private suspend fun handleFollowAction(post: Post, holder: PostViewHolder) {
        val currentUserID = getCurrentUserID()
        val friend = friendViewModel.getFriend(currentUserID, post.userID)

        if (friend == null) {
            // Add friend (send request)
            val newFriend = Friend(
                friendID = "0",
                requestUserID = currentUserID,
                receiveUserID = post.userID,
                status = "Pending",
                timeStamp = getCurrentTimestamp()
            )
            friendViewModel.addFriend(newFriend)

            holder.tvFollow.text = "Requested"
            Toast.makeText(context, "Friend request sent.", Toast.LENGTH_SHORT).show()

        } else if (friend.status == "Pending") {
            // Remove friend request
            friendViewModel.deleteFriend(friend.friendID)

            holder.tvFollow.text = "Follow"
            Toast.makeText(context, "Friend request canceled.", Toast.LENGTH_SHORT).show()
        }
    }


    // Add click listener for lovePostHolder
    suspend fun handleLikeClick(post: Post, holder: PostViewHolder) {
        val currentUserID = getCurrentUserID()

        // Check if the current user has already liked this post
        val existingLike = likeDAO.getLikeByUserIDAndPostID(currentUserID, post.postID)

        if (existingLike != null) {
            // Toggle like status
            val newStatus = if (existingLike.status == 1) 0 else 1
            existingLike.status = newStatus
            likeDAO.updateLikeStatus(existingLike)

            // Update UI
            holder.lovePostHolder.setImageResource(
                if (newStatus == 1) R.drawable.baseline_favorite_24 else R.drawable.love_border
            )
        } else {
            // Create and save a new like
            val like = Like(
                likeID = UUID.randomUUID().toString(),
                userID = currentUserID,
                postID = post.postID,
                status = 1,
                timeStamp = getCurrentTimestamp()
            )
            likeDAO.saveLike(like)

            // Update UI
            holder.lovePostHolder.setImageResource(R.drawable.baseline_favorite_24)
        }

        // Refresh posts after a successful action
        refreshPosts()
    }

    suspend fun handleBookmarkClick(post: Post, holder: PostViewHolder) {
        val currentUserID = getCurrentUserID()

        // Check if the current user has already bookmarked this post
        val existingSave = saveDAO.getSaveByUserIDAndPostID(currentUserID, post.postID)

        if (existingSave != null) {
            // Toggle save status
            val newStatus = if (existingSave.status == 1) 0 else 1
            existingSave.status = newStatus
            saveDAO.updateSaveStatus(existingSave)

            // Update UI
            holder.bookmarkPostHolder.setImageResource(
                if (newStatus == 1) R.drawable.bookmark_full else R.drawable.bookmark_border
            )
        } else {
            // Create and save a new bookmark
            val save = Save(
                saveID = UUID.randomUUID().toString(),
                userID = currentUserID,
                postID = post.postID,
                status = 1,
                timeStamp = getCurrentTimestamp()
            )
            saveDAO.saveSave(save)

            // Update UI
            holder.bookmarkPostHolder.setImageResource(R.drawable.bookmark_full)
        }

        // Refresh posts after a successful action
        refreshPosts()
    }

    private fun refreshPosts() {
        (context as? FragmentActivity)?.lifecycleScope?.launch {
            try {
                val updatedPosts = postViewModel.getAllPosts() // Fetch updated posts
                updatePosts(updatedPosts)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to refresh posts.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(context) // Use the passed context
    }

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun populateCategories(holder: PostViewHolder, categories: List<PostCategory>) {
        holder.cardViewTypeHolder.removeAllViews() // Clear any existing categories

        categories.forEach { category ->
            val cardView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.category_card_view, holder.cardViewTypeHolder, false) as CardView

            val textView = cardView.findViewById<TextView>(R.id.textViewType1)
            textView.text = category.category  // Ensure this matches your PostCategory property

            holder.cardViewTypeHolder.addView(cardView)
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

    private fun calculateRelativeTime(postDateTime: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            // Parse postDateTime into Date object
            val postDate = dateFormat.parse(postDateTime)
            val currentDate = Date()

            // Calculate the time difference in milliseconds
            val diffInMillis = currentDate.time - postDate.time

            // Convert to meaningful time units
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
            // Fallback in case of parsing error
            "Unknown time"
        }
    }

    // Setup indicators for ViewPager2
    private fun setupIndicators(indicatorContainer: LinearLayout, size: Int, viewPager: ViewPager2) {
        // Hide the indicator container if there's only one image
        if (size <= 1) {
            indicatorContainer.visibility = View.GONE
            return
        }

        // Show the indicator container if there are multiple images
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

    // Update posts in adapter
    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}
