package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.toUrnOrNull
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

enum class UtsendingType(val brevTittel: String, val skjemaKode: String) {
    VEDTAK_DAGPENGER("Vedtak om dagpenger", "NAV-DAGPENGER-VEDTAK"),
    KLAGEMELDING("Klagebrev", "NAV-DAGPENGER-KLAGE"),
}

data class Utsending(
    val id: UUID = UUIDv7.ny(),
    val oppgaveId: UUID,
    val ident: String,
    val type: UtsendingType = UtsendingType.VEDTAK_DAGPENGER,
    private var utsendingSak: UtsendingSak? = null,
    private val brev: String,
    private var pdfUrn: URN? = null,
    private var journalpostId: String? = null,
    private var distribusjonId: String? = null,
    private var tilstand: Tilstand = VenterPåVedtak,
) {
    fun brev(): String = brev

    fun pdfUrn(): URN? = pdfUrn

    fun journalpostId(): String? = journalpostId

    fun distribusjonId(): String? = distribusjonId

    fun tilstand() = tilstand

    fun sak() = utsendingSak

    override fun toString(): String {
        return """
            Utsending(id=$id, oppgaveId=$oppgaveId, pdfUrn=$pdfUrn, journalpostId=$journalpostId, 
            distribusjonId=$distribusjonId , tilstand=${tilstand.type}, type = $type, sak=$utsendingSak)
            """.trimIndent()
    }

    companion object {
        private val counter =
            Counter.builder()
                .name("dp_saksbehandling_utsending_vedtaksbrev")
                .labelNames("type")
                .help("Antall vedtaksbrev sendt ut av type")
                .register(PrometheusRegistry.defaultRegistry)

        fun rehydrer(
            id: UUID,
            oppgaveId: UUID,
            ident: String,
            tilstand: Tilstand,
            brev: String,
            pdfUrn: String?,
            journalpostId: String?,
            distribusjonId: String?,
            utsendingSak: UtsendingSak?,
            type: UtsendingType,
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
                utsendingSak = utsendingSak,
                type = type,
            )
        }
    }

    fun startUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        tilstand.mottaStartUtsendingHendelse(this, startUtsendingHendelse)
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
        counter.labelValues("avslagMinsteinntekt").inc()
    }

    object VenterPåVedtak : Tilstand {
        override val type = Tilstand.Type.VenterPåVedtak

        override fun mottaStartUtsendingHendelse(
            utsending: Utsending,
            startUtsendingHendelse: StartUtsendingHendelse,
        ) {
            withLoggingContext(
                "oppgaveId" to startUtsendingHendelse.oppgaveId.toString(),
                "behandlingId" to startUtsendingHendelse.behandlingId.toString(),
            ) {
                logger.info { "Mottok start_utsending hendelse" }
                utsending.tilstand = AvventerArkiverbarVersjonAvBrev
                utsending.utsendingSak = startUtsendingHendelse.utsendingSak
            }
        }
    }

    object AvventerArkiverbarVersjonAvBrev : Tilstand {
        override val type = Tilstand.Type.AvventerArkiverbarVersjonAvBrev

        override fun behov(utsending: Utsending) =
            ArkiverbartBrevBehov(
                oppgaveId = utsending.oppgaveId,
                html = utsending.brev,
                ident = utsending.ident,
                utsendingSak = utsending.utsendingSak ?: throw IllegalStateException("Sak mangler"),
            )

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            withLoggingContext("oppgaveId" to arkiverbartBrevHendelse.oppgaveId.toString()) {
                logger.info { "Mottok arkiverbart dokument med urn: ${arkiverbartBrevHendelse.pdfUrn}" }
                utsending.tilstand = AvventerJournalføring
            }
        }
    }

    object AvventerJournalføring : Tilstand {
        override val type = Tilstand.Type.AvventerJournalføring

        override fun behov(utsending: Utsending): Behov {
            return JournalføringBehov(
                oppgaveId = utsending.oppgaveId,
                pdfUrn = utsending.pdfUrn ?: throw IllegalStateException("pdfUrn mangler"),
                ident = utsending.ident,
                utsendingSak = utsending.utsendingSak ?: throw IllegalStateException("Sak mangler"),
                utsendingType = utsending.type,
            )
        }

        override fun mottaUrnTilPdfAvBrev(
            utsending: Utsending,
            arkiverbartBrevHendelse: ArkiverbartBrevHendelse,
        ) {
            withLoggingContext(
                "oppgaveId" to arkiverbartBrevHendelse.oppgaveId.toString(),
            ) {
                if (utsending.pdfUrn == arkiverbartBrevHendelse.pdfUrn) {
                    logger.warn { "Mystisk! Fikk pdfUrn på nytt. Den var lik. Se sikkerlogg." }
                    sikkerlogger.warn {
                        "Mystisk! Fikk pdfUrn på nytt. Den var lik. ArkiverbartBrevHendelse: $arkiverbartBrevHendelse " +
                            "Utsending: $utsending"
                    }
                } else {
                    logger.warn { "Mystisk! Fikk pdfUrn på nytt. Den var ulik. Se sikkerlogg." }
                    sikkerlogger.warn {
                        "Mystisk! Fikk pdfUrn på nytt. Den var ulik. Utsending.pdfurn = ${utsending.pdfUrn} " +
                            "ArkiverbartBrevHendelse: $arkiverbartBrevHendelse Utsending: $utsending "
                    }
                }
            }
        }

        override fun mottaJournalførtKvittering(
            utsending: Utsending,
            journalførtHendelse: JournalførtHendelse,
        ) {
            withLoggingContext(
                "oppgaveId" to journalførtHendelse.oppgaveId.toString(),
                "journalpostId" to journalførtHendelse.journalpostId,
            ) {
                logger.info { "Mottok journalført kvittering" }
                utsending.journalpostId = journalførtHendelse.journalpostId
                utsending.tilstand = AvventerDistribuering
            }
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
            withLoggingContext(
                "oppgaveId" to utsending.oppgaveId.toString(),
                "distribusjonId" to distribuertHendelse.distribusjonId,
            ) {
                logger.info {
                    "Mottok distribuert kvittering"
                }
                // TODO: Sanity check av noe slag
                utsending.distribusjonId = distribuertHendelse.distribusjonId
                utsending.tilstand = Distribuert
            }
        }
    }

    object Avbrutt : Tilstand {
        override val type = Tilstand.Type.Avbrutt
    }

    object Distribuert : Tilstand {
        override val type = Tilstand.Type.Distribuert
    }

    interface Tilstand {
        fun behov(utsending: Utsending): Behov = IngenBehov

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
            VenterPåVedtak,
            AvventerArkiverbarVersjonAvBrev,
            AvventerJournalføring,
            AvventerDistribuering,
            Distribuert,
            Avbrutt,
        }

        class UlovligUtsendingTilstandsendring(message: String) : RuntimeException(message)
    }
}
