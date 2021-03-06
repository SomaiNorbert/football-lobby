package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.view.marginLeft
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.google.api.Distribution
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.math.BigDecimal
import java.math.RoundingMode

class RatePlayersDialogFragment(private var lobbyUid: String) : DialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val punctuality = ArrayList<RadioGroup>()
    private val behavior = ArrayList<RadioGroup>()
    private val calmness = ArrayList<RadioGroup>()
    private val sportsmanship = ArrayList<RadioGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_rate_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var numberOfPlayersToRate = 0
        val names = ArrayList<String>()
        val uids = ArrayList<String>()
        val pComments = ArrayList<EditText>()

        val linearLayout = view.findViewById<LinearLayout>(R.id.linearLayout)
        linearLayout.gravity = LinearLayout.TEXT_ALIGNMENT_CENTER
        val params = linearLayout.layoutParams
        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        linearLayout.layoutParams = params
        val txt = TextView(requireContext())
        txt.text = "Rate players"
        txt.textSize = 30f
        linearLayout.addView(txt)

        val sendButton = Button(requireContext())
        sendButton.text = "Send"
        linearLayout.addView(sendButton)
        sendButton.setOnClickListener {
            var ok = true
            val rated = ArrayList<Int>()
            for(i in 0 until numberOfPlayersToRate){
                val pi = getIndexOfSelectedRadioButton(punctuality[i])
                val bi = getIndexOfSelectedRadioButton(behavior[i])
                val ci = getIndexOfSelectedRadioButton(calmness[i])
                val si = getIndexOfSelectedRadioButton(sportsmanship[i])
                val pci = pComments[i].text

                if(pi != -1 && bi != -1 && ci != -1 && si != -1){ // player is rated
                    if(!rated.contains(i))
                        rated.add(i)

                }else if(getIndexOfSelectedRadioButton(punctuality[i]) == -1 &&
                    getIndexOfSelectedRadioButton(behavior[i]) == -1 &&
                    getIndexOfSelectedRadioButton(calmness[i]) == -1 &&
                    getIndexOfSelectedRadioButton(sportsmanship[i]) == -1 &&
                    pComments[i].text.isEmpty()){ // player not rated

                    continue

                }else{ // player partially rated
                    Toast.makeText(requireContext(), "Finish rating player ${names[i]}!", Toast.LENGTH_SHORT).show()
                    ok = false
                }
            }
            if(ok){
                for(i in rated){
                    val pi = getIndexOfSelectedRadioButton(punctuality[i])
                    val bi = getIndexOfSelectedRadioButton(behavior[i])
                    val ci = getIndexOfSelectedRadioButton(calmness[i])
                    val si = getIndexOfSelectedRadioButton(sportsmanship[i])
                    val pci = pComments[i].text
                    db.collection("users").whereEqualTo("uid", uids[i]).get().addOnSuccessListener {
                        val ratings = ArrayList<HashMap<String, String>>()
                        if (it.documents[0]["ratings"] != null) {
                            ratings.addAll(it.documents[0]["ratings"] as ArrayList<HashMap<String, String>>)
                        }
                        val rating = HashMap<String, String>()
                        rating["punctuality"] = (pi + 1).toString()
                        rating["behavior"] = (bi + 1).toString()
                        rating["calmness"] = (ci + 1).toString()
                        rating["sportsmanship"] = (si + 1).toString()
                        rating["personalComment"] = pci.toString()
                        rating["fromUid"] = auth.currentUser!!.uid
                        rating["fromLobbyUid"] = lobbyUid
                        ratings.add(rating)
                        it.documents[0].reference.update("ratings", ratings).addOnSuccessListener {_->
                            db.collection("users").whereEqualTo("uid", uids[i]).get().addOnSuccessListener {updated->
                                updateOverallRatingForPlayer(updated.documents[0])
                            }
                        }
                    }
                }
                requireActivity().onBackPressed()
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            }
        }

        db.collection("oldLobbies").whereEqualTo("uid", lobbyUid).get().addOnSuccessListener { lobby->
            val doc = lobby.documents[0]
            numberOfPlayersToRate = doc["numberOfPlayersInLobby"].toString().toInt() - 1
            val playersUid = doc["players"] as ArrayList<String>
            playersUid.remove(auth.currentUser!!.uid)
            for(i in 0 until numberOfPlayersToRate){
                db.collection("users").whereEqualTo("uid", playersUid[i]).get().addOnSuccessListener { player->
                    val playerDoc = player.documents[0]
                    CoroutineScope(Dispatchers.Main).launch {

                        linearLayout.removeView(sendButton)

                        val separatorTxt = TextView(requireContext())
                        separatorTxt.textSize = 20f
                        separatorTxt.text = "   "
                        separatorTxt.visibility = View.INVISIBLE
                        linearLayout.addView(separatorTxt)

                        uids.add(playersUid[i])
                        val name = playerDoc["name"].toString()

                        val storageRef = Firebase.storage.reference
                        val img = CircleImageView(requireContext())
                        val params = LinearLayout.LayoutParams(100,100)
                        params.bottomMargin = 25
                        img.layoutParams = params
                        storageRef.child("images/${playersUid[i]}").downloadUrl.addOnSuccessListener {
                            try {
                                Glide.with(requireContext()).load(it).into(img)
                            } catch (e: Exception) {
                            }
                        }

                        val nameTxt = TextView(requireContext())
                        nameTxt.text = "    $name"
                        nameTxt.textSize = 25f
                        names.add(name)

                        val ll = LinearLayout(requireContext())
                        ll.orientation = LinearLayout.HORIZONTAL
                        ll.addView(img)
                        ll.addView(nameTxt)

                        linearLayout.addView(ll)

                        val punctualityTxt = TextView(requireContext())
                        punctualityTxt.text = "Punctuality"
                        linearLayout.addView(punctualityTxt)

                        val punctualityLinearLayout = createLinearLayoutWithRadioGroup(0)
                        linearLayout.addView(punctualityLinearLayout)

                        val behaviorTxt = TextView(requireContext())
                        behaviorTxt.text = "Behavior"
                        linearLayout.addView(behaviorTxt)

                        val behaviorLinearLayout = createLinearLayoutWithRadioGroup(1)
                        linearLayout.addView(behaviorLinearLayout)

                        val calmnessTxt = TextView(requireContext())
                        calmnessTxt.text = "Calmness"
                        linearLayout.addView(calmnessTxt)

                        val calmnessLinearLayout = createLinearLayoutWithRadioGroup(2)
                        linearLayout.addView(calmnessLinearLayout)

                        val sportsmanshipTxt = TextView(requireContext())
                        sportsmanshipTxt.text = "Sportsmanship"
                        linearLayout.addView(sportsmanshipTxt)

                        val sportsmanshipLinearLayout = createLinearLayoutWithRadioGroup(3)
                        linearLayout.addView(sportsmanshipLinearLayout)

                        val personalCommentEDT = EditText(requireContext())
                        personalCommentEDT.hint = "Personal comment"
                        linearLayout.addView(personalCommentEDT)
                        pComments.add(personalCommentEDT)

                        linearLayout.addView(sendButton)
                    }
                }
            }
        }
    }

    private fun updateOverallRatingForPlayer(playerDoc: DocumentSnapshot) {
        val ratings = ArrayList<HashMap<String, String>>()
        if(playerDoc["ratings"] != null){
            ratings.addAll(playerDoc["ratings"] as ArrayList<HashMap<String, String>>)
        }
        var punc = 0.0
        var beha = 0.0
        var calm = 0.0
        var spor = 0.0
        var nr = 0.0
        for(rating in ratings){
            punc += rating["punctuality"].toString().toDouble()
            beha += rating["behavior"].toString().toDouble()
            calm += rating["calmness"].toString().toDouble()
            spor += rating["sportsmanship"].toString().toDouble()
            nr ++
        }
        if(punc == 0.0){
            nr = 1.0
        }
        punc /= nr
        beha /= nr
        calm /= nr
        spor /= nr
        Log.d(TAG, BigDecimal(((punc+beha+calm+spor)/4)).setScale(2, RoundingMode.HALF_EVEN).toDouble().toString())
        playerDoc.reference.update("overallRating", BigDecimal(((punc+beha+calm+spor)/4)).setScale(2, RoundingMode.HALF_EVEN).toDouble())
    }

    private fun getIndexOfSelectedRadioButton(radioGroup: RadioGroup): Int {
        var i = 0
        for(radioButton in radioGroup.children){
            if((radioButton as RadioButton).isChecked){
                return i
            }
            i ++
        }
        return -1
    }

    private fun createLinearLayoutWithRadioGroup(index: Int): LinearLayout {
        val linearLayout = LinearLayout(requireContext())
        linearLayout.gravity = LinearLayout.TEXT_ALIGNMENT_CENTER
        val params = LinearLayout.LayoutParams(0,0)
        val params2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
        params2.leftMargin = 50
        params2.rightMargin = 50
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
        params.weight = 1.0f
        linearLayout.layoutParams = params2
        val badTxt = TextView(requireContext())
        badTxt.text = "Bad"
        //badTxt.layoutParams = params
        val goodTxt = TextView(requireContext())
        goodTxt.text = "Good"
        //goodTxt.layoutParams = params
        val txt = TextView(requireContext())
        txt.text = " "
        val txt2 = TextView(requireContext())
        txt2.text = " "
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.addView(txt)
        linearLayout.addView(badTxt)
        val radioGroup = RadioGroup(requireContext())
        radioGroup.orientation = RadioGroup.HORIZONTAL
        radioGroup.layoutParams = params
        for(i in 0 until 5){
            val radioButton = RadioButton(requireContext())
            if(i != 4)
                radioButton.layoutParams = params
            radioGroup.addView(radioButton)
        }
        when (index) {
            0 -> {punctuality.add(radioGroup)}
            1 -> {behavior.add(radioGroup)}
            2 -> {calmness.add(radioGroup)}
            3 -> {sportsmanship.add(radioGroup)}
            else -> {}
        }
        linearLayout.addView(radioGroup)
        linearLayout.addView(goodTxt)
        linearLayout.addView(txt2)
        return linearLayout
    }

}