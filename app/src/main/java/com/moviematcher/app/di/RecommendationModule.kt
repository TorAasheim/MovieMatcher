package com.moviematcher.app.di

import com.moviematcher.app.data.engine.MovieRecommendationEngine
import com.moviematcher.app.data.repository.MovieRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for movie recommendation dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RecommendationModule {

    @Provides
    @Singleton
    fun provideMovieRecommendationEngine(
        movieRepository: MovieRepository
    ): MovieRecommendationEngine {
        return MovieRecommendationEngine(movieRepository)
    }
}