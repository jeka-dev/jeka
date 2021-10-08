package de.jeka.example

class Sum(val a:Int, val b:Int) {

    fun sum() : Int {
        return a + b
    }

    fun print() {
        println("${a} + ${b} = ${sum()}")
    }

}