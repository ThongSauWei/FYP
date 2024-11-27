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
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

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

        // Initialize ViewModels
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // Retrieve success message from arguments (if any)
//        val successMessage = arguments?.getString("successMessage")
//        if (!successMessage.isNullOrEmpty()) {
//            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
//        }

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
            posts = listOf(), // Initially empty; populate below
            userViewModel = userViewModel,
            postImageDAO = PostImageDAO(storageRef, databaseRef), // Pass both storageRef and databaseRef
            postCategoryDAO = PostCategoryDAO(),                  // No need for databaseRef here
            likeDAO = LikeDAO(),                                  // No need for databaseRef here
            postCommentDAO = PostCommentDAO()                      // No need for databaseRef here
        )
        recyclerView.adapter = postAdapter

        // Fetch and Populate Posts
        lifecycleScope.launch {
            val postList = postViewModel.getAllPosts()
            postAdapter.updatePosts(postList) // Update the adapter's data
        }

        return view
    }
}
