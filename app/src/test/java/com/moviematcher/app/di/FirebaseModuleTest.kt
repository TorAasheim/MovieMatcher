package com.moviematcher.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import org.junit.Test
import kotlin.test.assertNotNull

class FirebaseModuleTest {
    
    private val firebaseModule = FirebaseModule
    
    @Test
    fun `provideFirebaseAuth returns non-null instance`() {
        // When
        val firebaseAuth = firebaseModule.provideFirebaseAuth()
        
        // Then
        assertNotNull(firebaseAuth)
    }
    
    @Test
    fun `provideFirebaseFirestore returns non-null instance`() {
        // When
        val firestore = firebaseModule.provideFirebaseFirestore()
        
        // Then
        assertNotNull(firestore)
    }
    
    @Test
    fun `provideFirebaseMessaging returns non-null instance`() {
        // When
        val messaging = firebaseModule.provideFirebaseMessaging()
        
        // Then
        assertNotNull(messaging)
    }
}