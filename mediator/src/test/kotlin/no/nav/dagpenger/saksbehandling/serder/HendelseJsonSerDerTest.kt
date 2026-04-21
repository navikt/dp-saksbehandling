package no.nav.dagpenger.saksbehandling.serder

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.FjernOppgaveAnsvarÅrsak
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.ReturnerTilSaksbehandlingÅrsak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SkriptHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.stream.Stream

class HendelseJsonSerDerTest {
    private val aUUID = "018fa4fe-8450-7fa2-9e47-cafa81f718cd"

    private fun behandlingOppretteHendelse(utførtAv: Behandler): BehandlingOpprettetHendelse {
        val uuid = UUID.fromString(aUUID)
        return BehandlingOpprettetHendelse(
            behandlingId = uuid,
            ident = "1234",
            sakId = uuid,
            opprettet = LocalDateTime.MIN.truncatedTo(ChronoUnit.HOURS),
            type = HendelseBehandler.DpBehandling.Søknad,
            utførtAv = utførtAv,
        )
    }

    companion object {
        private val aUUID = "018fa4fe-8450-7fa2-9e47-cafa81f718cd"
        private val fixedUUID = UUID.fromString(aUUID)
        private val fixedTime = LocalDateTime.MIN.truncatedTo(ChronoUnit.HOURS)

        @JvmStatic
        fun roundtripHendelser(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "TomHendelse",
                    TomHendelse,
                ),
                Arguments.of(
                    "SkriptHendelse",
                    SkriptHendelse(utførtAv = Applikasjon.Generell("test-skript")),
                ),
                Arguments.of(
                    "SøknadsbehandlingOpprettetHendelse",
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = fixedUUID,
                        behandlingId = fixedUUID,
                        ident = "ident",
                        opprettet = fixedTime,
                    ),
                ),
                Arguments.of(
                    "BehandlingOpprettetHendelse",
                    BehandlingOpprettetHendelse(
                        behandlingId = fixedUUID,
                        ident = "1234",
                        sakId = fixedUUID,
                        opprettet = fixedTime,
                        type = HendelseBehandler.DpBehandling.Søknad,
                        utførtAv = Applikasjon.DpMottak,
                    ),
                ),
                Arguments.of(
                    "VedtakFattetHendelse",
                    VedtakFattetHendelse(
                        behandlingId = fixedUUID,
                        behandletHendelseId = fixedUUID.toString(),
                        behandletHendelseType = "Søknad",
                        ident = "12345678901",
                        sak = UtsendingSak(id = fixedUUID.toString()),
                    ),
                ),
                Arguments.of(
                    "ReturnerTilSaksbehandlingHendelse",
                    ReturnerTilSaksbehandlingHendelse(
                        oppgaveId = fixedUUID,
                        årsak = ReturnerTilSaksbehandlingÅrsak.FEIL_HJEMMEL,
                        utførtAv =
                            Saksbehandler(
                                navIdent = "navIdent",
                                grupper = emptySet(),
                                tilganger = setOf(TilgangType.SAKSBEHANDLER),
                            ),
                    ),
                ),
                Arguments.of(
                    "FjernOppgaveAnsvarHendelse",
                    FjernOppgaveAnsvarHendelse(
                        oppgaveId = fixedUUID,
                        årsak = FjernOppgaveAnsvarÅrsak.MANGLER_KOMPETANSE,
                        utførtAv =
                            Saksbehandler(
                                navIdent = "navIdent",
                                grupper = emptySet(),
                                tilganger = setOf(TilgangType.SAKSBEHANDLER),
                            ),
                    ),
                ),
            )

        @JvmStatic
        fun dpBehandlingVarianter(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    HendelseBehandler.DpBehandling.Ferietillegg,
                    fixedUUID,
                    "ekstern-123",
                ),
                Arguments.of(
                    HendelseBehandler.DpBehandling.Revurdering,
                    null,
                    null,
                ),
                Arguments.of(
                    HendelseBehandler.DpBehandling.Arbeidssøkerperiode,
                    null,
                    null,
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("roundtripHendelser")
    fun `rehydrerHendelse skal serialisere og deserialisere alle hendelsestyper`(
        hendelseType: String,
        hendelse: Hendelse,
    ) {
        val json = hendelse.tilJson()
        val rehydrert = rehydrerHendelse(hendelseType, json)
        rehydrert shouldBe hendelse
    }

    @ParameterizedTest
    @MethodSource("dpBehandlingVarianter")
    fun `Kan serialisere og deserialisere DpBehandlingOpprettetHendelse`(
        type: HendelseBehandler.DpBehandling,
        basertPåBehandling: UUID?,
        eksternId: String?,
    ) {
        val hendelse =
            DpBehandlingOpprettetHendelse(
                behandlingId = fixedUUID,
                ident = "1234",
                opprettet = fixedTime,
                basertPåBehandling = basertPåBehandling,
                behandlingskjedeId = fixedUUID,
                type = type,
                eksternId = eksternId,
            )

        val json = hendelse.tilJson()
        val deserialisert = json.tilHendelse<DpBehandlingOpprettetHendelse>()
        deserialisert shouldBe hendelse
        deserialisert.type shouldBe type
    }

    @Test
    fun `Kan serialisere og deserialisere BehandlingOpprettetHendelse med polymorf utførtAv`() {
        behandlingOppretteHendelse(utførtAv = Applikasjon.DpMottak).let { hendelse ->
            val json = hendelse.tilJson()
            json shouldEqualJson
                //language=Json
                """
             { 
                "behandlingId": "$aUUID",
                "ident": "1234",
                "sakId": "${hendelse.sakId}",
                "opprettet": "-999999999-01-01T00:00:00",
                "type": "SØKNAD",
                "utførtAv": { "navn": "dp-mottak" }
             }
            """
            json.tilHendelse<BehandlingOpprettetHendelse>() shouldBe hendelse
        }

        behandlingOppretteHendelse(
            utførtAv =
                Saksbehandler(
                    navIdent = "navIdent",
                    grupper = setOf("gruppe1", "gruppe2"),
                    tilganger = setOf(TilgangType.BESLUTTER, TilgangType.SAKSBEHANDLER),
                ),
        ).let { hendelse ->
            val json = hendelse.tilJson()
            json shouldEqualJson
                //language=Json
                """
             { 
                "behandlingId": "$aUUID",
                "ident": "1234",
                "opprettet": "-999999999-01-01T00:00:00",
                "type": "SØKNAD",
                "sakId": "$aUUID",
                "utførtAv": {
                  "navIdent": "navIdent",
                  "grupper": [
                    "gruppe1",
                    "gruppe2"
                  ],
                  "tilganger": [
                    "BESLUTTER",
                    "SAKSBEHANDLER"
                  ]
                }              
             }
            """
            json.tilHendelse<BehandlingOpprettetHendelse>() shouldBe hendelse
        }
    }

    @Test
    fun `skal ignorere ukjente felter ved deserialisering av Json`() {
        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = UUIDv7.ny(),
                behandletHendelseId = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
                ident = "12345678901",
                sak =
                    UtsendingSak(
                        id = UUIDv7.ny().toString(),
                    ),
                automatiskBehandlet = false,
            )
        val json =
            """
            {
                "behandlingId": "${vedtakFattetHendelse.behandlingId}",
                "behandletHendelseId": "${vedtakFattetHendelse.behandletHendelseId}",
                "behandletHendelseType": "${vedtakFattetHendelse.behandletHendelseType}",
                "ident": "${vedtakFattetHendelse.ident}",
                "sak": {
                    "id": "${vedtakFattetHendelse.sak!!.id}",
                    "kontekst": "Arena"
                },
                "automatiskBehandlet": ${vedtakFattetHendelse.automatiskBehandlet},
                "ukjentFelt": "Dette feltet skal ignoreres"
            }
            """.trimIndent()

        json.tilHendelse<VedtakFattetHendelse>() shouldBe vedtakFattetHendelse
    }

    @Test
    fun `Skal deserialisere ReturnerTilSaksbehandlingHendelse med default årsak`() {
        val oppgaveId = UUIDv7.ny()
        val saksbehandler =
            Saksbehandler(
                navIdent = "navIdent",
                grupper = emptySet(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )

        //language=Json
        """{
              "oppgaveId": "$oppgaveId",
              "utførtAv": {
                  "navIdent": "navIdent",
                  "grupper": [],
                  "tilganger": [ "SAKSBEHANDLER" ]
              }
            }
            """.tilHendelse<ReturnerTilSaksbehandlingHendelse>() shouldBe
            ReturnerTilSaksbehandlingHendelse(
                oppgaveId = oppgaveId,
                årsak = ReturnerTilSaksbehandlingÅrsak.ANNET,
                utførtAv = saksbehandler,
            )
    }

    @Test
    fun `Skal deserialisere FjernOppgaveAnsvarHendelse med default årsak`() {
        val oppgaveId = UUIDv7.ny()
        val saksbehandler =
            Saksbehandler(
                navIdent = "navIdent",
                grupper = emptySet(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )

        //language=Json
        """{
              "oppgaveId": "$oppgaveId",
              "utførtAv": {
                  "navIdent": "navIdent",
                  "grupper": [],
                  "tilganger": [ "SAKSBEHANDLER" ]
              }
            }
            """.tilHendelse<FjernOppgaveAnsvarHendelse>() shouldBe
            FjernOppgaveAnsvarHendelse(
                oppgaveId = oppgaveId,
                årsak = FjernOppgaveAnsvarÅrsak.ANNET,
                utførtAv = saksbehandler,
            )
    }

    @Test
    fun `rehydrerHendelse-registeret finner alle konkrete Hendelse-subklasser`() {
        val alleKonkreteTyper = finnAlleKonkreteSubklasser(Hendelse::class)
        alleKonkreteTyper.size shouldNotBe 0

        for (klass in alleKonkreteTyper) {
            val typeName = klass.simpleName!!
            // Verifiser at rehydrerHendelse kjenner til typen — Jackson-feil er OK, "Ukjent hendelse type" er ikke
            val result = runCatching { rehydrerHendelse(typeName, "{}") }
            result.exceptionOrNull()?.let { error ->
                if (error is IllegalArgumentException && error.message?.contains("Ukjent hendelse type") == true) {
                    throw AssertionError("$typeName mangler i hendelse-registeret", error)
                }
            }
        }
    }

    private fun finnAlleKonkreteSubklasser(klass: kotlin.reflect.KClass<out Hendelse>): List<kotlin.reflect.KClass<out Hendelse>> =
        klass.sealedSubclasses.flatMap { sub ->
            if (sub.isSealed) finnAlleKonkreteSubklasser(sub) else listOf(sub)
        }
}
