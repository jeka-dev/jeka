package dev.jeka.example

class Suite(val sequence: Iterable<Int>) {

    fun sum() : Int {
        return sequence.fold(0) { a, b -> a + b }
    }

}