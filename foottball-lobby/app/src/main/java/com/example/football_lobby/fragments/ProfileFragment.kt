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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    private lateinit var profilePic: ImageView
    private lateinit var name: TextView
    private lateinit var playedGames: TextView
    private lateinit var overallRating: TextView
    private lateinit var email: TextView
    private lateinit var birthday: TextView
    private lateinit var aboutMe: TextView

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
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
        val user = auth.currentUser
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

            db.collection("users").whereEqualTo("uid", user.uid).get()
                .addOnSuccessListener { result ->
                    val userData = result.documents[0]
                    Glide.with(this).load(userData["profilePic"]).into(profilePic)
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

    fun signOut() {
        auth.signOut()
        navigateToStartScreen()
    }

    fun deleteUser(){
        val user = auth.currentUser!!
        user.delete();
        navigateToStartScreen()
    }

    private fun editProfile(){
        findNavController().navigate(R.id.action_profileFragment_to_registrationFragment)
    }

    private fun navigateToStartScreen(){
        findNavController().navigate(R.id.action_profileFragment_to_startFragment)
    }
}