package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.services.MyFirebaseMessagingService
import com.google.android.gms.tasks.Tasks
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*


class RegistrationFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordAgain: EditText
    private lateinit var birthday: EditText
    private lateinit var aboutMe: EditText
    private lateinit var profilePicture: ImageView
    private lateinit var validationErrorsTxt: TextView
    private lateinit var registerButton: Button
    private lateinit var goToLogInBtn: TextView
    private lateinit var alreadyRegisterdTxt: TextView
    private lateinit var storageRef: StorageReference
    private val validationErrors: ArrayList<String> = ArrayList()
    private var sUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        val storage = Firebase.storage
        storageRef = storage.reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "ResourceType")
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
        registerButton = view.findViewById(R.id.registerButton)
        goToLogInBtn = view.findViewById(R.id.goToLogInBtn)
        alreadyRegisterdTxt = view.findViewById(R.id.alreadyRegisterdTxt)

        var userDoc: QuerySnapshot? = null

        val user = auth.currentUser

        if(user != null){
            view.findViewById<TextInputLayout>(R.id.rPasswordTextInputLayout).hint = "New password"
            view.findViewById<TextInputLayout>(R.id.rPasswordAgainTextInputLayout).hint = "New password again"
            registerButton.text = "Save changes"
            goToLogInBtn.visibility = View.INVISIBLE
            alreadyRegisterdTxt.visibility = View.INVISIBLE
            db.collection("users").whereEqualTo("uid", user.uid).get()
                .addOnSuccessListener { result ->
                    userDoc = result
                    val userData = result.documents[0]
                    storageRef.child("images/${user.uid}").downloadUrl.addOnSuccessListener {
                        Glide.with(requireActivity().baseContext).load(it).into(profilePicture)
                    }

                    name.setText(userData["name"].toString())
                    email.setText(userData["email"].toString())
                    birthday.setText(userData["birthday"].toString())
                    aboutMe.setText(userData["aboutMe"].toString())
                }
        }else{
            profilePicture.setImageResource(R.drawable.profile_avatar)
        }

        birthday.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            var dpd = DatePickerDialog(requireContext(), 16973939, { _, mYear, mMonth, mDay ->
                val mmMonth = mMonth + 1
                val date = "$mDay/$mmMonth/$mYear"
                birthday.setText(date)
            }, year, month, day)
            calendar.add(Calendar.YEAR, -5)
            dpd.datePicker.maxDate = calendar.timeInMillis
            dpd.setTitle("Choose Birthday!")
            dpd.show()
        }

        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if(uri != null){
                sUri = uri.toString()
                Glide.with(this).load(uri).into(profilePicture)
            }
        }

        view.findViewById<ImageView>(R.id.profilePicture).setOnClickListener{
            getContent.launch("image/*")
        }

        goToLogInBtn.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        aboutMe.setOnTouchListener { view, event ->
            view.performClick()
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

        registerButton.setOnClickListener {
            if (validateInput()) {
                if(user == null){
                    auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val myToken = MyFirebaseMessagingService().getToken(requireContext())
                                val user = hashMapOf(
                                    "name" to name.text.toString(),
                                    "name_lower" to name.text.toString().lowercase(),
                                    "email" to email.text.toString(),
                                    "birthday" to birthday.text.toString(),
                                    "aboutMe" to aboutMe.text.toString(),
                                    "numberOfGamesPlayed" to 0,
                                    "overallRating" to 0,
                                    "uid" to auth.uid,
                                    "friends" to emptyList<String>(),
                                    "tokens" to arrayListOf(myToken)
                                )
                                auth.currentUser!!.sendEmailVerification()
                                    .addOnCompleteListener { task2 ->
                                        if (task2.isSuccessful) {
                                            Log.d(TAG, "Email sent.")
                                        }
                                    }
                                db.collection("users").add(user)
                                CoroutineScope(Dispatchers.Default).launch { uploadPhoto(sUri)}.invokeOnCompletion {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        findNavController().navigate(R.id.action_registrationFragment_to_findLobbyFragment)
                                    }
                                }
                            } else {
                                Toast.makeText(this.context, "Registration failed.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }else{
                    if(password.text.isNotEmpty()){
                        user.updatePassword(password.text.toString()).addOnCompleteListener{
                                task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "User password updated.")
                            }
                        }
                    }
                    val userData:HashMap<String, Any> = hashMapOf(
                        "name" to name.text.toString(),
                        "name_lower" to name.text.toString().lowercase(),
                        "email" to email.text.toString(),
                        "birthday" to birthday.text.toString(),
                        "aboutMe" to aboutMe.text.toString(),
                    )

                    val credential = EmailAuthProvider.getCredential(user.email!!, userDoc!!.documents[0]["password"].toString())
                    user.reauthenticate(credential).addOnCompleteListener {
                            user.updateEmail(email.text.toString())
                        }
                    db.collection("users").document(userDoc!!.documents[0].id)
                        .update(userData)
                    if(sUri!=""){
                        CoroutineScope(Dispatchers.Default).launch { uploadPhoto(sUri)}.invokeOnCompletion {
                            CoroutineScope(Dispatchers.Main).launch {
                                findNavController().navigate(R.id.action_registrationFragment_to_profileFragment)
                            }
                        }
                    }else{
                        findNavController().navigate(R.id.action_registrationFragment_to_profileFragment)
                    }
                }
            } else {
                printValidationError()
                view.findViewById<ScrollView>(R.id.scrollView).scrollTo(0,0)
            }
        }
    }

    private fun uploadPhoto(path:String){
        val currentUser = auth.currentUser
        val ref = storageRef.child("images/${currentUser!!.uid}")
        var bitmap = when {
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(requireContext().contentResolver, path.toUri())
            else -> {
                val source = ImageDecoder.createSource(requireContext().contentResolver, path.toUri())
                ImageDecoder.decodeBitmap(source)
            }
        }
        bitmap = cropToSquare(bitmap)
        val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        val bAOS = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 30,bAOS)
        Tasks.await(ref.putBytes(bAOS.toByteArray()).addOnCompleteListener{
            task ->
            if(task.isSuccessful){
                Log.d(TAG, "File uploaded successfully!")
            }else{
                Log.d(TAG, "File upload error!")
            }
        })
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - (height - width) else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
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
        if(auth.currentUser != null){
            if(password.text.isEmpty()){
                return true
            }
        }

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
        }
        return true
    }
}