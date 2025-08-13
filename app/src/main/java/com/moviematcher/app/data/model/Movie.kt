package com.moviematcher.app.data.model

/**
 * Represents a movie from TMDB API
 */
data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val genres: List<Genre>,
    val runtime: Int?
)