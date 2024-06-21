package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.toUrnOrNull
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class Utsending(
    val id: UUID = UUIDv7.ny(),
    val oppgaveId: UUID,
    val ident: String,
    private var sak: Sak? = null,
    private var brev: String? = null,
    private var pdfUrn: URN? = null,
    private var journalpostId: String? = null,
    private var distribusjonId: String? = null,
    private var tilstand: Tilstand = Opprettet,
) {
    fun brev(): String? = brev

    fun pdfUrn(): URN? = pdfUrn

    fun journalpostId(): String? = journalpostId

    fun distribusjonId(): String? = distribusjonId

    fun tilstand() = tilstand

    fun sak() = sak

    override fun toString(): String {
        return """
            Utsending(id=$id, oppgaveId=$oppgaveId, pdfUrn=$pdfUrn, journalpostId=$journalpostId, 
            distribusjonId=$distribusjonId , tilstand=${tilstand.type})
            """.trimIndent()
    }

    companion object {
        fun rehydrer(
            id: UUID,
            oppgaveId: UUID,
            ident: String,
            tilstand: Tilstand,
            brev: String?,
            pdfUrn: String?,
            journalpostId: String?,
            distribusjonId: String?,
            sak: Sak?,
        ): Utsending {
            return Utsending(
                id = id,
                oppgaveId = oppgaveId,
                ident = ident,
                tilstand = tilstand,
                brev = brev,
                pdfUrn = pdfUrn.toUrnOrNull(),
                journalpostId = journalpostId,
                distribusjonId = distribusjonId,
                sak = sak,
            )
        }
    }

    fun startUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        tilstand.mottaStartUtsendingHendelse(this, startUtsendingHendelse)
    }

    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        this.brev = vedtaksbrevHendelse.brev
        tilstand.mottaBrev(this, vedtaksbrevHendelse)
    }

    fun mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse: ArkiverbartBrevHendelse) {
        pdfUrn = arkiverbartBrevHendelse.pdfUrn
        tilstand.mottaUrnTilPdfAvBrev(this, arkiverbartBrevHendelse)
    }

    fun mottaJournalførtKvittering(journalførtHendelse: JournalførtHendelse) {
        tilstand.mottaJournalførtKvittering(this, journalførtHendelse)
    }

    fun mottaDistribuertKvittering(distribuertHendelse: DistribuertHendelse) {
        tilstand.mottaDistribuertKvittering(this, distribuertHendelse)
    }

    object Opprettet : Tilstand {
        override val type = Tilstand.Type.Opprettet

        override fun mottaBrev(
            utsending: Utsending,
            vedtaksbrevHendelse: VedtaksbrevHendelse,
        ) {
            logger.info { "Mottok brev for oppgaveId: ${vedtaksbrevHendelse.oppgaveId}" }
            utsending.tilstand = VenterPåVedtak
        }
    }

    object VenterPåVedtak : Tilstand {
        override val type = Tilstand.Type.VenterPåVedtak

        override fun mottaStartUtsendingHendelse(
            utsending: Utsending,
            startUtsendingHendelse: StartUtsendingHendelse,
        ) {
            logger.info { "Mottok start_utsending for oppgaveId: ${startUtsendingHendelse.oppgaveId}" }
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
                ident = utsending.ident,
            )

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            logger.info {
                "Mottok arkiverbart dokument med urn: ${arkiverbartBrevHendelse.pdfUrn}" +
                    " for oppgaveId: ${arkiverbartBrevHendelse.oppgaveId}"
            }
            utsending.tilstand = AvventerJournalføring
        }
    }

    object AvventerJournalføring : Tilstand {
        override val type = Tilstand.Type.AvventerJournalføring

        override fun behov(utsending: Utsending): Behov {
            return JournalføringBehov(
                oppgaveId = utsending.oppgaveId,
                pdfUrn = utsending.pdfUrn ?: throw IllegalStateException("pdfUrn mangler"),
                ident = utsending.ident,
                sak = utsending.sak ?: throw IllegalStateException("Sak mangler"),
            )
        }

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            if (utsending.pdfUrn == arkiverbartBrevHendelse.pdfUrn) {
                logger.warn {
                    "Mystisk! Fikk pdfUrn på nytt. Den var lik. ArkiverbartBrevHendelse: $arkiverbartBrevHendelse " +
                        "Utsending =$utsending"
                }
            } else {
                logger.warn {
                    "Mystisk! Fikk pdfUrn på nytt. Den var ulik. Utsending.pdfurn = ${utsending.pdfUrn} " +
                        "ArkiverbartBrevHendelse: $arkiverbartBrevHendelse Utsending =$utsending "
                }
            }
        }

        override fun mottaJournalførtKvittering(
            utsending: Utsending,
            journalførtHendelse: JournalførtHendelse,
        ) {
            logger.info {
                "Mottok journalført kvittering med journalpostId: ${journalførtHendelse.journalpostId}" +
                    " for oppgaveId: ${journalførtHendelse.oppgaveId}"
            }
            utsending.journalpostId = journalførtHendelse.journalpostId
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

        override fun mottaDistribuertKvittering(
            utsending: Utsending,
            distribuertHendelse: DistribuertHendelse,
        ) {
            logger.info {
                "Mottok distribuert kvittering med distribusjonId: ${distribuertHendelse.distribusjonId}" +
                    " for oppgaveId: ${distribuertHendelse.oppgaveId}"
            }
            // TODO: Sanity check av noe slag
            utsending.distribusjonId = distribuertHendelse.distribusjonId
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
            throw UlovligUtsendingTilstandsendring("Kan ikke motta urn til arkiverbart brev i tilstand: ${this.type}")
        }

        fun mottaJournalførtKvittering(
            utsending: Utsending,
            journalførtHendelse: JournalførtHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta kvittering på journalføring i tilstand: ${this.type}")
        }

        fun mottaDistribuertKvittering(
            utsending: Utsending,
            distribuertHendelse: DistribuertHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke motta kvittering på distribusjon i tilstand: ${this.type}")
        }

        fun mottaStartUtsendingHendelse(
            utsending: Utsending,
            startUtsendingHendelse: StartUtsendingHendelse,
        ) {
            throw UlovligUtsendingTilstandsendring("Kan ikke starte utsending i tilstand: ${this.type}")
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
