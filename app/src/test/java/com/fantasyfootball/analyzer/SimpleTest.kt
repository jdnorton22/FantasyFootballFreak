package com.fantasyfootball.analyzer

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test to verify basic functionality
 */
class SimpleTest {
    @Test
    fun `basic test passes`() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun `string concatenation works`() {
        val result = "Hello" + " " + "World"
        assertEquals("Hello World", result)
    }
}