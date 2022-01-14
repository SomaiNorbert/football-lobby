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
import com.example.football_lobby.R

class StartFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        //view.findViewById<ImageView>(R.id.logoImg).setImageDrawable(Drawable.createFromPath("drawable://" + R.drawable.logo))


        view.findViewById<View>(R.id.loginBtn).setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_startFragment_to_loginFragment)
        )
        view.findViewById<View>(R.id.registerBtn).setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_startFragment_to_registrationFragment)
        )
    }

}