package com.example.bluetoothlechat.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothlechat.bluetooth.Message
import com.example.bluetoothlechat.R
import java.lang.IllegalArgumentException

private const val TAG = "MessageAdapter"
private const val REMOTE_MESSAGE = 0
private const val LOCAL_MESSAGE = 1

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<Message>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder: ")
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            REMOTE_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_remote_message, parent, false)
                RemoteMessageViewHolder(view)
            }
            LOCAL_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_local_message, parent, false)
                LocalMessageViewHolder(view)
            }
            else -> {
                throw IllegalArgumentException("Unknown MessageAdapter view type")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: ")
        val message = messages[position]
        when(message) {
            is Message.RemoteMessage -> {
                (holder as RemoteMessageViewHolder).bind(message)
            }
            is Message.LocalMessage -> {
                (holder as LocalMessageViewHolder).bind(message)
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ")
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        Log.d(TAG, "getItemViewType: ")
        return when(messages[position]) {
            is Message.RemoteMessage -> REMOTE_MESSAGE
            is Message.LocalMessage -> LOCAL_MESSAGE
        }
    }

    // Add messages to the top of the list so they're easy to see
    fun addMessage(message: Message) {
        Log.d(TAG, "addMessage: ")
        messages.add(0, message)
        notifyDataSetChanged()
    }
}