package com.example.football_lobby.services

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class Services {
    companion object{

        fun checkLobbies(){
            CoroutineScope(Dispatchers.Default).launch {
                val lobbies = Tasks.await(Firebase.firestore.collection("lobbies").get())
                for(lobby in lobbies.documents){
                    if(isOld(lobby, Calendar.getInstance())){
                        //if(lobby["maximumNumberOfPlayers"] == lobby["numberOfPlayersInLobby"]){
                            lobby.reference.update("isOnGoing", true)
                        //}
                        //lobby.reference.delete()
                    }
                }
            }.invokeOnCompletion {
                checkOldLobbies()
            }
        }

        private fun checkOldLobbies(){
            CoroutineScope(Dispatchers.Default).launch {
                val onGoingLobbies = Tasks.await(Firebase.firestore.collection("lobbies")
                    .whereEqualTo("isOnGoing", true).get())
                for(lobby in onGoingLobbies){
                    if(isDone(lobby)){
                        Firebase.firestore.collection("oldLobbies").add(lobby.data)
                        lobby.reference.delete()
                        Firebase.firestore.collection("users").whereEqualTo("uid", lobby["creatorUid"].toString()).get()
                            .addOnSuccessListener {
                                var tokens = ArrayList<String>()
                                if(it.documents[0]["tokens"] != null){
                                    tokens = it.documents[0]["tokens"] as ArrayList<String>
                                }
                                MyFirebaseMessagingService().sendNotificationToOwnerOnLobbyDone(
                                    arrayListOf(lobby["creatorUid"].toString()), tokens,
                                    lobby["name"].toString(),
                                    lobby["uid"].toString()
                                )
                            }
                    }
                }
            }.invokeOnCompletion {
                archiveLobbies()
            }
        }

        private fun archiveLobbies(){
            CoroutineScope(Dispatchers.Default).launch {
                val oldLobbies = Tasks.await(Firebase.firestore.collection("oldLobbies").get())
                for(lobby in oldLobbies){
                    if(lobby["playersResponded"] != null)
                    if((lobby["playersResponded"]as ArrayList<String>).size == lobby["maximumNumberOfPlayers"].toString().toInt()){
                        Firebase.firestore.collection("lobbyArchive").add(lobby.data)
                        lobby.reference.delete()
                    }
                }
            }
        }

        private fun isDone(lobby: DocumentSnapshot): Boolean {
            val currentCalendar = Calendar.getInstance()
            currentCalendar.add(Calendar.HOUR_OF_DAY, 1)
            return isOld(lobby, currentCalendar)
        }

        private fun isOld(lobby: DocumentSnapshot, currentCalendar: Calendar) : Boolean {
            val lobbyCalendar = Calendar.getInstance()
            val date:List<String> = lobby["date"].toString().split("/")
            val time:List<String> = lobby["time"].toString().split(":")
            lobbyCalendar.time = Date()
            lobbyCalendar.set(date[2].toInt(), date[1].toInt()-1, date[0].toInt(), time[0].toInt(), time[1].toInt())
            return lobbyCalendar.before(currentCalendar)
        }

        fun removeNotificationFromPlayer(playerUid: String, notificationID: String){
            Firebase.firestore.collection("users").whereEqualTo("uid", playerUid).get().addOnSuccessListener {
                val notifications = ArrayList<HashMap<String, String>>()
                if(it.documents[0]["notifications"] != null){
                    for(notification in it.documents[0]["notifications"] as ArrayList<HashMap<String, String>>){
                        if(notification["id"] != notificationID){
                            notifications.add(notification)
                        }
                    }
                    it.documents[0].reference.update("notifications", notifications)
                }
            }
        }

    }
}