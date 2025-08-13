package com.moviematcher.app.data.remote.mapper

import com.moviematcher.app.data.model.Genre
import com.moviematcher.app.data.model.Movie
import com.moviematcher.app.data.model.StreamingProvider
import com.moviematcher.app.data.remote.dto.TmdbGenreDto
import com.moviematcher.app.data.remote.dto.TmdbMovieDto
import com.moviematcher.app.data.remote.dto.TmdbProviderDto
import com.moviematcher.app.data.remote.dto.TmdbWatchProvidersRegion
import com.moviematcher.app.data.remote.util.TmdbImageUtil

/**
 * Mapper to convert TMDB DTOs to domain models
 */
object TmdbMapper {
    
    /**
     * Convert TmdbMovieDto to Movie domain model
     */
    fun mapToMovie(dto: TmdbMovieDto, genreMap: Map<Int, String> = emptyMap()): Movie {
        val genres = when {
            // If genres are provided directly in the DTO (from movie details endpoint)
            dto.genres != null -> dto.genres.map { mapToGenre(it) }
            // If only genre IDs are provided (from search/trending endpoints)
            dto.genreIds != null -> dto.genreIds.mapNotNull { genreId ->
                genreMap[genreId]?.let { genreName ->
                    Genre(id = genreId, name = genreName)
                }
            }
            else -> emptyList()
        }
        
        return Movie(
            id = dto.id,
            title = dto.title,
            overview = dto.overview,
            posterPath = dto.posterPath,
            releaseDate = dto.releaseDate,
            voteAverage = dto.voteAverage,
            genres = genres,
            runtime = dto.runtime
        )
    }
    
    /**
     * Convert TmdbGenreDto to Genre domain model
     */
    fun mapToGenre(dto: TmdbGenreDto): Genre {
        return Genre(
            id = dto.id,
            name = dto.name
        )
    }
    
    /**
     * Convert TmdbProviderDto to StreamingProvider domain model
     */
    fun mapToStreamingProvider(dto: TmdbProviderDto, deepLinkUrl: String? = null): StreamingProvider {
        return StreamingProvider(
            id = dto.providerId,
            name = dto.providerName,
            logoPath = TmdbImageUtil.buildLogoUrl(dto.logoPath),
            deepLinkUrl = deepLinkUrl
        )
    }
    
    /**
     * Extract all streaming providers from a watch providers region
     */
    fun extractStreamingProviders(region: TmdbWatchProvidersRegion): List<StreamingProvider> {
        val providers = mutableListOf<StreamingProvider>()
        
        // Add flatrate providers (subscription services like Netflix, Hulu)
        region.flatrate?.forEach { provider ->
            providers.add(mapToStreamingProvider(provider, region.link))
        }
        
        // Add rental providers
        region.rent?.forEach { provider ->
            providers.add(mapToStreamingProvider(provider, region.link))
        }
        
        // Add purchase providers
        region.buy?.forEach { provider ->
            providers.add(mapToStreamingProvider(provider, region.link))
        }
        
        return providers.distinctBy { it.id } // Remove duplicates
    }
}