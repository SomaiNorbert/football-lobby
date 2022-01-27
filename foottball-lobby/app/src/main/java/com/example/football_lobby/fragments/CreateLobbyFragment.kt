package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateLobbyFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var lobbyName: EditText
    private lateinit var location: EditText
    private lateinit var date: EditText
    private lateinit var time: EditText
    private lateinit var maximumNumberOfPlayers: EditText
    private lateinit var radioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var public = true

        lobbyName = view.findViewById(R.id.lobbyNameTextInputEditText)
        location = view.findViewById(R.id.locationTextInputEditText)
        date = view.findViewById(R.id.dateTextInputEditText)
        time = view.findViewById(R.id.timeTextInputEditText)
        maximumNumberOfPlayers = view.findViewById(R.id.numberOfPlayersTextInputEditText)
        radioGroup = view.findViewById(R.id.radioGroup)

        radioGroup.setOnCheckedChangeListener { _, i ->
            public = (i == R.id.publicRB)
        }

        view.findViewById<Button>(R.id.createBtn).setOnClickListener {
            if(validateInput()){
                val user = auth.currentUser!!
                var userName = ""
                db.collection("users").whereEqualTo("uid", user.uid).get()
                    .addOnSuccessListener { result ->
                        userName = result.documents[0]["name"].toString()
                        val lobby = hashMapOf(
                            "name" to lobbyName.text.toString(),
                            "location" to location.text.toString(),
                            "date" to date.text.toString(),
                            "time" to time.text.toString(),
                            "createdBy" to userName,
                            "maximumNumberOfPlayers" to maximumNumberOfPlayers.text.toString().toInt(),
                            "numberOfPlayersInLobby" to 0,
                            "public" to public
                        )
                        db.collection("lobbies").add(lobby)
                        findNavController().navigate(R.id.action_global_findLobbyFragment)
                    }
            }else{
                showValidationError()
            }
        }

    }

    private fun showValidationError() {
        Log.d(TAG, "ERROR")
    }

    private fun validateInput(): Boolean {
        var valid = true
        if(lobbyName.text.isEmpty()){

            valid = false
        }
        if(location.text.isEmpty()){

            valid = false
        }
        if(date.text.isEmpty()){

            valid = false
        }
        if(time.text.isEmpty()){

            valid = false
        }
        if(maximumNumberOfPlayers.text.isEmpty()){

            valid = false
        }else if(maximumNumberOfPlayers.text.toString().toInt() < 1 || maximumNumberOfPlayers.text.toString().toInt() > 20){

            valid = false
        }
        return valid
    }
}