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
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test

class KlageDTOMapperTest {
    private val testSaksbehandler =
        Saksbehandler(
            navIdent = "12345612345",
            grupper = setOf(Configuration.saksbehandlerADGruppe),
            tilganger = setOf(TilgangType.SAKSBEHANDLER),
        )

    @Test
    fun `Skal mappe KlageBehandling til KlageDTO`() {
        runBlocking {
            val klageBehandling = KlageBehandling()
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
                        klageBehandling = klageBehandling,
                        saksbehandler = testSaksbehandler,
                    )
                //language=JSON
                objectMapper.writeValueAsString(klageDTO) shouldEqualJson
                    """
                    {
                      "behandlingId": "${klageBehandling.behandlingId}",
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
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER }.type.påkrevd},
      "gruppe": "KLAGESAK",
      "valgmuligheter": [
        "Avslag på søknad",
        "Dagpengenes størrelse",
        "Annet"
      ],
      "redigerbar": true,
      "type": "FLER_LISTEVALG"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.type.påkrevd},
      "gruppe": "KLAGESAK",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "TEKST"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.type.påkrevd},
      "gruppe": "FRIST",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "DATO"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.type.påkrevd},
      "gruppe": "FRIST",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "DATO"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.type.påkrevd},
      "gruppe": "FRIST",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "BOOLSK"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_SKRIFTLIG }.type.påkrevd},
      "gruppe": "FORMKRAV",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "BOOLSK"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.ER_KLAGEN_UNDERSKREVET }.type.påkrevd},
      "gruppe": "FORMKRAV",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "BOOLSK"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_NEVNER_ENDRING }.type.påkrevd},
      "gruppe": "FORMKRAV",
      "valgmuligheter": [],
      "redigerbar": true,
      "type": "BOOLSK"
    },
    {
      "opplysningId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.opplysningId}",
      "opplysningNavnId": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.name}",
      "navn": "${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.navn}",
      "paakrevd": ${klageBehandling.synligeOpplysninger().single { it.type == OpplysningType.RETTSLIG_KLAGEINTERESSE }.type.påkrevd},
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
