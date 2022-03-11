package com.example.football_lobby.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.models.Rating

class RatingsDataAdapter(private var list: ArrayList<Rating>) : RecyclerView.Adapter<RatingsDataAdapter.RecyclerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingsDataAdapter.RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.rating_item_layout, parent, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val currentItem = list[position]
        holder.lobbyNameRatingTxt.text = currentItem.lobbyName
        holder.fromNameTxt.text = currentItem.fromName
        holder.punctualityTxt.text = currentItem.punctuality.toString()
        holder.behaviorTxt.text = currentItem.behavior.toString()
        holder.calmnessTxt.text = currentItem.calmness.toString()
        holder.sportsmanshipTxt.text = currentItem.sportsmanship.toString()
        holder.commentTxt.text = currentItem.comment
        if(currentItem.comment.isEmpty()){
            holder.commentGroup.visibility = View.GONE
        }else{
            holder.commentGroup.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = list.size

    inner class RecyclerViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView), View.OnClickListener{

        val lobbyNameRatingTxt: TextView = itemView.findViewById(R.id.lobbyNameRatingTxt)
        val fromNameTxt: TextView = itemView.findViewById(R.id.fromNameTxt)
        val punctualityTxt: TextView = itemView.findViewById(R.id.punctualityTxt)
        val behaviorTxt: TextView = itemView.findViewById(R.id.behaviorTxt)
        val calmnessTxt: TextView = itemView.findViewById(R.id.calmnessTxt)
        val sportsmanshipTxt: TextView = itemView.findViewById(R.id.sportsmanshipTxt)
        val commentTxt: TextView = itemView.findViewById(R.id.commentTxt)
        val commentGroup: Group = itemView.findViewById(R.id.commentGroup)

        override fun onClick(p0: View?) {}
    }

    fun setData(list: ArrayList<Rating>){
        this.list = list
        notifyDataSetChanged()
    }

}