package com.example.football_lobby

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.football_lobby.services.Services
import com.example.football_lobby.fragments.LobbyDetailsFragment
import com.example.football_lobby.fragments.ProfileFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationProfile: BottomNavigationView
    private lateinit var bottomNavigationMain: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var navController:NavController
    private var quit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        requestPermissions()

        if(Firebase.auth.currentUser != null){
            Services.checkLobbies()
            ////////Delete this later TODO
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Firebase.firestore.collection("users").whereEqualTo("uid", Firebase.auth.currentUser!!.uid).get()
                    .addOnSuccessListener {
                        val tokens = it.documents[0]["tokens"] as ArrayList<String>
                        if(!tokens.contains(token)){
                            tokens.add(token)
                            it.documents[0].reference.update("tokens", tokens)
                        }
                }
            }
            ////////


        }

        bottomNavigationProfile = findViewById(R.id.bottomNavigationViewProfile)
        bottomNavigationMain = findViewById(R.id.bottomNavigationViewMain)

        topAppBar = findViewById(R.id.topAppToolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationMain.setupWithNavController(navController)
        bottomNavigationProfile.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            hideBottomNavigation()
            showTopNav()
            hideTopMenu()
            when (destination.id) {
                R.id.loginFragment, R.id.registrationFragment, R.id.forgotPasswordFragment, R.id.privateChatFragment -> {
                    hideBottomNavigationTotally()
                }
                R.id.startFragment -> {
                    hideTopNav()
                    hideBottomNavigationTotally()
                }
                R.id.profileFragment -> {
                    showTopMenu(R.id.profileGroup)
                    showBottomNavigationProfile()
                }
                R.id.findLobbiesFragment, R.id.createLobbyFragment, R.id.myLobbiesFragment,
                R.id.myFriendsFragment, R.id.findPlayersFragment-> {
                    hideTopNav()
                    showTopMenu(R.id.goToProfileGroup)
                    showBottomNavigationMain()
                }
                R.id.lobbyDetailsFragment -> {
                    showTopMenu(R.id.inLobbyGroup)
                    hideBottomNavigationTotally()
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
                R.id.myLobbiesItem -> {
                    navController.navigate(R.id.action_global_myLobbiesFragment)
                    true
                }
                R.id.friendsItem -> {
                    navController.navigate(R.id.action_global_myFriendsFragment)
                    true
                }
                R.id.findPlayerItem -> {
                    navController.navigate(R.id.action_global_findPlayersFragment)
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
                    fragment.deleteUser()
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
                R.id.leaveLobbyItem -> {
                    val fragment = navHostFragment.childFragmentManager.fragments[0] as LobbyDetailsFragment
                    fragment.leaveLobby()
                    true
                }
                R.id.addFriendItem -> {
                    val fragment = navHostFragment.childFragmentManager.fragments[0] as ProfileFragment
                    fragment.addFriend()
                    true
                }
                else -> {false}
            }
        }
    }

    override fun onBackPressed() {
        when (navController.currentDestination!!.id){
            R.id.findLobbiesFragment, R.id.createLobbyFragment, R.id.myLobbiesFragment, R.id.myFriendsFragment, R.id.startFragment -> {
                if(quit){
                    finishAndRemoveTask()
                }else{
                    quit = true
                    Toast.makeText(this, "Press again to quit!", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        quit = false
                    }, 2000)
                }
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun showTopMenu(group: Int) {
        topAppBar.menu.setGroupVisible(group ,true)
    }

    private fun hideTopMenu() {
        topAppBar.menu.setGroupVisible(R.id.profileGroup,false)
        topAppBar.menu.setGroupVisible(R.id.goToProfileGroup,false)
        topAppBar.menu.setGroupVisible(R.id.inLobbyGroup, false)
        topAppBar.menu.setGroupVisible(R.id.addFriendGroup, false)
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

    private fun requestPermissions(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
    }
}