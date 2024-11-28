package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Like
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.data.Save
import com.example.fyp.dataAdapter.ImageSliderAdapter
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

class Detail : Fragment() {

    private lateinit var postViewModel: PostViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var postImageDAO: PostImageDAO
    private lateinit var postCommentDAO: PostCommentDAO
    private lateinit var postCategoryDAO: PostCategoryDAO
    private lateinit var likeDAO: LikeDAO
    private lateinit var saveDAO: SaveDAO
    private lateinit var storageRef: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detail, container, false)

        //toolbar setting
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = ""

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() }

        val databaseRef = FirebaseDatabase.getInstance().reference
        this.storageRef = FirebaseStorage.getInstance().reference

        // Initialize DAOs and ViewModels
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        postImageDAO = PostImageDAO(storageRef, databaseRef)
        likeDAO = LikeDAO()
        saveDAO = SaveDAO()
        postCommentDAO = PostCommentDAO()
        postCategoryDAO = PostCategoryDAO()

        // Fetch the postID from arguments
        val postID = arguments?.getString("POST_ID")
        if (postID != null) {
            Log.d("DetailFragment", "Received postID: $postID")
            fetchPostDetails(postID, view)
        } else {
            Toast.makeText(requireContext(), "Post not found.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun fetchPostDetails(postID: String, view: View) {
        lifecycleScope.launch {
            try {
                // Fetch post details
                val post = postViewModel.getPostByID(postID)
                Log.d("DetailFragment", "Fetched post: $post")

                post?.let { populateUI(it, view) }
                    ?: Toast.makeText(requireContext(), "Post not found.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to fetch post details.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(post: Post, view: View) {
        Log.d("DetailFragment", "View found start")
        val currentUserID = getCurrentUserID()

        val tvNamePostHolder: TextView = view.findViewById(R.id.tvNamePostHolder)
        Log.d("DetailFragment", "View tvNamePostHolder successfully found")


        val tvDateTimePostHolder: TextView = view.findViewById(R.id.tvDateTimePostHolder)
        val profileUserImage: ImageView = view.findViewById(R.id.profileUserImage)
        val tvPostTitlePostHolder: TextView = view.findViewById(R.id.tvPostTitlePostHolder)
        val postDesc: TextView = view.findViewById(R.id.postDesc)
        val numLovePostHolder: TextView = view.findViewById(R.id.numLovePostHolder)
        val numCommentPostHolder: TextView = view.findViewById(R.id.numCommentPostHolder)
        val viewPagerPostImages: ViewPager2 = view.findViewById(R.id.viewPagerPostImages)
        val indicatorContainer: LinearLayout = view.findViewById(R.id.indicatorContainer)
        val cardViewTypeHolder: LinearLayout = view.findViewById(R.id.cardViewTypeHolder)
        val lovePostHolder: ImageView = view.findViewById(R.id.lovePostHolder)
        val bookmarkPostHolder: ImageView = view.findViewById(R.id.bookmarkPostHolder)
        val sharePostHolder: ImageView = view.findViewById(R.id.sharePostHolder)

        // Populate user details
        lifecycleScope.launch {
            val user = userViewModel.getUserByID(post.userID)
            tvNamePostHolder.text = user?.username ?: "Unknown User"

            user?.let {
                val ref = storageRef.child("imageProfile").child("${it.userID}.png")
                ref.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(requireContext()).load(uri).into(profileUserImage)
                }
            }
        }

        // Populate post details
        tvPostTitlePostHolder.text = post.postTitle
        postDesc.text = post.postDescription
        tvDateTimePostHolder.text = calculateRelativeTime(post.postDateTime)

        // Populate post images
        lifecycleScope.launch {
            val images = postImageDAO.getImagesByPostID(post.postID)
            val imageUrls = images.map { it.postImage }
            viewPagerPostImages.adapter = ImageSliderAdapter(imageUrls)
            setupIndicators(indicatorContainer, imageUrls.size, viewPagerPostImages)
        }

        // Fetch and display categories for the post
        CoroutineScope(Dispatchers.Main).launch {
            val categories = postCategoryDAO.getCategoriesByPostID(post.postID)
            populateCategories(cardViewTypeHolder, categories) // Pass cardViewTypeHolder here
        }


        // Fetch and display like count
        CoroutineScope(Dispatchers.Main).launch {
            val likeCount = likeDAO.getLikeCountByPostID(post.postID)
            numLovePostHolder.text = likeCount.toString()
        }

        // Fetch and display comment count
        CoroutineScope(Dispatchers.Main).launch {
            val commentCount = postCommentDAO.getCommentCountByPostID(post.postID)
            numCommentPostHolder.text = commentCount.toString()
        }

        GlobalScope.launch(Dispatchers.Main) {
            val existingLike = likeDAO.getLikeByUserIDAndPostID(currentUserID, post.postID)
            if (existingLike != null && existingLike.status == 1) {
                lovePostHolder.setImageResource(R.drawable.baseline_favorite_24)
            } else {
                lovePostHolder.setImageResource(R.drawable.love_border)
            }
        }

        GlobalScope.launch(Dispatchers.Main) {
            val existingSave = saveDAO.getSaveByUserIDAndPostID(currentUserID, post.postID)
            if (existingSave != null && existingSave.status == 1) {
                bookmarkPostHolder.setImageResource(R.drawable.bookmark_full)
            } else {
                bookmarkPostHolder.setImageResource(R.drawable.bookmark_border)
            }
        }

        // Like button
        lovePostHolder.setOnClickListener {
            lifecycleScope.launch {
                handleLikeClick(lovePostHolder, post)
            }
        }

// Bookmark button
        bookmarkPostHolder.setOnClickListener {
            lifecycleScope.launch {
                handleBookmarkClick(bookmarkPostHolder, post)
            }
        }

// Share button
        sharePostHolder.setOnClickListener {
            val postDetails = "Check out this amazing post: ${post.postTitle}\n\nShared from TARUMT Campus App!"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, postDetails)
            }
            requireContext().startActivity(Intent.createChooser(shareIntent, "Share post via"))
        }

        // Handle interactions
        handleLikeClick(lovePostHolder, post)
        handleBookmarkClick(bookmarkPostHolder, post)
        handleShareClick(sharePostHolder, post)
    }

    private fun populateCategories(container: LinearLayout, categories: List<PostCategory>) {
        container.removeAllViews() // Clear any existing categories

        categories.forEach { category ->
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.category_card_view, container, false) as CardView

            val textView = cardView.findViewById<TextView>(R.id.textViewType1)
            textView.text = category.category  // Ensure this matches your PostCategory property

            container.addView(cardView)
        }
    }


    private fun handleLikeClick(lovePostHolder: ImageView, post: Post) {
        lovePostHolder.setOnClickListener {
            lifecycleScope.launch {
                val currentUserID = getCurrentUserID()
                val existingLike = likeDAO.getLikeByUserIDAndPostID(currentUserID, post.postID)

                if (existingLike != null) {
                    val newStatus = if (existingLike.status == 1) 0 else 1
                    existingLike.status = newStatus
                    likeDAO.updateLikeStatus(existingLike)
                } else {
                    val newLike = Like(
                        likeID = UUID.randomUUID().toString(),
                        userID = currentUserID,
                        postID = post.postID,
                        status = 1,
                        timeStamp = getCurrentTimestamp()
                    )
                    likeDAO.saveLike(newLike)
                }
                lovePostHolder.setImageResource(
                    if (existingLike?.status == 1) R.drawable.baseline_favorite_24 else R.drawable.love_border
                )
            }
        }
    }

    private fun handleBookmarkClick(bookmarkPostHolder: ImageView, post: Post) {
        bookmarkPostHolder.setOnClickListener {
            lifecycleScope.launch {
                val currentUserID = getCurrentUserID()
                val existingSave = saveDAO.getSaveByUserIDAndPostID(currentUserID, post.postID)

                if (existingSave != null) {
                    val newStatus = if (existingSave.status == 1) 0 else 1
                    existingSave.status = newStatus
                    saveDAO.updateSaveStatus(existingSave)
                } else {
                    val newSave = Save(
                        saveID = UUID.randomUUID().toString(),
                        userID = currentUserID,
                        postID = post.postID,
                        status = 1,
                        timeStamp = getCurrentTimestamp()
                    )
                    saveDAO.saveSave(newSave)
                }
                bookmarkPostHolder.setImageResource(
                    if (existingSave?.status == 1) R.drawable.bookmark_full else R.drawable.bookmark_border
                )
            }
        }
    }

    private fun handleShareClick(sharePostHolder: ImageView, post: Post) {
        sharePostHolder.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this post: ${post.postTitle}")
            }
            startActivity(Intent.createChooser(shareIntent, "Share post via"))
        }
    }

    private fun setupIndicators(indicatorContainer: LinearLayout, size: Int, viewPager: ViewPager2) {
        if (size <= 1) {
            indicatorContainer.visibility = View.GONE
            return
        }
        indicatorContainer.visibility = View.VISIBLE
        indicatorContainer.removeAllViews()
        val indicators = Array(size) { ImageView(requireContext()) }

        indicators.forEachIndexed { index, imageView ->
            imageView.setImageResource(if (index == 0) R.drawable.option1 else R.drawable.option2)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 0, 8, 0) }
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

    private fun calculateRelativeTime(postDateTime: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val postDate = sdf.parse(postDateTime)
            val diff = Date().time - postDate.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            when {
                minutes < 60 -> "$minutes mins ago"
                hours < 24 -> "$hours hours ago"
                days < 30 -> "$days days ago"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(postDate)
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
