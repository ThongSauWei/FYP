package com.example.fyp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class EditProfile : Fragment() {

    private lateinit var txtNameEditProfile: EditText
    private lateinit var txtPhoneNoEditProfile: EditText
    private lateinit var txtEmailEditProfile: EditText
    private lateinit var txtBirthdayEditProfile: EditText
    private lateinit var txtDescriptionEditProfile: EditText
    private lateinit var spinnerCourseEditProfile: Spinner
    private lateinit var btnTickSaveEditProfile: ImageView
    private lateinit var imgProfile: ImageView
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView

    private var selectedImageUri: Uri? = null
    private lateinit var storageRef: StorageReference
    private lateinit var userViewModel: UserViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var currentUser: User
    private lateinit var currentProfile: Profile
    private lateinit var courseArray: Array<String>
    private val imagePickRequestCode = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        initViews(view)
        initViewModels()

        val userID = SaveSharedPreference.getUserID(requireContext())
        lifecycleScope.launch(Dispatchers.Main) {
            currentUser = userViewModel.getUserByID(userID) ?: User()
            currentProfile = profileViewModel.getProfile(currentUser.userID) ?: Profile()
            populateFields(currentUser, currentProfile)
        }

        setListeners()
        return view
    }

    private fun initViews(view: View) {
        txtNameEditProfile = view.findViewById(R.id.txtNameEditProfile)
        txtEmailEditProfile = view.findViewById(R.id.txtEmailEditProfile)
        txtPhoneNoEditProfile = view.findViewById(R.id.txtPhoneEditProfile)
        txtBirthdayEditProfile = view.findViewById(R.id.txtBirthdayEditProfile)
        txtDescriptionEditProfile = view.findViewById(R.id.txtDescriptionEditProfile)
        spinnerCourseEditProfile = view.findViewById(R.id.ddlCourseEditProfile)
        imgProfile = view.findViewById(R.id.imgProfileEdit)
        saveButton = view.findViewById(R.id.btnSaveEditProfile)
        btnTickSaveEditProfile = view.findViewById(R.id.btnTickSaveEditProfile)
        backButton = view.findViewById(R.id.btnBackEditProfile)

        storageRef = FirebaseStorage.getInstance().reference
        courseArray = resources.getStringArray(R.array.course)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourseEditProfile.adapter = adapter
    }

    private fun initViewModels() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
    }

    private fun populateFields(user: User, profile: Profile) {
        txtNameEditProfile.setText(user.username)
        txtEmailEditProfile.setText(user.email)
        txtPhoneNoEditProfile.setText(user.userMobileNo)
        txtBirthdayEditProfile.setText(user.userDOB)
        txtDescriptionEditProfile.setText(profile.userBio)
        val courseIndex = courseArray.indexOf(profile.userCourse)
        spinnerCourseEditProfile.setSelection(if (courseIndex != -1) courseIndex else 0)

        val profileRef = storageRef.child("imageProfile").child("${user.userID}.png")
        profileRef.downloadUrl.addOnSuccessListener { uri ->
            Picasso.get().load(uri).into(imgProfile)
        }.addOnFailureListener {
            imgProfile.setImageResource(R.drawable.nullprofile)
        }
    }

    private fun setListeners() {
        saveButton.setOnClickListener { updateProfile() }
        btnTickSaveEditProfile.setOnClickListener { updateProfile() }
        backButton.setOnClickListener { activity?.supportFragmentManager?.popBackStack() }
        imgProfile.setOnClickListener { openGallery() }
        txtBirthdayEditProfile.setOnClickListener { showDatePicker() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imgProfile.setImageURI(selectedImageUri)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                txtBirthdayEditProfile.setText("$day/${month + 1}/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateProfile() {
        val updatedUser = currentUser.copy(
            username = txtNameEditProfile.text.toString(),
            userMobileNo = txtPhoneNoEditProfile.text.toString(),
            userDOB = txtBirthdayEditProfile.text.toString()
        )

        val updatedProfile = currentProfile.copy(
            userBio = txtDescriptionEditProfile.text.toString(),
            userCourse = spinnerCourseEditProfile.selectedItem.toString()
        )

        lifecycleScope.launch {
            userViewModel.updateUser(updatedUser)
            profileViewModel.updateProfile(updatedProfile)

            selectedImageUri?.let { uri ->
                val profileRef = storageRef.child("imageProfile").child("${currentUser.userID}.png")
                profileRef.putFile(uri).addOnSuccessListener {
                    if (isAdded) {
                        parentFragmentManager.setFragmentResult("ProfileUpdatedKey", Bundle())
                        Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        activity?.supportFragmentManager?.popBackStack()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload profile image.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                if (isAdded) {
                    parentFragmentManager.setFragmentResult("ProfileUpdatedKey", Bundle())
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        }
    }
}
