package com.megalife.translator.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.translator.R
import com.megalife.translator.data.model.TranslationHistory
import com.megalife.translator.ui.adapter.HistoryAdapter
import com.megalife.translator.viewmodel.HistoryViewModel

class HistoryActivity : BaseActivity() {

    private val viewModel: HistoryViewModel by viewModels()

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: HistoryAdapter

    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        adapter = HistoryAdapter()
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        viewModel.historyItems.observe(this) { items ->
            adapter.submitList(items)
            if (items.isEmpty()) {
                rvHistory.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
            } else {
                rvHistory.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
                updateSelection()
            }
        }

        viewModel.loadHistory()
    }

    private fun updateSelection() {
        val items = viewModel.historyItems.value ?: return
        selectedIndex = selectedIndex.coerceIn(0, maxOf(0, items.size - 1))
        adapter.setSelectedIndex(selectedIndex)
        rvHistory.scrollToPosition(selectedIndex)
    }

    private fun showDeleteConfirmation(item: TranslationHistory) {
        AlertDialog.Builder(this, R.style.Theme_MegaLifeTranslator)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(R.string.delete_confirmation_message)
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteItem(item)
            }
            .show()
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        val items = viewModel.historyItems.value ?: emptyList()

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (items.isNotEmpty() && selectedIndex > 0) {
                    selectedIndex--
                    updateSelection()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (items.isNotEmpty() && selectedIndex < items.size - 1) {
                    selectedIndex++
                    updateSelection()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                event?.startTracking()
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
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            val items = viewModel.historyItems.value ?: return false
            if (items.isNotEmpty() && selectedIndex in items.indices) {
                showDeleteConfirmation(items[selectedIndex])
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event?.isTracking == true && !event.isCanceled) {
            // Short press CENTER — reload selected translation back to main screen
            val items = viewModel.historyItems.value ?: return true
            if (items.isNotEmpty() && selectedIndex in items.indices) {
                val item = items[selectedIndex]
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("source_text", item.sourceText)
                    putExtra("translated_text", item.translatedText)
                    putExtra("source_lang", item.sourceLanguageCode)
                    putExtra("target_lang", item.targetLanguageCode)
                }
                startActivityWithFade(intent)
                finish()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
