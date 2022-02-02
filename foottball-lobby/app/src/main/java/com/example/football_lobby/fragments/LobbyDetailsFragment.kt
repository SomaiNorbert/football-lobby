package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Player
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*


class LobbyDetailsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = arguments?.get("lobbyUid")
        Log.d(TAG, uid.toString())

        gameNameTxt = view.findViewById(R.id.gameNameTxt)
        locationDetailTxt = view.findViewById(R.id.locationDetailTxt)
        dateAndTimeTxt = view.findViewById(R.id.dateAndTimeTxt)
        numberOfPlayersInLobbyTxt = view.findViewById(R.id.numberOfPlayersInLobbyTxt)
        maximumNumberOfPlayersInLobbyTxt = view.findViewById(R.id.maximumNumberOfPlayersInLobbyTxt)
        detailRG = view.findViewById(R.id.detailRG)
        playersInLobbyRV = view.findViewById(R.id.playersInLobbyRV)
        publicRB = view.findViewById(R.id.publicDetailRB)
        privateRB = view.findViewById(R.id.privateDetailRB)
        setupRecyclerView()

        Log.d(TAG, uid.toString())
        if(uid != null){
            db.collection("lobbies").whereEqualTo("uid", uid).get().addOnSuccessListener {
                result ->
                val lobbyData = result.documents[0]
                gameNameTxt.text = lobbyData["name"] as String
                locationDetailTxt.text = lobbyData["location"] as String
                val dt = lobbyData["date"] as String + "  " + lobbyData["time"] as String
                dateAndTimeTxt.text = dt
                numberOfPlayersInLobbyTxt.text = lobbyData["numberOfPlayersInLobby"].toString()
                maximumNumberOfPlayersInLobbyTxt.text = lobbyData["maximumNumberOfPlayers"].toString()
                if(lobbyData["public"] as Boolean){//TODO
                    publicRB.isSelected = true
                }else{
                    privateRB.isSelected = true
                }
                val job = CoroutineScope(Dispatchers.Default).launch{loadPlayersInLobbyIntoDataAdapter(lobbyData["players"] as List<String>)}
                job.invokeOnCompletion {
                    CoroutineScope(Dispatchers.Main).launch{adapterPlayers.notifyDataSetChanged()}
                }


            }
        }
    }

    private fun loadPlayersInLobbyIntoDataAdapter(uidList: List<String>) {
        val players = ArrayList<Player>()
        Log.d(TAG, uidList.size.toString() + "asd")
        for(uid in uidList) {
            Log.d(TAG, "1")
            val result = Tasks.await(db.collection("users").whereEqualTo("uid", uid).get())
            val p = result.documents[0]
            players.add(Player(p["name"].toString(), p["birthday"].toString(),
                p["overallRating"].toString().toDouble(), p["profilePic"].toString(), uid))
        }
        Log.d(TAG, players.size.toString())
        adapterPlayers.setData(players)
    }


    private fun setupRecyclerView(){
        adapterPlayers = PlayersDataAdapter(ArrayList(), this)
        playersInLobbyRV.adapter = adapterPlayers
        playersInLobbyRV.layoutManager = LinearLayoutManager(requireContext())
        playersInLobbyRV.setHasFixedSize(true)
    }

    override fun onItemClick(position: Int) {
    }
}