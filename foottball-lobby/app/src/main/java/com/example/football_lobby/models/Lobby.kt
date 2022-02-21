package com.example.football_lobby.models

data class Lobby(var uid: String = "",
                 var lobbyName: String = "",
                 var location: String = "",
                 var date: String = "",
                 var time: String = "",
                 var creatorName: String = "",
                 var creatorUid: String = "",
                 var numberOfPlayersInLobby: Int = 0,
                 var maximumNumberOfPlayers: Int = 0,
                 var public: Boolean = true,
                 var latitude: Double = 0.0,
                 var longitude: Double = 0.0
)