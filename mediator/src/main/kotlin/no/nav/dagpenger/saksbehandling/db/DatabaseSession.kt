package no.nav.dagpenger.saksbehandling.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.sessionOf
import java.sql.Connection
import javax.sql.DataSource

private val dbLogger = KotlinLogging.logger {}

/**
 * [dataSource] er [Lazy] slik at vi utsetter oppretting av connection pool til første spørring.
 * Da unngår vi at applikasjonen kobler seg til databasen ved oppstart, før den faktisk trengs
 * (f.eks. i tester eller komponenter som ikke alltid leser/skriver).
 */
data class DatabaseSession(
    private val dataSource: Lazy<DataSource>,
) {
    fun <R> session(block: (Session) -> R): R = sessionOf(dataSource.value).use(block)

    fun transaction(transactionBlock: PostgresUnitOfWork.() -> Unit) {
        session { session ->
            session.connection.underlying.withTransaction {
                PostgresUnitOfWork(session).apply(transactionBlock)
            }
        }
    }

    fun <R> withTransaction(transactionBlock: PostgresUnitOfWork.() -> R): R =
        session { session ->
            session.connection.underlying.withTransaction {
                transactionBlock(PostgresUnitOfWork(session))
            }
        }
}

private fun <R> Connection.withTransaction(transactionBlock: () -> R): R {
    val transactionTimer = DbMetrics.transactionDuration.startTimer()
    val previousValue = autoCommit
    autoCommit = false
    try {
        DbMetrics.activeTransactions.inc()

        val result = transactionBlock()

        commitAndCount()

        return result
    } catch (err: Exception) {
        rollbackAndCount()
        dbLogger.error(err) { "Transaksjonen feilet, ruller tilbake" }
        throw err
    } finally {
        autoCommit = previousValue
        DbMetrics.activeTransactions.dec()
        transactionTimer.observeDuration()
    }
}

private fun Connection.commitAndCount() {
    val commitTimer = DbMetrics.commitDuration.startTimer()
    try {
        commit()
        DbMetrics.commitCounter.inc()
    } finally {
        commitTimer.observeDuration()
    }
}

private fun Connection.rollbackAndCount() {
    val timer = DbMetrics.transactionDuration.startTimer()
    try {
        rollback()
        DbMetrics.rollbackCounter.inc()
    } finally {
        timer.observeDuration()
    }
}
