package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.toUrnOrNull
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribueringKvitteringHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import java.util.UUID

data class Utsending(
    val id: UUID = UUIDv7.ny(),
    val oppgaveId: UUID,
    private var sak: Sak? = null,
    private var brev: String? = null,
    private var pdfUrn: URN? = null,
    private var journalpostId: String? = null,
    private var tilstand: Tilstand = Opprettet,
) {
    fun brev(): String? = brev

    fun pdfUrn(): URN? = pdfUrn

    fun journalpostId(): String? = journalpostId

    fun tilstand() = tilstand

    fun sak() = sak

    companion object {
        fun rehydrer(
            id: UUID,
            oppgaveId: UUID,
            tilstand: Tilstand,
            brev: String?,
            pdfUrn: String?,
            journalpostId: String?,
            sak: Sak?,
        ): Utsending {
            return Utsending(
                id = id,
                oppgaveId = oppgaveId,
                tilstand = tilstand,
                brev = brev,
                pdfUrn = pdfUrn.toUrnOrNull(),
                journalpostId = journalpostId,
                sak = sak,
            )
        }
    }

    fun startUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        tilstand.mottaStartUtsending(this, startUtsendingHendelse)
    }

    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        this.brev = vedtaksbrevHendelse.brev
        tilstand.mottaBrev(this, vedtaksbrevHendelse)
    }

    fun mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse: ArkiverbartBrevHendelse) {
        pdfUrn = arkiverbartBrevHendelse.pdfUrn
        tilstand.mottaUrnTilPdfAvBrev(this, arkiverbartBrevHendelse)
    }

    fun mottaJournalpost(journalpostHendelse: JournalpostHendelse) {
        tilstand.mottaJournalpost(this, journalpostHendelse)
    }

    fun mottaDistribueringKvittering(distribueringKvitteringHendelse: DistribueringKvitteringHendelse) {
        tilstand.mottaKvitteringPåUtsending(this, distribueringKvitteringHendelse)
    }

    object Opprettet : Tilstand {
        override val type = Tilstand.Type.Opprettet

        override fun mottaBrev(
            utsending: Utsending,
            vedtaksbrevHendelse: VedtaksbrevHendelse,
        ) {
            utsending.tilstand = VenterPåVedtak
        }
    }

    object VenterPåVedtak : Tilstand {
        override val type = Tilstand.Type.VenterPåVedtak

        override fun mottaStartUtsending(
            utsending: Utsending,
            startUtsendingHendelse: StartUtsendingHendelse,
        ) {
            utsending.tilstand = AvventerArkiverbarVersjonAvBrev
            utsending.sak = startUtsendingHendelse.sak
        }
    }

    object AvventerArkiverbarVersjonAvBrev : Tilstand {
        override val type = Tilstand.Type.AvventerArkiverbarVersjonAvBrev

        override fun behov(utsending: Utsending) =
            ArkiverbartBrevBehov(
                oppgaveId = utsending.oppgaveId,
                html = utsending.brev ?: throw IllegalStateException("Brev mangler"),
            )

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            utsending.tilstand = AvventerJournalføring
        }
    }

    object AvventerJournalføring : Tilstand {
        override val type = Tilstand.Type.AvventerJournalføring

        override fun behov(utsending: Utsending): Behov {
            return JournalføringBehov(
                oppgaveId = utsending.oppgaveId,
                pdfUrn = utsending.pdfUrn ?: throw IllegalStateException("pdfUrn mangler"),
            )
        }

        override fun mottaJournalpost(
            utsending: Utsending,
            journalpostHendelse: JournalpostHendelse,
        ) {
            utsending.journalpostId = journalpostHendelse.journalpostId
            utsending.tilstand = AvventerDistribuering
        }
    }

    object AvventerDistribuering : Tilstand {
        override val type = Tilstand.Type.AvventerDistribuering

        override fun behov(utsending: Utsending): Behov {
            return DistribueringBehov(
                oppgaveId = utsending.oppgaveId,
                journalpostId = utsending.journalpostId ?: throw IllegalStateException("journalpostId mangler"),
            )
        }

        override fun mottaKvitteringPåUtsending(
            utsending: Utsending,
            distribueringKvitteringHendelse: DistribueringKvitteringHendelse,
        ) {
            // TODO: Sanity check av noe slag
            utsending.tilstand = Distribuert
        }
    }

    object Distribuert : Tilstand {
        override val type = Tilstand.Type.Distribuert
    }

    interface Tilstand {
        fun behov(utsending: Utsending): Behov = IngenBehov

        fun mottaBrev(
            utsending: Utsending,
            vedtaksbrevHendelse: VedtaksbrevHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta brev i tilstand: ${this.type}")
        }

        fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta urn til pdf av brev i tilstand: ${this.type}")
        }

        fun mottaJournalpost(
            utsending: Utsending,
            journalpostHendelse: JournalpostHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta journalpost i tilstand: ${this.type}")
        }

        fun mottaKvitteringPåUtsending(
            utsending: Utsending,
            distribueringKvitteringHendelse: DistribueringKvitteringHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta kvittering på utsending i tilstand: ${this.type}")
        }

        fun mottaStartUtsending(
            utsending: Utsending,
            startUtsendingHendelse: StartUtsendingHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta vedtak i tilstand: ${this.type}")
        }

        val type: Type

        enum class Type {
            Opprettet,
            VenterPåVedtak,
            AvventerArkiverbarVersjonAvBrev,
            AvventerJournalføring,
            AvventerDistribuering,
            Distribuert,
        }

        class UlovligUtsendingTilstandsendring(message: String) : RuntimeException(message)
    }
}
