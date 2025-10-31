package no.nav.dagpenger.saksbehandling.henvendelse

import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

sealed class HåndterHenvendelseResultat {
    data class HåndtertHenvendelse(val sakId: UUID) : HåndterHenvendelseResultat()

    object UhåndtertHenvendelse : HåndterHenvendelseResultat()
}

class HenvendelseMediator(private val sakMediator: SakMediator) {
    fun taImotHenvendelse(hendelse: HenvendelseMottattHendelse): HåndterHenvendelseResultat {
        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)
        return when (sisteSakId) {
            null -> {
                HåndterHenvendelseResultat.UhåndtertHenvendelse
            }

            else -> {
                HåndterHenvendelseResultat.HåndtertHenvendelse(sisteSakId)
            }
        }
    }
}
