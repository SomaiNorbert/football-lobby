package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Player
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList


class LobbyDetailsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener {

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
    private lateinit var publicRB: RadioButton
    private lateinit var privateRB: RadioButton
    private lateinit var joinButton: Button
    private lateinit var toolbar: MaterialToolbar

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
        setupRecyclerView()


        if(currentLobbyUid != null){
            db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
                result ->
                val lobbyData = result.documents[0]
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
                val job = CoroutineScope(Dispatchers.Default).launch{loadPlayersInLobbyIntoDataAdapter(playersList)}
                job.invokeOnCompletion {
                    CoroutineScope(Dispatchers.Main).launch{adapterPlayers.notifyDataSetChanged()}
                }
                if(currentUser.uid == creatorUid){
                    detailRG.visibility = View.VISIBLE
                    joinButton.visibility = View.INVISIBLE
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


    private fun setupRecyclerView(){
        adapterPlayers = PlayersDataAdapter(ArrayList(), this)
        playersInLobbyRV.adapter = adapterPlayers
        playersInLobbyRV.layoutManager = LinearLayoutManager(requireContext())
        playersInLobbyRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_lobbyDetailsFragment_to_profileFragment, bundle)
    }
}