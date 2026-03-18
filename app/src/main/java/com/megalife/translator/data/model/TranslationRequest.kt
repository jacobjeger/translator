package com.megalife.translator.data.model

import com.google.gson.annotations.SerializedName

data class TranslationRequest(
    @SerializedName("Text")
    val text: String
)
