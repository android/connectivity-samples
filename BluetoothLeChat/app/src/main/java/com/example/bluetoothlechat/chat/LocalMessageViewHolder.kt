package com.example.bluetoothlechat.chat

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothlechat.bluetooth.Message
import com.example.bluetoothlechat.R

class LocalMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val messageText = itemView.findViewById<TextView>(R.id.message_text)

    fun bind(message: Message.LocalMessage) {
        messageText.text = message.text
    }
}