package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session

data class PostgresUnitOfWork(
    val session: Session,
)
