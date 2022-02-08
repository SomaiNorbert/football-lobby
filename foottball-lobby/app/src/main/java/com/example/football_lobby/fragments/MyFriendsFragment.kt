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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MyFriendsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var myFriendsRV: RecyclerView
    private lateinit var noFriendsFoundTxt: TextView
    private lateinit var adapterPlayers: PlayersDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
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

        myFriendsRV = view.findViewById(R.id.myFriendsRV)
        noFriendsFoundTxt = view.findViewById(R.id.noFriendsFoundTxt)

        setupRecyclerView()

        CoroutineScope(Dispatchers.Default).launch{
            val me = Tasks.await(db.collection("users").whereEqualTo("uid", Firebase.auth.currentUser!!.uid)
                .get()).documents[0]
            val friends = ArrayList<Player>()
            if(me["friends"] != null){
                val friendsUids = me["friends"] as ArrayList<String>
                for(friendUid in friendsUids){
                    val f = Tasks.await(db.collection("users").whereEqualTo("uid", friendUid).get()).documents[0]
                    friends.add(Player(f["name"].toString(), f["birthday"].toString(),
                        f["overallRating"].toString().toDouble(), f["uid"].toString()))
                }
            }
            adapterPlayers.setData(friends)
        }.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                adapterPlayers.notifyDataSetChanged()
                if(adapterPlayers.itemCount == 0){
                    noFriendsFoundTxt.visibility = View.VISIBLE
                }else{
                    noFriendsFoundTxt.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapterPlayers = PlayersDataAdapter(ArrayList(),this,"")
        myFriendsRV.adapter = adapterPlayers
        myFriendsRV.layoutManager = LinearLayoutManager(requireContext())
        myFriendsRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_myFriendsFragment_to_profileFragment, bundle)
    }
}