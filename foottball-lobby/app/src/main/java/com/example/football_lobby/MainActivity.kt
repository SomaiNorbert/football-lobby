package com.example.football_lobby

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.football_lobby.fragments.ProfileFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationProfile: BottomNavigationView
    private lateinit var bottomNavigationMain: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        bottomNavigationProfile = findViewById(R.id.bottomNavigationViewProfile)
        bottomNavigationMain = findViewById(R.id.bottomNavigationViewMain)
        topAppBar = findViewById(R.id.topAppToolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            hideBottomNavigation()
            showTopNav()
            hideTopMenu()
            when (destination.id) {
                R.id.loginFragment -> {
                    hideBottomNavigationTotally()
                }
                R.id.registrationFragment -> {
                    hideBottomNavigationTotally()
                }
                R.id.startFragment -> {
                    hideTopNav()
                    hideBottomNavigationTotally()
                }
                R.id.forgotPasswordFragment -> {
                    hideBottomNavigationTotally()
                }
                R.id.profileFragment -> {
                    showTopMenu(R.id.profileGroup)
                    showBottomNavigationProfile()
                }
                R.id.findLobbyFragment -> {
                    hideTopNav()
                    showTopMenu(R.id.goToProfileGroup)
                    showBottomNavigationMain()
                }
                R.id.createLobbyFragment -> {
                    hideTopNav()
                    showTopMenu(R.id.goToProfileGroup)
                    showBottomNavigationMain()
                }
                else -> {
                    showBottomNavigationMain()
                }
            }
        }

        bottomNavigationMain.setOnItemSelectedListener {
            when(it.itemId){
                R.id.findLobbyItem -> {
                    navController.navigate(R.id.action_global_findLobbyFragment)
                    true
                }
                R.id.createLobbyItem -> {
                    navController.navigate(R.id.action_global_createLobbyFragment)
                    true
                }
                else -> {
                    false
                }
            }
        }

        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }
        topAppBar.setOnMenuItemClickListener {
            when(it.itemId)
            {
                R.id.editProfileItem -> {
                    navController.navigate(R.id.action_profileFragment_to_registrationFragment)
                    true
                }
                R.id.deleteProfileItem -> {
                    val fragment = navHostFragment.childFragmentManager.fragments[0] as ProfileFragment
                    fragment.deleteUser();
                    true
                }
                R.id.logOutItem -> {
                    val fragment = navHostFragment.childFragmentManager.fragments[0] as ProfileFragment
                    fragment.signOut()
                    true
                }
                R.id.profileItem -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        navController.navigate(R.id.action_global_profileFragment)
                    }, 100)
                    true
                }
                else -> {false}
            }
        }
    }

    private fun showTopMenu(group: Int) {
        topAppBar.menu.setGroupVisible(group ,true)
    }

    private fun hideTopMenu() {
        topAppBar.menu.setGroupVisible(R.id.profileGroup,false)
        topAppBar.menu.setGroupVisible(R.id.goToProfileGroup,false)
    }

    private fun showTopNav() {
        topAppBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
    }

    private fun hideTopNav() {
        topAppBar.navigationIcon = null
    }

    private fun showBottomNavigationMain() {
        bottomNavigationMain.visibility = View.VISIBLE
    }

    private fun showBottomNavigationProfile() {
        bottomNavigationProfile.visibility = View.VISIBLE
    }

    private fun hideBottomNavigation() {
        bottomNavigationProfile.visibility = View.INVISIBLE
        bottomNavigationMain.visibility = View.INVISIBLE
    }

    private fun hideBottomNavigationTotally() {
        bottomNavigationProfile.visibility = View.GONE
        bottomNavigationMain.visibility = View.GONE
    }
}