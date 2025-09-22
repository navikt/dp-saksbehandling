package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ALDER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_STREIK
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_UTDANNING
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_UTESTENGT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_ORDINÆR
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_PERMITTERT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT
import org.junit.jupiter.api.Test
import java.util.UUID

class EmneknaggBuilderTest {
    private val behandlingId = UUID.randomUUID().toString()

    @Test
    fun `Feil eller mangler ved json`() {
        shouldThrow<IllegalArgumentException> {
            EmneknaggBuilder(json = "")
        }

        shouldThrow<IllegalArgumentException> {
            EmneknaggBuilder(json = "{}")
        }
        shouldThrow<IllegalArgumentException> {
            //language=JSON
            EmneknaggBuilder("""{ "opplysninger": [] }""")
        }

        shouldThrow<IllegalArgumentException> {
            //language=JSON
            EmneknaggBuilder("""{ "rettighetsperioder": [] }""")
        }

        shouldThrow<IllegalArgumentException> {
            //language=JSON
            EmneknaggBuilder("""{ "behandletHendelse":{} }""")
        }

        shouldNotThrowAny {
            //language=JSON
            EmneknaggBuilder(
                """
                {
                  "behandletHendelse": { "type": "Søknad" },
                  "rettighetsperioder": [],
                  "opplysninger": []
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Riktig håndtering av emneknagger der det finnes flere opplysninger av samme type`() {
        lagBehandlingResultat(
            behandletHendelseType = "Søknad",
            harRettighet = true,
            ForenkletOpplysning(
                id = OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId,
                verdi = true,
            ),
            ForenkletOpplysning(
                id = OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId,
                verdi = false,
            ),
        ).also {
            EmneknaggBuilder(it).bygg() shouldBe setOf(INNVILGELSE.visningsnavn, RETTIGHET_ORDINÆR.visningsnavn)
        }

        lagBehandlingResultat(
            behandletHendelseType = "Søknad",
            harRettighet = true,
            ForenkletOpplysning(
                id = OpplysningTyper.OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT.opplysningTypeId,
                verdi = false,
            ),
            ForenkletOpplysning(
                id = OpplysningTyper.OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT.opplysningTypeId,
                verdi = true,
            ),
        ).also {
            EmneknaggBuilder(it).bygg() shouldBe setOf(INNVILGELSE.visningsnavn)
        }
    }

    @Test
    fun `Skal sette riktig innvigelse eller avslag emneknagg  behandlinger med flere rettighetsperioder`() {
        //language=JSON
        EmneknaggBuilder(
            """
                {
                  "behandlingId": "$behandlingId",
                  "behandletHendelse": { "type": "Søknad" },
                  "rettighetsperioder": [{"harRett": false}, {"harRett": true}, {"harRett": false}],
                  "opplysninger": []
                }
            """,
        ).bygg() shouldBe setOf(INNVILGELSE.visningsnavn)

        //language=JSON
        EmneknaggBuilder(
            """
                {
                  "behandlingId": "$behandlingId",
                  "behandletHendelse": { "type": "Søknad" },
                  "rettighetsperioder": [{"harRett": false}, {"harRett": false}],
                  "opplysninger": []
                }
            """,
        ).bygg() shouldBe setOf(AVSLAG.visningsnavn)

        //language=JSON
        EmneknaggBuilder(
            """
                {
                  "behandlingId": "$behandlingId",
                  "behandletHendelse": { "type": "Søknad" },
                  "rettighetsperioder": [],
                  "opplysninger": []
                }
            """,
        ).bygg() shouldBe setOf(AVSLAG.visningsnavn)
    }

    @Test
    fun `Emneknagger ved avslag av minsteinntekt og rettighet permittering`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = false,
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPFYLLER_KRAV_TIL_MINSTEINNTEKT.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId,
                    verdi = true,
                ),
            )

        EmneknaggBuilder(json = behandlingResultat).bygg() shouldBe
            setOf(
                AVSLAG.visningsnavn,
                AVSLAG_MINSTEINNTEKT.visningsnavn,
                RETTIGHET_PERMITTERT.visningsnavn,
            )
    }

    @Test
    fun `emneknagger ved avslag pga alder og rettighet ordinær`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = false,
                ForenkletOpplysning(
                    id = OpplysningTyper.KRAV_TIL_ALDER.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId,
                    verdi = true,
                ),
            )

        EmneknaggBuilder(json = behandlingResultat).bygg() shouldBe
            setOf(
                AVSLAG.visningsnavn,
                AVSLAG_ALDER.visningsnavn,
                RETTIGHET_ORDINÆR.visningsnavn,
            )
    }

    @Test
    fun `Emneknagger ved avslag og med flere avslagsgrunner`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = false,
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPFYLLER_KRAV_TIL_MINSTEINNTEKT.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.KRAV_TIL_TAP_AV_ARBEIDSINNTEKT.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.TAP_AV_ARBEIDSTID_ER_MINST_TERSKEL.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.KRAV_TIL_ALDER.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.IKKE_FULLE_YTELSER.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPFYLLER_MEDLEMSKAP.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.IKKE_PÅVIRKET_AV_STREIK_ELLER_LOCKOUT.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPFYLLER_KRAVET_OPPHOLD.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.KRAV_TIL_ARBEIDSSØKER.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPYLLER_KRAV_TIL_REGISTRERT_ARBEIDSSØKER.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT.opplysningTypeId,
                    verdi = false,
                ),
                ForenkletOpplysning(
                    id = OpplysningTyper.KRAV_TIL_UTDANNING_ELLER_OPPLÆRING.opplysningTypeId,
                    verdi = false,
                ),
            )
        EmneknaggBuilder(json = behandlingResultat).bygg() shouldBe
            setOf(
                AVSLAG.visningsnavn,
                AVSLAG_MINSTEINNTEKT.visningsnavn,
                AVSLAG_ARBEIDSINNTEKT.visningsnavn,
                AVSLAG_ARBEIDSTID.visningsnavn,
                AVSLAG_ALDER.visningsnavn,
                AVSLAG_ANDRE_YTELSER.visningsnavn,
                AVSLAG_MEDLEMSKAP.visningsnavn,
                AVSLAG_STREIK.visningsnavn,
                AVSLAG_OPPHOLD_UTLAND.visningsnavn,
                AVSLAG_REELL_ARBEIDSSØKER.visningsnavn,
                AVSLAG_IKKE_REGISTRERT.visningsnavn,
                AVSLAG_UTESTENGT.visningsnavn,
                AVSLAG_UTDANNING.visningsnavn,
            )
    }

    @Test
    fun `emneknagger ved innvigelse av dagpenger etter verneplikt`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT.opplysningTypeId,
                    verdi = true,
                ),
            )
        EmneknaggBuilder(json = behandlingResultat).bygg() shouldBe
            setOf(
                INNVILGELSE.visningsnavn,
                RETTIGHET_VERNEPLIKT.visningsnavn,
            )
    }

    @Test
    fun `emneknagger ved innvilgelse av ordinære dagpenger`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId,
                    verdi = true,
                ),
            )
        EmneknaggBuilder(json = behandlingResultat).bygg() shouldBe
            setOf(
                INNVILGELSE.visningsnavn,
                RETTIGHET_ORDINÆR.visningsnavn,
            )
    }

    @Test
    fun ` emneknagger ved innvilgelse av dagpenger under permittering`() {
        val behanldingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId,
                    verdi = true,
                ),
            )
        EmneknaggBuilder(behanldingResultat).bygg() shouldBe
            setOf(
                INNVILGELSE.visningsnavn,
                RETTIGHET_PERMITTERT.visningsnavn,
            )
    }

    @Test
    fun `emneknagger ved innvilgelse av dagpenger under permittering fra fiskeindustri`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI.opplysningTypeId,
                    verdi = true,
                ),
            )
        EmneknaggBuilder(behandlingResultat).bygg() shouldBe
            setOf(
                INNVILGELSE.visningsnavn,
                Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
            )
    }

    @Test
    fun `emneknagger ved innvilgelse av dagpenger etter konkurs`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = OpplysningTyper.RETTIGHET_DAGPENGER_ETTER_KONKURS.opplysningTypeId,
                    verdi = true,
                ),
            )
        EmneknaggBuilder(behandlingResultat).bygg() shouldBe
            setOf(
                INNVILGELSE.visningsnavn,
                Regelknagg.RETTIGHET_KONKURS.visningsnavn,
            )
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilget ukjent rettighet - regelmotor har laget noe vi ikke kjenner`() {
        val behandlingResultat =
            lagBehandlingResultat(
                behandletHendelseType = "Søknad",
                harRettighet = true,
                ForenkletOpplysning(
                    id = UUID.randomUUID(),
                    verdi = true,
                ),
            )
        EmneknaggBuilder(behandlingResultat).bygg() shouldBe setOf(INNVILGELSE.visningsnavn)
    }

    private val objectMapper =
        jacksonObjectMapper().also {
            it.registerModule(JavaTimeModule())
        }

    private data class ForenkletOpplysning(val id: UUID, val verdi: Boolean)

    private fun lagBehandlingResultat(
        behandletHendelseType: String = "Søknad",
        harRettighet: Boolean,
        vararg opplysninger: ForenkletOpplysning,
    ): String {
        val behandletHendelseObject =
            objectMapper.createObjectNode().apply {
                put("id", UUID.randomUUID().toString())
                put("type", behandletHendelseType)
            }

        val rettighetPeriodeObject =
            objectMapper.createObjectNode().apply {
                put("harRett", harRettighet)
            }
        val rettighetsperioderArray =
            objectMapper.createArrayNode().apply {
                add(rettighetPeriodeObject)
            }

        val opplysningArray =
            objectMapper.createArrayNode().apply {
                opplysninger.forEach { opplysning ->
                    val verdiNode =
                        objectMapper.createObjectNode().apply {
                            put("verdi", opplysning.verdi)
                            put("datatype", "boolsk")
                        }
                    val periodeObject =
                        objectMapper.createObjectNode().apply {
                            set<ObjectNode>("verdi", verdiNode)
                        }
                    val periodeArray =
                        objectMapper.createArrayNode().apply {
                            add(periodeObject)
                        }
                    val opplysningObject =
                        objectMapper.createObjectNode().apply {
                            put("opplysningTypeId", opplysning.id.toString())
                            set<ArrayNode>("perioder", periodeArray)
                        }
                    add(opplysningObject)
                }
            }
        return objectMapper.createObjectNode().apply {
            put("behandlingId", UUID.randomUUID().toString())
            set<ObjectNode>("behandletHendelse", behandletHendelseObject)
            set<ArrayNode>("rettighetsperioder", rettighetsperioderArray)
            set<ArrayNode>("opplysninger", opplysningArray)
        }.toString()
    }
}
