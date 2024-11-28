package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Handler

class Home : Fragment() {

    private lateinit var postViewModel: PostViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Set toolbar layout
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                delay(2000) // Simulate loading or update your RecyclerView data
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), "Content refreshed", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize ViewModels
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

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
            postViewModel = postViewModel
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

        return view
    }

}
