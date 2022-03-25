package com.example.football_lobby.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.football_lobby.R
import com.example.football_lobby.models.NotificationData

class NotificationsDataAdapter(
    private var list: ArrayList<NotificationData>,
    private var listener: OnItemClickedListener
) : RecyclerView.Adapter<NotificationsDataAdapter.RecyclerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.notification_item_layout, parent, false)
        return RecyclerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val currentItem = list[position]
        holder.notificationBodyTxT.text = currentItem.message
        holder.notificationTitleTxt.text = currentItem.title
    }

    override fun getItemCount() = list.size

    fun setData(list: ArrayList<NotificationData>){
        this.list = list
        notifyDataSetChanged()
    }

    fun removeFromPos(position: Int){
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val notificationTitleTxt: TextView = itemView.findViewById(R.id.notificationTitleTxT)
        val notificationBodyTxT: TextView = itemView.findViewById(R.id.notificationBodyTxT)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position:Int = adapterPosition;
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClick(list[position], position)
            }
        }
    }

    interface OnItemClickedListener {
        fun onItemClick(notification: NotificationData, pos: Int)
    }

}