package com.example.fyp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.FriendDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostSharedDAO
import com.example.fyp.dao.UserDAO
import com.example.fyp.data.Post
import com.example.fyp.data.User
import com.example.fyp.dataAdapter.FriendAdapter
import com.example.fyp.dataAdapter.RestrictedUser
import com.example.fyp.dataAdapter.RestrictedUserAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostCategoryViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RestrictedUser : Fragment() {

    private lateinit var postViewModel: PostViewModel
    private lateinit var postCategoryViewModel: PostCategoryViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestrictedUserAdapter
    private val friendDAO = FriendDAO()

    private var userList : ArrayList<User> = arrayListOf()
    private val profileList : ArrayList<Profile> = arrayListOf()

    data class RestrictedUser(
        val name: String,
        val isSelected: Boolean,
        val userID: String // Add userID here
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restricted, container, false)

        // Set up toolbar (assumes MainActivity has a method for this)
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)
        postCategoryViewModel = ViewModelProvider(this).get(PostCategoryViewModel::class.java)

        // Extract data from the arguments
        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val categories = arguments?.getStringArrayList("categories")
        val imageUris = arguments?.getParcelableArrayList<Uri>("imageUris")

        // Customize toolbar appearance
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "Restricted User"

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() } // Set click behavior

        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE

        val btnSearchToolbarWithAnnouce = activity?.findViewById<ImageView>(R.id.btnSearchToolbarWithAnnouce)
        btnSearchToolbarWithAnnouce?.setImageResource(R.drawable.send_post)

        btnSearchToolbarWithAnnouce?.setOnClickListener {
            savePostToFirebase(title, description, categories, imageUris)
        }

//        adapter
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewFriendFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Sample data
        val sampleUsers = mutableListOf(
            RestrictedUser("Ali", "U10001", false),
            RestrictedUser("Adam", "U1001", true),
            RestrictedUser("Alice", "U1002", false)
        )

// Set adapter
        adapter = RestrictedUserAdapter(sampleUsers) { user ->
            // Handle item click (e.g., toggle selection)
            println("User clicked: ${user.name}")
        }
        recyclerView.adapter = adapter

        fetchFriends()

        return view
    }

    private fun fetchFriends() {
        val userID = getCurrentUserID() // Get the current user's ID
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val friends = friendDAO.getFriendList(userID)
                val friendUsers = mutableListOf<com.example.fyp.dataAdapter.RestrictedUser>()

                friends.forEach { friend ->
                    val friendUserID =
                        if (friend.requestUserID == userID) friend.receiveUserID else friend.requestUserID

                    val userDAO = UserDAO()
                    val user = userDAO.getUserByID(friendUserID) // Fetch user details
                    val userName = user?.username ?: "Unknown User"

                    friendUsers.add(
                        com.example.fyp.dataAdapter.RestrictedUser(
                            name = userName,
                            isSelected = false,
                            userID = friendUserID
                        )
                    )
                }

                adapter.updateData(friendUsers)
            } catch (e: Exception) {
                Log.e("fetchFriends", "Error fetching friends: ${e.message}")
            }
        }
    }

    // Assuming the userList is part of the adapter
    private fun savePostToFirebase(
        title: String?,
        description: String?,
        categories: ArrayList<String>?,
        imageUris: ArrayList<Uri>?
    ) {
        if (title.isNullOrEmpty() || description.isNullOrEmpty() || categories.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please ensure all fields are filled", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getCurrentUserID() // Get the current user ID
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val postType = "Restricted" // Always "Restricted" for this fragment
        val post = Post("", currentDateTime, description, title, userId, 0, postType)

        postViewModel.addPost(post) { postID, exception ->
            if (postID != null) {
                Log.d("savePostToFirebase", "Post created with ID: $postID")

                // Save categories
                postCategoryViewModel.addCategories(postID, categories, userId) { success, _ ->
                    if (!success) {
                        Log.e("savePostToFirebase", "Failed to save categories.")
                    } else {
                        Log.d("savePostToFirebase", "Categories saved successfully.")
                    }
                }

                // Upload images
                val postImageDAO = PostImageDAO(
                    FirebaseStorage.getInstance().getReference("PostImages"),
                    FirebaseDatabase.getInstance().getReference("PostImage")
                )
                postImageDAO.uploadImages(postID, imageUris ?: emptyList(), userId) { success, _ ->
                    if (!success) {
                        Log.e("savePostToFirebase", "Failed to upload images.")
                    } else {
                        Log.d("savePostToFirebase", "Images uploaded successfully.")
                    }
                }

                // Access the user list through the adapter
                val selectedUsers = adapter.getSelectedUsers()
                Log.d("savePostToFirebase", "Selected users: ${selectedUsers.joinToString { it.name }}")

                // Iterate over the selected friends and save each one
                selectedUsers.forEach { selectedUser ->
                    Log.d("savePostToFirebase", "Saving user: ${selectedUser.name} (ID: ${selectedUser.userID}) to PostShared")

                    val postSharedDAO = PostSharedDAO()
                    postSharedDAO.addSharedPost(postID, selectedUser.userID) { success, exception ->
                        if (success) {
                            Log.d("savePostToFirebase", "User ${selectedUser.name} saved successfully to PostShared.")
                        } else {
                            Log.e("savePostToFirebase", "Failed to save user ${selectedUser.name}: ${exception?.message}")
                        }
                    }
                }

                // Show success message and navigate back to the Home fragment
                Toast.makeText(requireContext(), "Post created successfully!", Toast.LENGTH_SHORT).show()

                // Pass success message to the Home fragment
                val homeFragment = Home()
                val bundle = Bundle()
                bundle.putString("successMessage", "Post created successfully!")
                homeFragment.arguments = bundle

                // Replace the current fragment with the Home fragment
                val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
                fragmentTransaction?.replace(R.id.fragmentContainerView, homeFragment)
                fragmentTransaction?.commit()
            } else {
                Toast.makeText(requireContext(), "Failed to create post: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }
}
