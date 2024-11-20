package com.mainapp.finalyearproject.data

data class Profile(
    val userID  : String,
    val userCourse : String,
    val userBio : String,
    val userImage : String,
    val userChosenLanguage : String
) {
    constructor() : this("", "", "", "", "")
}