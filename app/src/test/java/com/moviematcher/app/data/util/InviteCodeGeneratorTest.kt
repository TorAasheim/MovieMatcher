package com.moviematcher.app.data.util

import org.junit.Assert.*
import org.junit.Test

class InviteCodeGeneratorTest {
    
    @Test
    fun `generateCode returns code with correct format`() {
        val code = InviteCodeGenerator.generateCode()
        
        // Should be 7 characters long
        assertEquals(7, code.length)
        
        // Should have hyphen in the middle
        assertEquals('-', code[3])
        
        // Should be valid format
        assertTrue(InviteCodeGenerator.isValidFormat(code))
    }
    
    @Test
    fun `generateCode returns different codes on multiple calls`() {
        val codes = mutableSetOf<String>()
        
        // Generate 100 codes and ensure they're all different
        repeat(100) {
            val code = InviteCodeGenerator.generateCode()
            assertFalse("Generated duplicate code: $code", codes.contains(code))
            codes.add(code)
        }
    }
    
    @Test
    fun `generateCode follows CVC-CVC pattern`() {
        val consonants = "BCDFGHJKLMNPQRSTVWXYZ"
        val vowels = "AEIOU"
        
        repeat(50) {
            val code = InviteCodeGenerator.generateCode()
            val parts = code.split('-')
            
            assertEquals(2, parts.size)
            
            parts.forEach { part ->
                assertEquals(3, part.length)
                assertTrue("First char should be consonant: ${part[0]}", part[0] in consonants)
                assertTrue("Second char should be vowel: ${part[1]}", part[1] in vowels)
                assertTrue("Third char should be consonant: ${part[2]}", part[2] in consonants)
            }
        }
    }
    
    @Test
    fun `isValidFormat returns true for valid codes`() {
        val validCodes = listOf(
            "BAR-TOK",
            "FIG-MUN",
            "DOL-PEX",
            "KAT-BUZ"
        )
        
        validCodes.forEach { code ->
            assertTrue("Code should be valid: $code", InviteCodeGenerator.isValidFormat(code))
        }
    }
    
    @Test
    fun `isValidFormat returns false for invalid codes`() {
        val invalidCodes = listOf(
            "",                    // Empty
            "BARTOK",             // No hyphen
            "BAR-TOKS",           // Too long
            "BA-TOK",             // First part too short
            "BAR-TO",             // Second part too short
            "123-456",            // Numbers
            "bar-tok",            // Lowercase
            "BAR_TOK",            // Wrong separator
            "AAA-EEE",            // All vowels
            "BCD-FGH",            // All consonants
            "B2R-T0K"             // Contains numbers
        )
        
        invalidCodes.forEach { code ->
            assertFalse("Code should be invalid: $code", InviteCodeGenerator.isValidFormat(code))
        }
    }
    
    @Test
    fun `isValidFormat handles edge cases`() {
        // Null and empty cases
        assertFalse(InviteCodeGenerator.isValidFormat(""))
        
        // Wrong length cases
        assertFalse(InviteCodeGenerator.isValidFormat("A"))
        assertFalse(InviteCodeGenerator.isValidFormat("AB-CD"))
        assertFalse(InviteCodeGenerator.isValidFormat("ABCD-EFGH"))
        
        // Wrong separator cases
        assertFalse(InviteCodeGenerator.isValidFormat("ABC DEFG"))
        assertFalse(InviteCodeGenerator.isValidFormat("ABC.DEF"))
        assertFalse(InviteCodeGenerator.isValidFormat("ABCDEFG"))
        
        // Multiple separators
        assertFalse(InviteCodeGenerator.isValidFormat("AB-C-DE"))
        assertFalse(InviteCodeGenerator.isValidFormat("A-B-C-D"))
    }
}