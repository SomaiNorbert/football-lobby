package com.example.football_lobby.models

data class Rating(val punctuality: Int = 0,
                  val behavior: Int = 0,
                  val calmness: Int = 0,
                  val sportsmanship: Int = 0,
                  val comment: String = "",
                  val lobbyName: String = "",
                  val fromName: String = ""
)