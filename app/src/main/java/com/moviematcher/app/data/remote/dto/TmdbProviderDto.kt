package com.moviematcher.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for TMDB streaming provider response
 */
@JsonClass(generateAdapter = true)
data class TmdbProviderDto(
    @Json(name = "provider_id")
    val providerId: Int,
    @Json(name = "provider_name")
    val providerName: String,
    @Json(name = "logo_path")
    val logoPath: String?
)

/**
 * DTO for TMDB watch providers response for a specific region
 */
@JsonClass(generateAdapter = true)
data class TmdbWatchProvidersRegion(
    @Json(name = "link")
    val link: String?,
    @Json(name = "flatrate")
    val flatrate: List<TmdbProviderDto>?,
    @Json(name = "rent")
    val rent: List<TmdbProviderDto>?,
    @Json(name = "buy")
    val buy: List<TmdbProviderDto>?
)

/**
 * DTO for TMDB watch providers response
 */
@JsonClass(generateAdapter = true)
data class TmdbWatchProvidersResponse(
    @Json(name = "id")
    val id: Long,
    @Json(name = "results")
    val results: Map<String, TmdbWatchProvidersRegion>
)