package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggDTO
import no.nav.dagpenger.saksbehandling.hentEmneknaggKategori

internal fun Route.emneknaggApi(oppgaveMediator: OppgaveMediator) {
    authenticate("azureAd") {
        get("emneknagger") {
            val dbEmneknagger = oppgaveMediator.hentDistinkteEmneknagger()
            val emneknagger = byggEmneknaggListe(dbEmneknagger)
            call.respond(HttpStatusCode.OK, emneknagger)
        }
    }
}

internal fun byggEmneknaggListe(dbEmneknagger: Set<String>): List<EmneknaggDTO> {
    val kodedefinerte =
        Emneknagg.alleKodedefinerte.map {
            EmneknaggDTO(
                visningsnavn = it.visningsnavn,
                kategori = it.kategori.tilDTO(),
            )
        }
    val ekstraFraDb =
        dbEmneknagger
            .filter { db -> !db.startsWith("Ettersending") }
            .filter { db -> Emneknagg.alleKodedefinerte.none { it.visningsnavn == db } }
            .map {
                EmneknaggDTO(
                    visningsnavn = it,
                    kategori = hentEmneknaggKategori(it).tilDTO(),
                )
            }
    return (kodedefinerte + ekstraFraDb).sortedBy { it.visningsnavn }
}
