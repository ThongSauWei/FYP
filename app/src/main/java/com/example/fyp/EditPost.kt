package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fyp.viewModel.PostCategoryViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.PostImageViewModel
import com.example.fyp.viewModelFactory.PostImageViewModelFactory
import com.example.fyp.repository.PostImageRepository
import com.example.fyp.dao.PostImageDAO
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditPost : Fragment() {

    private lateinit var postViewModel: PostViewModel
    private lateinit var postCategoryViewModel: PostCategoryViewModel
    private lateinit var postImageViewModel: PostImageViewModel

    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var linearLayoutImagesEdit: LinearLayout

    private lateinit var publicCard: CardView
    private lateinit var privateCard: CardView
    private lateinit var resCard: CardView

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

    private val imageUris = mutableListOf<Uri>()
    private val imagesToDelete = mutableListOf<String>()
    private val imagePickRequestCode = 1000
    private var postID: String? = null

    private val selectedCategories = mutableSetOf<String>()
    private var selectedPrivacy: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_post, container, false)

        txtTitle = view.findViewById(R.id.txtTitleEditPost)
        txtDescription = view.findViewById(R.id.txtDescriptionEditPost)
        linearLayoutImagesEdit = view.findViewById(R.id.linearLayoutImagesEdit)

        initializeCategoryCards(view)
        initializePrivacyCards(view)

        postID = arguments?.getString("POST_ID")

        val storageRef = FirebaseStorage.getInstance().getReference("PostImages")
        val databaseRef = FirebaseDatabase.getInstance().getReference("PostImage")
        val postImageRepository = PostImageRepository(PostImageDAO(storageRef, databaseRef))
        val postImageViewModelFactory = PostImageViewModelFactory(postImageRepository)

        postImageViewModel = ViewModelProvider(this, postImageViewModelFactory)[PostImageViewModel::class.java]
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        postCategoryViewModel = ViewModelProvider(this)[PostCategoryViewModel::class.java]

        if (postID != null) {
            loadPostDetails(postID!!)
        }

        setupToolbar()

        return view
    }

    private fun initializeCategoryCards(view: View) {
        assBussCard = view.findViewById(R.id.assBussEdit)
        sciCard = view.findViewById(R.id.sciCardEdit)
        artCard = view.findViewById(R.id.artCardEdit)
        itCard = view.findViewById(R.id.itCardEdit)
        sportCard = view.findViewById(R.id.sportCardEdit)
        langCard = view.findViewById(R.id.langCardEdit)
        assCard = view.findViewById(R.id.assCardEdit)
        revisionCard = view.findViewById(R.id.revisionCardEdit)
        studyBuddyCard = view.findViewById(R.id.studyBuddyCardEdit)
        helperCard = view.findViewById(R.id.helperCardEdit)
        otherCard = view.findViewById(R.id.otherCardEdit)

        assBussCard.setOnClickListener { toggleCategory("Business", assBussCard) }
        sciCard.setOnClickListener { toggleCategory("Science", sciCard) }
        artCard.setOnClickListener { toggleCategory("Art / Design", artCard) }
        itCard.setOnClickListener { toggleCategory("IT", itCard) }
        sportCard.setOnClickListener { toggleCategory("Sport", sportCard) }
        langCard.setOnClickListener { toggleCategory("Language", langCard) }
        assCard.setOnClickListener { toggleCategory("Assignment", assCard) }
        revisionCard.setOnClickListener { toggleCategory("Revision", revisionCard) }
        studyBuddyCard.setOnClickListener { toggleCategory("Find Study Partner", studyBuddyCard) }
        helperCard.setOnClickListener { toggleCategory("Find Helper", helperCard) }
        otherCard.setOnClickListener { toggleCategory("Other", otherCard) }
    }

    private fun initializePrivacyCards(view: View) {
        publicCard = view.findViewById(R.id.publicCardEdit)
        privateCard = view.findViewById(R.id.privateCardEdit)
        resCard = view.findViewById(R.id.resCardEdit)

        publicCard.setOnClickListener { selectPrivacy("Public", publicCard) }
        privateCard.setOnClickListener { selectPrivacy("Private", privateCard) }
        resCard.setOnClickListener {
            selectPrivacy("Restricted", resCard)
            navigateToRestrictedUserPage()
        }
    }

    private fun setupToolbar() {
        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24)
        navIcon?.setOnClickListener { activity?.onBackPressed() }

        val btnSave = activity?.findViewById<ImageView>(R.id.btnChatToolbarWithAnnouce)
        btnSave?.setImageResource(R.drawable.send_post)
        btnSave?.setOnClickListener { savePost() }
    }

    private fun navigateToRestrictedUserPage() {
        val fragment = EditRestrictedUser()
        val bundle = Bundle().apply {
            putString("POST_ID", postID) // Pass the postID here
            putString("title", txtTitle.text.toString().trim())
            putString("description", txtDescription.text.toString().trim())
            putStringArrayList("categories", ArrayList(selectedCategories))
            putParcelableArrayList("imageUris", ArrayList(imageUris))
        }
        fragment.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragmentContainerView, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun loadPostDetails(postID: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val post = postViewModel.getPostByID(postID)
            val categories = postCategoryViewModel.getCategoriesByPostID(postID)
            val images = postImageViewModel.getImagesByPostID(postID)

            withContext(Dispatchers.Main) {
                txtTitle.setText(post?.postTitle)
                txtDescription.setText(post?.postDescription)

                when (post?.postType) {
                    "Public" -> selectPrivacy("Public", publicCard)
                    "Private" -> selectPrivacy("Private", privateCard)
                    "Restricted" -> selectPrivacy("Restricted", resCard)
                }

                categories.forEach { toggleCategory(it.category, null) }
                images.forEach { addImageToScrollViewWithRemove(Uri.parse(it.postImage), it.postImageID) }

                addAddButtonToScrollView()
            }
        }
    }

    private fun toggleCategory(category: String, cardView: CardView?) {
        val targetCardView = cardView ?: when (category) {
            "Business" -> assBussCard
            "Science" -> sciCard
            "Art / Design" -> artCard
            "IT" -> itCard
            "Sport" -> sportCard
            "Language" -> langCard
            "Assignment" -> assCard
            "Revision" -> revisionCard
            "Find Study Partner" -> studyBuddyCard
            "Find Helper" -> helperCard
            "Other" -> otherCard
            else -> null
        }

        targetCardView?.let {
            if (selectedCategories.contains(category)) {
                selectedCategories.remove(category)
                it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
            } else {
                selectedCategories.add(category)
                it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_yellow))
            }
        }
    }

    private fun selectPrivacy(privacy: String, selectedCardView: CardView) {
        listOf(publicCard, privateCard, resCard).forEach {
            it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
        }
        selectedCardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_yellow))
        selectedPrivacy = privacy
    }

    private fun savePost() {
        val title = txtTitle.text.toString().trim()
        val description = txtDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one category", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val existingImages = postImageViewModel.getImagesByPostID(postID!!)
                val remainingImagesCount = existingImages.size - imagesToDelete.size

                // 确保至少有一张图片
                if (remainingImagesCount <= 0 && imageUris.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Please upload at least one image", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 删除图片
                imagesToDelete.forEach { imageID ->
                    postImageViewModel.deleteImageByID(imageID) { success, exception ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (success) {
                                Log.d("EditPost", "Image $imageID deleted successfully")
                            } else {
                                Log.e("EditPost", "Failed to delete image $imageID: ${exception?.message}")
                                Toast.makeText(requireContext(), "Failed to delete some images. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 上传新图片
                if (imageUris.isNotEmpty()) {
                    val userId = SaveSharedPreference.getUserID(requireContext())
                    postImageViewModel.uploadImages(postID!!, imageUris, userId) { success, uploadException ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (success) {
                                Log.d("EditPost", "Images uploaded successfully for post: $postID")
                            } else {
                                Log.e("EditPost", "Failed to upload images: ${uploadException?.message}")
                                Toast.makeText(requireContext(), "Failed to upload images. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Update post
                val post = postViewModel.getPostByID(postID!!)
                if (post != null) {
                    post.postTitle = title
                    post.postDescription = description
                    post.postType = selectedPrivacy.toString()

                    val userID = SaveSharedPreference.getUserID(requireContext())
                    val existingCategories = postCategoryViewModel.getCategoriesByPostID(postID!!).map { it.category }

                    val categoriesToAdd = selectedCategories.filter { !existingCategories.contains(it) }
                    val categoriesToRemove = existingCategories.filter { !selectedCategories.contains(it) }

                    categoriesToAdd.forEach { category ->
                        postCategoryViewModel.addCategory(postID!!, category, userID) { success, _ ->
                            if (success) Log.d("EditPost", "Category added: $category")
                        }
                    }

                    categoriesToRemove.forEach { category ->
                        postCategoryViewModel.deleteCategoryByPostAndUser(postID!!, category, userID) { success, _ ->
                            if (success) Log.d("EditPost", "Category removed: $category")
                        }
                    }

                    postViewModel.updatePost(post) { success, exception ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (success) {
                                navigateToProfile()
                                Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to update post: ${exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EditPost", "Error saving post: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun navigateToProfile() {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val profileFragment = Profile()
        transaction?.replace(R.id.fragmentContainerView, profileFragment)
        transaction?.addToBackStack(null) // Optional, depending on whether you want to allow back navigation
        transaction?.commit()
    }



    private fun addImageToScrollViewWithRemove(uri: Uri, imageID: String) {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(16, 16, 16, 16) }
            radius = 10f
            elevation = 4f
        }

        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP // Make the image fill the box
            Glide.with(this@EditPost)
                .load(uri)
                .placeholder(R.drawable.nullprofile) // Placeholder image
                .error(R.drawable.red_icon) // Error image
                .into(this)
        }

        val removeButton = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(60, 60).apply {
                setMargins(8, 8, 0, 0)
                gravity = Gravity.END or Gravity.TOP
            }
            setImageResource(R.drawable.baseline_remove_circle_24)
            setOnClickListener {
                imagesToDelete.add(imageID)
                linearLayoutImagesEdit.removeView(cardView)
            }
        }

        container.addView(imageView)
        container.addView(removeButton)
        cardView.addView(container)
        linearLayoutImagesEdit.addView(cardView)
    }


    private fun addAddButtonToScrollView() {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(16, 16, 16, 16) }
            radius = 10f
            elevation = 4f
        }

        val addButton = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(R.drawable.baseline_add_24)
            setOnClickListener { pickImageFromGallery() }
        }

        cardView.addView(addButton)
        linearLayoutImagesEdit.addView(cardView)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                imageUris.add(imageUri)
                addImageToScrollViewWithRemove(imageUri, "")
            }
        }
    }
}
