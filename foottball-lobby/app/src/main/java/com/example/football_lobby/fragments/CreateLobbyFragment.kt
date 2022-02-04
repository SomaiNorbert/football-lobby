package com.example.football_lobby.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.football_lobby.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.util.*


class CreateLobbyFragment : Fragment(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var lobbyName: EditText
    private lateinit var location: EditText
    private lateinit var date: EditText
    private lateinit var time: EditText
    private lateinit var maximumNumberOfPlayers: Spinner
    private lateinit var radioGroup: RadioGroup
    private lateinit var validationErrorsTxt: TextView
    private lateinit var mapFragment:SupportMapFragment
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
        maximumNumberOfPlayers = view.findViewById(R.id.numberOfPlayersSpinner)
        radioGroup = view.findViewById(R.id.radioGroup)
        validationErrorsTxt = view.findViewById(R.id.validationErrorsTxt)

        maximumNumberOfPlayers.adapter = activity?.let{
            ArrayAdapter(it, android.R.layout.simple_spinner_item, (5..11).toList())
        }
        maximumNumberOfPlayers.setSelection(0)

        location.setOnClickListener {
            mapFragment = SupportMapFragment.newInstance()
            parentFragmentManager
                .beginTransaction()
                .add(R.id.nav_host_fragment, mapFragment).addToBackStack("MapFragment")
                .commit()
            mapFragment.getMapAsync(this)
        }

        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            var dpd = DatePickerDialog(requireContext(), 16973939, { _, mYear, mMonth, mDay ->
                val mmMonth = mMonth + 1
                val d = "$mDay/$mmMonth/$mYear"
                date.setText(d)
            }, year, month, day)
            dpd.datePicker.minDate = calendar.timeInMillis
            dpd.setTitle("Choose Game Date")
            dpd.show()
        }

        time.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val tpd = TimePickerDialog(requireContext(), 16973939, { _, mHour, mMinute ->
                var t = ""
                t = if(mMinute < 10){
                    "$mHour:0$mMinute"
                }else{
                    "$mHour:$mMinute"
                }
                time.setText(t)
            }, hour, minute, true)
            tpd.setTitle("Choose Game Time")
            tpd.show()
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
                            "uid" to UUID.randomUUID().toString(),
                            "name" to lobbyName.text.toString(),
                            "location" to location.text.toString(),
                            "date" to date.text.toString(),
                            "time" to time.text.toString(),
                            "creatorName" to userName,
                            "creatorUid" to user.uid,
                            "maximumNumberOfPlayers" to maximumNumberOfPlayers.selectedItem.toString().toInt(),
                            "numberOfPlayersInLobby" to 1,
                            "public" to public,
                            "players" to listOf(user.uid)
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

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        var marker: Marker? = null
        val geocoder = Geocoder(context, Locale.getDefault())

        if(location.text.isEmpty()){
            if(checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    marker = googleMap.addMarker(MarkerOptions().position(currentLocation).draggable(false))
                    marker!!.title = "Click to select:"
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
                }
            }
        }else{
            val add = geocoder.getFromLocationName(location.text.toString(), 1)
            val currentLocation = LatLng(add[0].latitude, add[0].longitude)
            marker = googleMap.addMarker(MarkerOptions().position(currentLocation).draggable(false))
            marker!!.title = "Click to select:"
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
        }

        googleMap.setOnCameraMoveListener {
            marker?.position = googleMap.cameraPosition.target
        }
        googleMap.setOnCameraIdleListener {
            val address = geocoder.getFromLocation(marker!!.position.latitude, marker!!.position.longitude, 1)[0]
            marker!!.snippet = address.getAddressLine(0).dropLastWhile { it != ','}.dropLast(1)
            marker!!.showInfoWindow()
        }

        googleMap.setOnInfoWindowClickListener{
            location.setText(marker!!.snippet)
            parentFragmentManager.beginTransaction().remove(mapFragment).commit()
        }

        googleMap.uiSettings.isZoomControlsEnabled = true
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
        return valid
    }
}