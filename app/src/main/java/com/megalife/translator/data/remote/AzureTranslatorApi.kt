package com.megalife.translator.data.remote

import com.megalife.translator.data.model.TranslationRequest
import com.megalife.translator.data.model.TranslationResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AzureTranslatorApi {

    @POST("translate")
    suspend fun translate(
        @Body body: List<TranslationRequest>,
        @Query("api-version") version: String = "3.0",
        @Query("from") from: String? = null,
        @Query("to") to: String,
        @Query("profanityAction") profanityAction: String = "Deleted",
        @Query("profanityMarker") profanityMarker: String = "Asterisk",
        @Header("Ocp-Apim-Subscription-Key") key: String,
        @Header("Ocp-Apim-Subscription-Region") region: String,
        @Header("Content-Type") contentType: String = "application/json"
    ): List<TranslationResponse>
}
