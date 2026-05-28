package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session

/**
 * Representerer transaksjonstilstand for kryssrepo-operasjoner.
 *
 * [IkkeAktiv] — ingen pågående transaksjon (repos starter egen transaksjon).
 * [Aktiv] — pågående transaksjon (repos gjenbruker eksisterende session).
 *
 * Repos bruker [DatabaseSession.inContext] som brancher på typen.
 */
sealed class Transaksjonskontekst {
    data object IkkeAktiv : Transaksjonskontekst()

    class Aktiv(
        internal val session: Session,
    ) : Transaksjonskontekst()
}

/**
 * Fabrikk for å starte kryssrepo-transaksjoner.
 *
 * Koordinerende mediatorer injiserer [Transaksjoner] og kaller [transaksjon]
 * for å utføre flere repo-operasjoner atomisk.
 */
class Transaksjoner(
    private val databaseSession: DatabaseSession,
) {
    fun <R> transaksjon(block: (Transaksjonskontekst.Aktiv) -> R): R =
        databaseSession.transaction {
            block(Transaksjonskontekst.Aktiv(this.session))
        }
}
