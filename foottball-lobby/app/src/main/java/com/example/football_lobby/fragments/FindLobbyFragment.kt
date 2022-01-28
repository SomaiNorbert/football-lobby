package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.DataAdapter
import com.example.football_lobby.models.Lobby
import com.google.android.material.slider.Slider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.awaitAll
import java.lang.Thread.sleep
import java.math.BigDecimal
import java.math.RoundingMode

class FindLobbyFragment : Fragment(), DataAdapter.OnItemClickedListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var foundLobbiesRecyclerView: RecyclerView

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

        foundLobbiesRecyclerView = view.findViewById(R.id.foundLobbiesRecyclerView)
        val distTxt = view.findViewById<TextView>(R.id.distanceTxt)

        view.findViewById<Slider>(R.id.distanceSlider).addOnChangeListener { _, value, _ ->
            if(value < 1){
                distTxt.text = "<1"
            }else{
                distTxt.text = value.toInt().toString()
            }
        }

        getAllLobbies()

    }

    private fun getAllLobbies():ArrayList<Lobby> {
        val list = ArrayList<Lobby>()
        db.collection("lobbies").get().addOnSuccessListener {
            result ->
            for(lobby in result.documents){
                Log.d(TAG, lobby.toString())
                list.add(Lobby(lobby["name"].toString(), lobby["location"].toString(), lobby["date"].toString(),
                lobby["time"].toString(), lobby["createdBy"].toString(), lobby["numberOfPlayersInLobby"].toString().toInt(),
                lobby["maximumNumberOfPlayers"].toString().toInt(), lobby["public"] as Boolean))
            }
            showLobbies(list)
        }
        return list
    }

    private fun showLobbies(list:ArrayList<Lobby>){
        Log.d(TAG, "List Size2" + list.size)
        foundLobbiesRecyclerView.adapter = DataAdapter(list, this)
        foundLobbiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        foundLobbiesRecyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(position: Int) {
    }
}