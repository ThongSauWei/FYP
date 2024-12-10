package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Post
import com.example.fyp.viewModel.PostCategoryViewModel
import com.example.fyp.viewModel.PostSharedViewModel
import com.example.fyp.viewModel.PostViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePost : Fragment() {

    private lateinit var linearLayoutImages: LinearLayout
    private val imageUris = mutableListOf<Uri>() // List to store image URIs
    private lateinit var imageView: ImageView
    private val imagePickRequestCode = 1000
    private var imageUri: Uri? = null

    // For storing the selected category and privacy values
    private var selectedCategory: String? = null
    private var selectedPrivacy: String? = null

//    category
    private lateinit var assBussCard: CardView
    private lateinit var sciCard: CardView
    private lateinit var artCard: CardView
    private lateinit var itCard: CardView
    private lateinit var sportCard: CardView
    private lateinit var langCard: CardView
    private lateinit var assCard: CardView
    private lateinit var revisionCard: CardView
    private lateinit var studyBuddyCard: CardView
    private lateinit var helperCard: CardView
    private lateinit var otherCard: CardView

//    privacy
    private lateinit var publicCard: CardView
    private lateinit var privateCard: CardView
    private lateinit var resCard: CardView

    //store selected
    private val selectedCategories = mutableSetOf<String>()
    private val selectedPrivacies = mutableSetOf<String>()

    // Image selection logic
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickRequestCode)
    }

//    viewmodel
    private lateinit var postViewModel: PostViewModel
    private lateinit var postCategoryViewModel: PostCategoryViewModel
    private lateinit var postSharedViewModel: PostSharedViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_post, container, false)

        // Set up toolbar (assumes MainActivity has a method for this)
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        // Initialize UI elements
        linearLayoutImages = view.findViewById(R.id.linearLayoutImages)

        // Initialize category and privacy cards
//        category
        assBussCard = view.findViewById(R.id.assBuss)
        sciCard = view.findViewById(R.id.sciCard)
        artCard = view.findViewById(R.id.artCard)
        itCard = view.findViewById(R.id.itCard)
        sportCard = view.findViewById(R.id.sportCard)
        langCard = view.findViewById(R.id.langCard)
        assCard = view.findViewById(R.id.assCard)
        revisionCard = view.findViewById(R.id.revisionCard)
        studyBuddyCard = view.findViewById(R.id.studyBuddyCard)
        helperCard = view.findViewById(R.id.helperCard)
        otherCard = view.findViewById(R.id.otherCard)

//        privacy
        publicCard = view.findViewById(R.id.publicCard)
        privateCard = view.findViewById(R.id.privateCard)
        resCard = view.findViewById(R.id.resCard)

        // Add the "Add" button dynamically to the scrollable layout
        addAddButtonToScrollView()

        // Customize toolbar appearance
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "CREATE POST"

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() } // Set click behavior

        val btnSearchToolbarWithAnnouce = activity?.findViewById<ImageView>(R.id.btnChatToolbarWithAnnouce)
        btnSearchToolbarWithAnnouce?.setImageResource(R.drawable.send_post)

// Convert 30dp to pixels
        val density = resources.displayMetrics.density
        val sizeInPixels = (30 * density).toInt()

// Set the width and height to 30dp (in pixels)
        val layoutParams = btnSearchToolbarWithAnnouce?.layoutParams
        layoutParams?.width = sizeInPixels
        layoutParams?.height = sizeInPixels
        btnSearchToolbarWithAnnouce?.layoutParams = layoutParams


        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE

        // Set up category selection listeners
        setupCategorySelection()

        // Set up privacy selection listeners
        setupPrivacySelection()

        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)
        postCategoryViewModel = ViewModelProvider(this).get(PostCategoryViewModel::class.java)
        postSharedViewModel = ViewModelProvider(this).get(PostSharedViewModel::class.java)

        btnSearchToolbarWithAnnouce?.setOnClickListener {
            savePostToFirebase()
        }

        return view
    }

    private fun resizeImage(uri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            Bitmap.createScaledBitmap(originalBitmap, 800, 800, true) // Resize to 800x800
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                imageUris.add(imageUri) // Add image URI to the list
                addImageToScrollView(imageUri)
            } else {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addImageToScrollView(uri: Uri) {
        val resizedBitmap = resizeImage(uri)
        if (resizedBitmap != null) {
            // Create a CardView for the image
            val cardView = createCardView()
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(resizedBitmap) // Set the resized image bitmap
            }

            cardView.addView(imageView) // Add the image to the card
            linearLayoutImages.addView(cardView, linearLayoutImages.childCount - 1) // Insert before the Add button
        } else {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCardView(): CardView {
        return CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(16, 16, 16, 16) // Margin between cards
            }
            radius = 10f // Rounded corners
            elevation = 4f // Shadow effect
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary))
        }
    }

    private fun addAddButtonToScrollView() {
        // Create a CardView for the "Add" button
        val cardView = createCardView()

        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // Add button inside the CardView
        val addButton = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE // Scale image inside CardView
            setImageResource(R.drawable.baseline_add_24)
            setPadding(16, 16, 16, 16)

            // open the image picker
            setOnClickListener {
                pickImageFromGallery()
            }
        }

        cardView.addView(addButton) // Add the button inside the CardView
        linearLayoutImages.addView(cardView) // Add the CardView to the end of the layout
    }

    private fun setupCategorySelection() {
        assBussCard.setOnClickListener { selectCategory("Business", assBussCard) }
        sciCard.setOnClickListener { selectCategory("Science", sciCard) }
        artCard.setOnClickListener { selectCategory("Art / Design", artCard) }
        itCard.setOnClickListener { selectCategory("IT", itCard) }
        sportCard.setOnClickListener { selectCategory("Sport", sportCard) }
        langCard.setOnClickListener { selectCategory("Language", langCard) }
        assCard.setOnClickListener { selectCategory("Assignment", assCard) }
        revisionCard.setOnClickListener { selectCategory("Revision", revisionCard) }
        studyBuddyCard.setOnClickListener { selectCategory("Find Study Partner", studyBuddyCard) }
        helperCard.setOnClickListener { selectCategory("Find Helper", helperCard) }
        otherCard.setOnClickListener { selectCategory("Other", otherCard) }
    }

    private fun setupPrivacySelection() {
        publicCard.setOnClickListener { selectPrivacy("Public", publicCard) }
        privateCard.setOnClickListener { selectPrivacy("Private", privateCard) }
        resCard.setOnClickListener { selectPrivacy("Restricted", resCard) }
    }

    private fun selectCategory(category: String, cardView: CardView) {
        if (selectedCategories.contains(category)) {
            // Deselect the card if it's already selected
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
            selectedCategories.remove(category) // Remove from the set
            Log.d("CreatePost", "Deselected Category: $category")
        } else {
            // Select the new category
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_yellow))
            selectedCategories.add(category) // Add to the set
            Log.d("CreatePost", "Selected Category: $category")
        }

        // Log all currently selected categories
        Log.d("CreatePost", "All Selected Categories: $selectedCategories")
    }

    private fun selectPrivacy(privacy: String, selectedCardView: CardView) {
        // Clear previously selected privacy
        listOf(publicCard, privateCard, resCard).forEach { cardView ->
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
        }
        selectedPrivacies.clear() // Clear the set of selected privacies

        // Select the new privacy
        selectedCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_yellow))
        selectedPrivacies.add(privacy) // Add the selected privacy to the set

        // Navigate to RestrictedUser fragment if "Restricted" privacy is selected
        if (privacy == "Restricted") {
            val fragment = RestrictedUser()

            // Prepare the post data bundle
            val bundle = Bundle().apply {
                putString("title", view?.findViewById<EditText>(R.id.txtTitleCreatePost)?.text.toString().trim())
                putString("description", view?.findViewById<EditText>(R.id.txtDescriptionCreatePost)?.text.toString().trim())
                putStringArrayList("categories", ArrayList(selectedCategories))
                putParcelableArrayList("imageUris", ArrayList(imageUris))
            }

            // Attach the bundle to the fragment
            fragment.arguments = bundle

            // Navigate to the fragment
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        // Log the selection
        Log.d("CreatePost", "Selected Privacy: $privacy")
        Log.d("CreatePost", "All Selected Privacy: $selectedPrivacies")
    }



    private fun validateFields(): Boolean {
        // Ensure all fields are filled in
        val title = view?.findViewById<EditText>(R.id.txtTitleCreatePost)?.text.toString().trim()
        val description = view?.findViewById<EditText>(R.id.txtDescriptionCreatePost)?.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please provide a title", Toast.LENGTH_SHORT).show()
            return false
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Please provide a description", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one category", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedPrivacies.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a privacy option", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun savePostToFirebase() {
        if (!validateFields()) return

        val postImageDAO = PostImageDAO(
            FirebaseStorage.getInstance().getReference("PostImages"),
            FirebaseDatabase.getInstance().getReference("PostImage")
        )

        // Get values from the UI
        val title = view?.findViewById<EditText>(R.id.txtTitleCreatePost)?.text.toString().trim()
        val description = view?.findViewById<EditText>(R.id.txtDescriptionCreatePost)?.text.toString().trim()
        val userId = getCurrentUserID()
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val postType = selectedPrivacies.firstOrNull() ?: "Public"
        val post = Post("", currentDateTime, description, title, userId, 0, postType)

        // Add the post to the database
        postViewModel.addPost(post) { postID, exception ->
            if (postID != null) {
                Log.d("CreatePost", "Post created with ID: $postID")

                // Save categories
                postCategoryViewModel.addCategories(postID, selectedCategories.toList(), userId) { success, catException ->
                    if (success) {
                        Log.d("CreatePost", "Categories added successfully for post: $postID")
                    } else {
                        Log.e("CreatePost", "Failed to add categories: ${catException?.message}")
                    }
                }

                // Upload images with the correct postID
                postImageDAO.uploadImages(postID, imageUris, userId) { success, uploadException ->
                    if (success) {
                        Log.d("CreatePost", "Images uploaded successfully for post: $postID")
                    } else {
                        Log.e("CreatePost", "Failed to upload images: ${uploadException?.message}")
                    }
                }

                // Handle private posts
                if (postType == "Private") {
                    postSharedViewModel.addSharedPost(postID, userId) { _, sharedException ->
                        if (sharedException != null) {
                            Log.e("CreatePost", "Failed to share post: ${sharedException.message}")
                        } else {
                            Log.d("CreatePost", "Private post shared successfully: $postID")
                        }
                    }
                }

                // Notify user of success
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
                // Notify user of failure
                Toast.makeText(requireContext(), "Failed to create post: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }
}
