package com.example.football_lobby.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Player
import com.example.football_lobby.services.MyFirebaseMessagingService
import com.example.football_lobby.services.Services
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFriendsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener{

    private lateinit var db: FirebaseFirestore
    private lateinit var myFriendsRV: RecyclerView
    private lateinit var noFriendsFoundTxt: TextView
    private lateinit var adapterPlayers: PlayersDataAdapter
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
        return inflater.inflate(R.layout.fragment_my_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notId = arguments?.get("notificationID").toString()
        if(notId.isNotEmpty() && notId != "null"){
            Services.removeNotificationFromPlayer(auth.currentUser!!.uid, notId)
        }

        myFriendsRV = view.findViewById(R.id.myFriendsRV)
        noFriendsFoundTxt = view.findViewById(R.id.noFriendsFoundTxt)

        setupRecyclerView()

        CoroutineScope(Dispatchers.Default).launch{
            val me = Tasks.await(db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid)
                .get()).documents[0]
            val friends = ArrayList<Player>()
            if(me["friends"] != null){
                val friendsUids = ArrayList<String>()
                if(me["requests"] != null){
                    friendsUids.addAll(me["requests"] as ArrayList<String>)
                }
                friendsUids.addAll(me["friends"] as ArrayList<String>)
                for(friendUid in friendsUids){
                    val f = Tasks.await(db.collection("users").whereEqualTo("uid", friendUid).get()).documents[0]
                    friends.add(Player(f["name"].toString(), f["birthday"].toString(),
                        f["overallRating"].toString().toDouble(), f["uid"].toString()))
                }
            }
            CoroutineScope(Dispatchers.Main).launch { adapterPlayers.setData(friends) }
        }.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                if(adapterPlayers.itemCount == 0){
                    noFriendsFoundTxt.visibility = View.VISIBLE
                }else{
                    noFriendsFoundTxt.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapterPlayers = PlayersDataAdapter(ArrayList(), this, "friends", auth.currentUser!!.uid)
        myFriendsRV.adapter = adapterPlayers
        myFriendsRV.layoutManager = LinearLayoutManager(requireContext())
        myFriendsRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_myFriendsFragment_to_profileFragment, bundle)
    }

    override fun onChatButtonClicked(uid: String) {
        val bundle = Bundle()
        bundle.putString("uid", uid)
        findNavController().navigate(R.id.action_myFriendsFragment_to_privateChatFragment, bundle)
    }

    override fun onInviteButtonClicked(uid: String) {}

    override fun onAcceptButtonClicked(uid: String, pos: Int) {
        db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid).get().addOnSuccessListener {
            removePlayerFromRequestsByUid(it, uid)
            val friends = ArrayList<String>()
            if(it.documents[0]["friends"] != null){
                friends.addAll(it.documents[0]["friends"] as ArrayList<String>)
            }
            friends.add(uid)
            it.documents[0].reference.update("friends", friends).addOnSuccessListener {
                adapterPlayers.notifyItemChanged(pos)
            }
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { he->
                val hisFriends = ArrayList<String>()
                if(he.documents[0]["friends"] != null){
                    hisFriends.addAll(he.documents[0]["friends"] as ArrayList<String>)
                }
                hisFriends.add(auth.currentUser!!.uid)
                he.documents[0].reference.update("friends", hisFriends)
                val tokens = ArrayList<String>()
                if (he.documents[0]["tokens"] != null) {
                    tokens.addAll(he.documents[0]["tokens"] as ArrayList<String>)
                }
                MyFirebaseMessagingService().sendNotificationToPlayerOnFriendRequestAccepted(arrayListOf(uid),tokens, it.documents[0]["name"].toString())
            }
        }
    }

    override fun onDeclineButtonClicked(uid: String) {
        db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid).get().addOnSuccessListener {
            removePlayerFromRequestsByUid(it, uid)
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { he->
                val tokens = ArrayList<String>()
                if (he.documents[0]["tokens"] != null) {
                    tokens.addAll(he.documents[0]["tokens"] as ArrayList<String>)
                }
                MyFirebaseMessagingService().sendNotificationToPlayerOnFriendRequestDenied(arrayListOf(uid), tokens, it.documents[0]["name"].toString())
                adapterPlayers.removePlayerByUid(uid)
            }
        }

    }

    private fun removePlayerFromRequestsByUid(doc: QuerySnapshot, uid: String){
        val requests = doc.documents[0]["requests"] as ArrayList<String>
        requests.remove(uid)
        doc.documents[0].reference.update("requests", requests)
    }

    override fun onKickButtonClicked(uid: String) {}
}