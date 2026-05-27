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

    fun <R> transaction(transactionBlock: PostgresUnitOfWork.() -> R): R =
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
    return runCatching {
        DbMetrics.activeTransactions.inc()
        val result = transactionBlock()
        handleCommit()
        result
    }.onFailure { e ->
        handleRollback(e)
    }.also {
        autoCommit = previousValue
        DbMetrics.activeTransactions.dec()
        transactionTimer.observeDuration()
    }.getOrThrow()
}

private fun Connection.handleCommit() {
    val commitTimer = DbMetrics.commitDuration.startTimer()
    runCatching {
        commit()
    }.onSuccess {
        DbMetrics.commitCounter.inc()
        commitTimer.observeDuration()
    }.onFailure {
        dbLogger.error(it) { "Commit feilet" }
    }.getOrThrow()
}

private fun Connection.handleRollback(t: Throwable) {
    runCatching {
        dbLogger.error(t) { "Transaksjonen feilet, ruller tilbake" }
        DbMetrics.rollbackCounter.inc()
        rollback()
    }.onFailure {
        dbLogger.error(t) { "Rollback feilet" }
    }
}
