package dev.jeka.example

fun main(args: Array<String>) {
    println(Suite(args.map { arg -> arg.toInt()}).sum())
}