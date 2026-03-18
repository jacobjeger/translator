package com.megalife.translator.data.model

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("translations")
    val translations: List<TranslationItem>
) {
    data class TranslationItem(
        @SerializedName("text")
        val text: String,
        @SerializedName("to")
        val to: String
    )
}
