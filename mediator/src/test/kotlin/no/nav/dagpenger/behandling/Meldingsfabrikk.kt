package no.nav.dagpenger.behandling

import java.util.UUID

object Meldingsfabrikk {

    //language=json
    internal fun `innsending ferdigstilt hendelse`(
        søknadId: UUID,
        journalpostId: String,
        type: String,
        ident: String
    ): String = //language=JSON
        """
        {
          "journalpostId": "$journalpostId",
          "type": "$type",
          "fødselsnummer": "$ident",
          "søknadsData": {
            "søknad_uuid": "$søknadId"
          },
          "@event_name": "innsending_ferdigstilt"
        } 
        """.trimIndent()

    internal fun dagpengerrettighetResultat(
        vilkårsvurderingId: String = "a9586759-b71b-4295-a077-89a86453b020",
        ident: String = "12345678901",
        versjonNavn: String = "Paragraf_4_23_alder"
    ): String =
        //language=JSON
        """{
  "@event_name" : "prosess_resultat",
  "@opprettet" : "2023-01-19T09:40:07.191987",
  "@id" : "50fb6e53-5057-4331-b839-5494f1f8a750",
  "søknad_uuid" : "$vilkårsvurderingId",
  "versjon_navn" : "$versjonNavn",
  "resultat" : false,
  "identer" : [ {
    "id" : "$ident",
    "type" : "folkeregisterident",
    "historisk" : false
  }, {
    "id" : "aktørId",
    "type" : "aktørid",
    "historisk" : false
  } ],
  "fakta" : [ {
    "navn" : "virkningsdato",
    "id" : "1",
    "roller" : [ "nav" ],
    "type" : "localdate",
    "godkjenner" : [ ],
    "svar" : "2023-01-15"
  }, {
    "navn" : "fødselsdato",
    "id" : "2",
    "roller" : [ "nav" ],
    "type" : "localdate",
    "godkjenner" : [ ],
    "svar" : "1955-01-15"
  }, {
    "navn" : "grensedato",
    "id" : "3",
    "roller" : [ ],
    "type" : "localdate",
    "godkjenner" : [ ],
    "svar" : "2022-02-01"
  } ],
  "subsumsjoner" : [ {
    "lokalt_resultat" : false,
    "navn" : "søkeren må være under aldersgrense ved virkningstidspunkt",
    "type" : "Deltre subsumsjon",
    "forklaring" : "saksbehandlerforklaring",
    "subsumsjoner" : [ {
      "lokalt_resultat" : false,
      "navn" : "under aldersgrense",
      "type" : "Deltre subsumsjon",
      "forklaring" : "saksbehandlerforklaring",
      "subsumsjoner" : [ {
        "lokalt_resultat" : false,
        "navn" : "Sjekk at 'virkningsdato med id 1' er før 'grensedato med id 3'",
        "forklaring" : "saksbehandlerforklaring",
        "type" : "Enkel subsumsjon",
        "fakta" : [ "1", "3" ]
      } ]
    } ]
  } ]
}
        """.trimIndent()
}
