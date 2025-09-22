package com.dip.pingtest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PingtestApplication

fun main(args: Array<String>) {
	runApplication<PingtestApplication>(*args)
    println("Hello world")
}
