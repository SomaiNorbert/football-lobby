package com.example.football_lobby.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.MainActivity
import com.example.football_lobby.R
import com.example.football_lobby.adapters.NotificationsDataAdapter
import com.example.football_lobby.models.NotificationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NotificationsFragment : Fragment(), NotificationsDataAdapter.OnItemClickedListener{

    private lateinit var adapterNotifications: NotificationsDataAdapter
    private lateinit var notificationsRV: RecyclerView

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationsRV = view.findViewById(R.id.notificationsRV)
        setupRecyclerView()

        db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid).addSnapshotListener { player, _ ->
            val notifications = ArrayList<NotificationData>()
            if(player == null) return@addSnapshotListener
            if(player!!.documents[0]["notifications"] != null){
                for(notification in player.documents[0]["notifications"] as ArrayList<HashMap<String, String>>){
                    notifications.add(NotificationData(notification["title"].toString(), notification["message"].toString(),
                        notification["destination"].toString(), notification["uid"].toString(), notification["id"].toString()))
                }
                adapterNotifications.setData(notifications)
            }
        }
    }

    private fun setupRecyclerView() {
        adapterNotifications = NotificationsDataAdapter(ArrayList(), this)
        notificationsRV.adapter = adapterNotifications
        notificationsRV.layoutManager = LinearLayoutManager(requireContext())
        notificationsRV.setHasFixedSize(true)
    }

    override fun onItemClick(notification: NotificationData, pos: Int) {

        val bundle = Bundle()
        bundle.putString("uid", notification.uid)
        bundle.putString("notificationID", notification.id)

        val destination = when (notification.destination){
            "privateChat" -> R.id.action_global_privateChatFragment
            "lobbyDetails" -> R.id.action_global_lobbyDetailsFragment
            "myFriends" -> R.id.action_global_myFriendsFragment
            "findPlayers" -> R.id.action_global_findPlayersFragment
            else -> R.id.action_global_findLobbyFragment
        }

        findNavController().navigate(destination, bundle)
    }
}