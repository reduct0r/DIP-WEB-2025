package com.dip.pingtest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.filter.HiddenHttpMethodFilter

@SpringBootApplication
class PingtestApplication {
    @Bean
    fun hiddenHttpMethodFilter(): HiddenHttpMethodFilter {
        return HiddenHttpMethodFilter()
    }
}

fun main(args: Array<String>) {
    runApplication<PingtestApplication>(*args)
    println("Application started")
}