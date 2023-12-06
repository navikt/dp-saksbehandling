package no.nav.dagpenger.behandling.iverksett

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import no.nav.dagpenger.kontrakter.iverksett.IverksettDto
import org.junit.jupiter.api.Test
import java.util.UUID

class IverksettDTOBuilderTest {
    val testHendelse = object : PersonHendelse(UUID.randomUUID(), testIdent) {}
    val steg = Steg.fastsettelse<Int>("1")
    val sakId = UUID.randomUUID()
    val behandling = Behandling(testPerson, testHendelse, setOf(steg), Sak(sakId))

    @Test
    fun `Dto builder bygger IverksettDto`() {
        IverksettDTOBuilder(behandling).bygg().let {
            it should beInstanceOf<IverksettDto>()
            it.sakId shouldBe sakId
            it.behandlingId shouldBe behandling.uuid
            it.personident.verdi shouldBe testIdent
        }
    }
}
