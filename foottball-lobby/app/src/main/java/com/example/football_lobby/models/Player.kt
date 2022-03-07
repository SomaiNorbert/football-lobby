package com.example.football_lobby.models

data class Player(var name: String = "",
                  var birthday: String = "",
                  var rating: Double = 0.0,
                  var uid: String = "",
                  val tokens: ArrayList<String> = ArrayList()
)
