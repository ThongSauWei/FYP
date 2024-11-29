package com.example.fyp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.ProfileDAO
import com.squareup.picasso.Picasso
import com.example.fyp.data.Post
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch

class Profile : Fragment() {

    private lateinit var friendViewModel : FriendViewModel
    private lateinit var userViewModel : UserViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var storageRef: StorageReference
    private lateinit var profileDao: ProfileDAO
    private lateinit var postList : List<Post>
    private lateinit var recyclerView : RecyclerView

    private lateinit var btnSettingProfile: ImageView
    private lateinit var changeImageClick: ImageView
    private lateinit var cardViewProfile: CardView
    private lateinit var btnEditProfile: Button
    private lateinit var currentUserID: String
    private lateinit var changeImage: ImageView
    private lateinit var scCreatePost: Button
    private lateinit var nameProfile: TextView
    private lateinit var DOBProfile: TextView
    private lateinit var courseProfile: TextView
    private lateinit var userBIOProfile: TextView
    private lateinit var ttlPostsProfile: TextView
    private lateinit var ttlFriendProfile: TextView
    private lateinit var languageProfile: TextView
    private lateinit var lblPostsClick: TextView
    private lateinit var lblFriendClick: TextView
    private lateinit var linearLayoutProfile: LinearLayout
    private lateinit var genderProfile: TextView
    private lateinit var imageGender: ImageView


    private val imagePickRequestCode = 1000
    private val backgroundPickRequestCode = 2000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        (activity as MainActivity).setToolbar(R.layout.toolbar, R.color.profile_color)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        nameProfile = view.findViewById(R.id.tvNameProfile)
        DOBProfile = view.findViewById(R.id.tvDOBProfile)
        courseProfile = view.findViewById(R.id.tvCoursesProfile)
        userBIOProfile = view.findViewById(R.id.tvBioProfile)
        ttlPostsProfile = view.findViewById(R.id.tvPostsProfile)
        ttlFriendProfile = view.findViewById(R.id.tvFriendsProfile)
        languageProfile = view.findViewById(R.id.tvLanguages)
        lblPostsClick = view.findViewById(R.id.lblPostsProfile)
        lblFriendClick = view.findViewById(R.id.lblFriendsProfile)
        genderProfile = view.findViewById(R.id.tvGenderProfile)
        imageGender = view.findViewById(R.id.imageGender)

        btnSettingProfile = view.findViewById(R.id.imgSettingsProfile)
        changeImageClick = view.findViewById(R.id.changeImage)
        changeImage = view.findViewById(R.id.imgProfileProfile)
        cardViewProfile = view.findViewById(R.id.cardViewProfile)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        scCreatePost = view.findViewById(R.id.btnCreatePostProfile)
        linearLayoutProfile = view.findViewById(R.id.linearLayoutProfile)
        currentUserID = SaveSharedPreference.getUserID(requireContext())
        profileDao = ProfileDAO()
        storageRef = FirebaseStorage.getInstance().getReference()

        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)

        parentFragmentManager.setFragmentResultListener("ProfileUpdatedKey", this) { _, _ ->
            setupProfileInfo(currentUserID) // Refresh profile info
        }

        loadProfilePicture()
        loadProfileBackground()
        onResume()

        recyclerView = view.findViewById(R.id.recyclerViewProfilePost)
        setupProfileInfo(currentUserID)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        linearLayoutProfile.setOnClickListener {
            pickBackgroundImageFromGallery()
        }

        scCreatePost.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = CreatePost()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        btnSettingProfile.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = Settings()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        changeImageClick.setOnClickListener {
            pickImageFromGallery()
        }

        ttlFriendProfile.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = Friends()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        lblFriendClick.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = Friends()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        btnEditProfile.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = EditProfile()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        cardViewProfile.setOnClickListener {
            val userID = SaveSharedPreference.getUserID(requireContext())
            val ref = storageRef.child("imageProfile").child("$userID.png")
            ref.downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val imageUrl = task.result.toString()
                    showImageInDialog(imageUrl) // Show image in a dialog
                } else {
                    Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }


    override fun onResume() {
        super.onResume()
        loadProfilePicture() // Refresh profile picture when returning to this fragment
        loadProfileBackground() // Refresh background as well, if necessary
        setupProfileInfo(currentUserID) // Reload user data
    }
    private fun setupProfileInfo(userID: String) {
        val userViewModel: UserViewModel =
            ViewModelProvider(this).get(UserViewModel::class.java)
        val postViewModel: PostViewModel =
            ViewModelProvider(this).get(PostViewModel::class.java)

        lifecycleScope.launch {
            val user = userViewModel.getUserByID(userID)!!
            val profile = profileViewModel.getProfile(userID)
            val totalFriend = friendViewModel.getFriendList(userID).size

            // Fetch user's posts
            postList = postViewModel.getPostByUser(userID)
            val totalPosts = postList.size

            // Populate UI fields
            nameProfile.text = user.username
            DOBProfile.text = user.userDOB
            courseProfile.text = profile?.userCourse ?: ""
            userBIOProfile.text = profile?.userBio ?: ""
            ttlFriendProfile.text = totalFriend.toString()
            ttlPostsProfile.text = totalPosts.toString()
            genderProfile.text = profile?.userGender ?: ""

            // Show appropriate gender icon
            if (profile?.userGender == "Female") {
                imageGender.setImageResource(R.drawable.baseline_female_24)
                imageGender.visibility = View.VISIBLE
            } else if(profile?.userGender == "Male"){
                imageGender.setImageResource(R.drawable.baseline_male_24)
                imageGender.visibility = View.VISIBLE
            } else {
                // Handle "Hide Gender" case or when gender is null
                imageGender.visibility = View.INVISIBLE // Completely hide the icon
                genderProfile.visibility = View.INVISIBLE // Completely hide the text

            }

            if (totalPosts == 0) {
                scCreatePost.visibility = View.VISIBLE
            } else {
                scCreatePost.visibility = View.INVISIBLE // Hide the button
            }

            // Populate RecyclerView
            if (postList.isNotEmpty()) {
                val adapter = PostAdapter(
                    posts = postList,
                    userViewModel = userViewModel,
                    postImageDAO = PostImageDAO(
                        FirebaseStorage.getInstance().reference,
                        FirebaseDatabase.getInstance().reference
                    ),
                    postCategoryDAO = PostCategoryDAO(),
                    likeDAO = LikeDAO(),
                    postCommentDAO = PostCommentDAO(),
                    isProfileMode = true // Custom flag for profile-specific logic
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun loadProfileBackground() {
        val userID = SaveSharedPreference.getUserID(requireContext())
        val ref = storageRef.child("userBackgroundImage").child("$userID.png")

        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                val linearLayoutProfile = view?.findViewById<LinearLayout>(R.id.linearLayoutProfile)
                Picasso.get().load(imageUrl).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                        val drawable = BitmapDrawable(resources, bitmap)
                        linearLayoutProfile?.background = drawable // Set the background as the loaded image
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        val defaultColor = resources.getColor(R.color.profile_color, null) // Get the default color
                        val linearLayoutProfile = view?.findViewById<LinearLayout>(R.id.linearLayoutProfile)
                        linearLayoutProfile?.setBackgroundColor(defaultColor) // Set the background color to default
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        // Placeholder logic (optional)
                    }
                })
            }
        }
    }



    private fun loadProfilePicture() {
        val userID = SaveSharedPreference.getUserID(requireContext())
        val ref = storageRef.child("imageProfile").child("$userID.png")

        val imageView = view?.findViewById<ImageView>(R.id.imgProfileProfile) // Declare imageView outside
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                imageView?.let {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.nullprofile) // Placeholder while loading
                        .error(R.drawable.nullprofile) // Default if loading fails
                        .into(it)
                }
            } else {
                // Set default image if no profile image exists
                imageView?.setImageResource(R.drawable.nullprofile)
            }
        }
    }


    private fun pickBackgroundImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, backgroundPickRequestCode)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickRequestCode)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { imageUri ->
                when (requestCode) {
                    imagePickRequestCode -> updateProfilePicture(imageUri) // Profile picture logic
                    backgroundPickRequestCode -> updateBackgroundImage(imageUri) // Background image logic
                }
            }
        }
    }


    private fun updateProfilePicture(imageUri: Uri) {
        val imageRef = storageRef.child("imageProfile").child("$currentUserID.png")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val newImageUrl = downloadUri.toString()
                    profileDao.updateProfilePicture(currentUserID, newImageUrl)
                    loadProfilePicture()
                    navigateToProfile()
                    Toast.makeText(requireContext(), "Image change successful!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateBackgroundImage(imageUri: Uri) {
        val imageRef = storageRef.child("userBackgroundImage").child("$currentUserID.png") // Firebase Storage path
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val newBackgroundUrl = downloadUri.toString()
                    profileDao.updateBackgroundImage(currentUserID, newBackgroundUrl) // Update database
                    loadProfileBackground() // Display the new image
                    Toast.makeText(requireContext(), "Background updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to upload background: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showImageInDialog(imageUrl: String) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_image_view) // Create this layout file (see below)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeFullImageButton)
        Picasso.get().load(imageUrl).into(imageView) // Load the image

        // Close the dialog when the button is clicked
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun navigateToProfile() {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val fragment = Profile()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}