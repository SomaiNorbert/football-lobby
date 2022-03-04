package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.LobbiesDataAdapter
import com.example.football_lobby.models.Lobby
import com.google.android.gms.tasks.Tasks
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class FindLobbiesFragment : Fragment(), LobbiesDataAdapter.OnItemClickedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var adapterLobbies: LobbiesDataAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var foundLobbiesRecyclerView: RecyclerView
    private lateinit var findLobbyByName: EditText
    private lateinit var findLobbyByCreator: EditText
    private lateinit var distanceSlider: Slider

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
        return inflater.inflate(R.layout.fragment_find_lobbies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findLobbyByName = view.findViewById(R.id.findLobbyByNameTextInputEditText)
        findLobbyByCreator = view.findViewById(R.id.findLobbyByPlayerNameTextInputEditText)
        foundLobbiesRecyclerView = view.findViewById(R.id.foundLobbiesRecyclerView)
        distanceSlider = view.findViewById(R.id.distanceSlider)
        val distTxt = view.findViewById<TextView>(R.id.distanceTxt)
        setupRecyclerView()
        CoroutineScope(Dispatchers.Default).launch { loadAllLobbiesIntoAdapter() }
            .invokeOnCompletion { CoroutineScope(Dispatchers.Main).launch { filter() } }

        distanceSlider.addOnChangeListener { _, value, _ ->
            if (value < 1) {
                distTxt.text = "<1"
            } else {
                distTxt.text = value.toInt().toString()
            }
            filter()
        }

        findLobbyByName.doOnTextChanged { _,_,_,_ ->
            filter()
        }
        findLobbyByCreator.doOnTextChanged { _, _, _, _ ->
            filter()
        }

    }

    private fun filter() {
        adapterLobbies.filter.filter(findLobbyByName.text.toString() + "/" +
                findLobbyByCreator.text.toString() + "/" + distanceSlider.value.toInt().toString())
    }

    private fun loadAllLobbiesIntoAdapter() {
        val list = ArrayList<Lobby>()
        val result = Tasks.await(db.collection("lobbies").get())
        for (lobby in result.documents) {
            if (lobby["public"] as Boolean)
                list.add(
                    Lobby(
                        lobby["uid"].toString(),
                        lobby["name"].toString(),
                        lobby["location"].toString(),
                        lobby["date"].toString(),
                        lobby["time"].toString(),
                        lobby["creatorName"].toString(),
                        lobby["creatorUid"].toString(),
                        lobby["numberOfPlayersInLobby"].toString().toInt(),
                        lobby["maximumNumberOfPlayers"].toString().toInt(),
                        lobby["public"] as Boolean,
                        lobby["latitude"] as Double,
                        lobby["longitude"] as Double
                    )
                )
        }
        CoroutineScope(Dispatchers.Main).launch { adapterLobbies.setData(list) }
    }

    private fun setupRecyclerView(){
        adapterLobbies = LobbiesDataAdapter(requireContext(), ArrayList<Lobby>(), this)
        foundLobbiesRecyclerView.adapter = adapterLobbies
        foundLobbiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        foundLobbiesRecyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("uid", uid)
        findNavController().navigate(R.id.action_global_lobbyDetailsFragment, bundle)
    }

}