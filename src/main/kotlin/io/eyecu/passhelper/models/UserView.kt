package io.eyecu.passhelper.models

data class UserView(
    val username: String,
    val emailAddress: String,
    val emailEnabled: Boolean,
    val owner: Boolean,
    val loginEnabled: Boolean
)