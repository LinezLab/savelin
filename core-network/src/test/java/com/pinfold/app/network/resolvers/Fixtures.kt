package com.pinfold.app.network.resolvers

object Fixtures {
    fun html(name: String): String = Fixtures::class.java.getResourceAsStream("/fixtures/$name")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("Missing recorded fixture: $name")
}
