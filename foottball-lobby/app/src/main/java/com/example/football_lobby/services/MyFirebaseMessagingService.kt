package com.example.football_lobby.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.ContentValues.TAG
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.football_lobby.MainActivity
import com.example.football_lobby.R
import com.example.football_lobby.retrofit.RetrofitInstance
import com.example.football_lobby.models.NotificationData
import com.example.football_lobby.models.PushNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val CHANNEL_ID = "my_channel"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("token", token).apply();
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        val bundle = Bundle()
        bundle.putString("uid", remoteMessage.data["uid"])

        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.navigation_graph)
            .setDestination(remoteMessage.data["destination"]!!.toInt())
            .setArguments(bundle)
            .createPendingIntent()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.profile_avatar)//notification.icon!!.toInt())
            .setContentTitle(remoteMessage.data["title"])
            .setContentText(remoteMessage.data["message"])
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply{
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }


    fun getToken(context: Context): String? {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("token", "");
    }

    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d(TAG, "Response: $response")//Gson().toJson(response)}")
                } else {
                    Log.e(TAG, response.errorBody().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }

    fun sendNotificationToOwnerOnLobbyDone(ownerTokens: ArrayList<String>, gameName: String, lobbyUid: String) {
        for(token in ownerTokens){
            PushNotification(
                NotificationData("Did the game happen?", "We noticed that, your game, $gameName is over. How was it?",
                    R.id.lobbyDetailsFragment, lobbyUid),
                token
            ).also{
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayersOnLobbyDone(playersTokens: ArrayList<String>, gameName: String, lobbyUid: String) {
        for(token in playersTokens){
            PushNotification(
                NotificationData("How was your game? Rate players",
                    "We noticed that, your game, $gameName ended. Come and rate the players!",
                R.id.lobbyDetailsFragment, lobbyUid),
                token
            ).also {
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnMessageReceived(playerTokens: ArrayList<String>, fromName: String, fromUid: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("New Message", "You have a new message from $fromName!",
                    R.id.privateChatFragment, fromUid),
                token
            ).also{
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnFriendRequest(playerTokens: ArrayList<String>, fromName: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("New Friend Request", "You have a new pending friend request from $fromName",
                    R.id.myFriendsFragment),
                token
            ).also{
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnFriendRequestAccepted(playerTokens: ArrayList<String>, fromName: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("Friend Request Accepted", "$fromName accepted your friend request!",
                    R.id.myFriendsFragment),
                token
            ).also {
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnFriendRequestDenied(playerTokens: ArrayList<String>, fromName: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("Friend Request Denied", "$fromName denied your friend request!",
                    R.id.findPlayersFragment),
                token
            ).also{
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToOwnerOnJoinRequest(ownerTokens: ArrayList<String>, fromName: String, lobbyName: String, lobbyUid: String) {
        for(token in ownerTokens){
            PushNotification(
                NotificationData("New Join Lobby Request", "$fromName wants to join your lobby: $lobbyName",
                    R.id.lobbyDetailsFragment, lobbyUid),
                token
            ).also {
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnJoinRequestAccepted(playerTokens: ArrayList<String>, fromName: String, lobbyName: String, lobbyUid: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("Join Request Accepted", "$fromName accepted your request to join the lobby: $lobbyName",
                    R.id.lobbyDetailsFragment, lobbyUid),
                token
            ).also {
                sendNotification(it)
            }
        }
    }

    fun sendNotificationToPlayerOnJoinRequestDenied(playerTokens: ArrayList<String>, fromName: String, lobbyName: String) {
        for(token in playerTokens){
            PushNotification(
                NotificationData("Join Request Denied", "$fromName denied your request to join the lobby: $lobbyName",
                    R.id.findLobbiesFragment),
                token
            ).also {
                sendNotification(it)
            }
        }
    }


}