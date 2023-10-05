package no.nav.dagpenger.behandling.iverksett

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.BehandlingVisitor
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import no.nav.dagpenger.kontrakter.iverksett.IverksettDto
import java.time.LocalDateTime
import java.util.UUID

internal class IverksettDTOBuilder(behandling: Behandling) : BehandlingVisitor {

    private lateinit var sakId: UUID
    private lateinit var behandlingId: UUID
    private lateinit var ident: String

    init {
        behandling.accept(this)
    }

    fun bygg() = IverksettDto(
        sakId = sakId,
        behandlingId = behandlingId,
        personIdent = ident,
    )

    override fun visit(
        person: Person,
        steg: Set<Steg<*>>,
        opprettet: LocalDateTime,
        behandlingId: UUID,
        tilstand: Behandling.TilstandType,
        behandler: List<PersonHendelse>,
        sak: Sak,
    ) {
        this.sakId = sak.id
        this.behandlingId = behandlingId
        this.ident = person.ident
    }
}
