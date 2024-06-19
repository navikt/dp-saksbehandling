package no.nav.dagpenger.saksbehandling.helper

import java.io.FileNotFoundException

internal fun String.fileAsText(): String {
    return object {}.javaClass.getResource(this)?.readText()
        ?: throw FileNotFoundException()
}
