package com.example.fyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Announcement
import com.example.fyp.data.Like
import com.example.fyp.data.Post
import com.example.fyp.data.PostCategory
import com.example.fyp.data.PostComment
import com.example.fyp.data.Save
import com.example.fyp.data.UserAnnouncement
import com.example.fyp.dataAdapter.CommentAdapter
import com.example.fyp.dataAdapter.ImageSliderAdapter
import com.example.fyp.viewModel.PostCommentViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
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
    private lateinit var recyclerViewComment: RecyclerView
    private lateinit var ttlComment: TextView

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

        recyclerViewComment = view.findViewById(R.id.recyclerViewComment)
        ttlComment = view.findViewById(R.id.ttlComment)

        // Initialize sendBtn and commentInput
        val sendBtn: ImageView = view.findViewById(R.id.sendBtn)


        //keyboard adjust side
        val writeComment: EditText = view.findViewById(R.id.writeComment)
        val recyclerViewComment: RecyclerView = view.findViewById(R.id.recyclerViewComment)

// Adjust layout when the keyboard is visible
        writeComment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {

                writeComment.text.clear()
                // Scroll the ScrollView to make writeComment visible
                view.post {
                    view.findViewById<ScrollView>(R.id.scrollView2).smoothScrollTo(0, writeComment.top)
                }
            }
        }

// Adjust when the keyboard visibility changes
        KeyboardVisibilityEvent.setEventListener(requireActivity()) { isOpen ->
            val scrollView = view.findViewById<ScrollView>(R.id.scrollView2)
            if (isOpen) {
                // Add padding when keyboard opens
                scrollView.setPadding(0, 0, 0, 900)
            } else {
                // Remove padding when keyboard closes
                scrollView.setPadding(0, 0, 0, 0)
            }
        }

        sendBtn.setOnClickListener {
            val postID = arguments?.getString("POST_ID")
            val commentText = writeComment.text.toString()

            if (!commentText.isBlank() && postID != null) {
                val postComment = PostComment(
                    postCommentID = UUID.randomUUID().toString(),
                    pcContent = commentText.trim(),
                    pcDateTime = getCurrentTimestamp(),
                    postID = postID,
                    userID = getCurrentUserID()
                )
                addCommentToFirebase(postID, postComment)

                // Clear input field
                writeComment.text.clear()

                val fragment = Detail()
                val bundle = Bundle()
                bundle.putString("POST_ID", postID)  // Pass the postID to the fragment
                fragment.arguments = bundle

                // Start the fragment transaction
                val fragmentManager = (context as FragmentActivity).supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainerView, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid comment.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up RecyclerView
        recyclerViewComment.layoutManager = LinearLayoutManager(activity)
//        recyclerViewComment.adapter = CommentAdapter(emptyList(), viewLifecycleOwner.lifecycleScope)

        val adapter = CommentAdapter(emptyList(), viewLifecycleOwner.lifecycleScope) { comment ->
            lifecycleScope.launch {
                val currentUserID = getCurrentUserID()
                val post = postViewModel.getPostByID(comment.postID)
                if (post != null && post.userID == currentUserID) {
                    val postCommentDAO = PostCommentDAO()
                    postCommentDAO.deletePostComment(comment.postCommentID)
                    Toast.makeText(context, "Comment deleted successfully", Toast.LENGTH_SHORT).show()
                    observeAndRefreshComments(post.postID) // Refresh the comments
                } else {
                    Toast.makeText(context, "You are not authorized to delete this comment.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recyclerViewComment.adapter = adapter

        if (postID != null) observeAndRefreshComments(postID)

        return view
    }

    private fun addCommentToFirebase(postID: String, postComment: PostComment) {
        val postCommentViewModel = ViewModelProvider(this).get(PostCommentViewModel::class.java)
        postCommentViewModel.addPostComment(postComment)
        postCommentViewModel.addCommentSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                // Create and add the announcement
                // Launch a coroutine to call the suspend function
                lifecycleScope.launch {
                    addAnnouncementAfterComment(postID, postComment)
                }
                observeAndRefreshComments(postID)
                Toast.makeText(requireContext(), "Comment added successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to add comment. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private suspend fun addAnnouncementAfterComment(postID: String, postComment: PostComment) {
        val currentUserID = getCurrentUserID()
        val post = postViewModel.getPostByID(postID)

        // Fetch post details (this could be done in parallel)
        post?.let {
            val announcementDAO = AnnoucementDAO()

            // First, generate unique IDs for the announcement and userAnnouncement
            val newAnnouncementID = generateNewAnnouncementID()
            val newUserAnnouncementID = generateNewUserAnnouncementID()

            // Create the Announcement object
            val announcement = Announcement(
                announcementID = newAnnouncementID,
                announcementType = "Comment", // Announcement type is always "Comment" here
                announcementDate = getCurrentTimestamp(),
                announcementStatus = 1 // Active
            )

            // Create the UserAnnouncement object
            val userAnnouncement = UserAnnouncement(
                userAnnID = newUserAnnouncementID,
                userID = post.userID, // Post's userID
                senderUserID = currentUserID, // Current logged in user
                postID = postID,
                announcementID = newAnnouncementID
            )

            // Add the announcement and userAnnouncement to the database
            announcementDAO.addAnnouncement(announcement, post.userID, currentUserID, postID) { success, exception ->
                if (success) {
                    Log.d("DetailFragment", "Announcement and UserAnnouncement added successfully.")
                } else {
                    Log.e("DetailFragment", "Failed to add announcement.", exception)
                }
            }
        }
    }

    // Helper methods to generate the new unique IDs
    private fun generateNewAnnouncementID(): String {
        val dbRef = FirebaseDatabase.getInstance().getReference("Announcement")
        var newAnnouncementID = "A1000" // Default starting ID

        dbRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val key = child.key
                        if (key != null && key.startsWith("A")) {
                            val idNumber = key.substring(1).toIntOrNull()
                            if (idNumber != null) {
                                newAnnouncementID = "A" + (idNumber + 1).toString() // Increment ID
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailFragment", "Error generating Announcement ID.", error.toException())
            }
        })

        return newAnnouncementID
    }

    private fun generateNewUserAnnouncementID(): String {
        // Assuming the UserAnnouncement ID follows the pattern "UA1000"
        val dbRef = FirebaseDatabase.getInstance().getReference("UserAnnouncement")
        var newUserAnnID = "UA1000" // Default starting ID

        dbRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val key = child.key
                        if (key != null && key.startsWith("UA")) {
                            val idNumber = key.substring(2).toIntOrNull()
                            if (idNumber != null) {
                                newUserAnnID = "UA" + (idNumber + 1).toString() // Increment ID
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailFragment", "Error generating UserAnnouncement ID.", error.toException())
            }
        })

        return newUserAnnID
    }





    private fun observeAndRefreshComments(postID: String) {
        lifecycleScope.launch {
            try {
                // Fetch and observe comments for the specified post ID
                val postCommentViewModel = ViewModelProvider(this@Detail).get(PostCommentViewModel::class.java)
                val userViewModel : UserViewModel =
                    ViewModelProvider(this@Detail).get(UserViewModel::class.java)
                postCommentViewModel.getPostComment(postID)
                postCommentViewModel.postComments.observe(viewLifecycleOwner, Observer { comments ->
                    // Update the RecyclerView adapter with the new list of comments
                    val adapter = recyclerViewComment.adapter as CommentAdapter
                    adapter.setViewModel(userViewModel)
                    adapter.updateComments(comments)

                    // Update the comment count TextView with the total count of comments
                    ttlComment.text = "${comments.size} comments"
                })
            } catch (e: Exception) {
                // Handle the exception
                Log.e("JoinGroup", "Error observing comment count: ${e.message}")
            }
        }
    }

    private fun fetchPostDetails(postID: String, view: View) {
        lifecycleScope.launch {
            try {
                // Fetch post details
                val post = postViewModel.getPostByID(postID)
                Log.d("DetailFragment", "Fetched post: $post")

                observeAndRefreshComments(postID)

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

        // Set the click listener for the profile image
        profileUserImage.setOnClickListener {
            openFriendProfile(post.userID) // Pass the userID to FriendProfile
        }

        // Set the click listener for the username
        tvNamePostHolder.setOnClickListener {
            openFriendProfile(post.userID) // Pass the userID to FriendProfile
        }


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
        val recyclerViewComment : RecyclerView = view.findViewById(R.id.recyclerViewComment)

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

    // Function to open the FriendProfile fragment and pass the userID
    private fun openFriendProfile(friendUserID: String) {
        val activity = context as? AppCompatActivity
        activity?.let {
            val transaction = it.supportFragmentManager.beginTransaction()
            val fragment = FriendProfile()

            val bundle = Bundle()
            bundle.putString("friendUserID", friendUserID) // Pass the userID to FriendProfile
            fragment.arguments = bundle

            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null) // Optional: Adds this fragment to the back stack
            transaction.commit()
        }
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
                    existingLike.timeStamp = getCurrentTimestamp()
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

                    // Add announcement
                    addAnnouncement("Like", post, currentUserID)
                }

                lovePostHolder.setImageResource(
                    if (existingLike?.status == 1) R.drawable.baseline_favorite_24 else R.drawable.love_border
                )

                refreshPosts(post.postID)
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
                    existingSave.timeStamp = getCurrentTimestamp()
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

                    // Add announcement
                    addAnnouncement("Save", post, currentUserID)
                }

                bookmarkPostHolder.setImageResource(
                    if (existingSave?.status == 1) R.drawable.bookmark_full else R.drawable.bookmark_border
                )

                refreshPosts(post.postID)
            }
        }
    }

    private fun refreshPosts(postID: String) {
        (context as? FragmentActivity)?.lifecycleScope?.launch {
            try {
                // Fetch updated like count for the specific post
                val likeCount = likeDAO.getLikeCountByPostID(postID)

                // Find the TextView and update its text
                val numLovePostHolder: TextView? = view?.findViewById(R.id.numLovePostHolder)
                numLovePostHolder?.text = likeCount.toString()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to refresh like count.", Toast.LENGTH_SHORT).show()
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

            // Add announcement
            val currentUserID = getCurrentUserID()
            addAnnouncement("Share", post, currentUserID)
        }
    }

    private fun addAnnouncement(actionType: String, post: Post, senderUserID: String) {
        val announcement = Announcement(
            announcementID = "",  // Will be generated in DAO
            announcementType = actionType,
            announcementDate = getCurrentTimestamp(),
            announcementStatus = 1  // Active status
        )

        val announcementDAO = AnnoucementDAO()
        announcementDAO.addAnnouncement(
            announcement,
            userID = post.userID,  // Post's userID
            senderUserID = senderUserID,
            postID = post.postID
        ) { success, exception ->
            if (success) {
                Log.d("DetailFragment", "$actionType announcement added successfully.")
            } else {
                Log.e("DetailFragment", "Failed to add $actionType announcement.", exception)
            }
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
