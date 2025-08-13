package com.moviematcher.app.di

import com.moviematcher.app.data.remote.api.TmdbApi
import com.moviematcher.app.data.repository.MovieRepository
import com.moviematcher.app.data.repository.TmdbRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Hilt module for network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    
    @Binds
    abstract fun bindMovieRepository(tmdbRepository: TmdbRepository): MovieRepository
    
    companion object {
        
        private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
        
        @Provides
        @Singleton
        fun provideMoshi(): Moshi {
            return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }
        
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
        }
        
        @Provides
        @Singleton
        fun provideRetrofit(
            okHttpClient: OkHttpClient,
            moshi: Moshi
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(TMDB_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }
        
        @Provides
        @Singleton
        fun provideTmdbApi(retrofit: Retrofit): TmdbApi {
            return retrofit.create(TmdbApi::class.java)
        }
    }
}