package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.toUrnOrNull
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.MidlertidigJournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.UtsendingKvitteringHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import java.util.UUID

data class Utsending(
    val id: UUID = UUIDv7.ny(),
    val oppgaveId: UUID,
    private var brev: String? = null,
    private var pdfUrn: URN? = null,
    private var journalpostId: String? = null,
    private var tilstand: Tilstand = Opprettet,
) {
    fun brev(): String? = brev

    fun pdfUrn(): URN? = pdfUrn

    fun journalpostId(): String? = journalpostId

    fun tilstand() = tilstand

    companion object {
        fun rehydrer(
            id: UUID,
            oppgaveId: UUID,
            tilstand: Tilstand,
            brev: String?,
            pdfUrn: String?,
            journalpostId: String?,
        ): Utsending {
            return Utsending(
                id = id,
                oppgaveId = oppgaveId,
                tilstand = tilstand,
                brev = brev,
                pdfUrn = pdfUrn.toUrnOrNull(),
                journalpostId = journalpostId,
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

    fun mottaMidlertidigJournalpost(midlertidigJournalpostHendelse: MidlertidigJournalpostHendelse) {
        journalpostId = midlertidigJournalpostHendelse.journalpostId
        tilstand.mottaMidlertidigJournalpost(this, midlertidigJournalpostHendelse)
    }

    fun mottaJournalpost(journalpostHendelse: JournalpostHendelse) {
        tilstand.mottaJournalpost(this, journalpostHendelse)
    }

    fun mottaKvitteringPåUtsending(utsendingKvitteringHendelse: UtsendingKvitteringHendelse) {
        tilstand.mottaKvitteringPåUtsending(this, utsendingKvitteringHendelse)
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
        }
    }

    object AvventerArkiverbarVersjonAvBrev : Tilstand {
        override val type = Tilstand.Type.AvventerArkiverbarVersjonAvBrev

        override fun behov(utsending: Utsending) =
            ArkiverbartBrevBehov(
                navn = "pdfPlease",
                html = utsending.brev ?: throw IllegalStateException("Brev mangler"),
            )

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            utsending.tilstand = AvventerMidlertidigJournalføring
        }
    }

    object AvventerMidlertidigJournalføring : Tilstand {
        override val type = Tilstand.Type.AvventerMidlertidigJournalføring

        override fun mottaMidlertidigJournalpost(
            utsending: Utsending,
            midlertidigJournalpostHendelse: MidlertidigJournalpostHendelse,
        ) {
            utsending.tilstand = AvventerJournalføring
        }
    }

    object AvventerJournalføring : Tilstand {
        override val type = Tilstand.Type.AvventerJournalføring

        override fun mottaJournalpost(
            utsending: Utsending,
            journalpostHendelse: JournalpostHendelse,
        ) {
            if (journalpostHendelse.journalpostId != utsending.journalpostId) {
                // TODO: Her må det tenkes mer på hva som skal skje
                throw IllegalArgumentException("JournalpostId mismatch")
            }
            utsending.tilstand = AvventerDistribuering
        }
    }

    object AvventerDistribuering : Tilstand {
        override val type = Tilstand.Type.AvventerDistribuering

        override fun mottaKvitteringPåUtsending(
            utsending: Utsending,
            utsendingKvitteringHendelse: UtsendingKvitteringHendelse,
        ) {
            if (utsendingKvitteringHendelse.utsendingId != utsending.id) {
                // TODO: Sanity check av noe slag
            }
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

        fun mottaMidlertidigJournalpost(
            utsending: Utsending,
            midlertidigJournalpostHendelse: MidlertidigJournalpostHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta midlertidig journalpost i tilstand: ${this.type}")
        }

        fun mottaJournalpost(
            utsending: Utsending,
            journalpostHendelse: JournalpostHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta journalpost i tilstand: ${this.type}")
        }

        fun mottaKvitteringPåUtsending(
            utsending: Utsending,
            utsendingKvitteringHendelse: UtsendingKvitteringHendelse,
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
            AvventerMidlertidigJournalføring,
            AvventerJournalføring,
            AvventerDistribuering,
            Distribuert,
        }

        class UlovligUtsendingTilstandsendring(message: String) : RuntimeException(message)
    }
}
