package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class InnstillingGodkjentHendelse(person: String) : PersonHendelse(UUID.randomUUID(), person)
