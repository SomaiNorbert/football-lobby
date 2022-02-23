package com.example.football_lobby.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.models.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PlayersDataAdapter(
    private var list: ArrayList<Player>,
    private var listener: OnItemClickedListener,
    private var creatorUid: String,
) : RecyclerView.Adapter<PlayersDataAdapter.RecyclerViewHolder>(), Filterable {

    private lateinit var auth: FirebaseAuth
    private val listFull = ArrayList<Player>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersDataAdapter.RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.players_item_layout, parent, false)
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
            listener.onChatButtonClicked(currentItem.uid)
        }

        holder.kickFromLobbyButton.setOnClickListener {
            listener.onKickButtonClicked(currentItem.uid)
        }
    }

    override fun getItemCount() = list.size

    fun setData(list: ArrayList<Player>) {
        this.list = list
        listFull.clear()
        listFull.addAll(list)
        notifyDataSetChanged()
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
            if(listFull.isEmpty()){
                listFull.addAll(list)
            }
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
        fun onKickButtonClicked(uid: String)
        fun onChatButtonClicked(uid: String)
    }

    override fun getFilter(): Filter {
        return searchFilter
    }

    private val searchFilter = object : Filter(){
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val results = FilterResults()

            if(p0 == null){
                results.values = listFull
                return results
            }

            val filters : List<String> = p0.split("/")
            val filteredByName = ArrayList<Player>()

            if(filters[0].isNotEmpty()){
                for(player in listFull){
                    if(player.name.lowercase().trim().contains(filters[0].lowercase().trim())){
                        filteredByName.add(player)
                    }
                }
            }else{
                filteredByName.addAll(listFull)
            }
            val filteredByAll = ArrayList<Player>()
            var rat = 0.0
            when(filters[1]){
                //listOf("All", "No rating!", "Minimum 1", "Minimum 2", "Minimum 3", "Minimum 4", "5"))
                "All" -> {results.values = filteredByName; return results}
                "No rating!" -> {rat = 0.0}
                "Minimum 1" -> {rat = 1.0}
                "Minimum 2" -> {rat = 2.0}
                "Minimum 3" -> {rat = 3.0}
                "Minimum 4" -> {rat = 4.0}
                "5" -> {rat = 5.0}
            }

            if(rat == 0.0){
                for(player in filteredByName){
                    if(player.rating == rat){
                        filteredByAll.add(player)
                    }
                }
                results.values = filteredByAll
                return results
            }
            for(player in filteredByName){
                if(player.rating >= rat){
                    filteredByAll.add(player)
                }
            }

            results.values = filteredByAll
            return results
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            list.clear()
            if(p1?.values != null)
                list.addAll(p1.values as ArrayList<Player>)
            notifyDataSetChanged()
        }

    }
}