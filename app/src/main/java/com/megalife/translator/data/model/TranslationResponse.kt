package com.megalife.translator.data.model

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("translations")
    val translations: List<Translation>
) {
    data class Translation(
        @SerializedName("text")
        val text: String,
        @SerializedName("to")
        val to: String
    )

    fun translatedText(): String = translations.firstOrNull()?.text ?: ""
}
