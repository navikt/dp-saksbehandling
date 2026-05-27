package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session

class PostgresUnitOfWork(
    val session: Session,
)
