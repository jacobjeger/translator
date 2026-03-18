package com.megalife.translator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megalife.translator.R
import com.megalife.translator.data.model.TranslationHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<TranslationHistory, HistoryAdapter.ViewHolder>(DiffCallback) {

    private var selectedIndex = -1

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedIndex)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemRoot: LinearLayout = itemView.findViewById(R.id.itemRoot)
        private val tvLanguagePair: TextView = itemView.findViewById(R.id.tvLanguagePair)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvSourceText: TextView = itemView.findViewById(R.id.tvSourceText)
        private val tvTranslatedText: TextView = itemView.findViewById(R.id.tvTranslatedText)

        private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        fun bind(item: TranslationHistory, isSelected: Boolean) {
            tvLanguagePair.text = item.languagePairDisplay
            tvTimestamp.text = dateFormat.format(Date(item.timestamp))
            tvSourceText.text = item.sourceText
            tvTranslatedText.text = item.translatedText

            itemRoot.isSelected = isSelected
            if (isSelected) {
                itemRoot.requestFocus()
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TranslationHistory>() {
        override fun areItemsTheSame(a: TranslationHistory, b: TranslationHistory) = a.id == b.id
        override fun areContentsTheSame(a: TranslationHistory, b: TranslationHistory) = a == b
    }
}
