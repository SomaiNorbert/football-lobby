package com.example.football_lobby.fragments

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
import com.example.football_lobby.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import java.util.*
import java.util.logging.Level.INFO
import javax.xml.datatype.DatatypeConstants.MONTHS
import kotlin.collections.ArrayList

class RegistrationFragment : Fragment(){

    private lateinit var auth: FirebaseAuth
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordAgain: EditText
    private lateinit var birthday: EditText
    private lateinit var aboutMe: EditText
    private lateinit var validationErrorsTxt: TextView
    private val validationErrors:ArrayList<String> = ArrayList()


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
        validationErrorsTxt = view.findViewById(R.id.validationErrorsTxt)

        birthday.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                var dpd = DatePickerDialog(context!!,{ _, mYear,mMonth , mDay ->
                    val mmMonth = mMonth+1
                    val date = "$mDay/$mmMonth/$mYear"
                    birthday.setText(date)
                },year,month,day)
                dpd.show()
                birthday.clearFocus()
            }
        }

        view.findViewById<TextView>(R.id.goToLogInBtn).setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        view.findViewById<Button>(R.id.registerButton).setOnClickListener{
            if(validateInput()){
                createAccount(email.text.toString(), password.text.toString())
            }else{
                printValidationError()
            }

        }
    }

    private fun printValidationError() {
        var error = ""
        for(item in validationErrors){
            error += item + "\n"
        }
        if(error != ""){
            validationErrorsTxt.visibility = View.VISIBLE
            validationErrorsTxt.text = error
            validationErrors.clear()
        }else{
            validationErrorsTxt.visibility = View.GONE
        }

    }

    private fun validateInput() : Boolean{
        var valid = validateName()
        valid = validateEmail() && valid
        valid = validatePassword() && valid
        valid = validatePasswordAgain() && valid
        valid = validateBirthday() && valid
        valid = validateAboutMe() && valid
        return valid
    }
    private fun validateName(): Boolean {
        if(name.text.isEmpty()){
            validationErrors.add("Name can not be empty!")
            return false
        }
        else return true
    }

    private fun validateEmail(): Boolean {
        var valid = true
        if(TextUtils.isEmpty(email.text)) {
            validationErrors.add("Email can not be empty!")
            valid = false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text).matches()){
            validationErrors.add("Email has to be a valid email address!")
            valid = false
        }
        return valid
    }

    private fun validatePassword(): Boolean {
        var valid = true
        if(password.text.length < 8){
            validationErrors.add("Password must contain at least 8 characters!")
            valid = false
        }

        if(!"[A-Z]".toRegex().containsMatchIn(password.text)){
            validationErrors.add("Password must contain an upper case letter!")
            valid = false
        }

        if(!"[0-9]".toRegex().containsMatchIn(password.text)){
            validationErrors.add("Password must contain a number!")
            valid = false
        }

        if(!"[a-z]".toRegex().containsMatchIn(password.text)){
            validationErrors.add("Password must contain a lower case letter!")
            valid = false
        }

        return valid
    }

    private fun validatePasswordAgain(): Boolean {
        if(password.text.toString() != passwordAgain.text.toString()){
            Log.d(TAG, "password:${password.text}")
            Log.d(TAG, "password2:${passwordAgain.text}")
            validationErrors.add("The two passwords has to match!")
            return false
        }
        return true
    }

    private fun validateBirthday(): Boolean {
        if(birthday.text.isEmpty()){
            validationErrors.add("Birthday can not be empty!")
            return false
        }
        return true
    }

    private fun validateAboutMe(): Boolean {
        if(aboutMe.text.isEmpty()){
            validationErrors.add("About me can not be empty!")
            return false
        }
        else return true
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                updateUI(user)
            } else {
                Toast.makeText(this.context, "Registration failed.", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    private fun reload() {

    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null){
            findNavController().navigate(R.id.action_registrationFragment_to_profileFragment)
        }

    }

}