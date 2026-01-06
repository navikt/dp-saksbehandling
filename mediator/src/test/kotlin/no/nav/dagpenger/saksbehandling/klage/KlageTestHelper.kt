package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLING_UTFORT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun lagKlagebehandling(
    hjemler: List<Hjemler> = listOf(Hjemler.FTRL_4_2, Hjemler.FTRL_4_4, Hjemler.FTRL_4_3_2),
    land: Land? = Land.NO,
    behandlendeEnhet: String? = null,
    tilstand: KlageTilstand.Type = BEHANDLES,
    klageMottattTidspunkt: LocalDateTime = LocalDateTime.now(),
): KlageBehandling {
    val opplysninger =
        mutableSetOf(
            Opplysning(
                type = OpplysningType.KLAGEN_GJELDER,
                verdi = Verdi.Flervalg(listOf(KlagenGjelderType.AVSLAG_PÅ_SØKNAD.name)),
            ),
            Opplysning(
                type = OpplysningType.KLAGEN_GJELDER_VEDTAK,
                verdi = Verdi.TekstVerdi("Vedtak 1"),
            ),
            Opplysning(
                type = OpplysningType.ER_KLAGEN_SKRIFTLIG,
                verdi = Verdi.Boolsk(true),
            ),
            Opplysning(
                type = OpplysningType.ER_KLAGEN_UNDERSKREVET,
                verdi = Verdi.Boolsk(true),
            ),
            Opplysning(
                type = OpplysningType.KLAGEN_NEVNER_ENDRING,
                verdi = Verdi.Boolsk(true),
            ),
            Opplysning(
                type = OpplysningType.RETTSLIG_KLAGEINTERESSE,
                verdi = Verdi.Boolsk(true),
            ),
            Opplysning(
                type = OpplysningType.KLAGE_MOTTATT,
                verdi = Verdi.Dato(klageMottattTidspunkt.toLocalDate()),
            ),
            Opplysning(
                type = OpplysningType.KLAGEFRIST,
                verdi = Verdi.Dato(LocalDate.of(2025, 1, 7)),
            ),
            Opplysning(
                type = OpplysningType.KLAGEFRIST_OPPFYLT,
                verdi = Verdi.Boolsk(true),
            ),
            Opplysning(
                type = OpplysningType.FULLMEKTIG_NAVN,
                verdi = Verdi.TekstVerdi("Djevelens Advokat"),
            ),
            Opplysning(
                type = OpplysningType.UTFALL,
                verdi = Verdi.TekstVerdi("OPPRETTHOLDELSE"),
            ),
            Opplysning(
                type = OpplysningType.VURDERING_AV_KLAGEN,
                verdi = Verdi.TekstVerdi("Veldig fin klage :)"),
            ),
            Opplysning(
                type = OpplysningType.HVEM_KLAGER,
                verdi = Verdi.TekstVerdi("FULLMEKTIG"),
            ),
            Opplysning(
                type = OpplysningType.INTERN_MELDING,
                verdi = Verdi.TekstVerdi("Kuleste klagen jeg noensinne har sett"),
            ),
        )
    if (hjemler.isNotEmpty()) {
        opplysninger.add(
            Opplysning(type = OpplysningType.HJEMLER, verdi = Verdi.Flervalg(hjemler.map { it.tittel })),
        )
    }
    if (land != null) {
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_ADRESSE_1,
                verdi = Verdi.TekstVerdi("Sydenveien 1"),
            ),
        )
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_ADRESSE_2,
                verdi = Verdi.TekstVerdi("Poste restante"),
            ),
        )
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_ADRESSE_3,
                verdi = Verdi.TekstVerdi("Teisen postkontor"),
            ),
        )
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_POSTNR,
                verdi = Verdi.TekstVerdi("0666"),
            ),
        )
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_POSTSTED,
                verdi = Verdi.TekstVerdi("Oslo"),
            ),
        )
        opplysninger.add(
            Opplysning(
                type = OpplysningType.FULLMEKTIG_LAND,
                verdi = Verdi.TekstVerdi(land.name),
            ),
        )
    }

    return KlageBehandling.rehydrer(
        opplysninger = opplysninger,
        behandlendeEnhet = behandlendeEnhet,
        tilstand =
            when (tilstand) {
                BEHANDLES -> KlageBehandling.Behandles
                OVERSEND_KLAGEINSTANS -> KlageBehandling.OversendKlageinstans
                FERDIGSTILT -> KlageBehandling.Ferdigstilt
                AVBRUTT -> KlageBehandling.Avbrutt
                BEHANDLING_UTFORT -> KlageBehandling.BehandlingUtført
                BEHANDLES_AV_KLAGEINSTANS -> KlageBehandling.BehandlesAvKlageinstans
            },
        behandlingId = UUIDv7.ny(),
        journalpostId = null,
        opprettet = klageMottattTidspunkt,
    )
}

internal fun oversendtKlageinstansOk(
    behandlingId: UUID,
    fagsakId: UUID,
    ident: String,
): String {
    //language=JSON
    return """
         {
            "@event_name" : "behov",
            "@behov" : [ "OversendelseKlageinstans" ],
            "behandlingId" : "$behandlingId",
            "ident" : "$ident",
            "fagsakId" : "$fagsakId",
            "@løsning": {
              "OversendelseKlageinstans": "OK"
            }
        }
        """.trimIndent()
}
