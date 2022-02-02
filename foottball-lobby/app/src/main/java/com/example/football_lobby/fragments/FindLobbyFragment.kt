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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.DataAdapter
import com.example.football_lobby.models.Lobby
import com.google.android.material.slider.Slider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.tasks.Tasks
import java.util.*
import kotlin.collections.ArrayList

class FindLobbyFragment : Fragment(), DataAdapter.OnItemClickedListener {

    private lateinit var adapter: DataAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var foundLobbiesRecyclerView: RecyclerView
    private lateinit var findLobbyByName: EditText
    private lateinit var findLobbyByCreator: EditText
    private lateinit var distanceSlider: Slider
    private var listFull = ArrayList<Lobby>()
    private var listCurrent = ArrayList<Lobby>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findLobbyByName = view.findViewById(R.id.findLobbyByNameTextInputEditText)
        findLobbyByCreator = view.findViewById(R.id.findLobbyByPlayerNameTextInputEditText)
        foundLobbiesRecyclerView = view.findViewById(R.id.foundLobbiesRecyclerView)
        distanceSlider = view.findViewById(R.id.distanceSlider)
        val distTxt = view.findViewById<TextView>(R.id.distanceTxt)
        setupRecyclerView()
        loadAllLobbiesIntoAdapter()

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
        //filter() TODO
    }

    private fun filter() {
        adapter.filter.filter(findLobbyByName.text.toString() + "/" +
                findLobbyByCreator.text.toString() + "/" + distanceSlider.value.toInt().toString())
    }

    private fun loadAllLobbiesIntoAdapter(){
        val list = ArrayList<Lobby>()
        db.collection("lobbies").get().addOnSuccessListener {
            result ->
            for(lobby in result.documents){
                list.add(Lobby(lobby["name"].toString(), lobby["location"].toString(), lobby["date"].toString(),
                lobby["time"].toString(), lobby["createdBy"].toString(), lobby["numberOfPlayersInLobby"].toString().toInt(),
                lobby["maximumNumberOfPlayers"].toString().toInt(), lobby["public"] as Boolean))
            }
            adapter.setData(list)
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView(){
        adapter = DataAdapter(ArrayList<Lobby>(), this)
        foundLobbiesRecyclerView.adapter = adapter
        foundLobbiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        foundLobbiesRecyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(position: Int) {
    }
}