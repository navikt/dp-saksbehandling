package no.nav.dagpenger.saksbehandling.audit

interface Auditlogg {
    fun les(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun opprett(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun oppdater(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    fun slett(
        melding: String,
        ident: String,
        saksbehandler: String,
    )

    companion object {
        val NoOp: Auditlogg =
            object : Auditlogg {
                override fun les(
                    melding: String,
                    ident: String,
                    saksbehandler: String,
                ) {}

                override fun opprett(
                    melding: String,
                    ident: String,
                    saksbehandler: String,
                ) {}

                override fun oppdater(
                    melding: String,
                    ident: String,
                    saksbehandler: String,
                ) {}

                override fun slett(
                    melding: String,
                    ident: String,
                    saksbehandler: String,
                ) {}
            }
    }
}
