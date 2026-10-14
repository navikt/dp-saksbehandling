package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggDTO
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggKategoriDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.UtlostAvTypeDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveOversikt
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OppgaveOversiktDTOMapperTest {
    @Test
    fun `skal mappe oppgave oversikt til DTO riktig`() {
        val now = LocalDate.now()
        val atStartOfDay = now.atStartOfDay()
        val oppgaveOversikt =
            OppgaveOversikt(
                oppgaveId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                personIdent = "detraxit",
                behandlerIdent = "dicant",
                saksbehandlerIdent = "discere",
                beslutterIdent = "libero",
                tidspunktOpprettet = atStartOfDay,
                utlostAv = HendelseBehandler.DpBehandling.Søknad,
                emneknagger = setOf("e1"),
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                utsattTilDato = now,
                totaltFeilutbetaltBelop = 2.3,
                sendtTilKontroll = atStartOfDay,
            )

        oppgaveOversikt.tilOppgaveOversiktDTO().let { dto ->
            dto.oppgaveId shouldBe oppgaveOversikt.oppgaveId
            dto.behandlingId shouldBe oppgaveOversikt.behandlingId
            dto.personIdent shouldBe oppgaveOversikt.personIdent
            dto.behandlerIdent shouldBe oppgaveOversikt.behandlerIdent
            dto.saksbehandlerIdent shouldBe oppgaveOversikt.saksbehandlerIdent
            dto.beslutterIdent shouldBe oppgaveOversikt.beslutterIdent
            dto.tidspunktOpprettet shouldBe oppgaveOversikt.tidspunktOpprettet
            dto.utlostAv shouldBe UtlostAvTypeDTO.SØKNAD
            dto.emneknagger shouldBe
                listOf(
                    EmneknaggDTO(
                        visningsnavn = "e1",
                        kategori = EmneknaggKategoriDTO.UDEFINERT,
                    ),
                )
            dto.skjermesSomEgneAnsatte shouldBe oppgaveOversikt.skjermesSomEgneAnsatte
            dto.adressebeskyttelseGradering shouldBe AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
            dto.tilstand shouldBe OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
            dto.utsattTilDato shouldBe oppgaveOversikt.utsattTilDato
            dto.totaltFeilutbetaltBelop shouldBe oppgaveOversikt.totaltFeilutbetaltBelop
            dto.sendtTilKontroll shouldBe oppgaveOversikt.sendtTilKontroll
        }
    }
}
