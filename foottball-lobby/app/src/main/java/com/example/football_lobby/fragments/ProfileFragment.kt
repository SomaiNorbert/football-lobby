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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.adapters.RatingsDataAdapter
import com.example.football_lobby.models.Rating
import com.example.football_lobby.services.MyFirebaseMessagingService
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference

    private lateinit var profilePic: ImageView
    private lateinit var name: TextView
    private lateinit var playedGames: TextView
    private lateinit var overallRating: TextView
    private lateinit var email: TextView
    private lateinit var birthday: TextView
    private lateinit var aboutMe: TextView
    private lateinit var ratingsRV: RecyclerView
    private lateinit var groupAboutMe: Group

    private lateinit var adapterRatings: RatingsDataAdapter

    private lateinit var userUid: String
    private var user:FirebaseUser? = null
    private lateinit var addFriendItem: MenuItem

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val storage = Firebase.storage
        storageRef = storage.reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = auth.currentUser
        if (user == null) {
            navigateToStartScreen()
        } else {

            profilePic = view.findViewById(R.id.profilePictureImageView)
            name = view.findViewById(R.id.playerNameTxt)
            playedGames = view.findViewById(R.id.playedGamesTxt)
            overallRating = view.findViewById(R.id.ratingTxt)
            email = view.findViewById(R.id.emailTxt)
            birthday = view.findViewById(R.id.birthdayTxt)
            aboutMe = view.findViewById(R.id.aboutMeTxt)
            ratingsRV = view.findViewById(R.id.myRatingsRV)
            groupAboutMe = view.findViewById(R.id.groupAbout)

            userUid = arguments?.get("playerUid").toString()
            if (userUid == "" || userUid == "null") {
                userUid = user!!.uid
            }else{

                val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.topAppToolbar)
                addFriendItem = toolbar.menu.findItem(R.id.addFriendItem)
                toolbar.menu.setGroupVisible(R.id.profileGroup, userUid == user!!.uid)
                toolbar.menu.setGroupVisible(R.id.addFriendGroup, userUid != user!!.uid)
                db.collection("users").whereEqualTo("uid", user!!.uid).get().addOnSuccessListener {
                    if (it.documents[0]["friends"] != null && (it.documents[0]["friends"] as ArrayList<String>).contains(
                            userUid
                        )
                    ) {
                        addFriendSetup(false)
                    } else {
                        addFriendSetup(true)
                    }
                }
            }
            setUpRecyclerView()
            db.collection("users").whereEqualTo("uid", userUid).get()
                .addOnSuccessListener { result ->
                    val userData = result.documents[0]

                    CoroutineScope(Dispatchers.Default).launch {
                        loadRatingsIntoDataAdapter(userData)
                    }

                    storageRef.child("images/${userUid}").downloadUrl.addOnSuccessListener {
                        Glide.with(requireContext()).load(it).into(profilePic)
                    }
                    if(userUid != user!!.uid){
                        addFriendItem.isVisible = true
                        if (userData["requests"] != null) {
                            if ((userData["requests"] as ArrayList<String>).contains(auth.currentUser!!.uid)) {
                                addFriendItem.isVisible = false
                            }
                        }
                    }
                    name.text = userData["name"].toString()
                    playedGames.text = userData["numberOfGamesPlayed"].toString()
                    if (userData["overallRating"].toString() == "0") {
                        overallRating.text = "-"
                    } else {
                        overallRating.text = userData["overallRating"].toString() + "/5"
                    }
                    email.text = userData["email"].toString()
                    birthday.text = userData["birthday"].toString()
                    aboutMe.text = userData["aboutMe"].toString()
                }
        }
    }

    private fun loadRatingsIntoDataAdapter(doc: DocumentSnapshot){
        val list = ArrayList<Rating>()
        val ratings = ArrayList<HashMap<String,String>>()
        if(doc["ratings"] != null){
            ratings.addAll(doc["ratings"] as ArrayList<HashMap<String, String>>)
        }
        for(rating in ratings) {
            val lobby = Tasks.await(db.collection("oldLobbies").whereEqualTo("uid", rating["fromLobbyUid"].toString()).get())
            val lobbyName = lobby.documents[0]["name"].toString()
            val player = Tasks.await(db.collection("users").whereEqualTo("uid", rating["fromUid"].toString()).get())
            val playerName = player.documents[0]["name"].toString()
            var comment = ""
            if(rating["personalComment"].toString().isNotEmpty()){
                comment = "Comment: "
            }
            list.add(
                Rating(
                    rating["punctuality"].toString().toInt(),
                    rating["behavior"].toString().toInt(),
                    rating["calmness"].toString().toInt(),
                    rating["sportsmanship"].toString().toInt(),
                    comment + rating["personalComment"].toString(),
                    lobbyName,
                    playerName
                )
            )
        }
        CoroutineScope(Dispatchers.Main).launch {
            adapterRatings.setData(list)
        }
    }

    private fun setUpRecyclerView(){
        adapterRatings = RatingsDataAdapter(ArrayList())
        ratingsRV.adapter = adapterRatings
        ratingsRV.layoutManager = LinearLayoutManager(requireContext())
        ratingsRV.setHasFixedSize(true)
    }

    private fun addFriendSetup(b: Boolean) {
        if(b){
            addFriendItem.title = "Send Friend Request"
        }else{
            addFriendItem.title = "Remove from Friends"
        }
    }

    fun loadAboutMe(){
        ratingsRV.visibility = View.INVISIBLE
        groupAboutMe.visibility = View.VISIBLE
    }

    fun loadMyRatings(){
        ratingsRV.visibility = View.VISIBLE
        groupAboutMe.visibility = View.INVISIBLE
    }

    fun signOut() {
        db.collection("users").whereEqualTo("uid", userUid).get().addOnSuccessListener {
            if (it.documents[0]["tokens"] != null) {
                val tokens = it.documents[0]["tokens"] as ArrayList<String>
                val myToken = MyFirebaseMessagingService().getToken(requireContext())
                if (myToken != null && myToken.isNotEmpty() && tokens.contains(myToken)){//MyFirebaseInstanceIdService().getToken())) {
                    tokens.remove(myToken)
                    it.documents[0].reference.update("tokens", tokens).addOnSuccessListener {
                        auth.signOut()
                        navigateToStartScreen()
                    }
                }
            }
        }
    }

    fun deleteUser(){
        //user!!.delete();
        //DELETE from DBs
        //navigateToStartScreen()
    }

    private fun navigateToStartScreen(){
        findNavController().navigate(R.id.action_profileFragment_to_startFragment)
    }

    fun addFriend() {
        var me:DocumentSnapshot
        var otherUser:DocumentSnapshot
        db.collection("users").whereEqualTo("uid", user!!.uid).get().addOnSuccessListener { resMe ->
            me = resMe.documents[0]
            db.collection("users").whereEqualTo("uid", userUid).get().addOnSuccessListener { resOtherUser ->
                otherUser = resOtherUser.documents[0]
                val myFriends = me["friends"] as ArrayList<String>
                val requests = ArrayList<String>()
                if(otherUser["requests"] != null){
                    requests.addAll(otherUser["requests"] as ArrayList<String>)
                }
                val otherUsersFriends = otherUser["friends"] as ArrayList<String>

                if(addFriendItem.title == "Send Friend Request"){
                    if(!requests.contains(me["uid"].toString())){
                        requests.add(me["uid"].toString())
                        addFriendSetup(false)
                        addFriendItem.isVisible = false
                        Toast.makeText(requireContext(), "Friend request sent!", Toast.LENGTH_SHORT).show()
                        val tokens = ArrayList<String>()
                        if(otherUser["tokens"] != null){
                            tokens.addAll(otherUser["tokens"] as ArrayList<String>)
                        }
                        MyFirebaseMessagingService().sendNotificationToPlayerOnFriendRequest(arrayListOf(userUid),
                            tokens, me["name"].toString())
                    }
                }else{
                    myFriends.remove(otherUser["uid"].toString())
                    otherUsersFriends.remove(me["uid"].toString())
                    addFriendSetup(true)

                }

                db.collection("users").document(me.id).update("friends", myFriends)
                db.collection("users").document(otherUser.id).update(
                    "friends", otherUsersFriends,
                    "requests", requests
                )
            }
        }
    }
}