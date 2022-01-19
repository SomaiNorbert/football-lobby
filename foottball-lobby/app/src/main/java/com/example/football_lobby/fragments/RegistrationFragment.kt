package com.example.football_lobby.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import java.util.*
import java.util.logging.Level.INFO
import javax.xml.datatype.DatatypeConstants.MONTHS
import kotlin.collections.ArrayList
import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toDrawable


class RegistrationFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordAgain: EditText
    private lateinit var birthday: EditText
    private lateinit var aboutMe: EditText
    private lateinit var profilePicture: ImageView
    private lateinit var validationErrorsTxt: TextView
    private val validationErrors: ArrayList<String> = ArrayList()
    private var sUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        name = view.findViewById(R.id.nameEdt)
        email = view.findViewById(R.id.emailEdt)
        password = view.findViewById(R.id.passwordEdt)
        passwordAgain = view.findViewById(R.id.passwordAgainEdt)
        birthday = view.findViewById(R.id.birthdayEdt)
        aboutMe = view.findViewById(R.id.aboutMeEdt)
        profilePicture = view.findViewById(R.id.profilePicture)
        validationErrorsTxt = view.findViewById(R.id.validationErrorsTxt)

        profilePicture.setImageResource(R.drawable.profil_avatar)

        birthday.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                var dpd = DatePickerDialog(context!!, { _, mYear, mMonth, mDay ->
                    val mmMonth = mMonth + 1
                    val date = "$mDay/$mmMonth/$mYear"
                    birthday.setText(date)
                }, year, month, day)
                dpd.show()
                birthday.clearFocus()
                aboutMe.requestFocus()
            }
        }

        val getContent = registerForActivityResult(ActivityResultContracts.GetContent(),) { uri: Uri? ->
            Log.d(TAG, uri.toString())
            sUri = uri.toString()
            Glide.with(this).load(uri).into(profilePicture)
        }

        view.findViewById<Button>(R.id.choosePictureBtn).setOnClickListener {
            getContent.launch("image/*")
        }

        view.findViewById<TextView>(R.id.goToLogInBtn).setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            if (validateInput()) {
                auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val db = Firebase.firestore
                            val user = hashMapOf(
                                "name" to name.text.toString(),
                                "email" to email.text.toString(),
                                "password" to password.text.toString(),
                                "birthday" to birthday.text.toString(),
                                "aboutMe" to aboutMe.text.toString(),
                                "profilePic" to sUri,
                                "numberOfGamesPlayed" to 0,
                                "overallRating" to 0,
                                "uid" to auth.uid
                            )
                            db.collection("users").add(user)
                            findNavController().navigate(R.id.action_registrationFragment_to_profileFragment)
                        } else {
                            Toast.makeText(this.context, "Registration failed.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                printValidationError()
            }
        }
    }

    private fun printValidationError() {
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
        var valid = validateProfilePic()
        valid = validateName() && valid
        valid = validateEmail() && valid
        valid = validatePassword() && valid
        valid = validatePasswordAgain() && valid
        valid = validateBirthday() && valid
        valid = validateAboutMe() && valid
        return valid
    }

    private fun validateProfilePic(): Boolean {
        if(sUri == ""){
            validationErrors.add("You must select a profile picture!")
            return false
        }
        return true
    }

    private fun validateName(): Boolean {
        if (name.text.isEmpty()) {
            validationErrors.add("Name can not be empty!")
            return false
        } else return true
    }

    private fun validateEmail(): Boolean {
        var valid = true
        if (TextUtils.isEmpty(email.text)) {
            validationErrors.add("Email can not be empty!")
            valid = false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text).matches()) {
            validationErrors.add("Email has to be a valid email address!")
            valid = false
        }
        return valid
    }

    private fun validatePassword(): Boolean {
        var valid = true
        if (password.text.length < 8) {
            validationErrors.add("Password must contain at least 8 characters!")
            valid = false
        }

        if (!"[A-Z]".toRegex().containsMatchIn(password.text)) {
            validationErrors.add("Password must contain an upper case letter!")
            valid = false
        }

        if (!"[0-9]".toRegex().containsMatchIn(password.text)) {
            validationErrors.add("Password must contain a number!")
            valid = false
        }

        if (!"[a-z]".toRegex().containsMatchIn(password.text)) {
            validationErrors.add("Password must contain a lower case letter!")
            valid = false
        }

        return valid
    }

    private fun validatePasswordAgain(): Boolean {
        if (password.text.toString() != passwordAgain.text.toString()) {
            Log.d(TAG, "password:${password.text}")
            Log.d(TAG, "password2:${passwordAgain.text}")
            validationErrors.add("The two passwords has to match!")
            return false
        }
        return true
    }

    private fun validateBirthday(): Boolean {
        if (birthday.text.isEmpty()) {
            validationErrors.add("Birthday can not be empty!")
            return false
        }
        return true
    }

    private fun validateAboutMe(): Boolean {
        if (aboutMe.text.isEmpty()) {
            validationErrors.add("About me can not be empty!")
            return false
        } else return true
    }
}