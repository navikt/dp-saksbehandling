package no.nav.dagpenger.saksbehandling.saksbehandler

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class KontaktInformasjon(
    val postadresse: PostAdresse?,
) {
    fun formatertPostAdresse(): String = postadresse?.formatertPostAdresse() ?: ""

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = PostBoksAdresse::class, name = "postboksadresse"),
        JsonSubTypes.Type(value = StedsAdresse::class, name = "stedsadresse"),
    )
    sealed class PostAdresse(
        val postnummer: String,
        val poststed: String,
    ) {
        abstract fun formatertPostAdresse(): String

        abstract val type: String
    }

    class StedsAdresse(
        postnummer: String,
        poststed: String,
        val gatenavn: String,
        val husnummer: String,
    ) : PostAdresse(postnummer, poststed) {
        override fun formatertPostAdresse(): String = "$gatenavn $husnummer, $postnummer $poststed"

        override val type: String = "stedsadresse"
    }

    class PostBoksAdresse(
        postnummer: String,
        poststed: String,
        val postboksnummer: String,
        val postboksanlegg: String?,
    ) : PostAdresse(postnummer, poststed) {
        override val type: String = "postboksadresse"

        fun postboksAnlegg(): String =
            when (postboksanlegg) {
                null -> ""
                else -> "$postboksanlegg,"
            }

        override fun formatertPostAdresse(): String = "Postboks $postboksnummer, ${postboksAnlegg()} $postnummer $poststed"
    }
}
