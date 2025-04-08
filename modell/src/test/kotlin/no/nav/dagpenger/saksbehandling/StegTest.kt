package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGE_FRIST
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.fristvurderingOpplysningTyper
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StegTest {
    @Test
    fun `Opprett fristvurdering steg`() {
        val steg = FristvurderingSteg()

        steg.hentOpplysninger().map { it.type } shouldBe fristvurderingOpplysningTyper

        steg.hentOpplysninger().single { it.type == KLAGE_MOTTATT }.svar(verdi = LocalDate.now())
        steg.hentOpplysninger().single { it.type == KLAGE_FRIST }.svar(verdi = LocalDate.now().plusDays(1))
        val klagefristOppfylt = steg.hentOpplysninger().single { it.type == KLAGEFRIST_OPPFYLT }
        klagefristOppfylt.svar(verdi = true)
        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe true
        klagefristOppfylt.svar(verdi = false)
        steg.hentOpplysninger().size shouldBe 5
    }
}
