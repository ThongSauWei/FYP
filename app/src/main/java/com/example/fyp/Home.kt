package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Post
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.logging.Filter
import java.util.logging.Handler

class Home : Fragment() {
    private lateinit var postViewModel: PostViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // Declare here
    private val postCategoryDAO = PostCategoryDAO()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize swipeRefreshLayout here
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Set toolbar layout
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener {

            lifecycleScope.launch {

                val fragment = Home()
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.fragmentContainerView, fragment)
                transaction?.addToBackStack(null)
                transaction?.commit()

                fetchPosts(isFollow = false) // Default: Show all posts

                delay(2000) // Simulate loading or update your RecyclerView data
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), "Content refreshed", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize ViewModels
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        friendViewModel = ViewModelProvider(this)[FriendViewModel::class.java]

        // Initialize RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setHasFixedSize(true)

        // Add Post Button Listener
        val btnAddHome = view.findViewById<ImageView>(R.id.btnAddPost)
        btnAddHome.setOnClickListener {
            val fragment = CreatePost()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

//search
        val searchImage = view.findViewById<ImageView>(R.id.searchImage)
        searchImage.setOnClickListener {
            val fragment = SearchPost()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        val filterImage = view.findViewById<ImageView>(R.id.filterImage)
        filterImage.setOnClickListener {
            val fragment = FilterPost()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        // Initialize Firebase Database and Storage references
        val databaseRef = FirebaseDatabase.getInstance().reference
        val storageRef = FirebaseStorage.getInstance().reference

        // Initialize RecyclerView Adapter with DAOs
        postAdapter = PostAdapter(
            posts = listOf(),
            userViewModel = userViewModel,
            postImageDAO = PostImageDAO(storageRef, databaseRef),
            postCategoryDAO = PostCategoryDAO(),
            likeDAO = LikeDAO(),
            postCommentDAO = PostCommentDAO(),
            saveDAO = SaveDAO(),
            context = requireContext(),
            postViewModel = postViewModel,
            friendViewModel = friendViewModel
        )

        recyclerView.adapter = postAdapter

        // Fetch and Populate Posts
        lifecycleScope.launch {
            try {
                val postList = postViewModel.getAllPosts()
                postAdapter.updatePosts(postList)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle dropdown selection (All / Follow)
        val spinnerFilter: Spinner = view.findViewById(R.id.spinnerFilter)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options,    // String array resource
            R.layout.spinner      // Custom layout for spinner item
        )

// Set dropdown layout explicitly
        adapter.setDropDownViewResource(R.layout.spinner)
        spinnerFilter.adapter = adapter



        spinnerFilter.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> fetchPosts(isFollow = false) // "All"
                    1 -> fetchPosts(isFollow = true)  // "Follow"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Default to showing all posts
                fetchPosts(isFollow = false)
            }
        })

        // Handle received filters from FilterPost
        val selectedCategories = arguments?.getStringArrayList("selectedCategories") ?: arrayListOf()
        val selectedDateRange = arguments?.getSerializable("selectedDateRange") as? Pair<Long?, Long?>
        Log.d("Home", "Received Categories: $selectedCategories")
        Log.d("Home", "Received Date Range: $selectedDateRange")

        if (selectedCategories.isNotEmpty() || selectedDateRange != null) {
            fetchFilteredPosts(selectedCategories, selectedDateRange)
        } else {
            fetchPosts(isFollow = false) // Default: Show all posts
        }

        val dateRange = arguments?.getSerializable("selectedDateRange") as? Pair<Long?, Long?>
        val startDate = dateRange?.first
        val endDate = dateRange?.second

        lifecycleScope.launch {
            try {
                // Fetch all posts
                val allPosts = postViewModel.getAllPosts()

                // Define a date format matching your `postDateTime` format
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Log the input parameters
                Log.d("Home", "Filtering posts with Date Range: Start = $startDate, End = $endDate")

                // Filter posts based on the date range
                val filteredPosts = allPosts.filter { post ->
                    try {
                        Log.d("Home", "Processing post with postDateTime: ${post.postDateTime}")

                        // Parse postDateTime to a timestamp
                        val postTimestamp = dateFormat.parse(post.postDateTime)?.time
                        Log.d("Home", "Parsed postDateTime to timestamp: $postTimestamp")

                        // Check if the post matches the filtering criteria
                        val matchesStartDate = startDate == null || (postTimestamp != null && postTimestamp >= startDate)
                        val matchesEndDate = endDate == null || (postTimestamp != null && postTimestamp <= endDate)

                        Log.d(
                            "Home",
                            "Post matchesStartDate: $matchesStartDate, matchesEndDate: $matchesEndDate"
                        )

                        postTimestamp != null && matchesStartDate && matchesEndDate
                    } catch (e: Exception) {
                        Log.e("Home", "Error parsing postDateTime: ${post.postDateTime}", e)
                        false // Exclude posts with invalid date format
                    }
                }

                // Log the filtered results
                Log.d("Home", "Filtered Posts Count: ${filteredPosts.size}")

                // Update the adapter with filtered posts
                postAdapter.updatePosts(filteredPosts)
            } catch (e: Exception) {
                Log.e("Home", "Failed to filter posts", e)
                Toast.makeText(requireContext(), "Failed to filter posts", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Function to fetch filtered posts
    private fun fetchFilteredPosts(categories: List<String>, dateRange: Pair<Long?, Long?>?) {
        lifecycleScope.launch {
            try {
                // Log the filtering process
                Log.d("Home", "Fetching filtered posts...")
                Log.d("Home", "Categories: $categories, Date Range: $dateRange")

                val allPosts = postViewModel.getAllPosts()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                val filteredPosts = mutableListOf<Post>()

                for (post in allPosts) {
                    // Fetch categories for the current post
                    val postCategories = postCategoryDAO.getCategoriesByPostID(post.postID)

                    val postTimestamp = try {
                        dateFormat.parse(post.postDateTime)?.time
                    } catch (e: Exception) {
                        Log.e("Home", "Error parsing post date: ${post.postDateTime}", e)
                        null
                    }

                    val matchesCategory = categories.isEmpty() || postCategories.any { it.category in categories }
                    val matchesDate = dateRange == null || (
                            postTimestamp != null &&
                                    (dateRange.first?.let { postTimestamp >= it } ?: true) &&
                                    (dateRange.second?.let { postTimestamp <= it } ?: true)
                            )

                    if (matchesCategory && matchesDate) {
                        filteredPosts.add(post)
                    }
                }

                Log.d("Home", "Filtered Posts Count: ${filteredPosts.size}")
                postAdapter.updatePosts(filteredPosts)
            } catch (e: Exception) {
                Log.e("Home", "Failed to load filtered posts", e)
                Toast.makeText(requireContext(), "Failed to load filtered posts", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun fetchPosts(isFollow: Boolean) {
        lifecycleScope.launch {
            try {
                val currentUserID = SaveSharedPreference.getUserID(requireContext())

                val postList = if (isFollow) {
                    val friends = friendViewModel.getFriendList(currentUserID)
                    val friendPosts = postViewModel.getPostsByUserIDs(friends.map { it.receiveUserID })
                    filterPostsByVisibility(friendPosts, currentUserID)
                } else {
                    val allPosts = postViewModel.getAllPosts()
                    filterPostsByVisibility(allPosts, currentUserID)
                }

                postAdapter.updatePosts(postList)
                swipeRefreshLayout.isRefreshing = false // Stop refreshing
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun filterPostsByVisibility(posts: List<Post>, currentUserID: String): List<Post> {
        val visiblePosts = mutableListOf<Post>()

        for (post in posts) {
            when (post.postType) {
                "Public" -> {
                    visiblePosts.add(post) // Public posts are always visible
                }
                "Private" -> {
                    if (post.userID == currentUserID) {
                        visiblePosts.add(post) // Private posts are visible to the user who created them
                    }
                }
                "Restricted" -> {
                    val hasAccess = postViewModel.checkIfUserHasAccessToPost(currentUserID, post.postID)
                    if (hasAccess) {
                        visiblePosts.add(post) // Restricted posts are visible if the user has access
                    }
                }
            }
        }

        return visiblePosts
    }
}
