package com.example.football_lobby.adapters

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.models.Lobby
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import java.util.*
import kotlin.collections.ArrayList

class LobbiesDataAdapter(
    private var context: Context,
    private var list: ArrayList<Lobby>,
    private var listener: OnItemClickedListener
    ) : RecyclerView.Adapter<LobbiesDataAdapter.RecyclerViewHolder>(), Filterable{

    private val listFull = ArrayList<Lobby>()

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
        holder.creator.text = currentItem.creatorName
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
        fun onItemClick(uid:String)
    }

    fun setData(list:ArrayList<Lobby>){
        this.list = list
        if(listFull.isEmpty()){
            listFull.addAll(list)
        }
    }

    override fun getFilter(): Filter {
        return searchFilter
    }

    private val searchFilter = object : Filter(){
        @SuppressLint("MissingPermission")
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val results = FilterResults()
            if(p0 == null){
                results.values = list
                return results
            }

            val filters: List<String> = p0.split("/")
            val filteredByNameAndCreator = ArrayList<Lobby>()
            if(filters[0].isEmpty() && filters[1].isEmpty()){  //Name and Creator is Empty: filtered list is equal to full list
                filteredByNameAndCreator.addAll(listFull)
            }else{
                if(filters[0].isEmpty()){   //Name is Empty: we filter the full list by Creator
                    for(lobby in 0 until listFull.size){
                        if(listFull[lobby].creatorName.lowercase().trim().contains(filters[1].lowercase().trim())){
                            filteredByNameAndCreator.add(listFull[lobby])
                        }
                    }
                }else{
                    if(filters[1].isEmpty()){    //Creator is Empty: we filter the full list by Name
                        for(lobby in 0 until listFull.size){
                            if(listFull[lobby].lobbyName.lowercase().trim().contains(filters[0].lowercase().trim())){
                                filteredByNameAndCreator.add(listFull[lobby])
                            }
                        }
                    }else{
                        for(lobby in 0 until listFull.size){   //Neither Name nor creator is empty: we filter by both
                            if(listFull[lobby].lobbyName.lowercase().trim().contains(filters[0].lowercase().trim()) &&
                                listFull[lobby].creatorName.lowercase().trim().contains(filters[1].lowercase().trim())){
                                filteredByNameAndCreator.add(listFull[lobby])
                            }
                        }
                    }
                }
            }
            //filter the filtered list by distance
            val filteredByAll = ArrayList<Lobby>()
            val geocoder = Geocoder(context, Locale.getDefault())
            if(ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                    val location = Tasks.await(LocationServices.getFusedLocationProviderClient(context!!).lastLocation);
                    val myLocation = LatLng(location.latitude, location.longitude)
                    for (lobby in 0 until filteredByNameAndCreator.size) {
                        val add = geocoder.getFromLocationName(
                            filteredByNameAndCreator[lobby].location,
                            1
                        )[0]
                        val gameLocation = LatLng(add.latitude, add.longitude)
                        val result = FloatArray(3)
                        Location.distanceBetween(
                            myLocation.latitude, myLocation.longitude,
                            gameLocation.latitude, gameLocation.longitude, result
                        )
                        if (result[0] / 1000 <= filters[2].toInt()) {
                            filteredByAll.add(filteredByNameAndCreator[lobby])
                        }
                    }
            }
            results.values = filteredByAll
            Log.d(TAG, results.values.toString())
            return results
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            list.clear()
            if(p1?.values != null)
                list.addAll(p1.values as ArrayList<Lobby>)
            notifyDataSetChanged()
        }

    }

}