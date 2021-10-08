package de.jeka.example

import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertEquals

internal class SumTest {

    @org.junit.jupiter.api.Test
    fun sum() {
        assertEquals(6, Sum(2, 4).sum())
    }

    @org.junit.jupiter.api.Test
    fun print() {
        Sum(9,18).print()
    }
}