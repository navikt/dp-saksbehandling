package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import org.junit.jupiter.api.Test

class ForslagTilVedtakMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val behandlingId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val ident = "123456678912"

    init {
        ForslagTilVedtakMottak(testRapid, oppgaveMediator)
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag minsteinntekt`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagMinsteinntektJson)

        val hendelse = slot<ForslagTilVedtakHendelse>()
        verify(exactly = 1) {
            oppgaveMediator.settOppgaveKlarTilBehandling(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.søknadId shouldBe søknadId
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.emneknagger shouldBe setOf("Avslag minsteinntekt")
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag pga alder`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagAlderJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Avslag alder"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med flere avslagsgrunner`() {
        testRapid.sendTestMessage(forslagTilVedtakFlereAvslagsgrunnerJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger =
                        setOf(
                            "Avslag minsteinntekt", "Avslag arbeidsinntekt", "Avslag arbeidstid", "Avslag alder",
                            "Avslag andre ytelser", "Avslag medlemskap", "Avslag streik", "Avslag opphold utland",
                            "Avslag reell arbeidssøker", "Avslag ikke registrert", "Avslag utestengt", "Avslag utdanning",
                        ),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse av dagpenger etter verneplikt`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelseVernepliktJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse verneplikt"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse av ordinære dagpenger`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelseOrdinærJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse ordinær"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse av dagpenger under permittering`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelsePermitteringJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse permittering"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse av dagpenger under permittering fra fiskeindustri`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelsePermitteringFiskJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse permittering fisk"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse av dagpenger etter konkurs`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelseEtterKonkursJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse etter konkurs"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilget ukjent rettighet - regelmotor har laget noe vi ikke kjenner`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelseUkjentTypeJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    //language=json
    private val forslagTilVedtakAvslagMinsteinntektJson =
        """
        {
          "@event_name": "forslag_til_vedtak",
          "prøvingsdato": "2024-12-01",
          "fastsatt": {
            "utfall": false
          },
          "vilkår": [
            {
              "navn": "Oppfyller kravet til alder",
              "status": "Oppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
              "hjemmel": "folketrygdloven § 4-23"
            },
            {
              "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "gjelderDato": "2024-11-19",
          "søknadId": "$søknadId",
          "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelseVernepliktJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 372084,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 893,
                    "dagsats": 893,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Verneplikt",
                        "type": "uker",
                        "verdi": 26
                    }, {
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 2679
                    }
                ]
            },
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelseOrdinærJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 500000,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 1200,
                    "dagsats": 1200,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Dagpengeperiode",
                        "type": "uker",
                        "verdi": 104
                    }, {
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 3600
                    }
                ]
            },
            "opplysninger": [
                {
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "true"
                },
                {
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelsePermitteringJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 500000,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 1200,
                    "dagsats": 1200,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Dagpengeperiode",
                        "type": "uker",
                        "verdi": 104
                    }, {
                        "navn": "Permittering",
                        "type": "uker",
                        "verdi": 26
                    },{
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 3600
                    }
                ]
            },
            "opplysninger": [
                {
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "true"
                },
                {
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelsePermitteringFiskJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 500000,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 1200,
                    "dagsats": 1200,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Dagpengeperiode",
                        "type": "uker",
                        "verdi": 104
                    }, {
                        "navn": "Fiskepermittering",
                        "type": "uker",
                        "verdi": 26
                    },{
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 3600
                    }
                ]
            },
            "opplysninger": [
                {
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "true"
                },
                {
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelseEtterKonkursJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 500000,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 1200,
                    "dagsats": 1200,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Dagpengeperiode",
                        "type": "uker",
                        "verdi": 104
                    }, {
                        "navn": "Lønnsgarantiperiode",
                        "type": "uker",
                        "verdi": 26
                    },{
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 3600
                    }
                ]
            },
            "opplysninger": [
                {
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "true"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelseUkjentTypeJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "fastsatt": {
                "utfall": true,
                "grunnlag": {
                    "grunnlag": 500000,
                    "begrunnelse": null
                },
                "fastsattVanligArbeidstid": {
                    "vanligArbeidstidPerUke": 37.5,
                    "nyArbeidstidPerUke": 0.0,
                    "begrunnelse": null
                },
                "sats": {
                    "dagsatsMedBarnetillegg": 1200,
                    "dagsats": 1200,
                    "begrunnelse": null,
                    "barn": [
                    ]
                },
                "kvoter": [ {
                        "navn": "Dagpengeperiode",
                        "type": "uker",
                        "verdi": 104
                    }, {
                        "navn": "Egenandel",
                        "type": "beløp",
                        "verdi": 3600
                    }
                ]
            },
            "opplysninger": [
                {
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakAvslagAlderJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "fastsatt": {
              "utfall": false
            },
            "vilkår": [
            {
              "navn": "Oppfyller kravet til alder",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
              "hjemmel": "folketrygdloven § 4-23"
            },
            {
              "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
              "status": "Oppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakFlereAvslagsgrunnerJson =
        """
        {
          "@event_name": "forslag_til_vedtak",
          "behandlingId": "$behandlingId",
          "fagsakId": "123",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "vedtakstidspunkt": "2025-01-08T13:28:28.164624",
          "virkningsdato": "2021-05-06",
          "behandletAv": [],
          "vilkår": [
            {
              "navn": "Oppfyller kravet til alder",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:25.847435",
              "hjemmel": "folketrygdloven § 4-23"
            },
            {
              "navn": "Registrert som arbeidssøker på søknadstidspunktet",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:25.976851",
              "hjemmel": "folketrygdloven § 4-8"
            },
            {
              "navn": "Oppfyller kravet til minsteinntekt eller verneplikt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.284199",
              "hjemmel": "folketrygdloven § 4-4"
            },
            {
              "navn": "Mottar ikke andre fulle ytelser",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.285808",
              "hjemmel": "folketrygdloven § 4-24"
            },
            {
              "navn": "Oppfyller kravet til medlemskap",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.291214",
              "hjemmel": "folketrygdloven § 4-2"
            },
            {
              "navn": "Oppfyller kravet til opphold i Norge",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.29125",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Er medlemmet ikke påvirket av streik eller lock-out?",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.291276",
              "hjemmel": "folketrygdloven § 4-22"
            },
            {
              "navn": "Oppfyller krav til ikke utestengt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.2923",
              "hjemmel": "folketrygdloven § 4-28"
            },
            {
              "navn": "Krav til tap av arbeidsinntekt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.297548",
              "hjemmel": "folketrygdloven § 4-3"
            },
            {
              "navn": "Tap av arbeidstid er minst terskel",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.319579",
              "hjemmel": "folketrygdloven § 4-3"
            },
            {
              "navn": "Oppfyller kravet til heltid- og deltidsarbeid",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.484995",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Oppfyller kravet til mobilitet",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.485021",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Oppfyller kravet til å være arbeidsfør",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.485033",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Oppfyller kravet til å ta ethvert arbeid",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.485043",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Krav til arbeidssøker",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.487577",
              "hjemmel": "folketrygdloven § 4-5"
            },
            {
              "navn": "Krav til utdanning eller opplæring",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2025-01-08T13:28:26.624419",
              "hjemmel": "folketrygdloven § 4-6"
            }
          ],
          "fastsatt": {
            "utfall": false
          },
          "@id": "ac96f190-9b18-4823-9f7d-5f7f68568e38",
          "@opprettet": "2025-01-08T13:28:28.243249",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "ac96f190-9b18-4823-9f7d-5f7f68568e38",
              "time": "2025-01-08T13:28:28.243249"
            }
          ]
        }
        """.trimIndent()
}
