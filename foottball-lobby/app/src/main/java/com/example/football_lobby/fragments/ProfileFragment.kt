package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.models.Player
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
        if(user == null){
            navigateToStartScreen()
        }else{

            profilePic = view.findViewById(R.id.profilePictureImageView)
            name = view.findViewById(R.id.playerNameTxt)
            playedGames = view.findViewById(R.id.playedGamesTxt)
            overallRating = view.findViewById(R.id.ratingTxt)
            email = view.findViewById(R.id.emailTxt)
            birthday = view.findViewById(R.id.birthdayTxt)
            aboutMe = view.findViewById(R.id.aboutMeTxt)

            userUid = arguments?.get("playerUid").toString()
            if(userUid == "" || userUid == "null"){
                userUid = user!!.uid
            }else{
                val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.topAppToolbar)
                addFriendItem = toolbar.menu.findItem(R.id.addFriendItem)
                toolbar.menu.setGroupVisible(R.id.profileGroup, userUid == user!!.uid)
                toolbar.menu.setGroupVisible(R.id.addFriendGroup, userUid != user!!.uid)
                db.collection("users").whereEqualTo("uid", user!!.uid).get().addOnSuccessListener {
                    if(it.documents[0]["friends"] != null && (it.documents[0]["friends"] as ArrayList<String>).contains(userUid)){
                        addFriendSetup(false)
                    }else{
                        addFriendSetup(true)
                    }
                }
            }
            db.collection("users").whereEqualTo("uid", userUid).get()
                .addOnSuccessListener { result ->
                    val userData = result.documents[0]
                    storageRef.child("images/${userUid}").downloadUrl.addOnSuccessListener {
                        Glide.with(requireContext()).load(it).into(profilePic)
                    }
                    name.text = userData["name"].toString()
                    playedGames.text = userData["numberOfGamesPlayed"].toString()
                    if(userData["overallRating"].toString() == "0"){
                        overallRating.text = "-"
                    }else{
                        overallRating.text = userData["overallRating"].toString() + "/10"
                    }
                    email.text = userData["email"].toString()
                    birthday.text = userData["birthday"].toString()
                    aboutMe.text = userData["aboutMe"].toString()
            }
        }
    }

    private fun addFriendSetup(b: Boolean) {
        if(b){
            addFriendItem.title = "Add to Friends"
        }else{
            addFriendItem.title = "Remove from Friends"
        }
    }

    fun signOut() {
        auth.signOut()
        navigateToStartScreen()
    }

    fun deleteUser(){
        user!!.delete();
        //DELETE from DBs TODO
        navigateToStartScreen()
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
                val otherUsersFriends = otherUser["friends"] as ArrayList<String>

                if(addFriendItem.title == "Add to Friends"){
                    myFriends.add(otherUser["uid"].toString())
                    otherUsersFriends.add(me["uid"].toString())
                    addFriendSetup(false)
                }else{
                    myFriends.remove(otherUser["uid"].toString())
                    otherUsersFriends.remove(me["uid"].toString())
                    addFriendSetup(true)
                }

                db.collection("users").document(me.id).update("friends", myFriends)
                db.collection("users").document(otherUser.id).update("friends", otherUsersFriends)
            }
        }
    }
}