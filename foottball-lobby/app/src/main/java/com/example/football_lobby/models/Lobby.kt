package com.example.football_lobby.models

data class Lobby(var lobbyName: String = "",
                 var location: String = "",
                 var date: String = "",
                 var time: String = "",
                 var creator: String = "",
                 var numberOfPlayersInLobby: Int = 0,
                 var maximumNumberOfPlayers: Int = 0,
                 var public: Boolean = true
)