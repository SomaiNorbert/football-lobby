package com.example.football_lobby.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football_lobby.R
import com.example.football_lobby.models.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView

class MessagesDataAdapter(
    private var list: ArrayList<Message>,
    private var context: Context
    ) : RecyclerView.Adapter<MessagesDataAdapter.RecyclerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessagesDataAdapter.RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.message_item_layout, parent, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessagesDataAdapter.RecyclerViewHolder, position: Int) {
        val currentItem = list[position]
        holder.messageTextView.text = currentItem.message

        if(Firebase.auth.currentUser!!.uid ==currentItem.senderUid){
            holder.messageTextView.setBackgroundResource(R.drawable.rounded_message_blue)
        }else{
            holder.messageTextView.setBackgroundResource(R.drawable.rounded_message_gray)
        }

        holder.senderTextView.text = currentItem.senderName
        val storageRef = Firebase.storage.reference
        Glide.with(holder.itemView.context).load(R.drawable.profile_avatar).into(holder.senderImageView)
        storageRef.child("images/${currentItem.senderUid}").downloadUrl.addOnSuccessListener {
            Glide.with(context).load(it).into(holder.senderImageView)
        }
    }

    override fun getItemCount() = list.size

    fun setData(list: ArrayList<Message>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun addItem(item: Message){
        list.add(item)
        notifyItemInserted(itemCount - 1)
    }

    inner class RecyclerViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val senderImageView: CircleImageView = itemView.findViewById(R.id.senderImageView)
        val senderTextView: TextView = itemView.findViewById(R.id.senderTextView)
    }

}