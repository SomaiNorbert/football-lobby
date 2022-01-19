package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var logo: ImageView
    private lateinit var errorTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = view.findViewById(R.id.emailEditText)
        password = view.findViewById(R.id.passwordEditText)
        logo = view.findViewById(R.id.logoImg)
        errorTxt = view.findViewById(R.id.errorTxt)

        logo.setImageResource(R.drawable.logo)

        view.findViewById<Button>(R.id.loginButton).setOnClickListener{
            if(validateLogIn())
                loginUser(email.text.toString(), password.text.toString())
        }

        view.findViewById<TextView>(R.id.goToForgotPasswordBtn).setOnClickListener{
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        view.findViewById<TextView>(R.id.goToRegisterBtn).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }
    }

    private fun validateLogIn() : Boolean{
        var error = ""
        if(email.text.isEmpty()){
            error = "Email can not be Empty!\n"
        }
        if(password.text.isEmpty()){
            error += "Password can not be empty!"
        }
        if(error == ""){
            errorTxt.visibility = View.GONE
            return true
        }
        errorTxt.text = error
        errorTxt.visibility = View.VISIBLE
        return false
    }

    private fun loginUser(email: String, password: String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
                } else {
                    errorTxt.text = "Email or password is incorrect!"
                    errorTxt.visibility = View.VISIBLE
                    Toast.makeText(this.context, "Authentication failed.", Toast.LENGTH_SHORT).show()

                }
            }
    }
}