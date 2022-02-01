package com.example.football_lobby.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.models.Lobby

class DataAdapter(
    private var list: List<Lobby>,
    private var listener: OnItemClickedListener
    ) : RecyclerView.Adapter<DataAdapter.RecyclerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.lobby_item_layout, parent, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val currentItem = list[position]
        holder.lobbyName.text = currentItem.lobbyName
        holder.location.text = currentItem.location
        holder.date.text = currentItem.date
        holder.time.text = currentItem.time
        holder.creator.text = currentItem.creator
        holder.numberOfPlayersInLobby.text = currentItem.numberOfPlayersInLobby.toString()
        holder.maximumNumberOfPlayers.text = (currentItem.maximumNumberOfPlayers*2).toString()
    }

    override fun getItemCount() = list.size

    inner class RecyclerViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView), View.OnClickListener{

        val lobbyName: TextView = itemView.findViewById(R.id.lobbyNameTxt)
        val location: TextView = itemView.findViewById(R.id.locationTxt)
        val date:TextView = itemView.findViewById(R.id.dateTxt)
        val time: TextView = itemView.findViewById(R.id.timeTxt)
        val creator: TextView = itemView.findViewById(R.id.createdByTxt)
        val numberOfPlayersInLobby: TextView = itemView.findViewById(R.id.playersInLobbyTxt)
        val maximumNumberOfPlayers: TextView = itemView.findViewById(R.id.maximumNumberOfPlayersTxt)

        init{
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