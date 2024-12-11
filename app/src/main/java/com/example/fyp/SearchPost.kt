package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.PostViewHistoryDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Post
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.repository.PostViewHistoryRepository
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostCategoryViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.UserViewModel
import com.example.fyp.viewModelFactory.PostViewHistoryViewModelFactory
import com.example.fyp.viewModel.PostViewHistoryViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SearchPost : Fragment() {
    private lateinit var postAdapter: PostAdapter
    private lateinit var postViewModel: PostViewModel
    private lateinit var postViewHistoryViewModel: PostViewHistoryViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var postCategoryViewModel: PostCategoryViewModel
    private var selectedCard: CardView? = null
    private lateinit var postViewHistoryRepository: PostViewHistoryRepository
    private lateinit var postViewHistoryDAO: PostViewHistoryDAO

    private val cardToTextViewMap = mapOf(
        R.id.allCard to R.id.tvAll,
        R.id.bussCard to R.id.tvBuss,
        R.id.sciCard to R.id.tvSci,
        R.id.artCard to R.id.tvArt,
        R.id.itCard to R.id.tvIt,
        R.id.sportCard to R.id.tvSport,
        R.id.langCard to R.id.tvLang,
        R.id.assCard to R.id.tvAss,
        R.id.revisionCard to R.id.tvRevision
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize postViewHistoryDAO before using it
        postViewHistoryDAO = PostViewHistoryDAO()


        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)
        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)
        postCategoryViewModel = ViewModelProvider(this).get(PostCategoryViewModel::class.java)
//        postViewHistoryViewModel = ViewModelProvider(this)[PostViewHistoryViewModel::class.java]

        postViewHistoryRepository = PostViewHistoryRepository(postViewHistoryDAO)
        val postViewHistoryViewModelFactory = PostViewHistoryViewModelFactory(postViewHistoryRepository)
        postViewHistoryViewModel = ViewModelProvider(this, postViewHistoryViewModelFactory).get(PostViewHistoryViewModel::class.java)


        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewFriendSearchFriend)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        val btnSearch: AppCompatButton = view.findViewById(R.id.btnSearchSearchFriend)
        val txtSearch: EditText = view.findViewById(R.id.txtSearchSearchFriend)

//        txtSearch.setOnClickListener {
//            txtSearch.text.clear()
//        }

        txtSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                txtSearch.text.clear()
            }
        }

        val databaseRef = FirebaseDatabase.getInstance().reference
        val storageRef = FirebaseStorage.getInstance().reference

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
            postViewHistoryViewModel = postViewHistoryViewModel,
            friendViewModel = friendViewModel
        )

        recyclerView.adapter = postAdapter
        fetchPosts()

        btnSearch.setOnClickListener {
            val inputText = txtSearch.text.toString().trim()
            searchPost(inputText)
        }

        val categories = mapOf(
            R.id.allCard to "All",
            R.id.bussCard to "Business",
            R.id.sciCard to "Science",
            R.id.artCard to "Art / Design",
            R.id.itCard to "IT",
            R.id.sportCard to "Sport",
            R.id.langCard to "Language",
            R.id.assCard to "Assignment",
            R.id.revisionCard to "Revision"
        )

        categories.forEach { (cardId, category) ->
            val cardView = view.findViewById<CardView>(cardId) // Safely find the CardView
            cardView?.setOnClickListener {
                setSelectedCard(cardView) // Highlight selected card
                if (category == "All") {
                    fetchAndDisplayPosts()
                } else {
                    fetchAndDisplayPostsByCategory(category)
                }
            }
        }

        return view
    }

    private fun setSelectedCard(cardView: CardView) {
        selectedCard?.let { resetCardStyle(it) } // Reset the previous card
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))

        cardToTextViewMap[cardView.id]?.let { textViewId ->
            val textView = view?.findViewById<TextView>(textViewId)
            textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } ?: Log.e("SearchPost", "No mapping found for CardView ID: ${cardView.id}")

        selectedCard = cardView // Update reference to the current card
    }



    private fun resetCardStyle(cardView: CardView) {
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))

        cardToTextViewMap[cardView.id]?.let { textViewId ->
            val textView = view?.findViewById<TextView>(textViewId)
            textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } ?: Log.e("SearchPost", "No mapping found for CardView ID: ${cardView.id}")
    }

    private fun fetchAndDisplayPosts() {
        lifecycleScope.launch {
            try {
//                val posts = postViewModel.getAllPosts()

                val allPosts = postViewModel.getAllPosts()
                val currentUserID = SaveSharedPreference.getUserID(requireContext())
                val postListFiltered = filterPostsByVisibility(allPosts, currentUserID)



                displayPosts(postListFiltered)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching posts", Toast.LENGTH_SHORT).show()
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

    private fun fetchAndDisplayPostsByCategory(category: String) {
        lifecycleScope.launch {
            try {
                Log.d("SearchPost", "Fetching posts for category: $category")
                val posts = postCategoryViewModel.getPostByCategory(category)


//                val allPosts = postViewModel.getAllPosts()
                val currentUserID = SaveSharedPreference.getUserID(requireContext())
                val postListFiltered = filterPostsByVisibility(posts, currentUserID)


                Log.d("SearchPost", "Fetched ${posts.size} posts for category: $category")
                displayPosts(postListFiltered)
            } catch (e: Exception) {
                Log.e("SearchPost", "Error fetching posts for category: $category", e)
                Toast.makeText(requireContext(), "Error fetching posts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayPosts(posts: List<Post>) {
        Log.d("SearchPost", "Displaying ${posts.size} posts")
        val noPostsTextView: TextView = view?.findViewById(R.id.noPostsTextView) ?: return
        if (posts.isEmpty()) {
            Log.d("SearchPost", "No posts to display")
            noPostsTextView.visibility = View.VISIBLE
        } else {
            Log.d("SearchPost", "Posts found: ${posts.map { it.postTitle }}")
            noPostsTextView.visibility = View.GONE
        }
        postAdapter.updatePosts(posts)
    }


    private fun searchPost(inputText: String) {
        if (inputText.isEmpty()) {
            fetchPosts()
        } else {
            lifecycleScope.launch {
                try {
                    val posts = postViewModel.searchPost(inputText)
                    displayPosts(posts)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error searching posts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchPosts() {
        lifecycleScope.launch {
            try {
//                val posts = postViewModel.getAllPosts()

                val allPosts = postViewModel.getAllPosts()
                val currentUserID = SaveSharedPreference.getUserID(requireContext())
                val postListFiltered = filterPostsByVisibility(allPosts, currentUserID)

                displayPosts(postListFiltered)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching posts", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
