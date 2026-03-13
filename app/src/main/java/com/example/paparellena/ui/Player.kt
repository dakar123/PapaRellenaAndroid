package com.example.paparellena.ui

data class Player(
    val id: String,
    val name: String,
    val hasPotato: Boolean = false,
    val isRequesting: Boolean = false
)
