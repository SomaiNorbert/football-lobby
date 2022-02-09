package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.MessagesDataAdapter
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Message
import com.example.football_lobby.models.Player
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlin.collections.ArrayList


class LobbyDetailsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener, PlayersDataAdapter.OnButtonClicked {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var gameNameTxt: TextView
    private lateinit var locationDetailTxt: TextView
    private lateinit var dateAndTimeTxt: TextView
    private lateinit var numberOfPlayersInLobbyTxt: TextView
    private lateinit var maximumNumberOfPlayersInLobbyTxt: TextView
    private lateinit var detailRG: RadioGroup
    private lateinit var playersInLobbyRV: RecyclerView
    private lateinit var adapterPlayers: PlayersDataAdapter
    private lateinit var adapterMessages: MessagesDataAdapter
    private lateinit var publicRB: RadioButton
    private lateinit var privateRB: RadioButton
    private lateinit var joinButton: Button
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var chatRV: RecyclerView
    private lateinit var chatLL: LinearLayout
    private lateinit var sendButton: ImageButton
    private lateinit var messageEDT: EditText

    private lateinit var lobbyData: DocumentSnapshot

    private lateinit var currentUser: FirebaseUser
    var creatorUid = ""
    var documentID = ""
    var playersList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
        currentUser = auth.currentUser!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby_details, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentLobbyUid = arguments?.get("lobbyUid")
        Log.d(TAG, currentLobbyUid.toString())

        gameNameTxt = view.findViewById(R.id.gameNameTxt)
        locationDetailTxt = view.findViewById(R.id.locationDetailTxt)
        dateAndTimeTxt = view.findViewById(R.id.dateAndTimeTxt)
        numberOfPlayersInLobbyTxt = view.findViewById(R.id.numberOfPlayersInLobbyTxt)
        maximumNumberOfPlayersInLobbyTxt = view.findViewById(R.id.maximumNumberOfPlayersInLobbyTxt)
        detailRG = view.findViewById(R.id.detailRG)
        playersInLobbyRV = view.findViewById(R.id.playersInLobbyRV)
        publicRB = view.findViewById(R.id.publicDetailRB)
        privateRB = view.findViewById(R.id.privateDetailRB)
        joinButton = view.findViewById(R.id.joinLobbyButton)
        toolbar = requireActivity().findViewById(R.id.topAppToolbar)
        tabLayout = view.findViewById(R.id.tabLayout)
        chatRV = view.findViewById(R.id.chatRV)
        chatLL = view.findViewById(R.id.chatLL)
        sendButton = view.findViewById(R.id.sendButton)
        messageEDT = view.findViewById(R.id.messageEDT)
        setupMessagesRecyclerView()

        if(currentLobbyUid != null){
            db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
                result ->
                lobbyData = result.documents[0]
                setupPlayersRecyclerView()
                documentID = lobbyData.id
                creatorUid = lobbyData["creatorUid"] as String
                gameNameTxt.text = lobbyData["name"] as String
                locationDetailTxt.text = lobbyData["location"] as String
                val dt = lobbyData["date"] as String + "  " + lobbyData["time"] as String
                dateAndTimeTxt.text = dt
                numberOfPlayersInLobbyTxt.text = lobbyData["numberOfPlayersInLobby"].toString()
                maximumNumberOfPlayersInLobbyTxt.text = (lobbyData["maximumNumberOfPlayers"].toString().toInt()*2).toString()
                if(lobbyData["public"] as Boolean){
                    publicRB.isChecked = true
                }else{
                    privateRB.isChecked = true
                }
                playersList = lobbyData["players"] as ArrayList<String>
                CoroutineScope(Dispatchers.Default).launch{loadPlayersInLobbyIntoDataAdapter(playersList)}
                    .invokeOnCompletion {
                        CoroutineScope(Dispatchers.Main).launch{adapterPlayers.notifyDataSetChanged()}
                }
                CoroutineScope(Dispatchers.Default).launch { loadMessagesIntoDataAdapter() }
                    .invokeOnCompletion {
                        CoroutineScope(Dispatchers.Main).launch { adapterMessages.notifyDataSetChanged() }
                    }
                if(currentUser.uid == creatorUid){
                    if(playersList.contains(currentUser.uid)){
                        detailRG.visibility = View.VISIBLE
                        joinButton.visibility = View.INVISIBLE
                    }else{
                        detailRG.visibility = View.INVISIBLE
                        joinButton.visibility = View.VISIBLE
                    }
                }else{
                    detailRG.visibility = View.INVISIBLE
                    if(playersList.contains(currentUser.uid)){
                        joinButton.visibility = View.GONE
                    }else{
                        joinButton.visibility = View.VISIBLE
                    }
                }
                setUpMenu()
            }
        }

        publicRB.setOnCheckedChangeListener { _, isChecked ->
            db.collection("lobbies").document(documentID).update("public", isChecked)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab!=null)
                    when(tab.position){
                        0 -> {
                            playersInLobbyRV.visibility = View.VISIBLE
                            chatRV.visibility = View.GONE
                            chatLL.visibility = View.GONE
                        }
                        1 -> {
                            if(isCurrentUserInLobby()){
                                playersInLobbyRV.visibility = View.INVISIBLE
                                chatRV.visibility = View.VISIBLE
                                chatLL.visibility = View.VISIBLE
                                chatRV.scrollToPosition(adapterMessages.itemCount-1)
                            }else{
                                Toast.makeText(requireContext(), "Please join first!", Toast.LENGTH_LONG).show()
                            }

                        }
                    }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?){}
        })

        sendButton.setOnClickListener {
            if(messageEDT.text.isNotEmpty())
                db.collection("users").whereEqualTo("uid", currentUser.uid).get().addOnSuccessListener {
                    val mes = Message(currentUser.uid,it.documents[0]["name"].toString(),messageEDT.text.toString())
                    adapterMessages.addItem(mes)
                    messageEDT.setText("")
                    chatRV.scrollToPosition(adapterMessages.itemCount-1)
                    var doc : DocumentSnapshot
                    db.collection("chat").whereEqualTo("lobbyUid", lobbyData["uid"]).get().addOnSuccessListener {
                        result -> doc = result.documents[0]
                        if(doc["messages"] != null){
                            val messages = doc["messages"] as ArrayList<Message>
                            messages.add(mes)
                            db.collection("chat").document(doc.id).update("messages", messages.toList())
                        }else{
                            db.collection("chat").document(doc.id).update("messages", mes)
                        }

                    }
                }
        }

        joinButton.setOnClickListener {
            if(currentUser.uid == creatorUid) {
                detailRG.visibility = View.VISIBLE
                joinButton.visibility = View.INVISIBLE
            }else{
                joinButton.visibility = View.GONE
            }

            playersList.add(currentUser.uid)
            var npil = numberOfPlayersInLobbyTxt.text.toString().toInt()
            npil += 1
            numberOfPlayersInLobbyTxt.text = npil.toString()
            val update = hashMapOf(
                "numberOfPlayersInLobby" to numberOfPlayersInLobbyTxt.text.toString().toInt(),
                "players" to playersList.toList()
            )
            db.collection("lobbies").document(documentID).update(update)
            db.collection("users").whereEqualTo("uid", currentUser.uid).get().addOnSuccessListener {
                val playerData = it.documents[0]
                adapterPlayers.addPlayer(Player(playerData["name"].toString(), playerData["birthday"].toString(),
                    playerData["overallRating"].toString().toDouble(), currentUser.uid))
            }
            setUpMenu()
        }

    }

    private fun isCurrentUserInLobby() : Boolean {
        if(playersList.contains(currentUser.uid))
            return true
        return false
    }

    private fun setUpMenu(){
        toolbar.menu.setGroupVisible(R.id.inLobbyGroup, isCurrentUserInLobby())
    }

    fun leaveLobby() {
        if(currentUser.uid == creatorUid){
            detailRG.visibility = View.INVISIBLE
        }
        joinButton.visibility = View.VISIBLE
        playersList.remove(currentUser.uid)
        var npil = numberOfPlayersInLobbyTxt.text.toString().toInt()
        npil -= 1
        numberOfPlayersInLobbyTxt.text = npil.toString()
        val update = hashMapOf(
            "numberOfPlayersInLobby" to numberOfPlayersInLobbyTxt.text.toString().toInt(),
            "players" to playersList.toList()
        )
        db.collection("lobbies").document(documentID).update(update)
        adapterPlayers.removePlayerByUid(currentUser.uid)
        setUpMenu()
    }

    private fun loadMessagesIntoDataAdapter(){
        val messages = ArrayList<Message>()
        val chatDoc = Tasks.await(db.collection("chat").whereEqualTo("lobbyUid", lobbyData["uid"]).get()).documents[0]
        val mes = chatDoc["messages"] as ArrayList<HashMap<String, String>>
        for(message in mes){
            messages.add(Message(message["senderUid"].toString(),message["senderName"].toString(), message["message"].toString()))
        }
        adapterMessages.setData(messages)
    }

    private fun loadPlayersInLobbyIntoDataAdapter(uidList: List<String>) {
        val players = ArrayList<Player>()
        for(uid in uidList) {
            Log.d(TAG, "1")
            val result = Tasks.await(db.collection("users").whereEqualTo("uid", uid).get())
            val p = result.documents[0]
            players.add(Player(p["name"].toString(), p["birthday"].toString(),
                p["overallRating"].toString().toDouble(), uid))
        }
        adapterPlayers.setData(players)
    }

    private fun setupMessagesRecyclerView(){
        adapterMessages = MessagesDataAdapter(ArrayList())
        chatRV.adapter = adapterMessages
        chatRV.layoutManager = LinearLayoutManager(requireContext())
        chatRV.setHasFixedSize(true)
    }

    private fun setupPlayersRecyclerView(){
        adapterPlayers = PlayersDataAdapter(ArrayList(), this,this, lobbyData["creatorUid"].toString())
        playersInLobbyRV.adapter = adapterPlayers
        playersInLobbyRV.layoutManager = LinearLayoutManager(requireContext())
        playersInLobbyRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_lobbyDetailsFragment_to_profileFragment, bundle)
    }

    override fun onKickButtonClicked(uid: String) {
        adapterPlayers.removePlayerByUid(uid)
        playersList.remove(uid)
        var npil = numberOfPlayersInLobbyTxt.text.toString().toInt()
        npil -= 1
        numberOfPlayersInLobbyTxt.text = npil.toString()
        val update = hashMapOf(
            "numberOfPlayersInLobby" to numberOfPlayersInLobbyTxt.text.toString().toInt(),
            "players" to playersList.toList()
        )
        db.collection("lobbies").document(documentID).update(update)
    }
}