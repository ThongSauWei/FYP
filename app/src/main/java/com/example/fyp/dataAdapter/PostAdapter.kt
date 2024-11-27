package com.example.fyp.dataAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.fyp.R
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.viewModel.UserViewModel
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

class PostAdapter(
    private var posts: List<Post>,
    private val userViewModel: UserViewModel,
    private val postImageDAO: PostImageDAO,
    private val postCategoryDAO: PostCategoryDAO,
    private val likeDAO: LikeDAO,
    private val postCommentDAO: PostCommentDAO
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private lateinit var storageRef : StorageReference

    // ViewHolder for individual post items
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.postholder, parent, false)

        storageRef = FirebaseStorage.getInstance().getReference()

        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set post date and title
//        holder.tvDateTimePostHolder.text = post.postDateTime
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

    // Function to dynamically create CardView elements for categories
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
