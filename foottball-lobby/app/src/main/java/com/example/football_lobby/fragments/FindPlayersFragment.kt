package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Player
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FindPlayersFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth:FirebaseAuth

    private lateinit var playerNameEDT: TextInputEditText
    private lateinit var ratingsSpinner: Spinner
    private lateinit var foundPlayersRV: RecyclerView
    private lateinit var adapterPlayers: PlayersDataAdapter

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
        return inflater.inflate(R.layout.fragment_find_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerNameEDT = view.findViewById(R.id.findPlayerByNameTextInputEditText)
        ratingsSpinner = view.findViewById(R.id.ratingsSpinner)
        foundPlayersRV = view.findViewById(R.id.foundPlayersRecyclerView)

        setupRecyclerView()

        ratingsSpinner.adapter = activity?.let{
            ArrayAdapter(it, android.R.layout.simple_spinner_item,
                listOf("All", "No rating!", "Minimum 1", "Minimum 2", "Minimum 3", "Minimum 4", "5"))
        }

        CoroutineScope(Dispatchers.Default).launch { loadAllPlayersIntoRV() }
            .invokeOnCompletion { CoroutineScope(Dispatchers.Main).launch { ratingsSpinner.setSelection(0) } }

        playerNameEDT.doOnTextChanged { _,_,_,_ ->
            filter()
        }

        ratingsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filter()
            }
        }

    }

    private fun loadAllPlayersIntoRV() {
        val list = ArrayList<Player>()
        val result = Tasks.await(db.collection("users").orderBy("name_lower").get())
        for(player in result.documents) {
            if(auth.currentUser!!.uid == player["uid"].toString()){
                continue
            }
            list.add(
                Player(
                    player["name"].toString(),
                    player["birthday"].toString(),
                    player["overallRating"].toString().toDouble(),
                    player["uid"].toString()
                )
            )
        }
        CoroutineScope(Dispatchers.Main).launch { adapterPlayers.setData(list) }
    }

    private fun filter(){
        adapterPlayers.filter.filter(playerNameEDT.text.toString() + "/" + ratingsSpinner.selectedItem.toString())
    }

    private fun setupRecyclerView() {
        adapterPlayers = PlayersDataAdapter(ArrayList(), this, "", "")
        foundPlayersRV.adapter = adapterPlayers
        foundPlayersRV.layoutManager = LinearLayoutManager(requireContext())
        foundPlayersRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_findPlayersFragment_to_profileFragment, bundle)
    }

    override fun onKickButtonClicked(uid: String){}

    override fun onChatButtonClicked(uid: String) {
        val bundle = Bundle()
        bundle.putString("uid", uid)
        findNavController().navigate(R.id.action_findPlayersFragment_to_privateChatFragment, bundle)
    }

    override fun onInviteButtonClicked(uid: String) {
        db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener {
            clicked ->
            db.collection("lobbies").get().addOnSuccessListener {
                val myLobbies = ArrayList<String>()
                val myLobbiesUid = ArrayList<String>()
                for(lobby in it.documents){
                    if((lobby["players"] as ArrayList<String>).contains(auth.currentUser!!.uid)){
                        if(lobby["maximumNumberOfPlayers"].toString().toInt() > lobby["numberOfPlayersInLobby"].toString().toInt()){
                            if(!(lobby["players"] as ArrayList<String>).contains(uid)){
                                myLobbies.add(lobby["name"].toString())
                                myLobbiesUid.add(lobby["uid"].toString())
                            }
                        }
                    }
                }
                if(myLobbies.size != 0){
                    var checkedItem = -1
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Invite ${clicked.documents[0]["name"].toString()} to:")
                        .setNeutralButton("Cancel") { _,_->}
                        .setPositiveButton("Invite") { _, _ ->
                            invitePlayerToLobby(uid, myLobbiesUid[checkedItem])
                        }
                        .setSingleChoiceItems(myLobbies.toTypedArray(), checkedItem) { _, which ->
                            checkedItem = which
                        }
                        .show()
                }
            }
        }
    }

    override fun onAcceptButtonClicked(uid: String, pos: Int) {}

    override fun onDeclineButtonClicked(uid: String) {}

    private fun invitePlayerToLobby(playerUid:String, lobbyUid: String) {
        db.collection("lobbies").whereEqualTo("uid", lobbyUid).get().addOnSuccessListener {
            val players = it.documents[0]["players"] as ArrayList<String>
            players.add(playerUid)
            it.documents[0].reference.update("players", players)
        }
    }

}