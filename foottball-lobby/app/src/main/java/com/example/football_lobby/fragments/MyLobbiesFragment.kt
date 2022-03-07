package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.LobbiesDataAdapter
import com.example.football_lobby.models.Lobby
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyLobbiesFragment : Fragment(), LobbiesDataAdapter.OnItemClickedListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var myLobbiesRV: RecyclerView
    private lateinit var noLobbiesFoundTxt: TextView
    private lateinit var adapterLobbies: LobbiesDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_lobbies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myLobbiesRV = view.findViewById(R.id.myLobbiesRV)
        noLobbiesFoundTxt = view.findViewById(R.id.noLobbyFoundMyLobbiesTxt)

        setupRecyclerView()

        db.collection("lobbies").get().addOnSuccessListener {
            val list = ArrayList<Lobby>()
            for(lobby in it.documents){
                if((lobby["players"] as ArrayList<String>).contains(Firebase.auth.currentUser!!.uid)){
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
            }
            adapterLobbies.setData(list)
            if(list.size == 0){
                noLobbiesFoundTxt.visibility = View.VISIBLE
            }else{
                noLobbiesFoundTxt.visibility = View.INVISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        adapterLobbies = LobbiesDataAdapter(requireContext(), ArrayList(), this)
        myLobbiesRV.adapter = adapterLobbies
        myLobbiesRV.layoutManager = LinearLayoutManager(requireContext())
        myLobbiesRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("uid", uid)
        findNavController().navigate(R.id.action_global_lobbyDetailsFragment, bundle)
    }

}