package com.example.football_lobby.adapters

import android.content.ContentValues.TAG
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
import com.example.football_lobby.models.Player

class PlayersDataAdapter(
    private var list: ArrayList<Player>,
    private var listener: OnItemClickedListener
) : RecyclerView.Adapter<PlayersDataAdapter.RecyclerViewHolder>() {

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
        Log.d(TAG, currentItem.profilePic)
        Glide.with(holder.itemView.context).load(currentItem.profilePic).into(holder.profileImg)
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

    inner class RecyclerViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView), View.OnClickListener{

        val playerNameItemTxt: TextView = itemView.findViewById(R.id.playerNameItemTxt)
        val birthdayItemTxt: TextView = itemView.findViewById(R.id.birthdayItemTxt)
        val ratingItemTxt: TextView = itemView.findViewById(R.id.ratingItemTxt)
        val profileImg: ImageView = itemView.findViewById(R.id.profileImg)
        val chatButton: Button = itemView.findViewById(R.id.chatButton)
        val kickFromLobbyButton: Button = itemView.findViewById(R.id.kickFromLobbyButton)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position:Int =  adapterPosition;
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClick(position)
            }
        }
    }

    interface OnItemClickedListener{
        fun onItemClick(position:Int)
    }
}