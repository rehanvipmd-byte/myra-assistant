package com.myra.assistant.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.databinding.DataBindingUtil
import com.myra.assistant.R
import com.myra.assistant.databinding.ItemChatMessageBinding

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding: ItemChatMessageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_chat_message,
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                chatText.text = message.text
                if (message.isUser) {
                    chatText.setTextColor(0xFFFFFFFF.toInt())
                    chatBubble.setBackgroundColor(0xFF1976D2.toInt())
                } else {
                    chatText.setTextColor(0xFF000000.toInt())
                    chatBubble.setBackgroundColor(0xFFE0E0E0.toInt())
                }
            }
        }
    }
}
