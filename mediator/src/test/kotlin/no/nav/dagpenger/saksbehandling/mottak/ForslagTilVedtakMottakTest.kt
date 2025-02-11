package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ALDER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_STREIK
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_UTDANNING
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.AVSLAG_UTESTENGT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_KONKURS
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_ORDINÆR
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK
import no.nav.dagpenger.saksbehandling.mottak.Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT
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
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag minsteinntekt og rettighet permittering`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagMinsteinntektJson)

        val hendelse = slot<ForslagTilVedtakHendelse>()
        verify(exactly = 1) {
            oppgaveMediator.settOppgaveKlarTilBehandling(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.søknadId shouldBe søknadId
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.emneknagger shouldBe setOf(AVSLAG.navn, AVSLAG_MINSTEINNTEKT.navn, RETTIGHET_PERMITTERT.navn)
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag pga alder og rettighet ordinær`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagAlderJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf(AVSLAG.navn, AVSLAG_ALDER.navn, RETTIGHET_ORDINÆR.navn),
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
                            AVSLAG.navn, AVSLAG_MINSTEINNTEKT.navn, AVSLAG_ARBEIDSINNTEKT.navn, AVSLAG_ARBEIDSTID.navn, AVSLAG_ALDER.navn,
                            AVSLAG_ANDRE_YTELSER.navn, AVSLAG_MEDLEMSKAP.navn, AVSLAG_STREIK.navn, AVSLAG_OPPHOLD_UTLAND.navn,
                            AVSLAG_REELL_ARBEIDSSØKER.navn, AVSLAG_IKKE_REGISTRERT.navn, AVSLAG_UTESTENGT.navn, AVSLAG_UTDANNING.navn,
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
                    emneknagger = setOf(INNVILGELSE.navn, RETTIGHET_VERNEPLIKT.navn),
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
                    emneknagger = setOf(INNVILGELSE.navn, RETTIGHET_ORDINÆR.navn),
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
                    emneknagger = setOf(INNVILGELSE.navn, RETTIGHET_PERMITTERT.navn),
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
                    emneknagger = setOf(INNVILGELSE.navn, RETTIGHET_PERMITTERT_FISK.navn),
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
                    emneknagger = setOf(INNVILGELSE.navn, RETTIGHET_KONKURS.navn),
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
                    emneknagger = setOf(INNVILGELSE.navn),
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
              "navn": "Oppfyller kravet til minsteinntekt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
            "opplysninger": [
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
            "opplysninger": [
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
                  "navn": "Oppfyller kravet til minsteinntekt",
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
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
              "navn": "Oppfyller kravet til minsteinntekt",
              "status": "Oppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
            "opplysninger": [
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d8a",
                  "navn": "Har rett til ordinære dagpenger",
                  "verdi": "true"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d86",
                  "navn": "Har rett til dagpenger under permittering",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d88",
                  "navn": "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "0194881f-9444-7a73-a458-0af81c034d87",
                  "navn": "Har rett til dagpenger etter konkurs",
                  "verdi": "false"
                },
                {
                  "opplysningTypeId": "01948d43-e218-76f1-b29b-7e604241d98a",
                  "navn": "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
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
              "navn": "Oppfyller kravet til minsteinntekt",
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
