package com.example.fyp

import android.os.Bundle
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
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Handler

class Home : Fragment() {
    private lateinit var postViewModel: PostViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // Declare here

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

        return view
    }

    private fun fetchPosts(isFollow: Boolean) {
        lifecycleScope.launch {
            try {
                val postList = if (isFollow) {
                    val currentUserID = SaveSharedPreference.getUserID(requireContext())
                    val friends = friendViewModel.getFriendList(currentUserID)
                    val friendPosts = postViewModel.getPostsByUserIDs(friends.map { it.receiveUserID })
                    friendPosts
                } else {
                    postViewModel.getAllPosts()
                }
                postAdapter.updatePosts(postList)
                swipeRefreshLayout.isRefreshing = false // This works now because swipeRefreshLayout is initialized at the class level
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
}
