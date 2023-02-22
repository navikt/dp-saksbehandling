package no.nav.dagpenger.behandling.cucumber

import io.cucumber.java8.No
import java.time.format.DateTimeFormatter

class MeldekortberegningTest : No {
    private val datoformatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    init {
        Gitt("at virkningsdatoen til vedtaket er fredag {string}, altså midt i meldeperioden") { string: String ->
        }

        Gitt("vilkår for dagpenger er oppfylt i perioden") { }

        Gitt("vedtaket har {int} ventedager") { int1: Int -> }

        Så("gjenstående ventedager være {int}") { int1: Int -> }

        Så("vil ventedager være avspasert på datoene {string}, {string}, {string}") { string: String, string2: String, string3: String -> }

        Så("vil gjenstående dagpengepengeperiode være redusert med {int} dager") { int1: Int -> }

        Så("vil {int} dagsatser gå til utbetaling") { int1: Int -> }
    }
}
