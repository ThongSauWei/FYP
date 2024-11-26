package com.example.fyp.data

data class Profile(
    val userID  : String,
    val userCourse : String,
    val userBio : String,
    val userImage : String,
    val userBackgroundImage : String,
    val userChosenLanguage : String
) {
    constructor() : this("", "", "", "", "","")
}