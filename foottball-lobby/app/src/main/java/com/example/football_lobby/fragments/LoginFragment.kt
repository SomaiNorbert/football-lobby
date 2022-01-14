package com.example.football_lobby.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.football_lobby.R

class LoginFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        view.findViewById<View>(R.id.goToForgotPasswordBtn).setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_forgotPasswordFragment)
        )

        view.findViewById<View>(R.id.goToRegisterBtn).setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_loginFragment_to_registrationFragment)
        )
    }

}