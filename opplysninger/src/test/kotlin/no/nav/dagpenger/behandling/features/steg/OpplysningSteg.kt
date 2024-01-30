package no.nav.dagpenger.behandling.features.steg

import io.cucumber.java8.No
import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Hypotese
import no.nav.dagpenger.behandling.Opplysning
import no.nav.dagpenger.behandling.Opplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelmotor
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate

class OpplysningSteg : No {
    private lateinit var opplysning: Opplysning<*>
    private val opplysninger = Opplysninger(Regelmotor())

    init {
        Gitt("at vi har en hypotese") {
            opplysning = Hypotese(Opplysningstype<LocalDate>("Fødselsdato"), LocalDate.now())
        }
        Når("den blir bekreftet") {
            opplysning = opplysning.bekreft()
        }
        Så("blir opplysningen et faktum") {
            assertTrue(opplysning is Faktum)
        }

        Når("vi har en opplysning om fødselsdato kan vi utlede alder") {
            TODO()
        }
        Gitt("at vi har en hypotese om {string}") { type: String ->
            opplysninger.leggTil(Hypotese(Opplysningstype<LocalDate>(type), LocalDate.now()))
        }
        Så("kan vi utlede {string}") { type: String ->
            assertTrue(opplysninger.har(Opplysningstype<LocalDate>(type)))
        }
    }
}
