package no.nav.dagpenger.saksbehandling.innsending

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import java.time.LocalDateTime
import java.util.UUID

class Innsending private constructor(
    val innsendingId: UUID = UUIDv7.ny(),
    val person: Person,
    val journalpostId: String,
    val mottatt: LocalDateTime,
    val skjemaKode: String,
    val kategori: Kategori,
    val søknadId: UUID? = null,
) {
    companion object {
        fun opprett(
            hendelse: InnsendingMottattHendelse,
            personProvider: (ident: String) -> Person,
        ): Innsending {
            return Innsending(
                person = personProvider.invoke(hendelse.ident),
                journalpostId = hendelse.journalpostId,
                mottatt = hendelse.registrertTidspunkt,
                skjemaKode = hendelse.skjemaKode,
                kategori = hendelse.kategori,
            )
        }

        fun rehydrer(
            innsendingId: UUID = UUIDv7.ny(),
            person: Person,
            journalpostId: String,
            mottatt: LocalDateTime,
            skjemaKode: String,
            kategori: Kategori,
        ): Innsending {
            return Innsending(
                innsendingId = innsendingId,
                person = person,
                journalpostId = journalpostId,
                mottatt = mottatt,
                skjemaKode = skjemaKode,
                kategori = kategori,
            )
        }
    }

    fun gjelderSøknadMedId(søknadId: UUID): Boolean =
        kategori in setOf(Kategori.NY_SØKNAD, Kategori.GJENOPPTAK) && this.søknadId == søknadId

    override fun toString(): String {
        return "Innsending(innsendingId=$innsendingId, person=$person, journalpostId='$journalpostId', " +
            "mottatt=$mottatt, skjemaKode='$skjemaKode', kategori=$kategori, søknadId=$søknadId"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Innsending

        if (innsendingId != other.innsendingId) return false
        if (person != other.person) return false
        if (journalpostId != other.journalpostId) return false
        if (mottatt != other.mottatt) return false
        if (skjemaKode != other.skjemaKode) return false
        if (kategori != other.kategori) return false
        if (søknadId != other.søknadId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = innsendingId.hashCode()
        result = 31 * result + person.hashCode()
        result = 31 * result + journalpostId.hashCode()
        result = 31 * result + mottatt.hashCode()
        result = 31 * result + skjemaKode.hashCode()
        result = 31 * result + søknadId.hashCode()
        result = 31 * result + kategori.hashCode()
        return result
    }

    fun harTilgang(utførtAv: Behandler) {
        when (utførtAv) {
            is Applikasjon -> {}
            is Saksbehandler -> {
                this.person.harTilgang(utførtAv)
            }
        }
    }
}
