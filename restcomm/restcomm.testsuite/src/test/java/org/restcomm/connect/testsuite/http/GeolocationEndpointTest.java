/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.connect.testsuite.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.ws.rs.core.MultivaluedMap;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.restcomm.connect.dao.entities.Geolocation;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.annotations.FeatureAltTests;
import org.restcomm.connect.commons.annotations.FeatureExpTests;
import org.restcomm.connect.commons.dao.Sid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeolocationEndpointTest {

    private static final Logger logger = Logger.getLogger(GeolocationEndpointTest.class);
    private static final String version = Version.getVersion();
    private static final String ImmediateGT = Geolocation.GeolocationType.Immediate.toString();
    private static final String NotificationGT = Geolocation.GeolocationType.Notification.toString();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @After
    public void after() throws InterruptedException {
        wireMockRule.resetRequests();
        Thread.sleep(1000);
    }

    String gmlcMapAtiResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"CGIorSAIorLAI\": {\n" +
        "    \"mcc\": \"598\",\n" +
        "    \"mnc\": \"1\",\n" +
        "    \"lac\": \"320\",\n" +
        "    \"cellid\": \"521\"\n" +
        "  },\n" +
        "  \"aol\": \"0\",\n" +
        "  \"vlrNumber\": \"598001\",\n" +
        "  \"subscriberState\": \"assumedIdle\"\n" +
        "}";

    String gmlcMapLsmResponse = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS-PSL\",\n" +
        "  \"lcsReferenceNumber\": \"2\",\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": \"59899077937\",\n" +
        "    \"imsi\": \"748026871012345\",\n" +
        "    \"lmsi\": \"2915\",\n" +
        "    \"networkNodeNumber\": \"5982123007\",\n" +
        "    \"gprsNodeIndicator\": \"false\",\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"sgsnName\": \"\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": \"134570\",\n" +
        "    \"vGmlcAddress\": \"157003\",\n" +
        "    \"pprAddress\": \"938012\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": \"34.78911995887756\",\n" +
        "      \"longitude\": \"-124.90201950073242\",\n" +
        "      \"uncertainty\": \"0.0\",\n" +
        "      \"uncertaintySemiMajorAxis\": \"35.949729863572216\",\n" +
        "      \"uncertaintySemiMinorAxis\": \"18.531167061100025\",\n" +
        "      \"angleOfMajorAxis\": \"30.0\",\n" +
        "      \"confidence\": \"0\",\n" +
        "      \"altitude\": \"1500\",\n" +
        "      \"uncertaintyAltitude\": \"487.8518112499371\",\n" +
        "      \"innerRadius\": \"0\",\n" +
        "      \"uncertaintyInnerRadius\": \"0.0\",\n" +
        "      \"offsetAngle\": \"0.0\",\n" +
        "      \"includedAngle\": \"0.0\"\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": \"0\",\n" +
        "    \"deferredMTLRresponseIndicator\": \"true\",\n" +
        "    \"moLrShortCircuitIndicator\": \"true\",\n" +
        "    \"accuracyFulfilmentIndicator\": \"0\",\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": \"437\",\n" +
        "      \"mnc\": \"109\",\n" +
        "      \"lac\": \"8304\",\n" +
        "      \"cellid\": \"17185\"\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningInfo\": \"29152\",\n" +
        "      \"geranGanssPositioningData\": \"820135\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningInfo\": \"933601\",\n" +
        "      \"utranGanssPositioningData\": \"933600\"\n" +
        "    },\n" +
        "    \"VelocityEstimate\": {\n" +
        "      \"horizontalSpeed\": \"101\",\n" +
        "      \"bearing\": \"3\",\n" +
        "      \"verticalSpeed\": \"2\",\n" +
        "      \"uncertaintyHorizontalSpeed\": \"5\",\n" +
        "      \"uncertaintyVerticalSpeed\": \"1\",\n" +
        "      \"velocityType\": \"HorizontalWithVerticalVelocityAndUncertainty\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    String gmlcLteLcsResponse = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"lcsReferenceNumber\": \"359\",\n" +
        "  \"gmlcTransactionId\": \"5\",\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": \"59899077937\",\n" +
        "    \"imsi\": \"748026871012345\",\n" +
        "    \"lmsi\": \"4294967295\",\n" +
        "    \"mmeName\": \"mme7480001\",\n" +
        "    \"mmeRealm\": \"02.com.uy\",\n" +
        "    \"sgsnNumber\": \"598990123\",\n" +
        "    \"sgsnName\": \"sgsn\",\n" +
        "    \"sgsnRealm\": \"02.com.uy\",\n" +
        "    \"3GPPAAAServerName\": \"aaa321\",\n" +
        "    \"gmlcAddress\": \"598999047\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": \"33.99999260902405\",\n" +
        "      \"longitude\": \"55.99999666213989\",\n" +
        "      \"uncertainty\": \"0.0\",\n" +
        "      \"uncertaintySemiMajorAxis\": \"1.0000000000000009\",\n" +
        "      \"uncertaintySemiMinorAxis\": \"2.100000000000002\",\n" +
        "      \"angleOfMajorAxis\": \"4.0\",\n" +
        "      \"confidence\": \"80\",\n" +
        "      \"altitude\": \"200\",\n" +
        "      \"uncertaintyAltitude\": \"11.435888100000016\",\n" +
        "      \"innerRadius\": \"0\",\n" +
        "      \"uncertaintyRadius\": \"0.0\",\n" +
        "      \"offsetAngle\": \"0.0\",\n" +
        "      \"includedAngle\": \"0.0\"\n" +
        "    },\n" +
        "    \"accuracyFulfilmentIndicator\": \"-1\",\n" +
        "    \"ageOfLocationEstimate\": \"0\",\n" +
        "    \"CGIorSAIorESMLCCellInfo\": {\n" +
        "      \"cellGlobalIdentity\": \"54108\",\n" +
        "      \"serviceAreaIdentity\": \"2718\",\n" +
        "      \"eUtranCgi\": \"7890104\",\n" +
        "      \"cellPortionId\": \"3\"\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningInfo\": \"0\",\n" +
        "      \"geranGanssPositioningData\": \"0\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningInfo\": \"81\",\n" +
        "      \"utranGanssPositioningData\": \"403\",\n" +
        "      \"utranAdditionalPositioningData\": \"0\"\n" +
        "    },\n" +
        "    \"E-UTRANPositioningInfo\": {\n" +
        "      \"eUtranPositioningData\": \"\"\n" +
        "    },\n" +
        "    \"VelocityEstimate\": {\n" +
        "      \"horizontalSpeed\": \"20\",\n" +
        "      \"bearing\": \"0\",\n" +
        "      \"verticalSpeed\": \"0\",\n" +
        "      \"uncertaintyHorizontalSpeed\": \"0\",\n" +
        "      \"uncertaintyVerticalSpeed\": \"0\",\n" +
        "      \"velocityType\": \"HorizontalVelocity\"\n" +
        "    },\n" +
        "    \"civicAddress\": \"DowningStreet2915\",\n" +
        "    \"barometricPressure\": \"1013\"\n" +
        "  }\n" +
        "}";


    @Test
    public void testCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gmlcMapAtiResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gmlcMapAtiResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString().equals("598"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("320"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("521"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("598001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Test asserts via GET to a Geolocation list
        JsonArray immediateGeolocationsListJson = RestcommGeolocationsTool.getInstance()
            .getGeolocations(deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid);
        geolocationJson = immediateGeolocationsListJson.get(0).getAsJsonObject();
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString().equals("598"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("320"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("521"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("598001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    @Category(FeatureAltTests.class)
    public void testCreateNotApiCompliantImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcMapAtiResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST with one missing mandatory parameter
        // Parameter values Assignment, StatusCallback missing
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        Sid rejectedGeolocationSid = null;
        // HTTP POST Geolocation creation with given parameters values
        try {
            JsonObject missingParamGeolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(
                deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
            rejectedGeolocationSid = new Sid(missingParamGeolocationJson.get("sid").getAsString());
            JsonObject rejectedGeolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                rejectedGeolocationSid.toString());
            assertTrue(rejectedGeolocationJson == null);
        } catch (Exception exception) {
            // Checking Test asserts via HTTP GET (no record found as POST returned a response status of 400 Bad Request)
            assertTrue(rejectedGeolocationSid == null);
            logger.info("Exception during HTTP POST: " + exception.getMessage());
        }

        // Test create Immediate type of Geolocation via POST with one prohibited parameter
        @SuppressWarnings("unused")
        String geofenceId = null;
        geolocationParams.add("GeofenceId", geofenceId = "21"); // "GeofenceId"
        // applicable only for Notification
        // type of Geolocation
        // HTTP POST Geolocation creation with given parameters values
        try {
            JsonObject prohibitedParamGeolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(
                deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
            rejectedGeolocationSid = new Sid(prohibitedParamGeolocationJson.get("sid").getAsString());
            JsonObject rejectedGeolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                rejectedGeolocationSid.toString());
            assertTrue(rejectedGeolocationJson == null);
        } catch (Exception exception) {
            // Checking Test asserts via HTTP GET (no record found as POST is rejected)
            assertTrue(rejectedGeolocationSid == null);
            logger.info("Exception during HTTP POST: " + exception.getMessage());
        }
    }

    @Test
    public void testUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, locationTimestamp, geolocationPositioningType, lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successfull");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "01");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "false");
        geolocationParamsUpdate.add("Cause", "Not API Compliant");
        // Update Geolocation via POST
        RestcommGeolocationsTool.getInstance().updateImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, false);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateTime dateTime = dtf.parseDateTime(locationTimestamp);
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equalsIgnoreCase(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successfull");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "03");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "707");
        geolocationParamsUpdate.add("CellId", cellId = "55777");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "598003245701");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:31:27.790-08:00");
        geolocationParamsUpdate.add("GeolocationPositioningType", geolocationPositioningType = "Network");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "API Not Compliant");
        // Update Geolocation via PUT
        RestcommGeolocationsTool.getInstance().updateImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        dateTime = dtf.parseDateTime(locationTimestamp);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_geofence_range") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    @Category(FeatureAltTests.class)
    public void testNotApiCompliantUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, internetAddress,
            physicalAddress, formattedAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00", geolocationPositioningType, lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateTime dateTime = dtf.parseDateTime(locationTimestamp);
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        locationTimestamp = df.format(dateTime.toDate());

        // Define malformed values for the Geolocation attributes (PUT test to fail)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "North 72.908134"); // WGS84 not compliant
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "170.908134");
        // Update failed Geolocation via PUT
        RestcommGeolocationsTool.getInstance().updateImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);
        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("source") == null);
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equalsIgnoreCase("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_positioning_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") != null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successfull");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "01");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "709");
        geolocationParamsUpdate.add("CellId", cellId = "34580");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "598003245702");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "S43\u00b038'19.39''");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "E169\u00b028'49.07''");
        geolocationParamsUpdate.add("InternetAddress", internetAddress = "180.7.2.141");
        geolocationParamsUpdate.add("PhysicalAddress", physicalAddress = "A8-77-CA-29-32-D1");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:31:28.388-05:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "API Not Compliant");
        // Update Geolocation via PUT
        // previous failed location is composed again with new proper geolocation data values
        RestcommGeolocationsTool.getInstance().updateImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        dateTime = dtf.parseDateTime(locationTimestamp);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address").getAsString()
            .equals(internetAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address").getAsString()
            .equals(physicalAddress));
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    public void testDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("MobileCountryCode", "748");
        geolocationParams.add("MobileNetworkCode","01");
        geolocationParams.add("LocationAreaCode","709");
        geolocationParams.add("CellId","34580");
        geolocationParams.add("NetworkEntityAddress","598003245702");
        geolocationParams.add("LocationAge", "1");
        geolocationParams.add("ResponseStatus", "successfull");
        geolocationParams.add("InternetAddress", "194.87.1.127");
        geolocationParams.add("PhysicalAddress", "D8-97-BA-19-02-D8");
        geolocationParams.add("LocationTimestamp", "2016-04-15");
        geolocationParams.add("LastGeolocationResponse", "true");
        geolocationParams.add("Cause", "Not API Compliant");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
        // Remove checking Test asserts via HTTP GET
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        assertTrue(geolocationJson == null);
    }

    @Test
    public void testCreateAndGetNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn").getAsString().equals("59899077937"));
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("2915"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status") == null);
        //assertTrue(geolocationJson.get("reference_number").getAsLong() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("109"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("8304"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("17185"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("34.78911995887756"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-124.90201950073242"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("35.949729863572216"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("18.531167061100025"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("30.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("1500"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("487.8518112499371"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals("101"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals("2"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("5"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("3"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Test asserts via GET to a Geolocation list
        JsonArray notificationGeolocationsListJson = RestcommGeolocationsTool.getInstance()
            .getGeolocations(deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid);
        geolocationJson = notificationGeolocationsListJson.get(0).getAsJsonObject();
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn").getAsString().equals("59899077937"));
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("2915"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status") == null);
        //assertTrue(geolocationJson.get("reference_number").getAsString().equals("3"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("109"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("8304"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("17185"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("34.78911995887756"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-124.90201950073242"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("35.949729863572216"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("18.531167061100025"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("30.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("1500"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("487.8518112499371"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals("101"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals("2"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("5"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("3"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    @Category(FeatureAltTests.class)
    public void testCreateNotApiCompliantNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier;

        // Test create Notification type of Geolocation via POST with one missing mandatory parameter
        // Parameter values Assignment, ResponseTime is not API compliant
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","UMTS");
        notificationGeolocationNotApiCompliantParams.add("Priority","high");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","verylow");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("GeofenceEventType", "inside");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", "locationAreaId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", "10");
        notificationGeolocationNotApiCompliantParams.add("OccurrenceInfo","once");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","33");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
        notificationGeolocationNotApiCompliantParams.add("EventIntervalTime","60");
        notificationGeolocationNotApiCompliantParams.add("EventReportingAmount","5");
        notificationGeolocationNotApiCompliantParams.add("EventReportingInterval","180");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        Sid rejectedGeolocationSid = null;
        // HTTP POST Geolocation creation with given parameters values
        try {
            JsonObject missingParamGeolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
                deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, notificationGeolocationNotApiCompliantParams);
            rejectedGeolocationSid = new Sid(missingParamGeolocationJson.get("sid").getAsString());
            JsonObject rejectedGeolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                rejectedGeolocationSid.toString());
            assertNull(rejectedGeolocationJson);
        } catch (Exception exception) {
            // Checking Test asserts via HTTP GET (no record found as POST returned a response status of 400 Bad Request)
            assertTrue(rejectedGeolocationSid == null);
            logger.info("Exception during HTTP POST: " + exception.getMessage());
        }

    }

    @Test
    public void testUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType, responseStatus, cellId, mobileCountryCode, mobileNetworkCode,
            locationAreaCode, networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10");
        geolocationParams.add("OccurrenceInfo","multiple");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParamsUpdate.add("CoreNetwork","UMTS");
        geolocationParamsUpdate.add("Priority","high");
        geolocationParamsUpdate.add("HorizontalAccuracy","1000");
        geolocationParamsUpdate.add("VerticalAccuracy","5000");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successfull");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "01");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "false");
        geolocationParamsUpdate.add("Cause", "Not API Compliant");
        // Update Geolocation via POST
        RestcommGeolocationsTool.getInstance().updateNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, false);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateTime dateTime = dtf.parseDateTime(locationTimestamp);
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();

        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "partially-successfull");
        geolocationParamsUpdate.add("CellId", cellId = "55777");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "707");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "03");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "598003245701");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "172.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "170.908134");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:42.771-03:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "API Not Compliant");
        // Update Geolocation via PUT
        RestcommGeolocationsTool.getInstance().updateNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        dateTime = dtf.parseDateTime(locationTimestamp);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    @Category(FeatureAltTests.class)
    public void testNotApiCompliantUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude,
            locationTimestamp = "2016-04-17T20:28:40.690-03:00", deferredLocationEventType,
            geofenceType, geofenceId, geolocationPositioningType, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateTime dateTime = dtf.parseDateTime(locationTimestamp);
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        locationTimestamp = df.format(dateTime.toDate());

        // Define malformed values for the Geolocation attributes (PUT test to fail)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "72.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "South 170.908134"); // WGS84 not compliant
        // Update failed Geolocation via PUT
        RestcommGeolocationsTool.getInstance().updateNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);
        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("source") == null);
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equalsIgnoreCase("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_positioning_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") != null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParamsUpdate.add("CoreNetwork","UMTS");
        geolocationParamsUpdate.add("Priority","high");
        geolocationParamsUpdate.add("HorizontalAccuracy","500");
        geolocationParamsUpdate.add("VerticalAccuracy","100");
        geolocationParamsUpdate.add("VerticalCoordinateRequest","true");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParamsUpdate.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("GeofenceEventType", deferredLocationEventType = "inside");
        geolocationParamsUpdate.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParamsUpdate.add("GeofenceId", geofenceId = "10");
        geolocationParamsUpdate.add("OccurrenceInfo","once");
        geolocationParamsUpdate.add("ReferenceNumber","33");
        geolocationParamsUpdate.add("ServiceTypeID","0");
        geolocationParamsUpdate.add("EventIntervalTime","60");
        geolocationParamsUpdate.add("EventReportingAmount","5");
        geolocationParamsUpdate.add("EventReportingInterval","180");
        geolocationParamsUpdate.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successfull");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "747");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "05");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "709");
        geolocationParamsUpdate.add("CellId", cellId = "34580");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "598003245703");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "172\u00b038'19.39''N");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "169\u00b028'44.07''E");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:32:29.488-07:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "API Not Compliant");
        // Update Geolocation via PUT
        // previous failed location is composed again with new proper geolocation data values
        RestcommGeolocationsTool.getInstance().updateNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString(), geolocationParamsUpdate, true);

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        dateTime = dtf.parseDateTime(locationTimestamp);
        locationTimestamp = df.format(dateTime.toDate());
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString().equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("ecid") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tac") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("rai") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis")  == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence")  == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude")  == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString().endsWith(locationTimestamp));
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    public void testDeleteNotificationGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse)));

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier",  msisdn);
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", "inside");
        geolocationParams.add("GeofenceType", "locationAreaId");
        geolocationParams.add("GeofenceId", "10");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
        // Remove checking Test asserts via HTTP GET
        geolocationJson = RestcommGeolocationsTool.getInstance().getNotificationGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        assertTrue(geolocationJson == null);
    }

    @Deployment(name = "GeolocationsEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = Maven.resolver()
            .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
            .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
archive.delete("/WEB-INF/web.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("web.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
