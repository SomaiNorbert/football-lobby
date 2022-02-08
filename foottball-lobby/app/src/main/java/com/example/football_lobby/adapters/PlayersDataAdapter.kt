package com.example.football_lobby.adapters

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.models.Lobby
import com.example.football_lobby.models.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayersDataAdapter(
    private var list: ArrayList<Player>,
    private var listener: OnItemClickedListener,
    private var creatorUid: String
) : RecyclerView.Adapter<PlayersDataAdapter.RecyclerViewHolder>() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersDataAdapter.RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.players_in_lobby_item_layout, parent, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val currentItem = list[position]
        holder.playerNameItemTxt.text = currentItem.name
        holder.birthdayItemTxt.text = currentItem.birthday
        if(currentItem.rating == 0.0){
            holder.ratingItemTxt.text = "-"
        }else{
            holder.ratingItemTxt.text = currentItem.rating.toString()
        }

        val storageRef = Firebase.storage.reference
        storageRef.child("images/${currentItem.uid}").downloadUrl.addOnSuccessListener {
            Glide.with(holder.itemView.context).load(it).into(holder.profileImg)
        }
        holder.kickFromLobbyButton.visibility = View.GONE
        if(auth.currentUser!!.uid == currentItem.uid){
            holder.chatButton.visibility = View.GONE
        }else{
            holder.chatButton.visibility = View.VISIBLE
            if(auth.currentUser!!.uid == creatorUid)
                holder.kickFromLobbyButton.visibility = View.VISIBLE
        }

        holder.chatButton.setOnClickListener {
            Log.d(TAG, "Chat Button Clicked")
        }

        holder.kickFromLobbyButton.setOnClickListener {
            Log.d(TAG, "Kick Button Clicked")
        }
    }

    override fun getItemCount() = list.size

    fun setData(list: ArrayList<Player>) {
        this.list = list
    }

    fun addPlayer(player: Player){
        list.add(player)
        notifyItemInserted(itemCount -1)
    }

    fun removePlayerByUid(uid: String){
        var player = Player()
        for(i in list){
            if(i.uid == uid){
                player = i
                break
            }
        }
        val pos = list.indexOf(player)
        list.remove(player)
        notifyItemRemoved(pos)
    }

    inner class RecyclerViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView), View.OnClickListener{

        val playerNameItemTxt: TextView = itemView.findViewById(R.id.playerNameItemTxt)
        val birthdayItemTxt: TextView = itemView.findViewById(R.id.birthdayItemTxt)
        val ratingItemTxt: TextView = itemView.findViewById(R.id.ratingItemTxt)
        val profileImg: ImageView = itemView.findViewById(R.id.profileImg)
        val chatButton: Button = itemView.findViewById(R.id.chatButton)
        val kickFromLobbyButton: Button = itemView.findViewById(R.id.kickFromLobbyButton)

        init {
            itemView.setOnClickListener(this)
            auth = Firebase.auth
            db = Firebase.firestore
        }

        override fun onClick(p0: View?) {
            val position:Int = adapterPosition;
            if(position != RecyclerView.NO_POSITION){
                val uid:String = list[position].uid
                listener.onItemClick(uid)
            }
        }
    }

    interface OnItemClickedListener{
        fun onItemClick(uid: String)
    }
}