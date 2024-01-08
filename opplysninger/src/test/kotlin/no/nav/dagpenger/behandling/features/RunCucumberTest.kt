package no.nav.dagpenger.behandling.features

import io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class RunCucumberTest

/*
Faktum vs hypotese (skal vi ha tilstand på opplysninger?)
Opplysningstype (med hierarki)
Utlede opplysning fra opplysning (vurdering?)
Gyldighetsperiode på opplysninger
Register/collection av opplysninger
DSL for å utlede opplysninger

Vise kjede av utledninger
Koble opplysning til meldingsreferanseId (ustrukturert logging)
Overgang til vedtak (når og hvordan lager vi vedtak)
 */
