package com.example.fyp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.FriendDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostSharedDAO
import com.example.fyp.dao.UserDAO
import com.example.fyp.data.User
import com.example.fyp.dataAdapter.RestrictedUser
import com.example.fyp.dataAdapter.RestrictedUserAdapter
import com.example.fyp.viewModel.PostCategoryViewModel
import com.example.fyp.viewModel.PostViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditRestrictedUser : Fragment() {
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
        val postID = arguments?.getString("POST_ID")
        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val categories = arguments?.getStringArrayList("categories")
        val imageUris = arguments?.getParcelableArrayList<Uri>("imageUris")

        // Customize toolbar appearance
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "Edit Restricted Users"

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() } // Set click behavior

        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE

        val btnSaveChanges = activity?.findViewById<ImageView>(R.id.btnChatToolbarWithAnnouce)
        btnSaveChanges?.setImageResource(R.drawable.send_post)

        btnSaveChanges?.setOnClickListener {
            updatePost(postID, title, description, categories, imageUris)
        }

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

    private fun updatePost(
        postID: String?,
        title: String?,
        description: String?,
        categories: ArrayList<String>?,
        imageUris: ArrayList<Uri>?
    ) {
        if (postID.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid Post ID", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val post = postViewModel.getPostByID(postID)
            if (post != null) {
                // Use existing values if no new data is provided
                val updatedTitle = title ?: post.postTitle
                val updatedDescription = description ?: post.postDescription
                val updatedCategories = categories ?: postCategoryViewModel.getCategoriesByPostID(postID).map { it.category }

                if (updatedTitle.isEmpty() || updatedDescription.isEmpty() || updatedCategories.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Please ensure all fields are filled", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                post.postTitle = updatedTitle
                post.postDescription = updatedDescription
                post.postType = "Restricted"

                postViewModel.updatePost(post) { success, exception ->
                    if (success) {
                        val userID = getCurrentUserID()

                        // Update categories
                        postCategoryViewModel.updateCategories(postID, ArrayList(updatedCategories), userID)

                        // Update images
                        val postImageDAO = PostImageDAO(
                            FirebaseStorage.getInstance().getReference("PostImages"),
                            FirebaseDatabase.getInstance().getReference("PostImage")
                        )
                        postImageDAO.uploadImages(postID, imageUris ?: emptyList(), userID) { success, _ ->
                            if (!success) {
                                Log.e("updatePost", "Failed to upload images.")
                            } else {
                                Log.d("updatePost", "Images uploaded successfully.")
                            }
                        }

                        // Update restricted users
                        val selectedUsers = adapter.getSelectedUsers()
                        val postSharedDAO = PostSharedDAO()
                        postSharedDAO.clearSharedPost(postID) { clearSuccess, _ ->
                            if (clearSuccess) {
                                selectedUsers.forEach { user ->
                                    postSharedDAO.addSharedPost(postID, user.userID) { addSuccess, exception ->
                                        if (addSuccess) {
                                            Log.d("updatePost", "User ${user.name} added to restricted list.")
                                        } else {
                                            Log.e("updatePost", "Failed to add user ${user.name}: ${exception?.message}")
                                        }
                                    }
                                }
                            }
                        }

                        // Notify the user and navigate back
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
                            navigateToProfile() // Navigate back to the profile
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to update post: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToProfile() {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val profileFragment = Profile()
        transaction?.replace(R.id.fragmentContainerView, profileFragment)
        transaction?.commit()
    }


    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }
}