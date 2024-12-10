package com.example.fyp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostViewHistoryDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Like
import com.example.fyp.data.Post
import com.example.fyp.data.Save
import com.example.fyp.dataAdapter.HistoryAdapter
import com.example.fyp.repository.PostViewHistoryRepository
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.example.fyp.viewModelFactory.PostViewHistoryViewModelFactory
import com.example.fyp.viewModel.PostViewHistoryViewModel
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HistoryPost : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var tvHistoryPost: TextView
    private lateinit var tvSavedPost: TextView
    private lateinit var tvLikedPost: TextView
    private lateinit var materialCardHistory: MaterialCardView
    private var currentUserID: String? = null

    private lateinit var postImageDAO: PostImageDAO
    private lateinit var postCategoryDAO: PostCategoryDAO
    private lateinit var likeDAO: LikeDAO
    private lateinit var postCommentDAO: PostCommentDAO
    private lateinit var saveDAO: SaveDAO
    private lateinit var postViewModel: PostViewModel
    private lateinit var postViewHistoryViewModel: PostViewHistoryViewModel
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var userViewModel: UserViewModel

    private lateinit var postViewHistoryRepository: PostViewHistoryRepository
    private lateinit var postDAO: PostDAO
    private lateinit var postViewHistoryDAO: PostViewHistoryDAO


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_post, container, false)

        // Set toolbar layout
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvHistoryPost = view.findViewById(R.id.tvHistoryPost)
        tvSavedPost = view.findViewById(R.id.tvSavedPost)
        tvLikedPost = view.findViewById(R.id.tvLikedPost)
        materialCardHistory = view.findViewById(R.id.materialCardView)

        val databaseRef = FirebaseDatabase.getInstance().reference
        val storageRef = FirebaseStorage.getInstance().reference

        // Initialize DAOs and ViewModels
        postImageDAO = PostImageDAO(storageRef, databaseRef)
        postCategoryDAO = PostCategoryDAO()
        postViewHistoryDAO = PostViewHistoryDAO()
        likeDAO = LikeDAO()
        postCommentDAO = PostCommentDAO()
        saveDAO = SaveDAO()
        postDAO = PostDAO()
        postViewHistoryDAO = PostViewHistoryDAO()
        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)

        postViewHistoryRepository = PostViewHistoryRepository(postViewHistoryDAO)
        val postViewHistoryViewModelFactory = PostViewHistoryViewModelFactory(postViewHistoryRepository)
        postViewHistoryViewModel = ViewModelProvider(this, postViewHistoryViewModelFactory).get(PostViewHistoryViewModel::class.java)

        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        adapter = HistoryAdapter(
            mutableListOf(),
            userViewModel,
            postImageDAO,
            postCategoryDAO,
            postViewHistoryDAO,
            likeDAO,
            postCommentDAO,
            saveDAO,
            requireContext(),
            postViewModel,
            postViewHistoryViewModel,
            friendViewModel
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Set click listeners
        tvHistoryPost.setOnClickListener { updateUI("history") }
        tvSavedPost.setOnClickListener { updateUI("saved") }
        tvLikedPost.setOnClickListener { updateUI("liked") }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUserID = SaveSharedPreference.getUserID(requireContext())

        // Default load history posts after view is fully created
        updateUI("history")
    }


    private fun updateUI(type: String) {
        resetStyles() // Reset all to default before updating the selected one

        when (type) {
            "history" -> {
                materialCardHistory.strokeColor = ContextCompat.getColor(requireContext(), R.color.blue)
                materialCardHistory.strokeWidth = 5
                tvHistoryPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_category))
                loadHistoryPosts()
            }
            "saved" -> {
                val materialCardSaved: MaterialCardView = requireView().findViewById(R.id.materialCardView1)
                materialCardSaved.strokeColor = ContextCompat.getColor(requireContext(), R.color.blue)
                materialCardSaved.strokeWidth = 5
                tvSavedPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_category))
                loadSavedPosts()
            }
            "liked" -> {
                val materialCardLiked: MaterialCardView = requireView().findViewById(R.id.materialCardView2)
                materialCardLiked.strokeColor = ContextCompat.getColor(requireContext(), R.color.blue)
                materialCardLiked.strokeWidth = 5
                tvLikedPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_category))
                loadLikedPosts()
            }
        }
    }


    private fun resetStyles() {
        // Reset MaterialCardView borders
        materialCardHistory.strokeColor = ContextCompat.getColor(requireContext(), R.color.tab_unused)
        materialCardHistory.strokeWidth = 0

        val materialCardSaved: MaterialCardView = requireView().findViewById(R.id.materialCardView1)
        materialCardSaved.strokeColor = ContextCompat.getColor(requireContext(), R.color.tab_unused)
        materialCardSaved.strokeWidth = 0

        val materialCardLiked: MaterialCardView = requireView().findViewById(R.id.materialCardView2)
        materialCardLiked.strokeColor = ContextCompat.getColor(requireContext(), R.color.tab_unused)
        materialCardLiked.strokeWidth = 0

        // Reset TextView colors
        tvHistoryPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_unused))
        tvSavedPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_unused))
        tvLikedPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_unused))
    }


    private fun loadHistoryPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Step 1: Fetch the current user ID
                val currentUserID = SaveSharedPreference.getUserID(requireContext())
                if (currentUserID == null) {
                    Log.e("HistoryPost", "User ID is null, cannot fetch history posts.")
                    return@launch
                }

                Log.d("HistoryPost", "Fetching post history for user: $currentUserID")

                // Step 2: Fetch PostViewHistory records for the current user
                val postHistory = postViewHistoryDAO.getPostHistoryForUser(currentUserID)
                Log.d("HistoryPost", "Fetched ${postHistory.size} post view history records.")

                if (postHistory.isEmpty()) {
                    // No history to display
                    adapter.updatePosts(emptyList())
                    Log.d("HistoryPost", "No post history found for the user.")
                    return@launch
                }

                // Step 3: Extract the postIDs from the history records
                val viewedPostIDs = postHistory.map { it.postID }

                // Step 4: Fetch the corresponding posts
                val posts = mutableListOf<Post>()
                for (postID in viewedPostIDs) {
                    val post = postDAO.getPostByID(postID) // Adjust this method as per your DAO implementation
                    if (post != null) {
                        posts.add(post)
                        Log.d("HistoryPost", "Loaded post: ${post.postTitle}")
                    } else {
                        Log.d("HistoryPost", "Post with ID $postID not found.")
                    }
                }

                // Step 5: Update the adapter with the fetched posts
                if (posts.isNotEmpty()) {
                    adapter.updatePosts(posts)
                } else {
                    Log.d("HistoryPost", "No valid posts to display.")
                }
            } catch (e: Exception) {
                Log.e("HistoryPost", "Error loading post history: ${e.message}")
            }
        }
    }



    private fun loadSavedPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentUserID == null) {
                    Log.e("HistoryPost", "User ID is null, cannot fetch saved posts.")
                    return@launch
                }

                Log.d("HistoryPost", "Fetching saved posts for user: $currentUserID")

                val savedPosts = mutableListOf<Post>()

                // Fetch all saves for the current user
                val saves = withContext(Dispatchers.IO) {
                    saveDAO.getSavesByUserID(currentUserID!!)
                }

                // Filter and group saves by postID, then keep the latest save record
                val latestSaves = saves
                    .filter { it.status == 1 } // Include only active saves
                    .groupBy { it.postID }    // Group by postID
                    .mapValues { entry ->
                        entry.value.maxByOrNull { save -> save.timeStamp } // Get the latest save by timestamp
                    }
                    .values
                    .filterNotNull() // Remove null values, just in case

                for (save in latestSaves) {
                    val post = withContext(Dispatchers.IO) {
                        postDAO.getPostByID(save.postID)
                    }
                    if (post != null) {
                        savedPosts.add(post)
                        Log.d("HistoryPost", "Loaded saved post: ${post.postTitle}")
                    }
                }

                if (savedPosts.isNotEmpty()) {
                    adapter.updatePosts(savedPosts)
                } else {
                    adapter.updatePosts(emptyList())
                    Log.d("HistoryPost", "No saved posts found for the user.")
                }
            } catch (e: Exception) {
                Log.e("HistoryPost", "Error loading saved posts: ${e.message}")
            }
        }
    }



    private fun loadLikedPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentUserID == null) {
                    Log.e("HistoryPost", "User ID is null, cannot fetch liked posts.")
                    return@launch
                }

                Log.d("HistoryPost", "Fetching liked posts for user: $currentUserID")

                val likedPosts = mutableListOf<Post>()

                // Fetch all likes for the current user
                val likes = withContext(Dispatchers.IO) {
                    likeDAO.getLikesByUserID(currentUserID!!)
                }

                // Filter and group likes by postID, then keep the latest like record
                val latestLikes = likes
                    .filter { it.status == 1 } // Include only active likes
                    .groupBy { it.postID }    // Group by postID
                    .mapValues { entry ->
                        entry.value.maxByOrNull { like -> like.timeStamp } // Get the latest like by timestamp
                    }
                    .values
                    .filterNotNull() // Remove null values, just in case

                for (like in latestLikes) {
                    val post = withContext(Dispatchers.IO) {
                        postDAO.getPostByID(like.postID)
                    }
                    if (post != null) {
                        likedPosts.add(post)
                        Log.d("HistoryPost", "Loaded liked post: ${post.postTitle}")
                    }
                }

                if (likedPosts.isNotEmpty()) {
                    adapter.updatePosts(likedPosts)
                } else {
                    adapter.updatePosts(emptyList())
                    Log.d("HistoryPost", "No liked posts found for the user.")
                }
            } catch (e: Exception) {
                Log.e("HistoryPost", "Error loading liked posts: ${e.message}")
            }
        }
    }



    private fun loadPostById(postID: String, onPostLoaded: (Post?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val post = postDAO.getPostByID(postID)
            Log.d("HistoryPost", "Loaded post: $post")
            onPostLoaded(post)
        }
    }
}
