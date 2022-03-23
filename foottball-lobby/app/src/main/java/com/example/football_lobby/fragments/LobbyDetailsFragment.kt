package com.example.football_lobby.fragments

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.adapters.MessagesDataAdapter
import com.example.football_lobby.adapters.PlayersDataAdapter
import com.example.football_lobby.models.Message
import com.example.football_lobby.models.Player
import com.example.football_lobby.services.MyFirebaseMessagingService
import com.example.football_lobby.services.Services
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.api.Distribution
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class LobbyDetailsFragment : Fragment(), PlayersDataAdapter.OnItemClickedListener{

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var gameNameTxt: TextView
    private lateinit var locationDetailTxt: TextView
    private lateinit var dateAndTimeTxt: TextView
    private lateinit var numberOfPlayersInLobbyTxt: TextView
    private lateinit var maximumNumberOfPlayersInLobbyTxt: TextView
    private lateinit var detailRG: RadioGroup
    private lateinit var playersInLobbyRV: RecyclerView
    private lateinit var adapterPlayers: PlayersDataAdapter
    private lateinit var adapterMessages: MessagesDataAdapter
    private lateinit var publicRB: RadioButton
    private lateinit var privateRB: RadioButton
    private lateinit var joinButton: Button
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var chatRV: RecyclerView
    private lateinit var chatLL: LinearLayout
    private lateinit var sendButton: ImageButton
    private lateinit var messageEDT: EditText
    private lateinit var fab: FloatingActionButton
    private lateinit var lobbyFullTxt: TextView

    private lateinit var lobbyData: DocumentSnapshot

    private lateinit var currentUser: FirebaseUser
    private var creatorUid = ""
    private var documentID = ""
    private var playersList = ArrayList<String>()
    private var currentLobbyUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        auth = Firebase.auth
        currentUser = auth.currentUser!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby_details, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLobbyUid = arguments?.get("uid").toString()
        val notId = arguments?.get("notificationID").toString()
        if(notId.isNotEmpty() && notId != "null"){
            Services.removeNotificationFromPlayer(currentUser.uid, notId)
        }

        gameNameTxt = view.findViewById(R.id.gameNameTxt)
        locationDetailTxt = view.findViewById(R.id.locationDetailTxt)
        dateAndTimeTxt = view.findViewById(R.id.dateAndTimeTxt)
        numberOfPlayersInLobbyTxt = view.findViewById(R.id.numberOfPlayersInLobbyTxt)
        maximumNumberOfPlayersInLobbyTxt = view.findViewById(R.id.maximumNumberOfPlayersInLobbyTxt)
        detailRG = view.findViewById(R.id.detailRG)
        playersInLobbyRV = view.findViewById(R.id.playersInLobbyRV)
        publicRB = view.findViewById(R.id.publicDetailRB)
        privateRB = view.findViewById(R.id.privateDetailRB)
        joinButton = view.findViewById(R.id.joinLobbyButton)
        toolbar = requireActivity().findViewById(R.id.topAppToolbar)
        tabLayout = view.findViewById(R.id.tabLayout)
        chatRV = view.findViewById(R.id.chatRV)
        chatLL = view.findViewById(R.id.chatLL)
        sendButton = view.findViewById(R.id.sendButton)
        messageEDT = view.findViewById(R.id.messageEDT)
        fab = view.findViewById(R.id.floatingActionButton)
        lobbyFullTxt = view.findViewById(R.id.lobbyFullTxt)
        setupMessagesRecyclerView()
        var collection = "lobbies"
        if (currentLobbyUid != "") {

            db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get()
                .addOnSuccessListener { result ->
                    if (result.documents.size == 0) {
                        isOld()
                        collection = "oldLobbies"
                    }
                    Log.d(TAG, collection)
                    loadMessagesIntoDataAdapter()
                    db.collection(collection).whereEqualTo("uid", currentLobbyUid)
                        .addSnapshotListener { value, _ ->
                            if(value == null)return@addSnapshotListener
                            if(value.documents.size != 0){
                                lobbyData = value.documents[0]
                                setupPlayersRecyclerView()
                                creatorUid = lobbyData["creatorUid"] as String
                                gameNameTxt.text = lobbyData["name"] as String
                                locationDetailTxt.text = lobbyData["location"] as String
                                val dt =
                                    lobbyData["date"] as String + "  " + lobbyData["time"] as String
                                dateAndTimeTxt.text = dt
                                playersList = ArrayList()
                                val lobbyData = value.documents[0]
                                documentID = lobbyData.id
                                numberOfPlayersInLobbyTxt.text =
                                    lobbyData["numberOfPlayersInLobby"].toString()
                                maximumNumberOfPlayersInLobbyTxt.text =
                                    lobbyData["maximumNumberOfPlayers"].toString()
                                if (lobbyData["public"] as Boolean) {
                                    publicRB.isChecked = true
                                } else {
                                    privateRB.isChecked = true
                                }
                                if (lobbyData["requests"] != null && lobbyData["creatorUid"].toString() == auth.currentUser!!.uid) {
                                    playersList.addAll(lobbyData["requests"] as ArrayList<String>)
                                }
                                playersList.addAll(lobbyData["players"] as ArrayList<String>)
                                CoroutineScope(Dispatchers.Default).launch {
                                    loadPlayersInLobbyIntoDataAdapter(
                                        playersList
                                    )
                                }
                                if (currentUser.uid == creatorUid) {
                                    if (playersList.contains(currentUser.uid)) {
                                        detailRG.visibility = View.VISIBLE
                                        joinButton.visibility = View.INVISIBLE
                                    } else {
                                        detailRG.visibility = View.INVISIBLE
                                        joinButton.visibility = View.VISIBLE
                                    }
                                } else {
                                    detailRG.visibility = View.INVISIBLE
                                    db.collection(collection).whereEqualTo("uid", currentLobbyUid)
                                        .get()
                                        .addOnSuccessListener {
                                            if (it.documents[0]["requests"] != null &&
                                                ((it.documents[0]["requests"] as ArrayList<String>).contains(
                                                    currentUser.uid
                                                )) ||
                                                (it.documents[0]["players"] as ArrayList<String>).contains(
                                                    currentUser.uid
                                                )
                                            ) {
                                                joinButton.visibility = View.GONE
                                            } else {
                                                joinButton.visibility = View.VISIBLE
                                            }
                                        }
                                }
                                lobbyFullTxt.visibility = View.INVISIBLE
                                fab.visibility = View.VISIBLE
                                if (maximumNumberOfPlayersInLobbyTxt.text.toString()
                                        .toInt() == numberOfPlayersInLobbyTxt.text.toString()
                                        .toInt()
                                ) {
                                    if (joinButton.visibility == View.VISIBLE) {
                                        joinButton.visibility = View.INVISIBLE
                                        lobbyFullTxt.visibility = View.VISIBLE
                                    }
                                    fab.visibility = View.INVISIBLE
                                }
                                setUpMenu()
                            }

                        }
                }

            publicRB.setOnCheckedChangeListener { _, isChecked ->
                db.collection("lobbies").document(documentID).update("public", isChecked)
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (tab != null)
                        when (tab.position) {
                            0 -> {
                                playersInLobbyRV.visibility = View.VISIBLE
                                fab.visibility = View.VISIBLE
                                chatRV.visibility = View.GONE
                                chatLL.visibility = View.GONE
                            }
                            1 -> {
                                if (isCurrentUserInLobby()) {
                                    playersInLobbyRV.visibility = View.INVISIBLE
                                    fab.visibility = View.INVISIBLE
                                    chatRV.visibility = View.VISIBLE
                                    chatLL.visibility = View.VISIBLE
                                    chatRV.scrollToPosition(adapterMessages.itemCount - 1)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Please join first!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    tabLayout.getTabAt(0)?.select()
                                }

                            }
                        }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
            })

            sendButton.setOnClickListener {
                if (messageEDT.text.isNotEmpty())
                    db.collection("users").whereEqualTo("uid", currentUser.uid).get()
                        .addOnSuccessListener {
                            val calendar = Calendar.getInstance()
                            val min = if(calendar.get(Calendar.MINUTE).toString().length == 1){"0" + calendar.get(Calendar.MINUTE)}else{calendar.get(Calendar.MINUTE)}
                            val mes = Message(
                                currentUser.uid,
                                it.documents[0]["name"].toString(),
                                messageEDT.text.toString(),
                                "" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + (calendar.get(Calendar.MONTH)+1) +
                                        "/" + calendar.get(Calendar.YEAR) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" +
                                        min
                            )
                            messageEDT.setText("")
                            var doc: DocumentSnapshot
                            db.collection("chat").whereEqualTo("lobbyUid", lobbyData["uid"])
                                .get()
                                .addOnSuccessListener { result ->
                                    doc = result.documents[0]
                                    if (doc["messages"] != null) {
                                        val messages = doc["messages"] as ArrayList<Message>
                                        messages.add(mes)
                                        db.collection("chat").document(doc.id)
                                            .update("messages", messages.toList())
                                    } else {
                                        db.collection("chat").document(doc.id)
                                            .update("messages", mes)
                                    }
                                }
                        }
            }

            joinButton.setOnClickListener {
                if (currentUser.uid == creatorUid) {
                    detailRG.visibility = View.VISIBLE
                    joinButton.visibility = View.INVISIBLE
                } else {
                    joinButton.visibility = View.GONE
                }
                db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get()
                    .addOnSuccessListener {
                        val requests = ArrayList<String>()
                        if (it.documents[0]["requests"] != null) {
                            requests.addAll(it.documents[0]["requests"] as ArrayList<String>)
                        }
                        requests.add(auth.currentUser!!.uid)
                        it.documents[0].reference.update("requests", requests)
                            .addOnSuccessListener { _ ->
                                db.collection("users")
                                    .whereEqualTo(
                                        "uid",
                                        it.documents[0]["creatorUid"].toString()
                                    )
                                    .get().addOnSuccessListener { owner ->
                                        val tokens = ArrayList<String>()
                                        if (owner.documents[0]["tokens"] != null) {
                                            tokens.addAll(owner.documents[0]["tokens"] as ArrayList<String>)
                                        }
                                        db.collection("users")
                                            .whereEqualTo("uid", auth.currentUser!!.uid)
                                            .get()
                                            .addOnSuccessListener { me ->
                                                MyFirebaseMessagingService().sendNotificationToOwnerOnJoinRequest(
                                                    arrayListOf(owner.documents[0]["uid"].toString()),tokens,
                                                    me.documents[0]["name"].toString(),
                                                    it.documents[0]["name"].toString(),
                                                    currentLobbyUid
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Join Request sent!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                    }
                            }
                    }
            }

            fab.setOnClickListener {
                if (isCurrentUserInLobby()) {
                    val names = ArrayList<String>()
                    val uids = ArrayList<String>()
                    CoroutineScope(Dispatchers.Default).launch {
                        val user =
                            Tasks.await(
                                db.collection("users").whereEqualTo("uid", currentUser.uid)
                                    .get()
                            )
                        for (friend in user.documents[0]["friends"] as ArrayList<String>) {
                            val tmp = Tasks.await(
                                db.collection("users").whereEqualTo("uid", friend).get()
                            )
                            names.add(tmp.documents[0]["name"].toString())
                            uids.add(friend)
                        }
                    }.invokeOnCompletion {
                        CoroutineScope(Dispatchers.Main).launch {
                            val list2 = ArrayList<String>()
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Add friends to lobby?")
                                .setNeutralButton("Cancel") { _, _ -> }
                                .setPositiveButton("Add") { _, _ ->
                                    invitePlayers(list2)
                                }
                                .setMultiChoiceItems(
                                    names.toTypedArray(),
                                    BooleanArray(names.size)
                                ) { _, which, checked ->
                                    if (checked) {
                                        list2.add(uids[which])
                                    } else {
                                        list2.remove(uids[which])
                                    }
                                }
                                .show()
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please join first!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    private fun isOld(){
        db.collection("oldLobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
            val doc = it.documents[0]
            if(doc["creatorUid"].toString() == auth.currentUser!!.uid && it.documents[0]["ownerResponded"] == null){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Did the game happen?")
                    .setMessage("We noticed that your game is over. Did it happen?")
                    .setCancelable(false)
                    .setNegativeButton("No") { _, _ ->
                        doc.reference.delete()
                        requireActivity().onBackPressed()
                    }
                    .setPositiveButton("Yes") { _, _ ->
                        doc.reference.update("ownerResponded", true).addOnSuccessListener {_->
                            db.collection("oldLobbies").document(it.documents[0].id).get().addOnSuccessListener {res->
                                ownerResponded(res)
                            }
                            increaseNumberOfGamesPlayedForPlayers(doc["players"] as ArrayList<String>)
                        }
                        CoroutineScope(Dispatchers.Default).launch {
                            for(playerUid in it.documents[0]["players"] as ArrayList<String>){
                                if(playerUid == auth.currentUser!!.uid)
                                    continue
                                db.collection("users").whereEqualTo("uid", playerUid).get()
                                    .addOnSuccessListener { player->
                                        val tokens = ArrayList<String>()
                                        if(player.documents[0]["tokens"] != null){
                                            tokens.addAll(player.documents[0]["tokens"] as ArrayList<String>)
                                        }
                                        MyFirebaseMessagingService().sendNotificationToPlayersOnLobbyDone(arrayListOf(playerUid),
                                            tokens, it.documents[0]["name"].toString(), it.documents[0]["uid"].toString())
                                }
                            }
                        }
                    }
                    .show()
            }else{
                ownerResponded(doc)
            }
        }
    }

    private fun increaseNumberOfGamesPlayedForPlayers(playerUids: java.util.ArrayList<String>) {
        for(playerUid in playerUids){
            db.collection("users").whereEqualTo("uid", playerUid).get().addOnSuccessListener {
                it.documents[0].reference.update("numberOfGamesPlayed", it.documents[0]["numberOfGamesPlayed"].toString().toInt() + 1)
            }
        }
    }

    private fun ownerResponded(doc: DocumentSnapshot){
        if(doc["ownerResponded"] != null){
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rate your teammates!")
                .setMessage("We noticed that your game is over.Would you like to rate your teammates")
                .setCancelable(false)
                .setNegativeButton("No") { _, _ ->
                    requireActivity().onBackPressed()
                }
                .setPositiveButton("Yes") { _, _ ->
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction
                        .add(android.R.id.content, RatePlayersDialogFragment(doc["uid"].toString()))
                        .addToBackStack("RatePlayers")
                        .commit()

                }
                .show()
            val responded = ArrayList<String>()
            if(doc["playersResponded"] != null){
                responded.addAll(doc["playersResponded"] as ArrayList<String>)
            }
            //responded.add(auth.currentUser!!.uid) TODO uncomment this
            doc.reference.update("playersResponded", responded)
        }
    }

    private fun invitePlayers(listOfPlayers: ArrayList<String>) {
        db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
            val players = ArrayList<String>()
            if(it.documents[0]["players"] != null)
                players.addAll(it.documents[0]["players"] as ArrayList<String>)
            for (playerUid in listOfPlayers) {
                if (!players.contains(playerUid)) {
                    addPlayerToLobby(playerUid)
                }
            }
        }
    }

    private fun addPlayerToLobby(playerUid: String) {
        db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {lobby->
            val players = ArrayList<String>()
            if(lobby.documents[0]["players"] != null)
                players.addAll(lobby.documents[0]["players"] as ArrayList<String>)
            players.add(playerUid)
            var npil = numberOfPlayersInLobbyTxt.text.toString().toInt()
            npil += 1
            numberOfPlayersInLobbyTxt.text = npil.toString()
            val update = hashMapOf(
                "numberOfPlayersInLobby" to numberOfPlayersInLobbyTxt.text.toString().toInt(),
                "players" to players.toList()
            )
            db.collection("lobbies").document(documentID).update(update)
            db.collection("users").whereEqualTo("uid", playerUid).get().addOnSuccessListener {
                val playerData = it.documents[0]
                adapterPlayers.addPlayer(Player(playerData["name"].toString(), playerData["birthday"].toString(),
                    playerData["overallRating"].toString().toDouble(), playerUid))
            }
        }

    }

    private fun removePlayerFromLobby(playerUid: String){
        val players = ArrayList<String>()
        db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
            if(it.documents[0]["players"] != null){
                players.addAll(it.documents[0]["players"] as ArrayList<String>)
            }
            players.remove(playerUid)
            var npil = numberOfPlayersInLobbyTxt.text.toString().toInt()
            npil -= 1
            numberOfPlayersInLobbyTxt.text = npil.toString()
            val update = hashMapOf(
                "numberOfPlayersInLobby" to numberOfPlayersInLobbyTxt.text.toString().toInt(),
                "players" to players.toList()
            )
            db.collection("lobbies").document(documentID).update(update)
            adapterPlayers.removePlayerByUid(playerUid)
        }
    }

    private fun isCurrentUserInLobby() : Boolean {
        if(playersList.contains(currentUser.uid))
            return true
        return false
    }

    private fun setUpMenu(){
        toolbar.menu.setGroupVisible(R.id.inLobbyGroup, isCurrentUserInLobby())
    }

    fun leaveLobby() {
        if(currentUser.uid == creatorUid){
            detailRG.visibility = View.INVISIBLE
        }
        joinButton.visibility = View.VISIBLE
        removePlayerFromLobby(currentUser.uid)
        setUpMenu()
    }

    private fun loadMessagesIntoDataAdapter(){
        db.collection("chat").whereEqualTo("lobbyUid", currentLobbyUid).addSnapshotListener { value, _ ->
            if(value == null) return@addSnapshotListener
            val mes = value.documents[0]["messages"] as ArrayList<HashMap<String, String>>
            val messages = ArrayList<Message>()
            for(message in mes){
                messages.add(Message(message["senderUid"].toString(), message["senderName"].toString(), message["message"].toString(),
                    message["time"].toString()))
            }
            if(adapterMessages.itemCount == 0){
                adapterMessages.setData(messages)
            } else{
                adapterMessages.addItem(messages.last())
            }
            chatRV.scrollToPosition(adapterMessages.itemCount-1)
        }
    }

    private fun loadPlayersInLobbyIntoDataAdapter(uidList: List<String>) {
        val players = ArrayList<Player>()
        for(uid in uidList) {
            val result = Tasks.await(db.collection("users").whereEqualTo("uid", uid).get())
            val p = result.documents[0]
            players.add(Player(p["name"].toString(), p["birthday"].toString(),
                p["overallRating"].toString().toDouble(), uid))
        }
        CoroutineScope(Dispatchers.Main).launch { adapterPlayers.setData(players) }
    }

    private fun setupMessagesRecyclerView(){
        adapterMessages = MessagesDataAdapter(ArrayList(), requireContext())
        chatRV.adapter = adapterMessages
        chatRV.layoutManager = LinearLayoutManager(context)
        chatRV.setHasFixedSize(true)
    }

    private fun setupPlayersRecyclerView(){
        adapterPlayers = PlayersDataAdapter(ArrayList(), this, lobbyData["creatorUid"].toString(), lobbyData["uid"].toString())
        playersInLobbyRV.adapter = adapterPlayers
        playersInLobbyRV.layoutManager = LinearLayoutManager(context)
        playersInLobbyRV.setHasFixedSize(true)
    }

    override fun onItemClick(uid: String) {
        val bundle = Bundle()
        bundle.putString("playerUid", uid)
        findNavController().navigate(R.id.action_lobbyDetailsFragment_to_profileFragment, bundle)
    }

    override fun onKickButtonClicked(uid: String) {
        removePlayerFromLobby(uid)
    }

    override fun onChatButtonClicked(uid: String) {
        val bundle = Bundle()
        bundle.putString("uid", uid)
        findNavController().navigate(R.id.action_lobbyDetailsFragment_to_privateChatFragment, bundle)
    }

    override fun onInviteButtonClicked(uid: String) {}

    override fun onAcceptButtonClicked(uid: String, pos: Int) {
        db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
            removePlayerFromRequestsByUid(it, uid)
            adapterPlayers.removePlayerByUid(uid)
            addPlayerToLobby(uid)
            val tokens = ArrayList<String>()
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { he->
                if(he.documents[0]["tokens"] != null){
                    tokens.addAll(he.documents[0]["tokens"] as ArrayList<String>)
                }
                db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid).get().addOnSuccessListener {me->
                    MyFirebaseMessagingService().sendNotificationToPlayerOnJoinRequestAccepted(arrayListOf(uid), tokens,
                        me.documents[0]["name"].toString(), it.documents[0]["name"].toString(), currentLobbyUid)
                }

            }
        }
    }

    override fun onDeclineButtonClicked(uid: String) {
        db.collection("lobbies").whereEqualTo("uid", currentLobbyUid).get().addOnSuccessListener {
            removePlayerFromRequestsByUid(it, uid)
            val tokens = ArrayList<String>()
            db.collection("users").whereEqualTo("uid", uid).get().addOnSuccessListener { he->
                if(he.documents[0]["tokens"] != null){
                    tokens.addAll(he.documents[0]["tokens"] as ArrayList<String>)
                }
                db.collection("users").whereEqualTo("uid", auth.currentUser!!.uid).get().addOnSuccessListener {me->
                    MyFirebaseMessagingService().sendNotificationToPlayerOnJoinRequestDenied(arrayListOf(uid), tokens,
                        me.documents[0]["name"].toString(), it.documents[0]["name"].toString())
                }
            }
        }
    }

    private fun removePlayerFromRequestsByUid(doc: QuerySnapshot, uid: String){
        val requests = doc.documents[0]["requests"] as ArrayList<String>
        requests.remove(uid)
        doc.documents[0].reference.update("requests", requests)
    }
}