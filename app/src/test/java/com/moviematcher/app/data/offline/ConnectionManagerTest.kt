package com.moviematcher.app.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ConnectionManager
 */
class ConnectionManagerTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setup() {
        context = mockk()
        connectivityManager = mockk()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        
        connectionManager = ConnectionManager(context)
    }

    @Test
    fun `isCurrentlyConnected returns true when network has internet capability`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val result = connectionManager.isCurrentlyConnected()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isCurrentlyConnected returns false when no active network`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = connectionManager.isCurrentlyConnected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isCurrentlyConnected returns false when network capabilities are null`() {
        // Given
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        // When
        val result = connectionManager.isCurrentlyConnected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isCurrentlyConnected returns false when network lacks internet capability`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val result = connectionManager.isCurrentlyConnected()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isCurrentlyConnected returns false when network lacks validated capability`() {
        // Given
        val network = mockk<Network>()
        val networkCapabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        // When
        val result = connectionManager.isCurrentlyConnected()

        // Then
        assertFalse(result)
    }
}