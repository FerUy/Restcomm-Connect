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

    String gmlcMapAtiCsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": \"819203961904\"\n" +
        "    },\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 21,\n" +
        "      \"lac\": 32005,\n" +
        "      \"sac\": 38221\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"latitude\": -44.72101807594299,\n" +
        "      \"longitude\": 105.99341154098511,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 9.487171000000012\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"latitude\": -45.002102851867676,\n" +
        "      \"longitude\": 110.10006666183472,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 4.641000000000004,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"tac\": 14649\n" +
        "      },\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"eNBId\": 334,\n" +
        "        \"ci\": 74\n" +
        "      },\n" +
        "      \"GeographicalInformation\": {\n" +
        "        \"latitude\": -44.72101807594299,\n" +
        "        \"longitude\": 105.99341154098511,\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"uncertainty\": 9.487171000000012\n" +
        "      },\n" +
        "      \"GeodeticInformation\": {\n" +
        "        \"latitude\": -45.002102851867676,\n" +
        "        \"longitude\": 110.10006666183472,\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"uncertainty\": 4.641000000000004,\n" +
        "        \"confidence\": 1,\n" +
        "        \"screeningAndPresentationIndicators\": 3\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"LSA\": {},\n" +
        "    \"RAI\": {},\n" +
        "    \"CGIorSAIorLAI\": {},\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {}\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {},\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": 4,\n" +
        "    \"mnpMsisdn\": \"59899077937\",\n" +
        "    \"mnpImsi\": \"748026871012345\",\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 5,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"vlrNumber\": \"59899000231\",\n" +
        "  \"mscNumber\": \"5982123007\",\n" +
        "  \"mmeName\": \"MME7480001\",\n" +
        "  \"subscriberState\": \"assumedIdle\",\n" +
        "  \"msClassmark\": \"003\"\n" +
        "}";

    String gmlcMapAtiPsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {},\n" +
        "    \"CGIorSAIorLAI\": {},\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"TAI\": {},\n" +
        "      \"ECGI\": {},\n" +
        "      \"GeographicalInformation\": {},\n" +
        "      \"GeodeticInformation\": {}\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"LSA\": {\n" +
        "      \"lsaIdType\": \"Universal\",\n" +
        "      \"lsaId\": \"131\"\n" +
        "    },\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 23,\n" +
        "      \"lac\": 32006,\n" +
        "      \"sac\": 38222\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"latitude\": -44.72101807594299,\n" +
        "      \"longitude\": 105.99341154098511,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 9.487171000000012\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"latitude\": -45.002102851867676,\n" +
        "      \"longitude\": 110.10006666183472,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 4.641000000000004,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    }\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {\n" +
        "    \"msNetworkCapability\": \"43879748\",\n" +
        "    \"msRadioAccessCapability\": \"8549839852932239493928\"\n" +
        "  },\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": 4,\n" +
        "    \"mnpMsisdn\": \"59899077937\",\n" +
        "    \"mnpImsi\": \"748026871012345\",\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 5,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"sgsnNumber\": \"5982133021\",\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\"\n" +
        "}";

    String getGmlcMapAtiCsNotReachableResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": \"819203961904\"\n" +
        "    },\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 21,\n" +
        "      \"lac\": 32005,\n" +
        "      \"sac\": 38221\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"TAI\": {},\n" +
        "      \"ECGI\": {},\n" +
        "      \"GeographicalInformation\": {},\n" +
        "      \"GeodeticInformation\": {}\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"LSA\": {},\n" +
        "    \"RAI\": {},\n" +
        "    \"CGIorSAIorLAI\": {},\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {}\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {},\n" +
        "  \"MNPInfoResult\": {},\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 1575,\n" +
        "  \"currentLocationRetrieved\": false,\n" +
        "  \"vlrNumber\": \"59899000231\",\n" +
        "  \"subscriberState\": \"netDetNotReachable\",\n" +
        "  \"notReachableReason\": \"imsiDetached\"\n" +
        "}";

    String gmlcMapLsmResponse1 = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS-PSL\",\n" +
        "  \"lcsReferenceNumber\": 153,\n" +
        "  \"gmlcTransactionId\": 13,\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": \"59899077937\",\n" +
        "    \"imsi\": \"748026871012345\",\n" +
        "    \"lmsi\": \"2915\",\n" +
        "    \"networkNodeNumber\": \"5982123007\",\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": \"134570\",\n" +
        "    \"vGmlcAddress\": \"157003\",\n" +
        "    \"pprAddress\": \"938012\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": 45.907005071640015,\n" +
        "      \"longitude\": -99.00022745132446,\n" +
        "      \"altitude\": 570,\n" +
        "      \"uncertaintySemiMajorAxis\": 24.522712143931038,\n" +
        "      \"uncertaintySemiMinorAxis\": 11.435888100000016,\n" +
        "      \"angleOfMajorAxis\": 30.0,\n" +
        "      \"uncertaintyAltitude\": 79.5430243255239,\n" +
        "      \"confidence\": 5\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": 0,\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"sac\": 17185\n" +
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
        "      \"horizontalSpeed\": 101,\n" +
        "      \"bearing\": 3,\n" +
        "      \"verticalSpeed\": 2,\n" +
        "      \"uncertaintyHorizontalSpeed\": 5,\n" +
        "      \"uncertaintyVerticalSpeed\": 1,\n" +
        "      \"velocityType\": \"HorizontalWithVerticalVelocityAndUncertainty\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    String gmlcMapLsmResponse2 = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS-PSL\",\n" +
        "  \"lcsReferenceNumber\": 153,\n" +
        "  \"gmlcTransactionId\": 6,\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": \"59899077937\",\n" +
        "    \"imsi\": \"748026871012345\",\n" +
        "    \"lmsi\": \"2915\",\n" +
        "    \"networkNodeNumber\": \"5982123007\",\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": \"134570\",\n" +
        "    \"vGmlcAddress\": \"157003\",\n" +
        "    \"pprAddress\": \"938012\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidArc\",\n" +
        "      \"latitude\": 45.907005071640015,\n" +
        "      \"longitude\": -99.00022745132446,\n" +
        "      \"innerRadius\": 5,\n" +
        "      \"uncertaintyInnerRadius\": 1.0000000000000009,\n" +
        "      \"offsetAngle\": 20.0,\n" +
        "      \"includedAngle\": 20.0,\n" +
        "      \"confidence\": 2\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": 0,\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"ci\": 17185\n" +
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
        "      \"horizontalSpeed\": 101,\n" +
        "      \"bearing\": 3,\n" +
        "      \"verticalSpeed\": 2,\n" +
        "      \"uncertaintyHorizontalSpeed\": 5,\n" +
        "      \"uncertaintyVerticalSpeed\": 1,\n" +
        "      \"velocityType\": \"HorizontalWithVerticalVelocityAndUncertainty\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    String gmlcMapPsiCsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": \"819203961904\"\n" +
        "    },\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 2,\n" +
        "      \"lac\": 53201,\n" +
        "      \"sac\": 23479\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"latitude\": -23.29102635383606,\n" +
        "      \"longitude\": 109.97780084609985,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 18.531167061100025\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"latitude\": -24.010008573532104,\n" +
        "      \"longitude\": 110.00985860824585,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 9.487171000000012,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"tac\": 14649\n" +
        "      },\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"eNBId\": 334,\n" +
        "        \"ci\": 74\n" +
        "      },\n" +
        "      \"GeographicalInformation\": {\n" +
        "        \"latitude\": -23.29102635383606,\n" +
        "        \"longitude\": 109.97780084609985,\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"uncertainty\": 18.531167061100025\n" +
        "      },\n" +
        "      \"GeodeticInformation\": {\n" +
        "        \"latitude\": -24.010008573532104,\n" +
        "        \"longitude\": 110.00985860824585,\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"uncertainty\": 9.487171000000012,\n" +
        "        \"confidence\": 1,\n" +
        "        \"screeningAndPresentationIndicators\": 3\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"LSA\": {},\n" +
        "    \"RAI\": {},\n" +
        "    \"CGIorSAIorLAI\": {},\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {}\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {},\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": 4,\n" +
        "    \"mnpMsisdn\": \"59899077937\",\n" +
        "    \"mnpImsi\": \"748026871012345\",\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 1,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"imsi\": \"124356871012345\",\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"lmsi\": \"2915\",\n" +
        "  \"vlrNumber\": \"59899000231\",\n" +
        "  \"mscNumber\": \"5982123007\",\n" +
        "  \"mmeName\": \"MME7480001\",\n" +
        "  \"subscriberState\": \"camelBusy\",\n" +
        "  \"msClassmark\": \"003\"\n" +
        "}";

    String gmlcMapPsiPsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {},\n" +
        "    \"CGIorSAIorLAI\": {},\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"TAI\": {},\n" +
        "      \"ECGI\": {},\n" +
        "      \"GeographicalInformation\": {},\n" +
        "      \"GeodeticInformation\": {}\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"LSA\": {\n" +
        "      \"lsaIdType\": \"Universal\",\n" +
        "      \"lsaId\": \"131\"\n" +
        "    },\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 23,\n" +
        "      \"lac\": 32006,\n" +
        "      \"sac\": 38222\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 48.00009369850159,\n" +
        "      \"longitude\": -121.40008449554443,\n" +
        "      \"uncertainty\": 9.487171000000012\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"latitude\": 24.010008573532104,\n" +
        "      \"longitude\": -99.00179386138916,\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"uncertainty\": 4.641000000000004,\n" +
        "      \"confidence\": 2,\n" +
        "      \"screeningAndPresentationIndicators\": 2\n" +
        "    }\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {\n" +
        "    \"msNetworkCapability\": \"43879748\",\n" +
        "    \"msRadioAccessCapability\": \"8549839852932239493928\"\n" +
        "  },\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": 4,\n" +
        "    \"mnpMsisdn\": \"59899077937\",\n" +
        "    \"mnpImsi\": \"748026871012345\",\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 14571,\n" +
        "  \"currentLocationRetrieved\": false,\n" +
        "  \"imsi\": \"124356871012345\",\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"lmsi\": \"2915\",\n" +
        "  \"sgsnNumber\": \"5982133021\",\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\"\n" +
        "}";

    String gmlcLteLcsResponse = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"lcsReferenceNumber\": 359,\n" +
        "  \"gmlcTransactionId\": 14,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": \"573195897484\",\n" +
        "    \"imsi\": \"732101509580853\",\n" +
        "    \"lmsi\": \"7213917157\",\n" +
        "    \"mmeName\": \"simulator.be-connect.us\",\n" +
        "    \"mmeRealm\": \"be-connect.us\",\n" +
        "    \"sgsnNumber\": \"5730100028\",\n" +
        "    \"sgsnName\": \"simulator.be-connect.us\",\n" +
        "    \"sgsnRealm\": \"be-connect.us\",\n" +
        "    \"3GPPAAAServerName\": \"aaa001\",\n" +
        "    \"gmlcAddress\": \"?*\\u0015?\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": 33.99999260902405,\n" +
        "      \"longitude\": 55.99999666213989,\n" +
        "      \"altitude\": 200,\n" +
        "      \"uncertaintySemiMajorAxis\": 1.0000000000000009,\n" +
        "      \"uncertaintySemiMinorAxis\": 2.100000000000002,\n" +
        "      \"angleOfMajorAxis\": 4.0,\n" +
        "      \"uncertaintyAltitude\": 11.435888100000016,\n" +
        "      \"confidence\": 80\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"CGIorSAIorESMLCCellInfo\": {\n" +
        "      \"CGI\": {\n" +
        "        \"mcc\": 733,\n" +
        "        \"mnc\": 233,\n" +
        "        \"lac\": 12336,\n" +
        "        \"ci\": 12344\n" +
        "      },\n" +
        "      \"SAI\": {\n" +
        "        \"mcc\": 733,\n" +
        "        \"mnc\": 233,\n" +
        "        \"lac\": 12336,\n" +
        "        \"sac\": 12344\n" +
        "      },\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 733,\n" +
        "        \"mnc\": 233,\n" +
        "        \"eNBId\": 3158064,\n" +
        "        \"ci\": 52,\n" +
        "        \"cellPortionId\": 197\n" +
        "      }\n" +
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
        "      \"eUtranPositioningData\": \"313233\"\n" +
        "    },\n" +
        "    \"VelocityEstimate\": {\n" +
        "      \"horizontalSpeed\": 20,\n" +
        "      \"bearing\": 0,\n" +
        "      \"verticalSpeed\": 0,\n" +
        "      \"uncertaintyHorizontalSpeed\": 0,\n" +
        "      \"uncertaintyVerticalSpeed\": 0,\n" +
        "      \"velocityType\": \"HorizontalVelocity\"\n" +
        "    },\n" +
        "    \"civicAddress\": \"Calle 2 Sur 20-185, 050022, Medellin, Colombia\",\n" +
        "    \"barometricPressure\": 1012\n" +
        "  }\n" +
        "}";

    String getGetGmlcLteLcsResponse2 = "";

    String gmlcDiameterShUdrResponse = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"msisdn\": \"59899077937\",\n" +
        "    \"imsPublicIdentity\": \"sip:john.doe@hp.com\"\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": true,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": \"56034254999\"\n" +
        "    },\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"ci\": 20042\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 19.484424591064453,\n" +
        "      \"longitude\": -99.23969507217407,\n" +
        "      \"uncertainty\": 0.0\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010008573532104,\n" +
        "      \"longitude\": 110.00985860824585,\n" +
        "      \"uncertainty\": 98.34705943388394,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"mscNumber\": \"59897901234\",\n" +
        "    \"vlrNumber\": \"59897901234\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 100\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"ci\": 20042\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.29102635383606,\n" +
        "      \"longitude\": 109.97780084609985,\n" +
        "      \"uncertainty\": 45.599173134922395\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010008573532104,\n" +
        "      \"longitude\": 110.00985860824585,\n" +
        "      \"uncertainty\": 98.34705943388394,\n" +
        "      \"confidence\": 0,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"sgsnNumber\": \"59897904322\",\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"ageOfLocationInformation\": 100\n" +
        "  },\n" +
        "  \"EPSLocationInformation\": {\n" +
        "    \"TAI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"tac\": 14649\n" +
        "    },\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"eNBId\": 334,\n" +
        "      \"ci\": 74\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.29102635383606,\n" +
        "      \"longitude\": 109.97780084609985,\n" +
        "      \"uncertainty\": 45.599173134922395\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010008573532104,\n" +
        "      \"longitude\": 110.00985860824585,\n" +
        "      \"uncertainty\": 98.34705943388394,\n" +
        "      \"confidence\": 0,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"mmeName\": \"mme742@be-connect.us\",\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"ageOfLocationInformation\": 100,\n" +
        "    \"csgId\": \"8191\"\n" +
        "  }\n" +
        "}";

    @Test
    public void testMapAtiCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gmlcMapAtiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gmlcMapAtiCsResponse)));

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
/*
{
  "sid": "GL2ab4da3b46d743878beea4fa93d0647a",
  "date_created": "Fri, 1 Feb 2019 16:40:32 -0200",
  "date_updated": "Fri, 1 Feb 2019 16:40:32 -0200",
  "date_executed": "Fri, 1 Feb 2019 16:40:32 -0200",
  "account_sid": "ACae6e420f425248d6a26948c17a9e2acf",
  "device_identifier": "59899077937",
  "imei": 11714004661050,
  "geolocation_type": "Immediate",
  "geolocation_data": {
    "mobile_country_code": 732,
    "mobile_network_code": 103,
    "location_area_code": 32005,
    "cell_id": 74,
    "service_area_code": 38221,
    "enodeb_id": 334,
    "network_entity_address": 5982123007,
    "network_entity_name": "MME7480001",
    "location_age": 5,
    "subscriber_state": "camelBusy",
    "tracking_area_code": 14649,
    "type_of_shape": "EllipsoidPointWithUncertaintyCircle",
    "device_latitude": "-45.002102851867676",
    "device_longitude": "110.10006666183472",
    "uncertainty": 4.641000000000004,
    "confidence": 1
  },
  "api_version": "2012-04-24",
  "uri": "/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Geolocation/Immediate/GL2ab4da3b46d743878beea4fa93d0647a.json"

 */
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        //assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002102851867676);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.10006666183472);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 1);
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
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        //assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002102851867676);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.10006666183472);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 1);
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
    public void testMapAtiCreateNotApiCompliantImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcMapAtiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

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
    public void testMapAtiUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

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
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
    public void testMapAtiNotApiCompliantUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_positioning_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") != null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
    public void testMapAtiDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

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
        geolocationParams.add("ResponseStatus", "successful");
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
    public void testMapLsmCreateAndGetNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

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
        geolocationParams.add("ReferenceNumber","3");
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
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("109"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("8304"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("17185"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("109"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("8304"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("17185"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5982123007"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
    public void testMapLsmCreateNotApiCompliantNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

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
    public void testMapLsmUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType, responseStatus, cellId, mobileCountryCode, mobileNetworkCode,
            locationAreaCode, networkEntityAddress, networkEntityName, ageOfLocationInfo, deviceLatitude, deviceLongitude, locationTimestamp,
            lastGeolocationResponse;

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
        geolocationParams.add("ReferenceNumber", "3");
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
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "01");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800021");
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
        assertTrue(geolocationJson.get("msisdn").getAsString().equals("59899077937"));
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("2915"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 3);
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
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
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "partially-successful");
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
        assertTrue(geolocationJson.get("msisdn").getAsString().equals("59899077937"));
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("2915"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 3);
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
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
    public void testMapLsmNotApiCompliantUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
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
        geolocationParamsUpdate.add("OccurrenceInfo","once");
        geolocationParamsUpdate.add("ReferenceNumber","33");
        geolocationParamsUpdate.add("ServiceTypeID","0");
        geolocationParamsUpdate.add("EventIntervalTime","60");
        geolocationParamsUpdate.add("EventReportingAmount","5");
        geolocationParamsUpdate.add("EventReportingInterval","180");
        geolocationParamsUpdate.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
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
    public void testMapLsmDeleteNotificationGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

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

    @Test
    public void testMapPsiCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("PsiService", "true");
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
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("01171400466105"));
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("5"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("3479"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("mme7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 5092171L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals("13295"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString().equals("132952"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("98.34705943388394"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("90"));
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
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("01171400466105"));
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals("1"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals("5"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("3479"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("mme7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 5092171L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals("13295"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString().equals("132952"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("98.34705943388394"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("90"));
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

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    public void testMapPsiUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, responseStatus, imsi, imei, lmsi, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, lteCellId, tac, rai,
            typeOfShape, deviceLatitude, deviceLongitude, locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("PsiService", "true");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MSISDN", msisdn = "59899077937");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871012345");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466105");
        geolocationParamsUpdate.add("LMSI", lmsi = "2915");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "03");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "3579");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092171");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800021");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "camelBusy");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaId", rai = "132952");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("98.34705943388394"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("90"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("MSISDN", msisdn = "59898999012");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871054321");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466104");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "02");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092172");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaId", rai = "132952");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "partially-successful");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "API Not Compliant");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("98.34705943388394"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("90"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    @Category(FeatureAltTests.class)
    public void testMapPsiCreateNotApiCompliantImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcMapPsiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST with one wrong mandatory parameter
        // Parameter values Assignment, PsiService is not API compliant
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("PsiService", "yes");
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
    @Category(FeatureAltTests.class)
    public void testMapPsiNotApiCompliantUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, responseStatus, cellId, rai, tac, lteCellId, locationAreaCode,
            mobileCountryCode, mobileNetworkCode, networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState,
            typeOfShape, deviceLatitude, deviceLongitude, internetAddress, physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00",
            lastGeolocationResponse;

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
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "72.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "West 170.908134"); // WGS84 not compliant
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_positioning_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") != null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("MSISDN", msisdn = "59898999012");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871054321");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466104");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "02");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092172");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaId", rai = "132952");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "S43\u00b038'19.39''");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "E169\u00b028'49.07''");
        geolocationParamsUpdate.add("InternetAddress", internetAddress = "180.7.2.141");
        geolocationParamsUpdate.add("PhysicalAddress", physicalAddress = "A8-77-CA-29-32-D1");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:31:28.388-05:00");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
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
    public void testMapPsiDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("PsiService", "true");
        geolocationParams.add("ResponseStatus", "successful");
        geolocationParams.add("MSISDN", "59899077937");
        geolocationParams.add("IMSI", "124356871012345");
        geolocationParams.add("IMEI", "01171400466105");
        geolocationParams.add("LMSI", "2915");
        geolocationParams.add("MobileCountryCode", "749");
        geolocationParams.add("MobileNetworkCode", "03");
        geolocationParams.add("LocationAreaCode", "321");
        geolocationParams.add("CellId", "3579");
        geolocationParams.add("LteCellId", "5092171");
        geolocationParams.add("NetworkEntityAddress", "5980042343201");
        geolocationParams.add("NetworkEntityName", "mme74800021");
        geolocationParams.add("SubscriberState", "camelBusy");
        geolocationParams.add("TrackingAreaCode", "13295");
        geolocationParams.add("RoutingAreaId", "132952");
        geolocationParams.add("LocationAge", "0");
        geolocationParams.add("TypeOfShape", "ellipsoidArc");
        geolocationParams.add("DeviceLatitude", "34.908134");
        geolocationParams.add("DeviceLongitude", "-55.087134");
        geolocationParams.add("LocationTimestamp", "2016-04-17T20:28:40.690-03:00");
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
    public void testLteLcsCreateAndGetNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "456");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ClientType","emergency");
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("4294967295"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 371);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("54108"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals("2718"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 7890104);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5989900021"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("1.0000000000000009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("2.100000000000002"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("4.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("80"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("200"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("11.435888100000016"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals("20"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Avenida Italia 8973, 11500, Montevideo, Uruguay"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1013);
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals("4294967295"));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status") == null);
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 371);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals("54108"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals("2718"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 7890104);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals("5989900021"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("1.0000000000000009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("2.100000000000002"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("4.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("80"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("200"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("11.435888100000016"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals("20"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Avenida Italia 8973, 11500, Montevideo, Uruguay"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1013);
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
    public void testMapLteLcsUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sai, tac, lteCellId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress, barometricPressure,
            locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "leaving");
        geolocationParams.add("GeofenceType", geofenceType = "trackingAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "301");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ClientType","emergency");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("CoreNetwork","LTE");
        geolocationParamsUpdate.add("Priority","normal");
        geolocationParamsUpdate.add("MSISDN", msisdn = "59898999012");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871054321");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466104");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("ReferenceNumber", referenceNumber = "239");
        geolocationParamsUpdate.add("HorizontalAccuracy","1000");
        geolocationParamsUpdate.add("VerticalAccuracy","5000");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "01");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("Sai", sai = "24327");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092171");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800021@be-connect.us");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
        geolocationParamsUpdate.add("HorizontalSpeed", horizontalSpeed = "140");
        geolocationParamsUpdate.add("VerticalSpeed", verticalSpeed = "10");
        geolocationParamsUpdate.add("CivicAddress", civicAddress = "Boulevard Artigas 2710, 11300, Montevideo, Uruguay");
        geolocationParamsUpdate.add("BarometricPressure", barometricPressure = "1039");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == Long.valueOf(referenceNumber));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("1.0000000000000009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("2.100000000000002"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("4.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("80"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("200"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("11.435888100000016"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals(horizontalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals(verticalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals(civicAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == Long.valueOf(barometricPressure));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("CoreNetwork","LTE");
        geolocationParamsUpdate.add("Priority","normal");
        geolocationParamsUpdate.add("MSISDN", msisdn = "5989899936");
        geolocationParamsUpdate.add("IMSI", imsi = "432156871054321");
        geolocationParamsUpdate.add("IMEI", imei = "011714004661041");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967293");
        geolocationParamsUpdate.add("ReferenceNumber", referenceNumber = "239");
        geolocationParamsUpdate.add("HorizontalAccuracy","100");
        geolocationParamsUpdate.add("VerticalAccuracy","50");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "02");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "329");
        geolocationParamsUpdate.add("CellId", cellId = "50231");
        geolocationParamsUpdate.add("Sai", sai = "24328");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092173");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "23296");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343243");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024@be-connect.us");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidPointWithUncertaintyEllipse");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "-21.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "75.087134");
        geolocationParamsUpdate.add("HorizontalSpeed", horizontalSpeed = "10");
        geolocationParamsUpdate.add("VerticalSpeed", verticalSpeed = "1");
        geolocationParamsUpdate.add("CivicAddress", civicAddress = "Boulevard Artigas 2705, 11300, Montevideo, Uruguay");
        geolocationParamsUpdate.add("BarometricPressure", barometricPressure = "1041");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "Not API Compliant");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == Long.valueOf(referenceNumber));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsString().equals("1.0000000000000009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsString().equals("2.100000000000002"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsString().equals("4.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsString().equals("80"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsString().equals("200"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsString().equals("11.435888100000016"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsString().equals("0.0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals(horizontalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals(verticalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsString().equals("0"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals(civicAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == Long.valueOf(barometricPressure));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());

    }

    @Test
    @Category(FeatureAltTests.class)
    public void testLteLcsCreateNotApiCompliantNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, deferredLocationEventType, geofenceType, geofenceId;

        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment, OccurrenceInfo is not API compliant
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","LTE");
        notificationGeolocationNotApiCompliantParams.add("Priority","high");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","low");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("GeofenceEventType", deferredLocationEventType = "inside");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", geofenceType = "locationAreaId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", geofenceId = "456");
        notificationGeolocationNotApiCompliantParams.add("OccurrenceInfo","twice");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
        notificationGeolocationNotApiCompliantParams.add("EventIntervalTime","60");
        notificationGeolocationNotApiCompliantParams.add("EventReportingAmount","5");
        notificationGeolocationNotApiCompliantParams.add("EventReportingInterval","180");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        notificationGeolocationNotApiCompliantParams.add("ClientName","Beconnect");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat","msisdn");
        notificationGeolocationNotApiCompliantParams.add("ClientType","emergency");
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
    @Category(FeatureAltTests.class)
    public void testLteLcsNotApiCompliantUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sai, tac, lteCellId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress, barometricPressure,
            locationTimestamp = "2016-04-17T20:28:40.690-03:00", lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "leaving");
        geolocationParams.add("GeofenceType", geofenceType = "trackingAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "301");
        geolocationParams.add("OccurrenceInfo","once");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("EventIntervalTime","60");
        geolocationParams.add("EventReportingAmount","5");
        geolocationParams.add("EventReportingInterval","180");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ClientType","emergency");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_positioning_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") != null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("CoreNetwork","LTE");
        geolocationParamsUpdate.add("Priority","normal");
        geolocationParamsUpdate.add("MSISDN", msisdn = "5989899936");
        geolocationParamsUpdate.add("IMSI", imsi = "432156871054321");
        geolocationParamsUpdate.add("IMEI", imei = "011714004661041");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967293");
        geolocationParamsUpdate.add("ReferenceNumber", referenceNumber = "239");
        geolocationParamsUpdate.add("HorizontalAccuracy","100");
        geolocationParamsUpdate.add("VerticalAccuracy","50");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "02");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "329");
        geolocationParamsUpdate.add("CellId", cellId = "50231");
        geolocationParamsUpdate.add("Sai", sai = "24328");
        geolocationParamsUpdate.add("LteCellId", lteCellId = "5092173");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "23296");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343243");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024@be-connect.us");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidPointWithUncertaintyEllipse");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "-21.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "75.087134");
        geolocationParamsUpdate.add("HorizontalSpeed", horizontalSpeed = "10");
        geolocationParamsUpdate.add("VerticalSpeed", verticalSpeed = "1");
        geolocationParamsUpdate.add("CivicAddress", civicAddress = "Boulevard Artigas 2705, 11300, Montevideo, Uruguay");
        geolocationParamsUpdate.add("BarometricPressure", barometricPressure = "1041");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:28:40.690-03:00");
        geolocationParamsUpdate.add("LastGeolocationResponse", lastGeolocationResponse = "true");
        geolocationParamsUpdate.add("Cause", "Not API Compliant");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == Long.valueOf(msisdn));
        assertTrue(geolocationJson.get("imsi").getAsString().equals(imsi));
        assertTrue(geolocationJson.get("imei").getAsString().equals(imei));
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == Long.valueOf(referenceNumber));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(lteCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals(typeOfShape));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals(horizontalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals(verticalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals(civicAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == Long.valueOf(barometricPressure));
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
    public void testLteLcsDeleteNotificationGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("Priority","normal");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","tolerant");
        geolocationParams.add("LocationEstimateType","notificationVerificationOnly");
        geolocationParams.add("GeofenceEventType", deferredLocationEventType = "max-interval-expiration");
        geolocationParams.add("GeofenceType", geofenceType = "eUtranCellId");
        geolocationParams.add("GeofenceId", geofenceId = "406");
        geolocationParams.add("OccurrenceInfo","multiple");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","1");
        geolocationParams.add("EventIntervalTime","90");
        geolocationParams.add("EventReportingAmount","2");
        geolocationParams.add("EventReportingInterval","360");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("ClientName","Trg");
        geolocationParams.add("ClientNameFormat","sip");
        geolocationParams.add("ClientType","vas");
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
