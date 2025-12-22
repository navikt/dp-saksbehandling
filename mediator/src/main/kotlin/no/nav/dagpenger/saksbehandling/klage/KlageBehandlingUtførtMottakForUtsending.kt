package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.util.UUID

internal class KlageBehandlingUtførtMottakForUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : AbstractKlageBehandlingUtførtMottak(rapidsConnection) {
    override val mottakNavn: String = "KlageBehandlingUtførtMottakForUtsending"

    override fun håndter(
        behandlingId: UUID,
        sakId: UUID,
        utfall: UtfallType,
        ident: String,
        saksbehandler: Saksbehandler,
    ) {
        utsendingMediator.mottaStartUtsending(
            StartUtsendingHendelse(
                behandlingId = behandlingId,
                utsendingSak =
                    UtsendingSak(
                        id = sakId.toString(),
                        kontekst = "Dagpenger",
                    ),
                ident = ident,
            ),
        )
    }
}
