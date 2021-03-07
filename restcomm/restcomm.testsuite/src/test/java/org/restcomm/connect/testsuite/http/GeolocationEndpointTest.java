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
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
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
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressPresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 819203961904\n" +
        "    },\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 21,\n" +
        "      \"lac\": 32005,\n" +
        "      \"sac\": 38221\n" +
        "    },\n" +
        "    \"saiPresent\": true,\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -44.721018,\n" +
        "      \"longitude\": 105.993412,\n" +
        "      \"uncertainty\": 9.5\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -45.002103,\n" +
        "      \"longitude\": 110.100067,\n" +
        "      \"uncertainty\": 4.6,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"ageOfLocationInformation\": 5,\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"vlrNumber\": 59899000231,\n" +
        "    \"mscNumber\": 5982123007,\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"eci\": 207631107,\n" +
        "        \"eNBId\": 811059,\n" +
        "        \"ci\": 3\n" +
        "      },\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"tac\": 14645\n" +
        "      },\n" +
        "      \"GeographicalInformation\": {\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"latitude\": -44.721018,\n" +
        "        \"longitude\": 105.993412,\n" +
        "        \"uncertainty\": 9.5\n" +
        "      },\n" +
        "      \"GeodeticInformation\": {\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"latitude\": -45.002103,\n" +
        "        \"longitude\": 110.100067,\n" +
        "        \"uncertainty\": 4.6,\n" +
        "        \"confidence\": 1,\n" +
        "        \"screeningAndPresentationIndicators\": 3\n" +
        "      },\n" +
        "      \"ageOfLocationInformation\": 0,\n" +
        "      \"currentLocationRetrieved\": true,\n" +
        "      \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"subscriberState\": \"camelBusy\",\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberNotPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"msClassmark\": \"393A52\"\n" +
        "}";

    String gmlcMapAtiPsResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"EPSLocationInformation\": {}\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 23,\n" +
        "      \"lac\": 32006,\n" +
        "      \"sac\": 38222\n" +
        "    },\n" +
        "    \"saiPresent\": true,\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"LSA\": {\n" +
        "      \"lsaIdType\": \"Universal\",\n" +
        "      \"lsaId\": \"131\"\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -44.721018,\n" +
        "      \"longitude\": 105.993412,\n" +
        "      \"uncertainty\": 9.5\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"ageOfLocationInformation\": 5,\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"sgsnNumber\": 5982133021\n" +
        "  },\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\",\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberNotPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {\n" +
        "    \"msNetworkCapability\": \"3130303032303331\",\n" +
        "    \"msRadioAccessCapability\": \"31303030323033313730383134\"\n" +
        "  }\n" +
        "}";

    String gmlcMapAtiCsNotReachableResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressPresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 819203961904\n" +
        "    },\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 21,\n" +
        "      \"lac\": 32005,\n" +
        "      \"sac\": 38221\n" +
        "    },\n" +
        "    \"saiPresent\": true,\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"ageOfLocationInformation\": 1575,\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"vlrNumber\": 59899000231,\n" +
        "    \"EPSLocationInformation\": {}\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"subscriberState\": \"netDetNotReachable\",\n" +
        "  \"notReachableReason\": \"imsiDetached\"\n" +
        "}";

    String gmlcMapAtiLocationErrorResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[ATI NOT ALLOWED, MAP error code: 49]\"\n" +
        "}";

    String gmlcMapLsmResponse1 = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRILCS-PSL\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 15270,\n" +
        "  \"lcsReferenceNumber\": 20,\n" +
        "  \"SRILCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa04.aaa3000.aaa.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"hGmlcAddress\": \"181.104.201.3\",\n" +
        "    \"vGmlcAddress\": \"180.53.105.48\",\n" +
        "    \"pprAddress\": \"181.104.97.21\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": 45.907005,\n" +
        "      \"longitude\": -99.000227,\n" +
        "      \"altitude\": 570,\n" +
        "      \"uncertaintySemiMajorAxis\": 24.5,\n" +
        "      \"uncertaintySemiMinorAxis\": 11.4,\n" +
        "      \"angleOfMajorAxis\": 30.0,\n" +
        "      \"uncertaintyAltitude\": 79.5,\n" +
        "      \"confidence\": 5\n" +
        "    },\n" +
        "    \"AdditionalLocationEstimate\": {},\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"ci\": 17185\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"3239313532\",\n" +
        "      \"geranGanssPositioningData\": \"383230313335\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"393333363031\",\n" +
        "      \"utranGanssPositioningData\": \"393333363030\"\n" +
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

    String gmlcMapLsmResponse2 = " {\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRILCS-PSL\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 15271,\n" +
        "  \"lcsReferenceNumber\": 28,\n" +
        "  \"SRILCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa04.aaa3000.aaa.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"hGmlcAddress\": \"181.104.201.3\",\n" +
        "    \"vGmlcAddress\": \"180.53.105.48\",\n" +
        "    \"pprAddress\": \"181.104.97.21\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidArc\",\n" +
        "      \"latitude\": 45.907005,\n" +
        "      \"longitude\": -99.000227,\n" +
        "      \"innerRadius\": 5,\n" +
        "      \"uncertaintyInnerRadius\": 1.0,\n" +
        "      \"offsetAngle\": 20.0,\n" +
        "      \"includedAngle\": 20.0,\n" +
        "      \"confidence\": 2\n" +
        "    },\n" +
        "    \"AdditionalLocationEstimate\": {},\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"ci\": 17185\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"3239313532\",\n" +
        "      \"geranGanssPositioningData\": \"383230313335\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"393333363031\",\n" +
        "      \"utranGanssPositioningData\": \"393333363030\"\n" +
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

    String gmlcMapLsmResponse3 = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRILCS-PSL\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 15281,\n" +
        "  \"lcsReferenceNumber\": 25,\n" +
        "  \"SRILCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa04.aaa3000.aaa.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"hGmlcAddress\": \"181.104.201.3\",\n" +
        "    \"vGmlcAddress\": \"180.53.105.48\",\n" +
        "    \"pprAddress\": \"181.104.97.21\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"Polygon\"\n" +
        "    },\n" +
        "    \"AdditionalLocationEstimate\": {\n" +
        "      \"typeOfShape\": \"Polygon\",\n" +
        "      \"numberOfPoints\": 4,\n" +
        "      \"polygonPoint1\": {\n" +
        "        \"latitude\": 26.646513,\n" +
        "        \"longitude\": 73.492076\n" +
        "      },\n" +
        "      \"polygonPoint2\": {\n" +
        "        \"latitude\": 26.648026,\n" +
        "        \"longitude\": 73.495703\n" +
        "      },\n" +
        "      \"polygonPoint3\": {\n" +
        "        \"latitude\": 26.648744,\n" +
        "        \"longitude\": 73.495638\n" +
        "      },\n" +
        "      \"polygonPoint4\": {\n" +
        "        \"latitude\": 26.648755,\n" +
        "        \"longitude\": 73.495724\n" +
        "      },\n" +
        "      \"polygonCentroid\": {\n" +
        "        \"latitude\": 26.647746,\n" +
        "        \"longitude\": 73.494472\n" +
        "      }\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"sac\": 17185\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"3239313532\",\n" +
        "      \"geranGanssPositioningData\": \"383230313335\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"393333363031\",\n" +
        "      \"utranGanssPositioningData\": \"393333363030\"\n" +
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

    String gmlcMapLsmLocationErrorResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"clientReferenceNumber\": 153,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[POSITION METHOD FAILURE, MAP error code: 54]\"\n" +
        "}";

    String gmlcMapPsiCsResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressPresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 819203961904\n" +
        "    },\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 2,\n" +
        "      \"lac\": 53201,\n" +
        "      \"sac\": 23479\n" +
        "    },\n" +
        "    \"saiPresent\": true,\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.291026,\n" +
        "      \"longitude\": 109.977801,\n" +
        "      \"uncertainty\": 18.5\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010009,\n" +
        "      \"longitude\": 110.009859,\n" +
        "      \"uncertainty\": 9.5,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"ageOfLocationInformation\": 1,\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"vlrNumber\": 59899000231,\n" +
        "    \"mscNumber\": 5982123007,\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"eci\": 207631107,\n" +
        "        \"eNBId\": 811059,\n" +
        "        \"ci\": 3\n" +
        "      },\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"tac\": 14645\n" +
        "      },\n" +
        "      \"GeographicalInformation\": {\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"latitude\": -23.291026,\n" +
        "        \"longitude\": 109.977801,\n" +
        "        \"uncertainty\": 18.5\n" +
        "      },\n" +
        "      \"GeodeticInformation\": {\n" +
        "        \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "        \"latitude\": -24.010009,\n" +
        "        \"longitude\": 110.009859,\n" +
        "        \"uncertainty\": 9.5,\n" +
        "        \"confidence\": 1,\n" +
        "        \"screeningAndPresentationIndicators\": 3\n" +
        "      },\n" +
        "      \"ageOfLocationInformation\": 1,\n" +
        "      \"currentLocationRetrieved\": true,\n" +
        "      \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imsi\": 124356871012345,\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"lmsi\": \"7202eb37\",\n" +
        "  \"subscriberState\": \"assumedIdle\",\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberNotPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"msClassmark\": \"393A52\"\n" +
        "}";

    String gmlcMapPsiPsResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"EPSLocationInformation\": {}\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 23,\n" +
        "      \"lac\": 32006,\n" +
        "      \"ci\": 38222\n" +
        "    },\n" +
        "    \"saiPresent\": false,\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"LSA\": {\n" +
        "      \"lsaIdType\": \"Universal\",\n" +
        "      \"lsaId\": \"131\"\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 48.000094,\n" +
        "      \"longitude\": -121.400084,\n" +
        "      \"uncertainty\": 9.5\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 24.010009,\n" +
        "      \"longitude\": -99.001794,\n" +
        "      \"uncertainty\": 4.6,\n" +
        "      \"confidence\": 2,\n" +
        "      \"screeningAndPresentationIndicators\": 2\n" +
        "    },\n" +
        "    \"ageOfLocationInformation\": 14571,\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"sgsnNumber\": 5982133021\n" +
        "  },\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imsi\": 124356871012345,\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"lmsi\": \"71ffacce\",\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\",\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberNotPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": \"598123\"\n" +
        "  },\n" +
        "  \"GPRSMSClass\": {\n" +
        "    \"msNetworkCapability\": \"3130303032303331\",\n" +
        "    \"msRadioAccessCapability\": \"31303030323033313730383134\"\n" +
        "  }\n" +
        "}";

    String gmlcMapPsiLocationErrorResponse = "{\n" +
        "  \"network\": \"GSM/UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[Dialog Rejected: PSI Application Context Not Supported]\"\n" +
        "}";

    String gmlcLteLcsResponse1 = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter SLh-SLg(ELP)\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 1971,\n" +
        "  \"lcsReferenceNumber\": 18,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 4294967295,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"mmeRealm\": \"restcomm.org\",\n" +
        "    \"sgsnNumber\": 5989900021,\n" +
        "    \"sgsnName\": \"sgsnc03.sgsner3000.sgsn.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"sgsnRealm\": \"restcomm.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa04.aaa3000.aaa.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"gmlcAddress\": \"200.123.44.108\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidArc\",\n" +
        "      \"latitude\": 42.999995,\n" +
        "      \"longitude\": 87.199988,\n" +
        "      \"innerRadius\": 32,\n" +
        "      \"uncertaintyInnerRadius\": 3.3,\n" +
        "      \"offsetAngle\": 12.0,\n" +
        "      \"includedAngle\": 20.0,\n" +
        "      \"confidence\": 20\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 133,\n" +
        "      \"eci\": 31122709,\n" +
        "      \"eNBId\": 121573,\n" +
        "      \"ci\": 21,\n" +
        "      \"cellPortionId\": 3\n" +
        "    },\n" +
        "    \"E-UTRANPositioningInfo\": {\n" +
        "      \"eUtranPositioningData\": \"235C6A1911\"\n" +
        "    },\n" +
        "    \"VelocityEstimate\": {\n" +
        "      \"horizontalSpeed\": 20,\n" +
        "      \"bearing\": 0,\n" +
        "      \"verticalSpeed\": 0,\n" +
        "      \"uncertaintyHorizontalSpeed\": 0,\n" +
        "      \"uncertaintyVerticalSpeed\": 0,\n" +
        "      \"velocityType\": \"HorizontalVelocity\"\n" +
        "    },\n" +
        "    \"civicAddress\": \"Avenida Italia 8973, 11500, Montevideo, Uruguay\",\n" +
        "    \"barometricPressure\": 1013\n" +
        "  }\n" +
        "}";

    String gmlcLteLcsResponse2 = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter SLh-SLg(ELP)\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 1979,\n" +
        "  \"lcsReferenceNumber\": 31,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": 573195897484,\n" +
        "    \"imsi\": 732101509580853,\n" +
        "    \"lmsi\": 7213917157,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"mmeRealm\": \"restcomm.org\",\n" +
        "    \"sgsnNumber\": 5730100028,\n" +
        "    \"sgsnName\": \"sgsnc03.sgsner3000.sgsn.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"sgsnRealm\": \"restcomm.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa04.aaa3000.aaa.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"gmlcAddress\": \"191.42.21.204\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": 71.999996,\n" +
        "      \"longitude\": -3.999989,\n" +
        "      \"altitude\": 200,\n" +
        "      \"uncertaintySemiMajorAxis\": 2.1,\n" +
        "      \"uncertaintySemiMinorAxis\": 2.1,\n" +
        "      \"angleOfMajorAxis\": 4.0,\n" +
        "      \"uncertaintyAltitude\": 11.4,\n" +
        "      \"confidence\": 80\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 733,\n" +
        "      \"mnc\": 233,\n" +
        "      \"lac\": 12336,\n" +
        "      \"ci\": 12344\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"30\",\n" +
        "      \"geranGanssPositioningData\": \"30\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"3831\",\n" +
        "      \"utranGanssPositioningData\": \"343033\",\n" +
        "      \"utranAdditionalPositioningData\": \"30\"\n" +
        "    },\n" +
        "    \"E-UTRANPositioningInfo\": {\n" +
        "      \"eUtranPositioningData\": \"235C6A1911\"\n" +
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

    String gmlcLteLcsResponse3 = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter SLh-SLg(ELP)\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"clientReferenceNumber\": 2979,\n" +
        "  \"lcsReferenceNumber\": 32,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": 573195897489,\n" +
        "    \"imsi\": 732101509580859,\n" +
        "    \"lmsi\": 7213917157,\n" +
        "    \"mmeName\": \"mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"mmeRealm\": \"restcomm.org\",\n" +
        "    \"sgsnNumber\": 5730100028,\n" +
        "    \"sgsnName\": \"sgsnc03.sgsner3000.sgsn.epc.mnc002.mcc748.3gppnetwork.org\",\n" +
        "    \"sgsnRealm\": \"restcomm.org\",\n" +
        "    \"3GPPAAAServerName\": \"aaa001\",\n" +
        "    \"gmlcAddress\": \"191.42.21.204\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"Polygon\",\n" +
        "      \"numberOfPoints\": 4,\n" +
        "      \"polygonPoint1\": {\n" +
        "        \"latitude\": 26.646513,\n" +
        "        \"longitude\": 73.492055\n" +
        "      },\n" +
        "      \"polygonPoint2\": {\n" +
        "        \"latitude\": 26.648026,\n" +
        "        \"longitude\": 73.495703\n" +
        "      },\n" +
        "      \"polygonPoint3\": {\n" +
        "        \"latitude\": 26.648734,\n" +
        "        \"longitude\": 73.495617\n" +
        "      },\n" +
        "      \"polygonPoint4\": {\n" +
        "        \"latitude\": 26.648744,\n" +
        "        \"longitude\": 73.495703\n" +
        "      },\n" +
        "      \"polygonCentroid\": {\n" +
        "        \"latitude\": 26.647743,\n" +
        "        \"longitude\": 73.494458\n" +
        "      }\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 733,\n" +
        "      \"mnc\": 233,\n" +
        "      \"eci\": 38676245,\n" +
        "      \"eNBId\": 151079,\n" +
        "      \"ci\": 21,\n" +
        "      \"cellPortionId\": 197\n" +
        "    },\n" +
        "    \"E-UTRANPositioningInfo\": {\n" +
        "      \"eUtranPositioningData\": \"235C6A1911\"\n" +
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

    String gmlcLteLcsLocationErrorResponse = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter SLg (ELP)\",\n" +
        "  \"operation\": \"PLR\",\n" +
        "  \"subscriberIdentity\": 59894455666,\n" +
        "  \"clientReferenceNumber\": 875,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[PLR/PLA SLg POSITIONING FAILED, Diameter error code: 4225]\"\n" +
        "}";

    String gmlcShUdrResponseCS = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter Sh\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"imsPublicIdentity\": \"sip:john.doe@hp.com\",\n" +
        "    \"msisdn\": 59899077937\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": true,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressPresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 56034254999\n" +
        "    },\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"sac\": 20042\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010009,\n" +
        "      \"longitude\": 110.009859,\n" +
        "      \"uncertainty\": 98.3,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"mscNumber\": 598978934,\n" +
        "    \"vlrNumber\": 598978935,\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"eci\": 38676245,\n" +
        "        \"eNBId\": 151079,\n" +
        "        \"ci\": 21\n" +
        "      },\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 103,\n" +
        "        \"tac\": 14645\n" +
        "      },\n" +
        "    \"GeographicalInformation\": {},\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"mmeName\": \"MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 1\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"EPSLocationInformation\": {},\n" +
        "  \"5GSLocationInformation\": {}\n" +
        "}";

    String gmlcShUdrResponsePS = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter Sh\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"imsPublicIdentity\": \"sip:john.doe@hp.com\",\n" +
        "    \"msisdn\": 59899077937\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {},\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"sac\": 20042\n" +
        "    },\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.291026,\n" +
        "      \"longitude\": 109.977801,\n" +
        "      \"uncertainty\": 45.6\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"sgsnNumber\": 598978936,\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"ageOfLocationInformation\": 5,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 21\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"+1\",\n" +
        "      \"daylightSavingTime\": 2\n" +
        "    },\n" +
        "    \"ratType\": \"GERAN\"\n" +
        "  },\n" +
        "  \"EPSLocationInformation\": {},\n" +
        "  \"5GSLocationInformation\": {}\n" +
        "}";

    String gmlcShUdrResponseEPS = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter Sh\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"msisdn\": 60192235906\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {},\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"EPSLocationInformation\": {\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"eci\": 38676245,\n" +
        "      \"eNBId\": 151079,\n" +
        "      \"ci\": 21\n" +
        "    },\n" +
        "    \"TAI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"tac\": 774\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 19.484425,\n" +
        "      \"longitude\": -99.239695,\n" +
        "      \"uncertainty\": 0.0\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {},\n" +
        "    \"mmeName\": \"MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 31\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"-9\",\n" +
        "      \"daylightSavingTime\": 0\n" +
        "    },\n" +
        "    \"ratType\": \"EUTRAN\"" +
        "  },\n" +
        "  \"5GSLocationInformation\": {}\n" +
        "}";

    String gmlcShUdrResponse5GS = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter Sh\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"msisdn\": 60192235906\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {},\n" +
        "  \"PSLocationInformation\": {},\n" +
        "  \"EPSLocationInformation\": {},\n" +
        "  \"5GSLocationInformation\": {\n" +
        "    \"NCGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"nci\": 512063008768\n" +
        "    },\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"eci\": 38676245,\n" +
        "      \"eNBId\": 151079,\n" +
        "      \"ci\": 21\n" +
        "    },\n" +
        "    \"TAI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"tac\": 774\n" +
        "    },\n" +
        "    \"amfAddress\": \"amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org\",\n" +
        "    \"smsfAddress\": \"smset12.smsf01.5gc.mnc012.mcc345.3gppnetwork.org\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 1\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"-3\",\n" +
        "      \"daylightSavingTime\": 0\n" +
        "    },\n" +
        "    \"ratType\": \"NR\"\n" +
        "  }\n" +
        "}";

    String gmlcDiameterShUdrResponse_all = "{\n" +
        "  \"network\": \"IMS\",\n" +
        "  \"protocol\": \"Diameter Sh\",\n" +
        "  \"operation\": \"UDR-UDA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"PublicIdentifiers\": {\n" +
        "    \"imsPublicIdentity\": \"sip:john.doe@hp.com\",\n" +
        "    \"msisdn\": 59898077937\n" +
        "  },\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": true,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressPresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 56034254999\n" +
        "    },\n" +
        "    \"SAI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"sac\": 20142\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 19.484425,\n" +
        "      \"longitude\": -99.239695,\n" +
        "      \"uncertainty\": 0.0\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010009,\n" +
        "      \"longitude\": 110.009859,\n" +
        "      \"uncertainty\": 98.3,\n" +
        "      \"confidence\": 1,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"mscNumber\": 598978934,\n" +
        "    \"vlrNumber\": 598978935,\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"EPSLocationInformation\": {\n" +
        "      \"ECGI\": {\n" +
        "        \"mcc\": 502,\n" +
        "        \"mnc\": 19,\n" +
        "        \"eci\": 38676245,\n" +
        "        \"eNBId\": 151079,\n" +
        "        \"ci\": 21\n" +
        "      },\n" +
        "      \"TAI\": {\n" +
        "        \"mcc\": 502,\n" +
        "        \"mnc\": 19,\n" +
        "        \"tac\": 774\n" +
        "      },\n" +
        "      \"LocalTimeZone\": {\n" +
        "        \"timeZone\": \"-5\",\n" +
        "        \"daylightSavingTime\": 0\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"PSLocationInformation\": {\n" +
        "    \"CGI\": {\n" +
        "      \"mcc\": 732,\n" +
        "      \"mnc\": 103,\n" +
        "      \"lac\": 1,\n" +
        "      \"ci\": 20042\n" +
        "    },\n" +
        "    \"RAI\": {\n" +
        "      \"mcc\": 748,\n" +
        "      \"mnc\": 1,\n" +
        "      \"lac\": 14645,\n" +
        "      \"rac\": 50\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.291026,\n" +
        "      \"longitude\": 109.977801,\n" +
        "      \"uncertainty\": 45.6\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010009,\n" +
        "      \"longitude\": 110.009859,\n" +
        "      \"uncertainty\": 98.3,\n" +
        "      \"confidence\": 0,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"sgsnNumber\": 598978936,\n" +
        "    \"currentLocationRetrieved\": false,\n" +
        "    \"ageOfLocationInformation\": 5,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 21\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"+1\",\n" +
        "      \"daylightSavingTime\": 2\n" +
        "    },\n" +
        "    \"ratType\": \"GERAN\"\n" +
        "  },\n" +
        "  \"EPSLocationInformation\": {\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"eci\": 38676245,\n" +
        "      \"eNBId\": 151079,\n" +
        "      \"ci\": 21\n" +
        "    },\n" +
        "    \"TAI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"tac\": 774\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.291026,\n" +
        "      \"longitude\": 109.977801,\n" +
        "      \"uncertainty\": 45.6\n" +
        "    },\n" +
        "    \"GeodeticInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -24.010009,\n" +
        "      \"longitude\": 110.009859,\n" +
        "      \"uncertainty\": 98.3,\n" +
        "      \"confidence\": 0,\n" +
        "      \"screeningAndPresentationIndicators\": 3\n" +
        "    },\n" +
        "    \"mmeName\": \"MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"csgId\": \"8191\",\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 31\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"-9\",\n" +
        "      \"daylightSavingTime\": 0\n" +
        "    },\n" +
        "    \"ratType\": \"EUTRAN\"\n" +
        "  },\n" +
        "  \"5GSLocationInformation\": {\n" +
        "    \"NCGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"nci\": 512063008768\n" +
        "    },\n" +
        "    \"ECGI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"eci\": 38676245,\n" +
        "      \"eNBId\": 151079,\n" +
        "      \"ci\": 21\n" +
        "    },\n" +
        "    \"TAI\": {\n" +
        "      \"mcc\": 502,\n" +
        "      \"mnc\": 19,\n" +
        "      \"tac\": 774\n" +
        "    },\n" +
        "    \"GeographicalInformation\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": -23.291026,\n" +
        "      \"longitude\": 109.977801,\n" +
        "      \"uncertainty\": 45.6\n" +
        "    },\n" +
        "    \"amfAddress\": \"amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org\",\n" +
        "    \"smsfAddress\": \"smset12.smsf01.5gc.mnc012.mcc345.3gppnetwork.org\",\n" +
        "    \"currentLocationRetrieved\": true,\n" +
        "    \"ageOfLocationInformation\": 0,\n" +
        "    \"VisitedPLMNId\": {\n" +
        "      \"mcc\": 598,\n" +
        "      \"mnc\": 1\n" +
        "    },\n" +
        "    \"LocalTimeZone\": {\n" +
        "      \"timeZone\": \"-3\",\n" +
        "      \"daylightSavingTime\": 0\n" +
        "    },\n" +
        "    \"ratType\": \"NR\"\n" +
        "  }\n" +
        "}";

    @Test
    public void testMapAtiCsCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Domain", "cs");
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("camelBusy"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.100067);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("camelBusy"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.100067);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapAtiPsCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiPsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiPsResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("Domain", "ps");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -44.721018);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 105.993412);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -44.721018);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 105.993412);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapAtiCsNotReachableCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsNotReachableResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsNotReachableResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("Domain", "cs");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 59899000231L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1575);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("netDetNotReachable"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason").getAsString().equals("imsiDetached"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 59899000231L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1575);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("netDetNotReachable"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason").getAsString().equals("imsiDetached"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    @Category(FeatureAltTests.class)
    public void testMapAtiCreateNotApiCompliantImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcMapAtiCsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // Test create Immediate type of Geolocation via POST with one missing mandatory parameter
        // Parameter values Assignment, DeviceIdentifier missing
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "10");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("camelBusy"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.100067);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("camelBusy"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == -45.002103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() == 110.100067);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("event_geofence_type") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, responseStatus, cellId, eCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, internetAddress,
            physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00", lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "1");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "709");
        geolocationParamsUpdate.add("CellId", cellId = "34580");
        geolocationParamsUpdate.add("ECellId", eCellId = "207631107");
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testFailedMapAtiCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiLocationErrorResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiLocationErrorResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[ATI NOT ALLOWED, MAP error code: 49]"));
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[ATI NOT ALLOWED, MAP error code: 49]"));
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testMapAtiDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsResponse)));

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", msisdn);
        geolocationParams.add("Operation", "ATI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("MobileCountryCode", "748");
        geolocationParams.add("MobileNetworkCode","1");
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
    public void testMapLsm1CreateAndGetNotificationGeolocationVasLastKnown()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");
        geolocationParams.add("ClientExternalID", "6543210987654321");
        geolocationParams.add("RequestorID", "http://www.example.com/index.html");
        geolocationParams.add("RequestorIDFormat", "url");
        geolocationParams.add("LocationEstimateType", "lastKnown");

        geolocationParams.add("ServiceTypeID", "9");
        // QoS parameters
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "127");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "false");
        geolocationParams.add("ResponseTime", "tolerant");

        geolocationParams.add("ReferenceNumber", "15270");
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm1CreateAndGetNotificationGeolocationEmergencyCurrent()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "emergency");
        geolocationParams.add("ClientExternalID", "6543210987654321");
        geolocationParams.add("LocationEstimateType", "current");

        geolocationParams.add("ServiceTypeID", "0");
        // QoS parameters
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "0");
        geolocationParams.add("VerticalAccuracy", "0");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("ReferenceNumber", "15271");
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm1CreateAndGetNotificationGeolocationVasDeferredGeofence()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");
        geolocationParams.add("ClientExternalID", "6543210987654321");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "cellGlobalId");
        geolocationParams.add("AreaEventId", geofenceId = "502-19-1472-15079");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "32767");

        geolocationParams.add("ReferenceNumber", "15273");
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 24.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 30.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 570);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 79.5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm2CreateAndGetNotificationGeolocationEmergencyGeofence()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse2)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse2)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "emergency");
        geolocationParams.add("ClientExternalID", "6543210987654321");

        geolocationParams.add("LocationEstimateType", "cancelDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "leaving");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "utranCellId");
        geolocationParams.add("AreaEventId", geofenceId = "502-17-134283263");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "500");

        geolocationParams.add("ReferenceNumber","15274");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15271);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15271);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.000227"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm3CreateAndGetNotificationGeolocationPlmnGeofence()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "operator");
        geolocationParams.add("ClientInternalID", "3");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "inside");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "locationAreaId");
        geolocationParams.add("AreaEventId", geofenceId = "736-2-13100");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "600");

        geolocationParams.add("ReferenceNumber","15281");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm3CreateAndGetNotificationGeolocationLawfulPeriodicLDR()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "lawful");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "periodic-ldr");

        geolocationParams.add("ServiceTypeID", "127");
        // QoS parameters
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "127");
        geolocationParams.add("VerticalAccuracy", "0");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "tolerant");

        // Periodic LDR parameters
        geolocationParams.add("PeriodicReportingAmount",  "8639999");
        geolocationParams.add("PeriodicReportingInterval","8639999");

        geolocationParams.add("ReferenceNumber","15282");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapLsm3CreateAndGetNotificationGeolocationVasAvailable()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "5989912340945");
        geolocationParams.add("ClientNameFormat", "msisdn");
        geolocationParams.add("ClientExternalID", "6543210987654321");
        geolocationParams.add("RequestorID", "Extended-SDP");
        geolocationParams.add("RequestorIDFormat", "name");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "available");

        geolocationParams.add("ServiceTypeID", "54");
        // QoS parameters
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "10");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "false");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("ReferenceNumber","15285");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15281);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647746"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494472"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 101);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // Test create Notification type of Geolocation via POST with one incorrect parameter
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);
        notificationGeolocationNotApiCompliantParams.add("Operation", "PSL");
        notificationGeolocationNotApiCompliantParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        notificationGeolocationNotApiCompliantParams.add("ClientType", "lawful");

        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType", "cancelDeferred");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationType", "available"); // <-- Not API compliant

        // Area Event parameters
        notificationGeolocationNotApiCompliantParams.add("AreaEventType",  "utranCellId");
        notificationGeolocationNotApiCompliantParams.add("AreaEventId", "502-17-134283263");
        notificationGeolocationNotApiCompliantParams.add("AreaEventOccurrence", "multiple");
        notificationGeolocationNotApiCompliantParams.add("PeriodicReportingInterval", "100");

        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber", "19270");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
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

        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "lawful");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType",  geofenceType = "utranCellId");
        geolocationParams.add("AreaEventId", geofenceId = "502-17-134283263");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "100");

        geolocationParams.add("ReferenceNumber", "19270");
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParamsUpdate.add("Priority", "high");
        geolocationParamsUpdate.add("HorizontalAccuracy", "127");
        geolocationParamsUpdate.add("VerticalAccuracy", "0");
        geolocationParamsUpdate.add("ResponseTime", "low");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "1");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();

        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("CellId", cellId = "55777");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "707");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "598003245701");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "0");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 15270);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
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

        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "lawful");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType",  geofenceType = "routingAreaId");
        geolocationParams.add("AreaEventId", geofenceId = "748-2-32005-245");
        geolocationParams.add("AreaEventOccurrence", "once");

        geolocationParams.add("ReferenceNumber", "19270");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        geolocationParamsUpdate.add("Priority", "high");
        geolocationParamsUpdate.add("HorizontalAccuracy", "10");
        geolocationParamsUpdate.add("VerticalAccuracy", "1");
        geolocationParamsUpdate.add("VerticalCoordinateRequest", "true");
        geolocationParamsUpdate.add("ResponseTime", "low");
        geolocationParamsUpdate.add("LocationEstimateType", "current");
        geolocationParamsUpdate.add("ReferenceNumber", "15270");
        geolocationParamsUpdate.add("ServiceTypeID", "0");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "747");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "5");
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
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 15270);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString().equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testFailedMapLsmNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmLocationErrorResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmLocationErrorResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "emergency");
        geolocationParams.add("ClientExternalID", "6543210987654321");
        geolocationParams.add("LocationEstimateType", "current");

        geolocationParams.add("ServiceTypeID", "0");
        // QoS parameters
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "0");
        geolocationParams.add("VerticalAccuracy", "0");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("ReferenceNumber","153");
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 153);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[POSITION METHOD FAILURE, MAP error code: 54]"));
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 153);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[POSITION METHOD FAILURE, MAP error code: 54]"));
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testMapLsmDeleteNotificationGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse1)));

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier",  msisdn);

        geolocationParams.add("Operation", "PSL");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "5989912340945");
        geolocationParams.add("ClientNameFormat", "msisdn");
        geolocationParams.add("ClientExternalID", "6543210987654321");
        geolocationParams.add("RequestorID", "Extended-SDP");
        geolocationParams.add("RequestorIDFormat", "name");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", "available");

        geolocationParams.add("ServiceTypeID", "54");
        // QoS parameters
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "10");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "false");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("ReferenceNumber", "78438");
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
    public void testMapPsiCsCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 53201);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 23479);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 53201);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 207631107L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 23479);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 811059);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapPsiPsCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiPsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiPsResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 14571);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.001794"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 14571);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.001794"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.6);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapPsiUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, responseStatus, imsi, imei, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, eCellId, sac,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, eNodeBId, tac, rac,
            typeOfShape, deviceLatitude, deviceLongitude, locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "21");
        geolocationParamsUpdate.add("ECellId", eCellId = "31122709");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3579");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "121573");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "23295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme01.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "camelBusy");
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
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == Integer.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values for the Geolocation attributes (PUT test)
        geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("MSISDN", msisdn = "59898999012");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871054321");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466104");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "20");
        geolocationParamsUpdate.add("ECellId", eCellId = "31122708");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3079");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "121573");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13195");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme04.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "7");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp").getAsString()
            .equals(locationTimestamp));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == Integer.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.5);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcMapPsiCsResponse)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("RequestLteLocation", "yes"); // <-- Not API compliant
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, responseStatus, cellId, eCellId, rac, tac, eNodeBId, locationAreaCode,
            mobileCountryCode, mobileNetworkCode, networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState,
            typeOfShape, deviceLatitude, deviceLongitude, internetAddress, physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00",
            lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "21");
        geolocationParamsUpdate.add("ECellId", eCellId = "31122709");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "121573");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme03.mmeh2000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13291");
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
        assertTrue(geolocationJson.get("reference_number") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
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
    public void testFailedMapPsiImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiLocationErrorResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiLocationErrorResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("RequestLteLocation", "true");
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[Dialog Rejected: PSI Application Context Not Supported]"));
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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[Dialog Rejected: PSI Application Context Not Supported]"));
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testMapPsiDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiCsResponse)));

        // This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Operation", "PSI");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("RequestLteLocation", "true");
        geolocationParams.add("Domain", "cs");
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
    public void testLteLcs1CreateAndGetNotificationGeolocationVasLastKnown()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");
        geolocationParams.add("LocationEstimateType", "lastKnown");

        geolocationParams.add("RequestorID", "http://www.example.com/index.html");
        geolocationParams.add("RequestorIDFormat", "url");

        geolocationParams.add("ServiceTypeID", "9");
        // QoS parameters
        geolocationParams.add("QoSClass", "best-effort");
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "127");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "false");
        geolocationParams.add("ResponseTime", "tolerant");

        geolocationParams.add("ReferenceNumber","1971");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testLteLcs1CreateAndGetNotificationGeolocationEmergencyCurrent()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "emergency");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");
        geolocationParams.add("LocationEstimateType", "current");

        geolocationParams.add("RequestorID", "Extended-SDP");
        geolocationParams.add("RequestorIDFormat", "name");

        geolocationParams.add("ServiceTypeID", "0");
        // QoS parameters
        geolocationParams.add("QoSClass", "assured");
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "0");
        geolocationParams.add("VerticalAccuracy", "0");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("ReferenceNumber","1973");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testLteLcs1CreateAndGetNotificationVasAreaEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "trackingAreaId");
        geolocationParams.add("AreaEventId", geofenceId = "502-18-1029");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "32767");
        geolocationParams.add("AreaEventMaxInterval", "86400");
        geolocationParams.add("AreaEventSamplingInterval", "3600");
        geolocationParams.add("AreaEventReportingDuration", "8640000");

        geolocationParams.add("ReferenceNumber","1975");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testLteLcs2CreateAndGetNotificationLawfulAreaEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "lawful");
        geolocationParams.add("ClientName", "sip:fer.bloggs@212.123.1.213");
        geolocationParams.add("ClientNameFormat", "sip");

        geolocationParams.add("RequestorID", "http://www.example.com/index.html");
        geolocationParams.add("RequestorIDFormat", "url");

        geolocationParams.add("ServiceTypeID", "8");
        // QoS parameters
        geolocationParams.add("QoSClass", "best-effort");
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "127");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "false");
        geolocationParams.add("ResponseTime", "tolerant");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "eUtranCellId");
        geolocationParams.add("AreaEventId", geofenceId = "502-18-811059-3");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "32767");
        geolocationParams.add("AreaEventMaxInterval", "86400");
        geolocationParams.add("AreaEventSamplingInterval", "3600");
        geolocationParams.add("AreaEventReportingDuration", "8640000");
        geolocationParams.add("AdditionalAreaEventType", "trackingAreaId");
        geolocationParams.add("AdditionalAreaEventId", "502-18-1029");

        geolocationParams.add("ReferenceNumber","1979");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 573195897484L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 732101509580853L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1979);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("71.999996"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-3.999989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 2.1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Calle 2 Sur 20-185, 050022, Medellin, Colombia"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1012);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 573195897484L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 732101509580853L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1979);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("71.999996"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-3.999989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 2.1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Calle 2 Sur 20-185, 050022, Medellin, Colombia"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1012);
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
    public void testLteLcs3CreateAndGetNotificationOperatorAreaEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580859";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse3)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse3)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "operator");
        geolocationParams.add("ClientName", "598989302928");
        geolocationParams.add("ClientNameFormat", "msisdn");

        geolocationParams.add("LocationEstimateType", "cancelDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "leaving");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "plmnId");
        geolocationParams.add("AreaEventId", geofenceId = "748-1");
        geolocationParams.add("AreaEventOccurrence", "once");

        geolocationParams.add("ReferenceNumber","2979");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 573195897489L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 732101509580859L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 2979);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 151079L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647743"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494458"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Calle 2 Sur 20-185, 050022, Medellin, Colombia"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1012);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 573195897489L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 732101509580859L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 2979);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 151079L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("Polygon"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("26.647743"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("73.494458"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals("Calle 2 Sur 20-185, 050022, Medellin, Colombia"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() == 1012);
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
    public void testLteLcsCreateAndGetNotificationVasMotionEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, motionEventRange, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "https://mendioroz.com/index");
        geolocationParams.add("ClientNameFormat", "url");

        geolocationParams.add("RequestorID", "Extended-SDP");
        geolocationParams.add("RequestorIDFormat", "name");

        geolocationParams.add("ServiceTypeID", "45");
        // QoS parameters
        geolocationParams.add("QoSClass", "assured");
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "0");
        geolocationParams.add("VerticalAccuracy", "0");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "motion-event");

        geolocationParams.add("MotionEventRange", motionEventRange = "9999");
        geolocationParams.add("MotionEventOccurrence", "multiple");
        geolocationParams.add("MotionEventInterval", "32767");
        geolocationParams.add("MotionEventMaxInterval", "86400");
        geolocationParams.add("MotionEventSamplingInterval", "3600");
        geolocationParams.add("MotionEventReportingDuration", "8640000");

        geolocationParams.add("ReferenceNumber","1971");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range").getAsString().equals(motionEventRange));
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range").getAsString().equals(motionEventRange));
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
    public void testLteLcsCreateAndGetNotificationLawfulAvailableGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, motionEventRange, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "lawful");
        geolocationParams.add("ClientName", "sip:fer.bloggs@212.123.1.213");
        geolocationParams.add("ClientNameFormat", "sip");

        geolocationParams.add("RequestorID", "Extended-SDP");
        geolocationParams.add("RequestorIDFormat", "name");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "available");

        geolocationParams.add("ReferenceNumber","1971");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testLteLcsCreateAndGetNotificationPeriodicLDRGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, motionEventRange, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "Extended-SDP");
        geolocationParams.add("ClientNameFormat", "name");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "periodic-ldr");

        geolocationParams.add("PeriodicReportingAmount",  "8639999");
        geolocationParams.add("PeriodicReportingInterval","8639999");

        geolocationParams.add("ReferenceNumber","1981");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 748026871012345L);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1971);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 133);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 31122709L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 121573L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5989900021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("42.999995"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("87.199988"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 32);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 3.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 12.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsInt() == 20);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse3)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse3)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sac, tac, eCellId, eNodeBId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress,
            barometricPressure, locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando@yahoo.com");
        geolocationParams.add("ClientNameFormat", "email");

        geolocationParams.add("RequestorID", "5876584989");
        geolocationParams.add("RequestorIDFormat", "msisdn");

        geolocationParams.add("ServiceTypeID", "10");
        // QoS parameters
        geolocationParams.add("QoSClass", "best-effort");
        geolocationParams.add("Priority", "normal");
        geolocationParams.add("HorizontalAccuracy", "127");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "tolerant");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", deferredLocationEventType = "entering");

        // Area Event parameters
        geolocationParams.add("AreaEventType", geofenceType = "eUtranCellId");
        geolocationParams.add("AreaEventId", geofenceId = "502-18-811059-3");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "32767");
        geolocationParams.add("AreaEventMaxInterval", "86400");
        geolocationParams.add("AreaEventSamplingInterval", "3600");
        geolocationParams.add("AreaEventReportingDuration", "8640000");
        geolocationParams.add("AdditionalAreaEventType", "trackingAreaId");
        geolocationParams.add("AdditionalAreaEventId", "502-18-1029");

        geolocationParams.add("ReferenceNumber","20979");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Define new values to the application attributes (POST test)
        MultivaluedMap<String, String> geolocationParamsUpdate = new MultivaluedMapImpl();
        geolocationParamsUpdate.add("Priority","normal");
        geolocationParamsUpdate.add("MSISDN", msisdn = "59898999012");
        geolocationParamsUpdate.add("IMSI", imsi = "124356871054321");
        geolocationParamsUpdate.add("IMEI", imei = "01171400466104");
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("ReferenceNumber", referenceNumber = "239");
        geolocationParamsUpdate.add("HorizontalAccuracy","1000");
        geolocationParamsUpdate.add("VerticalAccuracy","5000");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParamsUpdate.add("VelocityRequested","true");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "1");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "21");
        geolocationParamsUpdate.add("ECellId", eCellId = "31122709");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "24327");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "121573");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() ==
            Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals(horizontalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals(verticalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().
            equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals(civicAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() ==
            Long.valueOf(barometricPressure));
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
        geolocationParamsUpdate.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "329");
        geolocationParamsUpdate.add("CellId", cellId = "50231");
        geolocationParamsUpdate.add("ECellId", eCellId = "38676245");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "24328");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "151079");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "23296");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343243");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
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
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == Long.valueOf(referenceNumber));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_horizontal_speed").getAsString().equals(horizontalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_vertical_speed").getAsString().equals(verticalSpeed));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_horizontal_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_vertical_speed").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("bearing").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type").getAsString().equals(deferredLocationEventType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type").getAsString().equals(geofenceType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id").getAsString().equals(geofenceId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment, GeofenceId length is not API compliant for that GeofenceType
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);

        notificationGeolocationNotApiCompliantParams.add("Operation", "PLR");
        notificationGeolocationNotApiCompliantParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        notificationGeolocationNotApiCompliantParams.add("ClientType", "emergency");
        notificationGeolocationNotApiCompliantParams.add("ClientName", "fernando.mendioroz@gmail.com");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat", "email");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType", "current");

        notificationGeolocationNotApiCompliantParams.add("RequestorID", "Extended-SDP");
        notificationGeolocationNotApiCompliantParams.add("RequestorIDFormat", "name");

        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID", "128"); // <-- Not API compliant
        // QoS parameters
        notificationGeolocationNotApiCompliantParams.add("QoSClass", "assured");
        notificationGeolocationNotApiCompliantParams.add("Priority", "high");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy", "0");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy", "0");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest", "true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime", "low");

        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
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

        //GeofenceId format is still not API compliant
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);

        notificationGeolocationNotApiCompliantParams.add("Operation", "PLR");
        notificationGeolocationNotApiCompliantParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        notificationGeolocationNotApiCompliantParams.add("ClientType", "vas");
        notificationGeolocationNotApiCompliantParams.add("ClientName", "fernando@yahoo.com");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat", "email");

        notificationGeolocationNotApiCompliantParams.add("RequestorID", "5876584989");
        notificationGeolocationNotApiCompliantParams.add("RequestorIDFormat", "msisdn");

        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID", "10");
        // QoS parameters
        notificationGeolocationNotApiCompliantParams.add("QoSClass", "best-effort");
        notificationGeolocationNotApiCompliantParams.add("Priority", "normal");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy", "127");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy", "127");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest", "true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime", "tolerant");

        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType", "activateDeferred");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationType", "inside");

        // Area Event parameters
        notificationGeolocationNotApiCompliantParams.add("AreaEventType", "eUtranCellId");
        notificationGeolocationNotApiCompliantParams.add("AreaEventId", "502-18-811059-3");
        notificationGeolocationNotApiCompliantParams.add("AreaEventOccurrence", "multiple");
        notificationGeolocationNotApiCompliantParams.add("AreaEventInterval", "32768"); // <-- Not API compliant
        notificationGeolocationNotApiCompliantParams.add("AreaEventMaxInterval", "86400");
        notificationGeolocationNotApiCompliantParams.add("AreaEventSamplingInterval", "3600");
        notificationGeolocationNotApiCompliantParams.add("AreaEventReportingDuration", "8640000");
        notificationGeolocationNotApiCompliantParams.add("AdditionalAreaEventType", "trackingAreaId");
        notificationGeolocationNotApiCompliantParams.add("AdditionalAreaEventId", "502-18-1029");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

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

        //Motion event parameters don't apply for that DeferredLocationType
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);

        notificationGeolocationNotApiCompliantParams.add("Operation", "PLR");
        notificationGeolocationNotApiCompliantParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        notificationGeolocationNotApiCompliantParams.add("ClientType", "vas");
        notificationGeolocationNotApiCompliantParams.add("ClientName", "https://mendioroz.com/index");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat", "url");

        notificationGeolocationNotApiCompliantParams.add("RequestorID", "Extended-SDP");
        notificationGeolocationNotApiCompliantParams.add("RequestorIDFormat", "name");

        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID", "45");
        // QoS parameters
        notificationGeolocationNotApiCompliantParams.add("QoSClass", "assured");
        notificationGeolocationNotApiCompliantParams.add("Priority", "normal");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy", "0");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy", "0");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest", "true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime", "low");

        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType", "activateDeferred");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationType", "available");

        notificationGeolocationNotApiCompliantParams.add("MotionEventRange", "9999");
        notificationGeolocationNotApiCompliantParams.add("MotionEventOccurrence", "multiple");
        notificationGeolocationNotApiCompliantParams.add("MotionEventInterval", "32767");
        notificationGeolocationNotApiCompliantParams.add("MotionEventMaxInterval", "86400");
        notificationGeolocationNotApiCompliantParams.add("MotionEventSamplingInterval", "3600");
        notificationGeolocationNotApiCompliantParams.add("MotionEventReportingDuration", "8640000");

        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","379");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");

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

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, eCellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sai, tac, eNodeBId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress, barometricPressure,
            locationTimestamp = "2016-04-17T20:28:40.690-03:00", lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", deviceIdentifier = msisdn);

        notificationGeolocationNotApiCompliantParams.add("Operation", "PLR");
        notificationGeolocationNotApiCompliantParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        notificationGeolocationNotApiCompliantParams.add("ClientType", "vas");
        notificationGeolocationNotApiCompliantParams.add("ClientName", "fernando@yahoo.com");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat", "email");

        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType", "activateDeferred");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationType", "inside");

        // Area Event parameters
        notificationGeolocationNotApiCompliantParams.add("AreaEventType", "eUtranCellId");
        notificationGeolocationNotApiCompliantParams.add("AreaEventId", "502-18-811059-3");
        notificationGeolocationNotApiCompliantParams.add("AreaEventOccurrence", "once");

        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","1371");
        notificationGeolocationNotApiCompliantParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createNotificationGeolocation(
            deploymentUrl.toString(), adminAccountSid, adminUsername, adminAuthToken, notificationGeolocationNotApiCompliantParams);
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equalsIgnoreCase("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
        geolocationParamsUpdate.add("Priority", "normal");
        geolocationParamsUpdate.add("MSISDN", msisdn = "5989899936");
        geolocationParamsUpdate.add("IMSI", imsi = "432156871054321");
        geolocationParamsUpdate.add("IMEI", imei = "011714004661041");
        geolocationParamsUpdate.add("ReferenceNumber", referenceNumber = "239");
        geolocationParamsUpdate.add("HorizontalAccuracy", "100");
        geolocationParamsUpdate.add("VerticalAccuracy", "50");
        geolocationParamsUpdate.add("ResponseTime", "low");
        geolocationParamsUpdate.add("VelocityRequested", "true");
        geolocationParamsUpdate.add("LocationEstimateType", "current");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "329");
        geolocationParamsUpdate.add("CellId", cellId = "21");
        geolocationParamsUpdate.add("ECellId", eCellId = "38676245");
        geolocationParamsUpdate.add("ServiceAreaCode", sai = "24328");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "151079");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "23296");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343243");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
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
        assertTrue(geolocationJson.get("reference_number").getAsLong() == Long.valueOf(referenceNumber));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sai));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString().equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString().equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == Integer.valueOf(ageOfLocationInfo));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address").getAsString().
            equals(civicAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure").getAsLong() ==
            Long.valueOf(barometricPressure));
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
    public void testFailedLteLcsNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsLocationErrorResponse)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsLocationErrorResponse)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando@yahoo.com");
        geolocationParams.add("ClientNameFormat", "email");

        geolocationParams.add("RequestorID", "5876584989");
        geolocationParams.add("RequestorIDFormat", "msisdn");

        geolocationParams.add("ServiceTypeID", "10");
        // QoS parameters
        geolocationParams.add("QoSClass", "assured");
        geolocationParams.add("Priority", "high");
        geolocationParams.add("HorizontalAccuracy", "0");
        geolocationParams.add("VerticalAccuracy", "127");
        geolocationParams.add("VerticalCoordinateRequest", "true");
        geolocationParams.add("ResponseTime", "low");

        geolocationParams.add("LocationEstimateType", "activateDeferred");
        geolocationParams.add("DeferredLocationType", "leaving");

        // Area Event parameters
        geolocationParams.add("AreaEventType", "eUtranCellId");
        geolocationParams.add("AreaEventId", "502-19-811059-2");
        geolocationParams.add("AreaEventOccurrence", "multiple");
        geolocationParams.add("AreaEventInterval", "32767");
        geolocationParams.add("AreaEventMaxInterval", "86400");
        geolocationParams.add("AreaEventSamplingInterval", "3600");
        geolocationParams.add("AreaEventReportingDuration", "8640000");
        geolocationParams.add("AdditionalAreaEventType", "trackingAreaId");
        geolocationParams.add("AdditionalAreaEventId", "502-19-1451");

        geolocationParams.add("ReferenceNumber","875");
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 875);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[PLR/PLA SLg POSITIONING FAILED, Diameter error code: 4225]"));
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 875);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause").getAsString().equals("[PLR/PLA SLg POSITIONING FAILED, Diameter error code: 4225]"));
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteNotificationGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testLteLcsDeleteNotificationGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, geofenceType, geofenceId, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);

        geolocationParams.add("Operation", "PLR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");

        geolocationParams.add("ClientType", "vas");
        geolocationParams.add("ClientName", "fernando.mendioroz@gmail.com");
        geolocationParams.add("ClientNameFormat", "email");
        geolocationParams.add("LocationEstimateType", "lastKnown");

        geolocationParams.add("ReferenceNumber","1971");
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
    public void testShUdrCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59898077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("RequestRatType", "true");
        geolocationParams.add("RequestActiveLocation", "true");
        geolocationParams.add("Request5GLocation", "true");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59898077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsLong() == 512063008768L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20142);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978936L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("NR"));
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59898077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsLong() == 512063008768L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20142);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978936L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("NR"));
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testShUdrCreateAndGetImmediateGeolocation_CSLocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponseCS)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponseCS)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("Domain", "cs");
        geolocationParams.add("RequestRatType", "true");
        geolocationParams.add("RequestActiveLocation", "false");
        geolocationParams.add("Request5GLocation", "false");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20042);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978934L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type") == null);
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20042);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14645);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978934L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010009"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.009859"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type") == null);
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testShUdrCreateAndGetImmediateGeolocation_PSLocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponsePS)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponsePS)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("Domain", "ps");
        geolocationParams.add("RequestRatType", "false");
        geolocationParams.add("RequestActiveLocation", "false");
        geolocationParams.add("Request5GLocation", "false");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20042);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978936L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-23.291026"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("109.977801"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 45.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("GERAN"));
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 20042);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 598978936L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-23.291026"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("109.977801"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 45.6);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("GERAN"));
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testShUdrCreateAndGetImmediateGeolocation_EPSLocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "60192235906";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponseEPS)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponseEPS)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("Domain", "ps");
        geolocationParams.add("RequestRatType", "true");
        geolocationParams.add("RequestActiveLocation", "true");
        geolocationParams.add("Request5GLocation", "false");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 60192235906L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("19.484425"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.239695"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 0.0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("EUTRAN"));
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 60192235906L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("MMEC18.MMEGI8001.MME.EPC.MNC019.MCC502.3GPPNETWORK.ORG"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("19.484425"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.239695"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 0.0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("EUTRAN"));
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testShUdrCreateAndGetImmediateGeolocation_5GSLocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "60192235906";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponse5GS)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcShUdrResponse5GS)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("RequestRatType", "true");
        geolocationParams.add("RequestActiveLocation", "true");
        geolocationParams.add("Request5GLocation", "true");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 60192235906L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("partially-successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsLong() == 512063008768L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("NR"));
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 60192235906L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("partially-successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 502);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 19);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsLong() == 38676245);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsLong() == 512063008768L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 151079);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 774);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("amf1.cluster1.net2.amf.5gc.mnc012.mcc345.3gppnetwork.org"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("NR"));
        assertTrue(geolocationJson.get("last_geolocation_response") == null);
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    public void testShUdrUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, responseStatus, imsi, imei, lmsi, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, nrCellId, sac,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, eCellId, eNodeBId, tac, rac,
            typeOfShape, deviceLatitude, deviceLongitude, locationTimestamp, ratType, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "21");
        geolocationParamsUpdate.add("ECellId", eCellId = "31122709");
        geolocationParamsUpdate.add("NRCellId", nrCellId = "68719476735");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3579");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "121573");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "23295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "camelBusy");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsString().equals(nrCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == Integer.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals("NR"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("NRCellId", nrCellId = "68719476735");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3079");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "50172");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13195");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme03.mmen300.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "5");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "34.908134");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "-55.087134");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "partially-successful");
        geolocationParamsUpdate.add("RadioAccessType", ratType = "EUTRAN");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id").getAsString().equals(nrCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsString().equals(sac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == Integer.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString()
            .equals(typeOfShape));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString()
            .equals(deviceLatitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString()
            .equals(deviceLongitude));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.3);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 0);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals(ratType));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("physical_address") == null);
        assertTrue(geolocationJson.get("last_geolocation_response").getAsString().equals(lastGeolocationResponse));
        assertTrue(geolocationJson.get("cause") == null);
        assertTrue(geolocationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Remove created & updated Geolocation via HTTP DELETE
        RestcommGeolocationsTool.getInstance().deleteImmediateGeolocation(deploymentUrl.toString(), adminUsername,
            adminAuthToken, adminAccountSid, geolocationSid.toString());
    }

    @Test
    @Category(FeatureAltTests.class)
    public void testShUdrCreateNotApiCompliantImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST with one wrong mandatory parameter
        // Parameter values Assignment, DeviceIdentifier is not API compliant: missing
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
    public void testShUdrNotApiCompliantUpdateImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // Define Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, responseStatus, cellId, eCellId, rac, tac, eNodeBId, locationAreaCode,
            mobileCountryCode, mobileNetworkCode, networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState,
            typeOfShape, deviceLatitude, deviceLongitude, internetAddress, physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00",
            ratType, lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("formatted_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("barometric_pressure") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("internet_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type") == null);
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "3");
        geolocationParamsUpdate.add("ECellId", eCellId = "207631107");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "811059");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mmec03.mmeer3000.mme.epc.mnc002.mcc748.3gppnetwork.org@restcomm.org");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13291");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "1");
        geolocationParamsUpdate.add("TypeOfShape", typeOfShape = "ellipsoidArc");
        geolocationParamsUpdate.add("DeviceLatitude", deviceLatitude = "S43\u00b038'19.39''");
        geolocationParamsUpdate.add("DeviceLongitude", deviceLongitude = "E169\u00b028'49.07''");
        geolocationParamsUpdate.add("InternetAddress", internetAddress = "180.7.2.141");
        geolocationParamsUpdate.add("PhysicalAddress", physicalAddress = "A8-77-CA-29-32-D1");
        geolocationParamsUpdate.add("LocationTimestamp", locationTimestamp = "2016-04-17T20:31:28.388-05:00");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.2.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("RadioAccessType", ratType = "UTRAN");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("e_cell_id").getAsString().equals(eCellId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("nr_cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == Long.valueOf(eNodeBId));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsString()
            .equals(tac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsString()
            .equals(rac));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString()
            .equals(networkEntityName));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString()
            .equals(subscriberState));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("civic_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("radio_access_type").getAsString().equals(ratType));
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
    public void testShUdrDeleteImmediateGeolocation() throws IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        // This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse_all)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("Operation", "UDR");
        geolocationParams.add("Token", "RmVybmFuZG9NZW5kaW9yb3o");
        geolocationParams.add("ResponseStatus", "successful");
        geolocationParams.add("MSISDN", "59899077937");
        geolocationParams.add("IMSI", "124356871012345");
        geolocationParams.add("IMEI", "01171400466105");
        geolocationParams.add("LMSI", "2915");
        geolocationParams.add("MobileCountryCode", "749");
        geolocationParams.add("MobileNetworkCode", "3");
        geolocationParams.add("LocationAreaCode", "321");
        geolocationParams.add("CellId", "3");
        geolocationParams.add("ECellId", "207631107");
        geolocationParams.add("ENodeBId", "811059");
        geolocationParams.add("NetworkEntityAddress", "5980042343201");
        geolocationParams.add("NetworkEntityName", "mme74800021");
        geolocationParams.add("SubscriberState", "camelBusy");
        geolocationParams.add("TrackingAreaCode", "13295");
        geolocationParams.add("RoutingAreaCode", "1801");
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

