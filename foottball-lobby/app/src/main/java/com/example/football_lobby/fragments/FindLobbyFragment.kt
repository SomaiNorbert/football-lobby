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
import kotlinx.coroutines.awaitAll
import java.lang.Thread.sleep
import java.math.BigDecimal
import java.math.RoundingMode

class FindLobbyFragment : Fragment(), DataAdapter.OnItemClickedListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var foundLobbiesRecyclerView: RecyclerView
    private lateinit var findLobbyByName: EditText
    private lateinit var findLobbyByCreator: EditText
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
        val distTxt = view.findViewById<TextView>(R.id.distanceTxt)

        view.findViewById<Slider>(R.id.distanceSlider).addOnChangeListener { _, value, _ ->
            if(value < 1){
                distTxt.text = "<1"
            }else{
                distTxt.text = value.toInt().toString()
            }
        }

        getAllLobbies()

        findLobbyByName.doOnTextChanged { _,_,_,_ ->
            filter()
        }
        findLobbyByCreator.doOnTextChanged { _, _, _, _ ->
            filter()
        }

    }

    private fun filter(){
        listCurrent.clear()
        if(findLobbyByName.text.isEmpty() && findLobbyByCreator.text.isEmpty() ||
                findLobbyByCreator.text == null && findLobbyByName.text == null){
            listCurrent.addAll(listFull)
        }else{
            if(findLobbyByName.text.isEmpty()){
                for(lobby in 0 until listFull.size){
                    if(listFull[lobby].creator.lowercase().trim().contains(findLobbyByCreator.text.toString().lowercase().trim())){
                        listCurrent.add(listFull[lobby])
                    }
                }
            }else{
                if(findLobbyByCreator.text.isEmpty()){
                    for(lobby in 0 until listFull.size){
                        if(listFull[lobby].lobbyName.lowercase().trim().contains(findLobbyByName.text.toString().lowercase().trim())){
                            listCurrent.add(listFull[lobby])
                        }
                    }
                }else{
                    for(lobby in 0 until listFull.size){
                        if(listFull[lobby].lobbyName.lowercase().trim().contains(findLobbyByName.text.toString().lowercase().trim()) &&
                            listFull[lobby].creator.lowercase().trim().contains(findLobbyByCreator.text.toString().lowercase().trim())){
                            listCurrent.add(listFull[lobby])
                        }
                    }
                }
            }
        }
        //Filter listCurrent by distance

        showLobbies(listCurrent)
    }

    private fun getAllLobbies(){
        val list = ArrayList<Lobby>()
        db.collection("lobbies").get().addOnSuccessListener {
            result ->
            for(lobby in result.documents){
                list.add(Lobby(lobby["name"].toString(), lobby["location"].toString(), lobby["date"].toString(),
                lobby["time"].toString(), lobby["createdBy"].toString(), lobby["numberOfPlayersInLobby"].toString().toInt(),
                lobby["maximumNumberOfPlayers"].toString().toInt(), lobby["public"] as Boolean))
            }
            showLobbies(list)
            listFull = list
        }
    }

    private fun showLobbies(list:ArrayList<Lobby>){
        foundLobbiesRecyclerView.adapter = DataAdapter(list, this)
        foundLobbiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        foundLobbiesRecyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(position: Int) {
    }
}