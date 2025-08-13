package com.moviematcher.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for TMDB movie response
 */
@JsonClass(generateAdapter = true)
data class TmdbMovieDto(
    @Json(name = "id")
    val id: Long,
    @Json(name = "title")
    val title: String,
    @Json(name = "overview")
    val overview: String,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "release_date")
    val releaseDate: String?,
    @Json(name = "vote_average")
    val voteAverage: Double,
    @Json(name = "genre_ids")
    val genreIds: List<Int>?,
    @Json(name = "genres")
    val genres: List<TmdbGenreDto>?,
    @Json(name = "runtime")
    val runtime: Int?
)

/**
 * DTO for TMDB genre response
 */
@JsonClass(generateAdapter = true)
data class TmdbGenreDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String
)

/**
 * DTO for TMDB movies list response
 */
@JsonClass(generateAdapter = true)
data class TmdbMoviesResponse(
    @Json(name = "page")
    val page: Int,
    @Json(name = "results")
    val results: List<TmdbMovieDto>,
    @Json(name = "total_pages")
    val totalPages: Int,
    @Json(name = "total_results")
    val totalResults: Int
)

/**
 * DTO for TMDB genres list response
 */
@JsonClass(generateAdapter = true)
data class TmdbGenresResponse(
    @Json(name = "genres")
    val genres: List<TmdbGenreDto>
)