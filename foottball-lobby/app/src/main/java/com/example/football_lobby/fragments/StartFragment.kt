package com.example.football_lobby.fragments

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class StartFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            findNavController().navigate(R.id.action_startFragment_to_profileFragment)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.logoImg).setImageResource(R.drawable.logo)

        view.findViewById<View>(R.id.loginBtn).setOnClickListener{
            findNavController().navigate(R.id.action_startFragment_to_loginFragment)
        }
        view.findViewById<View>(R.id.registerBtn).setOnClickListener{
            findNavController().navigate(R.id.action_startFragment_to_registrationFragment)
        }
    }

}