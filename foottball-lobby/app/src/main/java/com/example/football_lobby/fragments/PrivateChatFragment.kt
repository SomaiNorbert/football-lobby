package com.example.football_lobby.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.adapters.MessagesDataAdapter
import com.example.football_lobby.models.Message
import com.example.football_lobby.models.Player
import com.example.football_lobby.services.MyFirebaseMessagingService
import com.example.football_lobby.services.Services
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PrivateChatFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth:FirebaseAuth
    private lateinit var user: FirebaseUser

    private lateinit var profileImgPrivate: CircleImageView
    private lateinit var toNameTxt: TextView
    private lateinit var privateChatRV: RecyclerView
    private lateinit var messagePEDT: EditText
    private lateinit var sendPButton: ImageButton

    private lateinit var toUid: String
    private var toPlayerTokens = ArrayList<String>()
    private var doc: DocumentSnapshot? = null

    private lateinit var adapterMessages: MessagesDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
        user = auth.currentUser!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toUid = arguments?.get("uid").toString()

        val notId = arguments?.get("notificationID").toString()
        if(notId.isNotEmpty() && notId != "null"){
            Services.removeNotificationFromPlayer(user.uid, notId)
        }

        profileImgPrivate = view.findViewById(R.id.profileImgPrivate)
        toNameTxt = view.findViewById(R.id.nameTxt)
        privateChatRV = view.findViewById(R.id.privateChatRV)
        messagePEDT = view.findViewById(R.id.messagePEDT)
        sendPButton = view.findViewById(R.id.sendPButton)

        setupRecyclerView()

        db.collection("users").whereEqualTo("uid", toUid).get().addOnSuccessListener { toUser ->
            toNameTxt.text = toUser.documents[0]["name"].toString()
            val storageRef = Firebase.storage.reference
            storageRef.child("images/${toUid}").downloadUrl.addOnSuccessListener {
                try {
                    Glide.with(this).load(it).into(profileImgPrivate)
                } catch (e: Exception) {
                }
            }
            toPlayerTokens = toUser.documents[0]["tokens"] as ArrayList<String>

        }

        CoroutineScope(Dispatchers.Default).launch { loadMessages(toUid) }

        sendPButton.setOnClickListener {
            if(messagePEDT.text.isNotEmpty())
            db.collection("users").whereEqualTo("uid", user.uid).get()
                .addOnSuccessListener { resMe ->
                    val calendar = Calendar.getInstance()
                    val min = if(calendar.get(Calendar.MINUTE).toString().length == 1){"0" + calendar.get(Calendar.MINUTE)}else{calendar.get(Calendar.MINUTE)}
                    val mes = Message(
                        user.uid,
                        resMe.documents[0]["name"].toString(),
                        messagePEDT.text.toString(),
                        "" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + (calendar.get(Calendar.MONTH)+1) +
                                "/" + calendar.get(Calendar.YEAR) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" +
                                min
                    )
                    val fromName = resMe.documents[0]["name"].toString()
                    val fromUid = resMe.documents[0]["uid"].toString()
                    messagePEDT.setText("")
                    CoroutineScope(Dispatchers.Default).launch {
                        doc = getDoc()
                    }.invokeOnCompletion {
                        if (doc!!.get("messages") == null) {
                            db.collection("privateChat").document(doc!!.id).update("messages", mes)
                        } else {
                            val messages = doc!!["messages"] as ArrayList<Message>
                            messages.add(mes)
                            db.collection("privateChat").document(doc!!.id)
                                .update("messages", messages.toList())
                            MyFirebaseMessagingService().sendNotificationToPlayerOnMessageReceived(arrayListOf(toUid), toPlayerTokens, fromName, fromUid)
                        }
                    }
                }
        }

        val onClickListener = View.OnClickListener() {
            val bundle = Bundle()
            bundle.putString("playerUid", toUid)
            findNavController().navigate(R.id.action_global_profileFragment, bundle)
        }
        toNameTxt.setOnClickListener(onClickListener)
        profileImgPrivate.setOnClickListener(onClickListener)
    }

    private fun getDoc():DocumentSnapshot? {
        val a = Tasks.await(
            db.collection("privateChat")
                .whereEqualTo("player1UID", toUid)
                .whereEqualTo("player2UID", user.uid)
                .get()
        )
        val b = Tasks.await(
            db.collection("privateChat")
                .whereEqualTo("player1UID", user.uid)
                .whereEqualTo("player2UID", toUid)
                .get()
        )
        return if (a.documents.size == 0 && b.documents.size == 0) {
            null
        } else {
            if (a.documents.size > b.documents.size) {
                a.documents[0]
            } else {
                b.documents[0]
            }
        }
    }

    private fun loadMessages(toUID: String) {
        doc = getDoc()
        if (doc == null) {// NO MESSAGES BETWEEN THE TWO USERS YET
            val privateChat = hashMapOf(
                "player1UID" to toUID,
                "player2UID" to user.uid,
                "messages" to emptyList<HashMap<String, String>>()
            )
            db.collection("privateChat").add(privateChat).addOnSuccessListener {
                CoroutineScope(Dispatchers.Default).launch{doc = Tasks.await(it.get()); loadMessages(toUID)}
            }
            CoroutineScope(Dispatchers.Main).launch {adapterMessages.setData(ArrayList())}
        } else {
            doc!!.reference.addSnapshotListener { value, _ ->
                val messages = ArrayList<Message>()
                for (message in value!!["messages"] as ArrayList<HashMap<String, String>>) {
                    messages.add(Message(message["senderUid"].toString(), message["senderName"].toString(), message["message"].toString(),
                                            message["time"].toString()))
                }
                if(adapterMessages.itemCount == 0){
                    CoroutineScope(Dispatchers.Main).launch {
                        adapterMessages.setData(messages)
                        privateChatRV.scrollToPosition(adapterMessages.itemCount - 1)
                    }
                } else{
                    CoroutineScope(Dispatchers.Main).launch {
                        adapterMessages.addItem(messages.last())
                        privateChatRV.scrollToPosition(adapterMessages.itemCount - 1)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapterMessages = MessagesDataAdapter(ArrayList(), requireContext())
        privateChatRV.adapter = adapterMessages
        privateChatRV.layoutManager = LinearLayoutManager(requireContext())
        privateChatRV.setHasFixedSize(true)
    }
}