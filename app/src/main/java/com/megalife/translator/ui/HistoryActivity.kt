package com.megalife.translator.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.translator.R
import com.megalife.translator.data.model.TranslationHistory
import com.megalife.translator.ui.adapter.HistoryAdapter
import com.megalife.translator.viewmodel.HistoryViewModel

class HistoryActivity : BaseActivity() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: HistoryAdapter

    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        rvHistory = findViewById(R.id.rvHistory)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        adapter = HistoryAdapter(
            onItemClick = { item -> loadTranslation(item) },
            onItemLongClick = { item -> showDeleteDialog(item) }
        )

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        viewModel.historyItems.observe(this) { items ->
            adapter.submitList(items)
            if (items.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE
                // Set focus on first item
                selectedPosition = 0
                updateListFocus()
            }
        }

        viewModel.loadHistory()
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        val items = viewModel.historyItems.value ?: return false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (selectedPosition > 0) {
                    selectedPosition--
                    updateListFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (selectedPosition < items.size - 1) {
                    selectedPosition++
                    updateListFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                event?.startTracking()
                if (items.isNotEmpty() && selectedPosition < items.size) {
                    loadTranslation(items[selectedPosition])
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return false
    }

    override fun handleLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val items = viewModel.historyItems.value ?: return false
            if (items.isNotEmpty() && selectedPosition < items.size) {
                showDeleteDialog(items[selectedPosition])
            }
            return true
        }
        return false
    }

    private fun loadTranslation(item: TranslationHistory) {
        val resultIntent = Intent()
        resultIntent.putExtra("source_text", item.sourceText)
        resultIntent.putExtra("translated_text", item.translatedText)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showDeleteDialog(item: TranslationHistory) {
        AlertDialog.Builder(this, R.style.AppTheme)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null) // Cancel focused by default
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteItem(item)
            }
            .show()
    }

    private fun updateListFocus() {
        rvHistory.scrollToPosition(selectedPosition)
        rvHistory.post {
            val viewHolder = rvHistory.findViewHolderForAdapterPosition(selectedPosition)
            viewHolder?.itemView?.requestFocus()
        }
    }
}
