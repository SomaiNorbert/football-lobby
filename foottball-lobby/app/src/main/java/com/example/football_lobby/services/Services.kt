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
                    if(isOld(lobby)){
                        //lobby.reference.delete()
                        //if(lobby["maximumNumberOfPlayers"] == lobby["numberOfPlayersInLobby"]){
                            //Firebase.firestore.collection("onGoingLobbies").add(lobby.data!!)
                            Firebase.firestore.collection("users").whereEqualTo("uid", lobby["creatorUid"].toString()).get()
                                .addOnSuccessListener {
                                    var tokens = ArrayList<String>()
                                    if(it.documents[0]["tokens"] != null){
                                        tokens = it.documents[0]["tokens"] as ArrayList<String>
                                    }
                                    MyFirebaseMessagingService().sendNotificationToOwnerOnLobbyDone(
                                        tokens,
                                        lobby["name"].toString(),
                                        lobby["uid"].toString()
                                    )
                                }
                        //}
                    }
                }
            }
        }

        private fun isOld(lobby: DocumentSnapshot) : Boolean {
            val currentCalendar = Calendar.getInstance()
            val lobbyCalendar = Calendar.getInstance()
            val date:List<String> = lobby["date"].toString().split("/")
            val time:List<String> = lobby["time"].toString().split(":")
            lobbyCalendar.time = Date()
            lobbyCalendar.set(date[2].toInt(), date[1].toInt()-1, date[0].toInt(), time[0].toInt(), time[1].toInt())
            return lobbyCalendar.before(currentCalendar)
        }
    }
}