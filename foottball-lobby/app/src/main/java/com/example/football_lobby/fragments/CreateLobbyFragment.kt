package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class CreateLobbyFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var lobbyName: EditText
    private lateinit var location: EditText
    private lateinit var date: EditText
    private lateinit var time: EditText
    private lateinit var maximumNumberOfPlayers: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var validationErrorsTxt: TextView
    private val validationErrors: ArrayList<String> = ArrayList()

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

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var public = true

        lobbyName = view.findViewById(R.id.lobbyNameTextInputEditText)
        location = view.findViewById(R.id.locationTextInputEditText)
        date = view.findViewById(R.id.dateTextInputEditText)
        time = view.findViewById(R.id.timeTextInputEditText)
        maximumNumberOfPlayers = view.findViewById(R.id.numberOfPlayersTextInputEditText)
        radioGroup = view.findViewById(R.id.radioGroup)
        validationErrorsTxt = view.findViewById(R.id.validationErrorsTxt)

        date.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                var dpd = DatePickerDialog(context!!,16973939, { _, mYear, mMonth, mDay ->
                    val mmMonth = mMonth + 1
                    val d = "$mDay/$mmMonth/$mYear"
                    date.setText(d)
                }, year, month, day)
                dpd.datePicker.minDate = calendar.timeInMillis
                dpd.setTitle("Choose Game Date")
                dpd.show()
                date.clearFocus()
            }
        }

        time.setOnFocusChangeListener { view, hasFocus ->
            if(hasFocus){
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR)
                val minute = calendar.get(Calendar.MINUTE)
                var tpd = TimePickerDialog(context!!,16973939, { _, mHour, mMinute ->
                    val t = "$mHour:$mMinute"
                    time.setText(t)
                }, hour, minute, true)
                tpd.setTitle("Choose Game Time")
                tpd.show()
                time.clearFocus()
            }
        }


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
                view.findViewById<ScrollView>(R.id.scrollView).scrollTo(0,0)
            }
        }

    }

    private fun showValidationError() {
        var error = ""
        for (item in validationErrors) {
            error += item + "\n"
        }
        if (error != "") {
            validationErrorsTxt.visibility = View.VISIBLE
            validationErrorsTxt.text = error
            validationErrors.clear()
        } else {
            validationErrorsTxt.visibility = View.GONE
        }
    }

    private fun validateInput(): Boolean {
        var valid = true
        if(lobbyName.text.isEmpty()){
            validationErrors.add("Lobby name can not be empty!")
            valid = false
        }
        if(location.text.isEmpty()){
            validationErrors.add("Location can not be empty!")
            valid = false
        }
        if(date.text.isEmpty()){
            validationErrors.add("You must choose a date!")
            valid = false
        }
        if(time.text.isEmpty()){
            validationErrors.add("You must choose a time!")
            valid = false
        }
        if(maximumNumberOfPlayers.text.isEmpty()){
            validationErrors.add("Number of players per team can not be empty!")
            valid = false
        }else if(maximumNumberOfPlayers.text.toString().toInt() < 1 || maximumNumberOfPlayers.text.toString().toInt() > 20){
            validationErrors.add("Number of players per team has to be between 1 and 20!")
            valid = false
        }
        return valid
    }
}