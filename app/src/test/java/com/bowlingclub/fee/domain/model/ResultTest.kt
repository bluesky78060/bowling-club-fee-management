package com.bowlingclub.fee.domain.model

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success result returns correct value`() {
        val result = Result.Success("test data")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals("test data", result.getOrNull())
    }

    @Test
    fun `Error result returns null for getOrNull`() {
        val exception = RuntimeException("Test error")
        val result = Result.Error(exception)

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("Test error", result.message)
    }

    @Test
    fun `Loading result has correct state`() {
        val result = Result.Loading

        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `getOrDefault returns data on success`() {
        val result = Result.Success(42)

        assertEquals(42, result.getOrDefault(0))
    }

    @Test
    fun `getOrDefault returns default on error`() {
        val result: Result<Int> = Result.Error(RuntimeException("error"))

        assertEquals(0, result.getOrDefault(0))
    }

    @Test
    fun `map transforms success value`() {
        val result = Result.Success(10)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(20, mapped.getOrNull())
    }

    @Test
    fun `map preserves error`() {
        val error = Result.Error(RuntimeException("error"))
        val mapped = error.map { "transformed" }

        assertTrue(mapped.isError)
    }

    @Test
    fun `runCatching returns success on normal execution`() {
        val result = Result.runCatching { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching returns error on exception`() {
        val result = Result.runCatching { throw RuntimeException("error") }

        assertTrue(result.isError)
    }
}
