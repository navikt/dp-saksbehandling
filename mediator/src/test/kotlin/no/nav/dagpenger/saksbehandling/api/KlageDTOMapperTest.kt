package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.klage.Klage
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageDTOMapperTest {
    private val testSaksbehandler =
        Saksbehandler(
            navIdent = "12345612345",
            grupper = setOf(Configuration.saksbehandlerADGruppe),
            tilganger = setOf(TilgangType.SAKSBEHANDLER),
        )
    private val opprettet = LocalDateTime.of(2025, 1, 1, 1, 1)

    @Test
    fun `Skal mappe KlageBehandling til KlageDTO`() {
        runBlocking {
            val klage =
                Klage(
                    opprettet = opprettet,
                )
            val saksbehandlerDTO =
                BehandlerDTO(
                    ident = testSaksbehandler.navIdent,
                    fornavn = "Saksbehandler Fornavn",
                    etternavn = "Saksbehandler Etternavn",
                    enhet =
                        BehandlerDTOEnhetDTO(
                            navn = "Saksbehandler Enhetsnavn",
                            enhetNr = "Saksbehandler Enhetsnummer",
                            postadresse = "Saksbehandler Postadresse",
                        ),
                )
            KlageDTOMapper(
                oppslag =
                    mockk<Oppslag>().also {
                        coEvery { it.hentBehandler(ident = testSaksbehandler.navIdent) } returns saksbehandlerDTO
                    },
            ).let { mapper ->
                val klageDTO =
                    mapper.tilDto(
                        klage = klage,
                        saksbehandler = testSaksbehandler,
                    )
                //language=JSON
                objectMapper.writeValueAsString(klageDTO) shouldEqualJson
                    """
                    {
                        "behandlingId": "${klage.behandlingId}",
                        "saksbehandler": {
                            "ident": "${saksbehandlerDTO.ident}",
                            "fornavn": "${saksbehandlerDTO.fornavn}",
                            "etternavn": "${saksbehandlerDTO.etternavn}",
                            "enhet": {
                                "navn": "${saksbehandlerDTO.enhet.navn}",
                                "enhetNr": "${saksbehandlerDTO.enhet.enhetNr}",
                                "postadresse": "${saksbehandlerDTO.enhet.postadresse}"
                            }
                        },
                        "behandlingOpplysninger": [
                            {
                                "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.opplysningId}",
                                "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.name}",
                                "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.navn}",
                                "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.påkrevd},
                                "gruppe": "KLAGESAK",
                                "valgmuligheter": [
                                    "Avslag på søknad",
                                    "For lite utbetalt",
                                    "Vedtak om tilbakebetaling",
                                    "Annet"
                                ] ,
                                "redigerbar": true,
                                "type": "FLER_LISTEVALG"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.påkrevd},
                              "gruppe": "KLAGESAK",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "TEKST"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO }.type.påkrevd},
                              "gruppe": "KLAGESAK",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "DATO"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.påkrevd},
                              "gruppe": "FRIST",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "DATO"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.påkrevd},
                              "gruppe": "FRIST",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "DATO"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.påkrevd},
                              "gruppe": "FRIST",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "BOOLSK"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.påkrevd},
                              "gruppe": "FORMKRAV",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "BOOLSK"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.påkrevd},
                              "gruppe": "FORMKRAV",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "BOOLSK"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.påkrevd},
                              "gruppe": "FORMKRAV",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "BOOLSK"
                            },
                            {
                              "opplysningId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.opplysningId}",
                              "opplysningNavnId": "${klage.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.name}",
                              "navn": "${klage.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.navn}",
                              "paakrevd": ${klage.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.påkrevd},
                              "gruppe": "FORMKRAV",
                              "valgmuligheter": [],
                              "redigerbar": true,
                              "type": "BOOLSK"
                            }
                        ],
                        "utfallOpplysninger": [],
                        "utfall": {
                            "verdi": "IKKE_SATT",
                            "tilgjengeligeUtfall": [
                                "AVVIST",
                                "OPPRETTHOLDELSE",
                                "DELVIS_MEDHOLD",
                                "MEDHOLD"
                            ]
                        }
                    }
                    """
            }
        }
    }
}
