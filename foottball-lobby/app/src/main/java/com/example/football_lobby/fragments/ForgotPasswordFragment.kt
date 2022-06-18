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
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var emailSentTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = view.findViewById(R.id.fPEmailEdt)
        emailSentTxt = view.findViewById(R.id.emailSentTxt)

        view.findViewById<Button>(R.id.emailMeBtn).setOnClickListener {
            auth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    emailSentTxt.visibility = View.VISIBLE
                    emailSentTxt.text = "Email sent to " + email.text.toString() + " address!"
                    email.setText("")
                    email.clearFocus()
                }else{
                    emailSentTxt.visibility = View.VISIBLE
                    emailSentTxt.text = "The given email address is not registered or it is incorrect!"
                    email.setText("")
                    email.clearFocus()
                }
            }
        }
    }

}