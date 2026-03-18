package com.megalife.translator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.megalife.translator.R
import com.megalife.translator.data.model.TranslationHistory
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (TranslationHistory) -> Unit,
    private val onItemLongClick: (TranslationHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items = listOf<TranslationHistory>()

    fun submitList(newItems: List<TranslationHistory>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLanguagePair: TextView = itemView.findViewById(R.id.tvLanguagePair)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvSourceText: TextView = itemView.findViewById(R.id.tvSourceText)
        private val tvTranslatedText: TextView = itemView.findViewById(R.id.tvTranslatedText)

        fun bind(item: TranslationHistory) {
            tvLanguagePair.text = item.languagePairDisplay
            tvTimestamp.text = formatTimestamp(item.timestamp)
            tvSourceText.text = item.sourceText
            tvTranslatedText.text = item.translatedText

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
