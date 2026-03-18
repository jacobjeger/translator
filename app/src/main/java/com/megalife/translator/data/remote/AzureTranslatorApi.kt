package com.megalife.translator.data.remote

import com.megalife.translator.data.model.TranslationRequest
import com.megalife.translator.data.model.TranslationResponse
import retrofit2.http.*

interface AzureTranslatorApi {

    @POST("translate")
    suspend fun translate(
        @Query("api-version") apiVersion: String = "3.0",
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("profanityAction") profanityAction: String = "Deleted",
        @Query("profanityMarker") profanityMarker: String = "Asterisk",
        @Body body: List<TranslationRequest>,
        @Header("Ocp-Apim-Subscription-Key") key: String,
        @Header("Ocp-Apim-Subscription-Region") region: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): List<TranslationResponse>
}
