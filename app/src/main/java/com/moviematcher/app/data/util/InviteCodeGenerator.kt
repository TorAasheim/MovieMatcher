package com.moviematcher.app.data.util

import kotlin.random.Random

/**
 * Generates unique, user-friendly invite codes for room sharing
 */
object InviteCodeGenerator {
    
    // Using a mix of consonants and vowels to create pronounceable codes
    private val consonants = "BCDFGHJKLMNPQRSTVWXYZ"
    private val vowels = "AEIOU"
    
    /**
     * Generates a 6-character invite code in the format CVC-CVC
     * where C = consonant, V = vowel
     * This creates pronounceable codes like "BAR-TOK"
     */
    fun generateCode(): String {
        val random = Random.Default
        
        val part1 = buildString {
            append(consonants.random(random))
            append(vowels.random(random))
            append(consonants.random(random))
        }
        
        val part2 = buildString {
            append(consonants.random(random))
            append(vowels.random(random))
            append(consonants.random(random))
        }
        
        return "$part1-$part2"
    }
    
    /**
     * Validates that an invite code matches the expected format
     */
    fun isValidFormat(code: String): Boolean {
        if (code.length != 7) return false
        if (code[3] != '-') return false
        
        val parts = code.split('-')
        if (parts.size != 2) return false
        
        return parts.all { part ->
            part.length == 3 &&
            part[0] in consonants &&
            part[1] in vowels &&
            part[2] in consonants
        }
    }
}