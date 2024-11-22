package com.example.fyp.data

data class User (
    var userID : String,
    val username : String,
    val email : String,
    val userMobileNo : String,
    val userDOB : String,
    var password : String,
    val securityQuestion : String
) {
    constructor() : this("", "", "", "", "", "", "")
}