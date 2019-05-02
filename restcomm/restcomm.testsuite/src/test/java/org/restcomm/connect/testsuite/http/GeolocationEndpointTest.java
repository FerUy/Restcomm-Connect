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
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 819203961904\n" +
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
        "    \"mnpStatus\": \"ownNumberPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": 598123\n" +
        "  },\n" +
        "  \"saiPresent\": false,\n" +
        "  \"ageOfLocationInformation\": 1,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"vlrNumber\": 59899000231,\n" +
        "  \"mscNumber\": 5982123007,\n" +
        "  \"mmeName\": \"MME7480001\",\n" +
        "  \"subscriberState\": \"assumedIdle\",\n" +
        "  \"msClassmark\": \"003\"\n" +
        "}";

    String gmlcMapAtiPsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
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
        "      \"ci\": 38222\n" +
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
        "    \"msNetworkCapability\": \"73498934943\",\n" +
        "    \"msRadioAccessCapability\": \"849594859435389385989\"\n" +
        "  },\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": 598123\n" +
        "  },\n" +
        "  \"saiPresent\": false,\n" +
        "  \"ageOfLocationInformation\": 5,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"sgsnNumber\": 5982133021,\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\"\n" +
        "}";

    String gmlcMapAtiCsNotReachableResponse = "{\n" +
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
        "      \"lac\": 32005\n" +
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
        "  \"msisdn\": \"59899077937\",\n" +
        "  \"vlrNumber\": \"59899000231\",\n" +
        "  \"subscriberState\": \"netDetNotReachable\",\n" +
        "  \"notReachableReason\": \"imsiDetached\"\n" +
        "}";

    String gmlcMapAtiLocationErrorResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"ATI\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[ATI NOT ALLOWED, MAP error code: 49]\"\n" +
        "}";

    String gmlcMapLsmResponse1 = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS-PSL\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"lcsReferenceNumber\": 4,\n" +
        "  \"clientReferenceNumber\": 153,\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": 134570,\n" +
        "    \"vGmlcAddress\": 157003,\n" +
        "    \"pprAddress\": 938012\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithAltitudeAndUncertaintyEllipsoid\",\n" +
        "      \"latitude\": -2.9069995880126953,\n" +
        "      \"longitude\": -109.00023221969604,\n" +
        "      \"altitude\": 50,\n" +
        "      \"uncertaintySemiMajorAxis\": 1.0000000000000009,\n" +
        "      \"uncertaintySemiMinorAxis\": 2.2000000000000009,\n" +
        "      \"angleOfMajorAxis\": 10.0,\n" +
        "      \"uncertaintyAltitude\": 9.487171000000012,\n" +
        "      \"confidence\": 4\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"ci\": 17185\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"29152\",\n" +
        "      \"geranGanssPositioningData\": \"820135\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"933601\",\n" +
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
        "  \"result\": \"SUCCESS\",\n" +
        "  \"lcsReferenceNumber\": 1,\n" +
        "  \"clientReferenceNumber\": 153,\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": 134570,\n" +
        "    \"vGmlcAddress\": 157003,\n" +
        "    \"pprAddress\": 938012\n" +
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
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGIorSAIorLAI\": {\n" +
        "      \"mcc\": 437,\n" +
        "      \"mnc\": 109,\n" +
        "      \"lac\": 8304,\n" +
        "      \"sac\": 17185\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"29152\",\n" +
        "      \"geranGanssPositioningData\": \"820135\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"933601\",\n" +
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

    String gmlcMapLsmResponse3 = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS-PSL\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"lcsReferenceNumber\": 54,\n" +
        "  \"clientReferenceNumber\": 1591,\n" +
        "  \"SRIforLCS\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 2915,\n" +
        "    \"networkNodeNumber\": 5982123007,\n" +
        "    \"gprsNodeIndicator\": false,\n" +
        "    \"mmeName\": \"MME7480001\",\n" +
        "    \"3GPPAAAServerName\": \"AAA74800017\",\n" +
        "    \"hGmlcAddress\": \"181.104.201.3\",\n" +
        "    \"vGmlcAddress\": \"180.53.105.48\",\n" +
        "    \"pprAddress\": \"181.104.97.21\"\n" +
        "  },\n" +
        "  \"PSL\": {\n" +
        "    \"LocationEstimate\": {\n" +
        "      \"typeOfShape\": \"EllipsoidPointWithUncertaintyCircle\",\n" +
        "      \"latitude\": 51.123000383377075,\n" +
        "      \"longitude\": -102.10871458053589,\n" +
        "      \"uncertainty\": 4.641000000000004\n" +
        "    },\n" +
        "    \"AdditionalLocationEstimate\": {\n" +
        "      \"typeOfShape\": \"Polygon\",\n" +
        "      \"numberOfPoints\": 5,\n" +
        "      \"polygonPoint1\": {\n" +
        "        \"latitude\": -2.9069995880126953,\n" +
        "        \"longitude\": -173.96554470062256\n" +
        "      },\n" +
        "      \"polygonPoint2\": {\n" +
        "        \"latitude\": -3.0172276496887207,\n" +
        "        \"longitude\": -174.11722898483276\n" +
        "      },\n" +
        "      \"polygonPoint3\": {\n" +
        "        \"latitude\": -2.941385507583618,\n" +
        "        \"longitude\": -173.9199686050415\n" +
        "      },\n" +
        "      \"polygonPoint4\": {\n" +
        "        \"latitude\": -3.040015697479248,\n" +
        "        \"longitude\": -173.91001224517822\n" +
        "      },\n" +
        "      \"polygonPoint5\": {\n" +
        "        \"latitude\": -3.0449938774108887,\n" +
        "        \"longitude\": 70.70008993148804\n" +
        "      }\n" +
        "    },\n" +
        "    \"ageOfLocationEstimate\": 0,\n" +
        "    \"accuracyFulfilmentIndicator\": \"REQUESTED_ACCURACY_FULFILLED\",\n" +
        "    \"deferredMTLRresponseIndicator\": true,\n" +
        "    \"moLrShortCircuitIndicator\": true,\n" +
        "    \"CGIorSAIorLAI\": {\n" +
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
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"SRIforLCS\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"clientReferenceNumber\": 153,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[POSITION METHOD FAILURE, MAP error code: 54]\"\n" +
        "}";

    String gmlcMapPsiCsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"CSLocationInformation\": {\n" +
        "    \"LocationNumber\": {\n" +
        "      \"oddFlag\": false,\n" +
        "      \"natureOfAddressIndicator\": 4,\n" +
        "      \"internalNetworkNumberIndicator\": 1,\n" +
        "      \"numberingPlanIndicator\": 1,\n" +
        "      \"addressRepresentationRestrictedIndicator\": 1,\n" +
        "      \"screeningIndicator\": 3,\n" +
        "      \"address\": 819203961904\n" +
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
        "    \"mnpStatus\": \"ownNumberPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": 598123\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 1,\n" +
        "  \"currentLocationRetrieved\": true,\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imsi\": 124356871012345,\n" +
        "  \"imei\": \"011714004661050\",\n" +
        "  \"lmsi\": 2915,\n" +
        "  \"vlrNumber\": 59899000231,\n" +
        "  \"mscNumber\": 5982123007,\n" +
        "  \"mmeName\": \"MME7480001\",\n" +
        "  \"subscriberState\": \"assumedIdle\",\n" +
        "  \"msClassmark\": \"003\"\n" +
        "}";

    String gmlcMapPsiPsResponse = "{\n" +
        "  \"network\": \"GSM\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
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
        "    \"msNetworkCapability\": \"684834\",\n" +
        "    \"msRadioAccessCapability\": \"879543945839\"\n" +
        "  },\n" +
        "  \"MNPInfoResult\": {\n" +
        "    \"mnpStatus\": \"ownNumberPortedOut\",\n" +
        "    \"mnpMsisdn\": 59899077937,\n" +
        "    \"mnpImsi\": 748026871012345,\n" +
        "    \"mnpRouteingNumber\": 598123\n" +
        "  },\n" +
        "  \"saiPresent\": true,\n" +
        "  \"ageOfLocationInformation\": 14571,\n" +
        "  \"currentLocationRetrieved\": false,\n" +
        "  \"msisdn\": 59899077937,\n" +
        "  \"imsi\": 124356871012345,\n" +
        "  \"imei\": \"011714004661051\",\n" +
        "  \"lmsi\": 2915,\n" +
        "  \"sgsnNumber\": 5982133021,\n" +
        "  \"subscriberState\": \"psAttachedReachableForPaging\"\n" +
        "}";

    String gmlcMapPsiLocationErrorResponse = "{\n" +
        "  \"network\": \"UMTS\",\n" +
        "  \"protocol\": \"MAP\",\n" +
        "  \"operation\": \"PSI\",\n" +
        "  \"subscriberIdentity\": 59899077937,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[Dialog Rejected: PSI Application Context Not Supported]\"\n" +
        "}";

    String gmlcLteLcsResponse1 = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"lcsReferenceNumber\": 1,\n" +
        "  \"clientReferenceNumber\": 875,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": 573195897484,\n" +
        "    \"imsi\": 732101509580853,\n" +
        "    \"lmsi\": 7213917157,\n" +
        "    \"mmeName\": \"simulator.be-connect.us\",\n" +
        "    \"mmeRealm\": \"be-connect.us\",\n" +
        "    \"sgsnNumber\": 5730100028,\n" +
        "    \"sgsnName\": \"simulator.be-connect.us\",\n" +
        "    \"sgsnRealm\": \"be-connect.us\",\n" +
        "    \"3GPPAAAServerName\": \"aaa001\"\n" +
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
        "    \"CGIorSAIorECGIorESMLCCellInfo\": {\n" +
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
        "      \"ECGIorESMLCCellInfo\": {\n" +
        "        \"mcc\": 733,\n" +
        "        \"mnc\": 233,\n" +
        "        \"eNBId\": 331,\n" +
        "        \"ci\": 52,\n" +
        "        \"cellPortionId\": 197\n" +
        "      }\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"0\",\n" +
        "      \"geranGanssPositioningData\": \"0\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"81\",\n" +
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

    String gmlcLteLcsResponse2 = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter\",\n" +
        "  \"operation\": \"RIR-RIA-PLR-PLA\",\n" +
        "  \"result\": \"SUCCESS\",\n" +
        "  \"lcsReferenceNumber\": 20,\n" +
        "  \"clientReferenceNumber\": 359,\n" +
        "  \"Routing-Info-Answer\": {\n" +
        "    \"msisdn\": 59899077937,\n" +
        "    \"imsi\": 748026871012345,\n" +
        "    \"lmsi\": 4294967295,\n" +
        "    \"mmeName\": \"simulator\",\n" +
        "    \"mmeRealm\": \"be-connect.us\",\n" +
        "    \"sgsnNumber\": 5989900021,\n" +
        "    \"sgsnName\": \"simulator\",\n" +
        "    \"sgsnRealm\": \"be-connect.us\",\n" +
        "    \"3GPPAAAServerName\": \"aaa003\"\n" +
        "  },\n" +
        "  \"Provide-Location-Answer\": {\n" +
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
        "    \"CGIorSAIorECGIorESMLCCellInfo\": {\n" +
        "      \"CGI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 133,\n" +
        "        \"lac\": 12336,\n" +
        "        \"ci\": 12341\n" +
        "      },\n" +
        "      \"SAI\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 133,\n" +
        "        \"lac\": 12336,\n" +
        "        \"sac\": 12341\n" +
        "      },\n" +
        "      \"ECGIorESMLCCellInfo\": {\n" +
        "        \"mcc\": 732,\n" +
        "        \"mnc\": 133,\n" +
        "        \"eNBId\": 3158064,\n" +
        "        \"ci\": 52,\n" +
        "        \"cellPortionId\": 3\n" +
        "      }\n" +
        "    },\n" +
        "    \"GERANPositioningInfo\": {\n" +
        "      \"geranPositioningData\": \"0\",\n" +
        "      \"geranGanssPositioningData\": \"0\"\n" +
        "    },\n" +
        "    \"UTRANPositioningInfo\": {\n" +
        "      \"utranPositioningData\": \"81\",\n" +
        "      \"utranGanssPositioningData\": \"403\",\n" +
        "      \"utranAdditionalPositioningData\": \"0\"\n" +
        "    },\n" +
        "    \"E-UTRANPositioningInfo\": {\n" +
        "      \"eUtranPositioningData\": \"3132\"\n" +
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

    String gmlcLteLcsLocationErrorResponse = "{\n" +
        "  \"network\": \"LTE\",\n" +
        "  \"protocol\": \"Diameter SLg (ELP)\",\n" +
        "  \"operation\": \"PLR\",\n" +
        "  \"subscriberIdentity\": 59894455666,\n" +
        "  \"clientReferenceNumber\": 875,\n" +
        "  \"result\": \"ERROR\",\n" +
        "  \"errorReason\": \"[PLR/PLA SLg POSITIONING FAILED, Diameter error code: 4225]\"\n" +
        "}";

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
    public void testMapAtiCsCreateAndGetImmediateGeolocation()
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
        geolocationParams.add("Domain", "cs");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapAtiPsCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiPsResponse)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Domain", "ps");
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        // HTTP POST Geolocation creation with given parameters values and those returned via GMLC stub
        JsonObject geolocationJson = RestcommGeolocationsTool.getInstance().createImmediateGeolocation(deploymentUrl.toString(),
            adminAccountSid, adminUsername, adminAuthToken, geolocationParams);
        Sid geolocationSid = new Sid(geolocationJson.get("sid").getAsString());

        // Test asserts via GET to a single Geolocation
        geolocationJson = RestcommGeolocationsTool.getInstance().getImmediateGeolocation(deploymentUrl.toString(),
            adminUsername, adminAuthToken, adminAccountSid, geolocationSid.toString());

        logger.info("**** geolocationJson.get(\"geolocation_data\").getAsJsonObject() : "+geolocationJson.get("geolocation_data").getAsJsonObject().toString());
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(geolocationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_updated").getAsString()) != null);
        assertTrue(df.parse(geolocationJson.get("date_executed").getAsString()) != null);
        assertTrue(geolocationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(geolocationJson.get("device_identifier").getAsString().equals(deviceIdentifier));
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 23);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32006 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 38222);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("deferred_location_event_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range") == null);
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
    public void testMapAtiCsNotReachableCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiCsNotReachableResponse)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("Domain", "cs");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("partially-successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("partially-successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 748);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 21);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 32005);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithUncertaintyCircle"));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 38221);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsString()
            .equals(networkEntityAddress));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsString()
            .equals(ageOfLocationInfo));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
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
            physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00", lastGeolocationResponse;

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
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapAtiLocationErrorResponse)));

        //This is for GET requests - REMOVE if not needed
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
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
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
    public void testMapLsm1CreateAndGetNotificationGeolocation()
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
        geolocationParams.add("ClientType","emergency");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "1234567");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        //geolocationParams.add("MotionEventRange","9999");
        geolocationParams.add("ReferenceNumber","153");
        //geolocationParams.add("EventReportingAmount","5");
        //geolocationParams.add("EventReportingInterval","180");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-2.9069995880126953"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-109.00023221969604"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.2000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 10.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 9.487171000000012);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-2.9069995880126953"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-109.00023221969604"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.2000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 10.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 4);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 9.487171000000012);
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
    public void testMapLsm2CreateAndGetNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse2)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("ClientType","emergency");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "entering");
        geolocationParams.add("GeofenceType", geofenceType = "utranCellId");
        geolocationParams.add("GeofenceId", geofenceId = "54321");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","153");
        geolocationParams.add("ServiceTypeID","0");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005071640015"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.00022745132446"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0000000000000009);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidArc"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("45.907005071640015"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.00022745132446"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0000000000000009);
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
    public void testMapLsm3CreateAndGetNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "748026871012345";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmResponse3)));

        // Define Geolocation attributes for this test method
        String deviceIdentifier, deferredLocationEventType;
        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = imsi);
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("ClientType","emergency");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "periodic-ldr");
        geolocationParams.add("EventReportingAmount",  "8639999");
        geolocationParams.add("EventReportingInterval","8639999");
        geolocationParams.add("ReferenceNumber","1591");
        geolocationParams.add("ServiceTypeID","0");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1591);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == 51.123000383377075);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() ==-102.10871458053589);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915 );
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 1591);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 437);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 109);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 8304 );
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 17185);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsDouble() == 51.123000383377075);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsDouble() ==-102.10871458053589);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
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
        // Parameter values Assignment, DeferredLocationEventType refers to an area event thus EventRange,
        // EventReportingAmount and EventReportingInterval are not allowed
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","UMTS");
        notificationGeolocationNotApiCompliantParams.add("Priority","high");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","low");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationEventType", "inside");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", "locationAreaId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", "10");
        notificationGeolocationNotApiCompliantParams.add("GeofenceOccurrenceInfo","once");
        notificationGeolocationNotApiCompliantParams.add("MotionEventRange","9999");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","33");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
        notificationGeolocationNotApiCompliantParams.add("GeofenceIntervalTime","60");
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
        geolocationParams.add("ClientType","vas");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10");
        geolocationParams.add("GeofenceOccurrenceInfo","multiple");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber", "153");
        geolocationParams.add("ServiceTypeID","0");
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "1");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsString().equals("748026871012345"));
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915);
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 153);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString()
            .equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString()
            .equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString()
            .equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
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
        geolocationParams.add("ClientType","vas");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "available");
        geolocationParams.add("GeofenceType", geofenceType = "plmnId");
        geolocationParams.add("GeofenceId", geofenceId = "53412");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
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
        geolocationParamsUpdate.add("CoreNetwork","UMTS");
        geolocationParamsUpdate.add("Priority","high");
        geolocationParamsUpdate.add("HorizontalAccuracy","500");
        geolocationParamsUpdate.add("VerticalAccuracy","100");
        geolocationParamsUpdate.add("VerticalCoordinateRequest","true");
        geolocationParamsUpdate.add("ResponseTime","low");
        geolocationParamsUpdate.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("ReferenceNumber","33");
        geolocationParamsUpdate.add("ServiceTypeID","0");
        geolocationParamsUpdate.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
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
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 33);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals(responseStatus));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsString().equals(mobileCountryCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsString().equals(mobileNetworkCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsString().equals(locationAreaCode));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsString().equals(cellId));
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapLsmLocationErrorResponse)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("ClientType","operator");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "entering");
        geolocationParams.add("GeofenceType", geofenceType = "routingAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10101");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","153");
        geolocationParams.add("ServiceTypeID","0");
        //geolocationParams.add("EventReportingAmount","5");
        //geolocationParams.add("EventReportingInterval","180");
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 153);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 153);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        geolocationParams.add("ClientType","lawful");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", "inside");
        geolocationParams.add("GeofenceType", "locationAreaId");
        geolocationParams.add("GeofenceId", "10");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","33");
        geolocationParams.add("ServiceTypeID","0");
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661050"));
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 53201);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 23479);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.487171000000012);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 53201);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 23479);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 819203961904L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982123007L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("MME7480001"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("assumedIdle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.487171000000012);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiPsResponse)));

        //This is for GET requests - REMOVE if not needed
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
        assertTrue(geolocationJson.get("msisdn").getAsLong() == 59899077937L);
        assertTrue(geolocationJson.get("imsi").getAsLong() == 124356871012345L);
        assertTrue(geolocationJson.get("imei").getAsString().equals("011714004661051"));
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 14571);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.00179386138916"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 2915L);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5982133021L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 14571);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state").getAsString().equals("psAttachedReachableForPaging"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("-99.00179386138916"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 4.641000000000004);
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
        String deviceIdentifier, responseStatus, imsi, imei, lmsi, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sac,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, eNodeBId, tac, rac,
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "3579");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3579");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "52171");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "23295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800021");
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.487171000000012);
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
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3079");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "50172");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13195");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 9.487171000000012);
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
        String deviceIdentifier, imsi, imei, lmsi, responseStatus, cellId, rac, tac, eNodeBId, locationAreaCode,
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        geolocationParamsUpdate.add("LMSI", lmsi = "4294967295");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "304");
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "65535");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcMapPsiLocationErrorResponse)));

        //This is for GET requests - REMOVE if not needed
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
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
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
        geolocationParams.add("MobileNetworkCode", "3");
        geolocationParams.add("LocationAreaCode", "321");
        geolocationParams.add("CellId", "3579");
        geolocationParams.add("ENodeBId", "50179");
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

    @Test
    public void testLteLcsCreateAndGetNotificationAreaEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("ClientType","emergency");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "456");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","371");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
    public void testLteLcsCreateAndGetNotificationMotionEventInfoGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("ClientType","vas");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "motion-event");
        geolocationParams.add("MotionEventRange",motionEventRange = "9999");
        geolocationParams.add("MotionEventOccurrence","multiple");
        geolocationParams.add("MotionEventInterval","3600");
        geolocationParams.add("MotionEventMaxInterval","86400");
        geolocationParams.add("MotionEventSamplingInterval","32767");
        geolocationParams.add("MotionEventReportingDuration","8640000");
        geolocationParams.add("ReferenceNumber","371");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range").getAsString().equals(motionEventRange));
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("motion_event_range").getAsString().equals(motionEventRange));
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
    public void testLteLcsCreateAndGetNotificationPeriodicLDRGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String imsi = "732101509580853";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("ClientType","operator");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "periodic-ldr");
        geolocationParams.add("EventReportingAmount",  "8639999");
        geolocationParams.add("EventReportingInterval","8639999");
        geolocationParams.add("ReferenceNumber","178");
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
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
        assertTrue(geolocationJson.get("lmsi").getAsLong() == 7213917157L);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("successful"));
        assertTrue(geolocationJson.get("reference_number").getAsLong() == 875);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 733);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 233);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 12336);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 52);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code").getAsInt() == 12344);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsLong() == 331);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 5730100028L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().
            equals("simulator.be-connect.us@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().
            equals("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("33.99999260902405"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("55.99999666213989"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_major_axis").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_semi_minor_axis").getAsDouble() == 2.100000000000002);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("angle_of_major_axis").getAsDouble() == 4.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 80);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude").getAsInt() == 200);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty").getAsDouble() == 11.435888100000016);
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_type") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("geofence_id") == null);
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
    public void testMapLteLcsUpdateNotificationGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "5989738292";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sac, tac, eNodeBId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress, barometricPressure,
            locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("ClientName","Beconnect");
        geolocationParams.add("ClientNameFormat","msisdn");
        geolocationParams.add("ClientType","emergency");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "leaving");
        geolocationParams.add("GeofenceType", geofenceType = "eUtranCellId");
        geolocationParams.add("GeofenceId", geofenceId = "1234567");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","0");
        geolocationParams.add("StatusCallback","http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
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
        geolocationParamsUpdate.add("VelocityRequested","true");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "749");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "1");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "12345");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "24327");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "59871");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
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
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "24328");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "42173");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "23296");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343243");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024@be-connect.us");
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
        assertTrue(geolocationJson.get("lmsi").getAsString().equals(lmsi));
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("confidence").getAsInt() == 2);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_altitude_uncertainty") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("inner_radius").getAsInt() == 5);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty_inner_radius").getAsDouble() == 1.0000000000000009);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("offset_angle").getAsDouble() == 20.0);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("included_angle").getAsDouble() == 20.0);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        // Test create Notification type of Geolocation via POST
        // Parameter values Assignment, GeofenceId length is not API compliant for that GeofenceType
        MultivaluedMap<String, String> notificationGeolocationNotApiCompliantParams = new MultivaluedMapImpl();
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","LTE");
        notificationGeolocationNotApiCompliantParams.add("ClientType","emergency");
        notificationGeolocationNotApiCompliantParams.add("ClientName","Beconnect");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat","msisdn");
        notificationGeolocationNotApiCompliantParams.add("Priority","high");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","low");
        notificationGeolocationNotApiCompliantParams.add("VelocityRequested","true");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationEventType", "inside");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", "eUtranCellId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", "12345");
        notificationGeolocationNotApiCompliantParams.add("GeofenceOccurrenceInfo","once");
        notificationGeolocationNotApiCompliantParams.add("GeofenceIntervalTime","60");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
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
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","LTE");
        notificationGeolocationNotApiCompliantParams.add("ClientType","vas");
        notificationGeolocationNotApiCompliantParams.add("ClientName","Beconnect");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat","msisdn");
        notificationGeolocationNotApiCompliantParams.add("Priority","normal");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","low");
        notificationGeolocationNotApiCompliantParams.add("VelocityRequested","false");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationEventType", "leaving");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", "cellGlobalId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", "seven");
        notificationGeolocationNotApiCompliantParams.add("GeofenceOccurrenceInfo","multiple");
        notificationGeolocationNotApiCompliantParams.add("GeofenceIntervalTime","60");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
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

        //Geofence parameters don't apply for that DeferredLocationEventType
        notificationGeolocationNotApiCompliantParams.add("DeviceIdentifier", msisdn);
        notificationGeolocationNotApiCompliantParams.add("CoreNetwork","LTE");
        notificationGeolocationNotApiCompliantParams.add("ClientType","vas");
        notificationGeolocationNotApiCompliantParams.add("ClientName","Beconnect");
        notificationGeolocationNotApiCompliantParams.add("ClientNameFormat","msisdn");
        notificationGeolocationNotApiCompliantParams.add("Priority","normal");
        notificationGeolocationNotApiCompliantParams.add("HorizontalAccuracy","500");
        notificationGeolocationNotApiCompliantParams.add("VerticalAccuracy","100");
        notificationGeolocationNotApiCompliantParams.add("VerticalCoordinateRequest","true");
        notificationGeolocationNotApiCompliantParams.add("ResponseTime","low");
        notificationGeolocationNotApiCompliantParams.add("VelocityRequested","true");
        notificationGeolocationNotApiCompliantParams.add("LocationEstimateType","current");
        notificationGeolocationNotApiCompliantParams.add("DeferredLocationEventType", "motion-event");
        notificationGeolocationNotApiCompliantParams.add("GeofenceType", "cellGlobalId");
        notificationGeolocationNotApiCompliantParams.add("GeofenceId", "sev12345en");
        notificationGeolocationNotApiCompliantParams.add("GeofenceOccurrenceInfo","multiple");
        notificationGeolocationNotApiCompliantParams.add("GeofenceIntervalTime","60");
        notificationGeolocationNotApiCompliantParams.add("ReferenceNumber","371");
        notificationGeolocationNotApiCompliantParams.add("ServiceTypeID","0");
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse2)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, geofenceType, geofenceId, deferredLocationEventType, responseStatus, referenceNumber,
            mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, networkEntityAddress, networkEntityName, ageOfLocationInfo,
            sai, tac, eNodeBId, typeOfShape, deviceLatitude, deviceLongitude, horizontalSpeed, verticalSpeed, civicAddress, barometricPressure,
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
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("VelocityRequested","false");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "leaving");
        geolocationParams.add("GeofenceType", geofenceType = "trackingAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "30145");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","0");
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
        assertTrue(geolocationJson.get("msisdn") == null);
        assertTrue(geolocationJson.get("imsi") == null);
        assertTrue(geolocationJson.get("imei") == null);
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equalsIgnoreCase("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParamsUpdate.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParamsUpdate.add("ResponseStatus", responseStatus = "successful");
        geolocationParamsUpdate.add("MobileCountryCode", mobileCountryCode = "748");
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "2");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "329");
        geolocationParamsUpdate.add("CellId", cellId = "50231");
        geolocationParamsUpdate.add("ServiceAreaCode", sai = "24328");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "24173");
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsLocationErrorResponse)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","UMTS");
        geolocationParams.add("ClientType","vas");
        geolocationParams.add("Priority","high");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","low");
        geolocationParams.add("VelocityRequested","true");
        geolocationParams.add("LocationEstimateType","current");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "inside");
        geolocationParams.add("GeofenceType", geofenceType = "locationAreaId");
        geolocationParams.add("GeofenceId", geofenceId = "10");
        geolocationParams.add("GeofenceOccurrenceInfo","once");
        geolocationParams.add("GeofenceIntervalTime","60");
        geolocationParams.add("ReferenceNumber","875");
        geolocationParams.add("ServiceTypeID","0");
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 875);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number").getAsInt() == 875);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(NotificationGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("failed"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id") == null);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcLteLcsResponse1)));

        //This is for GET requests - REMOVE if not needed
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
        geolocationParams.add("CoreNetwork","LTE");
        geolocationParams.add("Priority","normal");
        geolocationParams.add("HorizontalAccuracy","500");
        geolocationParams.add("VerticalAccuracy","100");
        geolocationParams.add("VerticalCoordinateRequest","true");
        geolocationParams.add("ResponseTime","tolerant");
        geolocationParams.add("LocationEstimateType","notificationVerificationOnly");
        geolocationParams.add("DeferredLocationEventType", deferredLocationEventType = "max-interval-expiration");
        geolocationParams.add("ReferenceNumber","371");
        geolocationParams.add("ServiceTypeID","1");
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

    @Test
    public void testShUdrCreateAndGetImmediateGeolocation()
        throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        String msisdn = "59899077937";

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST (only mandatory parameters)
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("CoreNetwork", "IMS");
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 59897904322L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("mme742@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 100);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.34705943388394);
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
        assertTrue(geolocationJson.get("lmsi") == null);
        assertTrue(geolocationJson.get("reference_number") == null);
        assertTrue(geolocationJson.get("geolocation_type").getAsString().equals(ImmediateGT));
        assertTrue(geolocationJson.get("response_status").getAsString().equals("last-known"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_timestamp") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_country_code").getAsInt() == 732);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("mobile_network_code").getAsInt() == 103);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_area_code").getAsInt() == 1);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("cell_id").getAsInt() == 74);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("service_area_code") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("enodeb_id").getAsInt() == 334);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("tracking_area_code").getAsInt() == 14649);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("routing_area_code").getAsInt() == 50);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_number_address").getAsLong() == 56034254999L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_address").getAsLong() == 59897904322L);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("network_entity_name").getAsString().equals("mme742@be-connect.us"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("location_age").getAsInt() == 100);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("subscriber_state") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("not_reachable_reason") == null);
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("type_of_shape").getAsString().equals("EllipsoidPointWithUncertaintyCircle"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_latitude").getAsString().equals("-24.010008573532104"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("device_longitude").getAsString().equals("110.00985860824585"));
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.34705943388394);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        // Define Notification Geolocation attributes
        String deviceIdentifier, responseStatus, imsi, imei, lmsi, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sac,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, eNodeBId, tac, rac,
            typeOfShape, deviceLatitude, deviceLongitude, locationTimestamp, lastGeolocationResponse;

        // Create Notification type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("CoreNetwork", "IMS");
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
        geolocationParamsUpdate.add("MobileNetworkCode", mobileNetworkCode = "3");
        geolocationParamsUpdate.add("LocationAreaCode", locationAreaCode = "321");
        geolocationParamsUpdate.add("CellId", cellId = "3579");
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3579");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "52171");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "23295");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343201");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800021");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.34705943388394);
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
        geolocationParamsUpdate.add("ServiceAreaCode", sac = "3079");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "50172");
        geolocationParamsUpdate.add("TrackingAreaCode", tac = "13295");
        geolocationParamsUpdate.add("RoutingAreaCode", rac = "13195");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
        geolocationParamsUpdate.add("SubscriberState", subscriberState = "assumedIdle");
        geolocationParamsUpdate.add("LocationAge", ageOfLocationInfo = "5");
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
        assertTrue(geolocationJson.get("geolocation_data").getAsJsonObject().get("uncertainty").getAsDouble() == 98.34705943388394);
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody(gmlcDiameterShUdrResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Test create Immediate type of Geolocation via POST with one wrong mandatory parameter
        // Parameter values Assignment, StatusCallback is not API compliant: missing
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("CoreNetwork", "IMS");
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        // Define Geolocation attributes
        String deviceIdentifier, imsi, imei, lmsi, responseStatus, cellId, rac, tac, eNodeBId, locationAreaCode,
            mobileCountryCode, mobileNetworkCode, networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState,
            typeOfShape, deviceLatitude, deviceLongitude, internetAddress, physicalAddress, locationTimestamp = "2016-04-17T20:28:40.690-03:00",
            lastGeolocationResponse;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.0.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("CoreNetwork", "IMS");
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
        geolocationParamsUpdate.add("CellId", cellId = "5517");
        geolocationParamsUpdate.add("ENodeBId", eNodeBId = "65535");
        geolocationParamsUpdate.add("NetworkEntityAddress", networkEntityAddress = "5980042343202");
        geolocationParamsUpdate.add("NetworkEntityName", networkEntityName = "mme74800024");
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

        //This is for POST requests
        stubFor(post(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        //This is for GET requests - REMOVE if not needed
        stubFor(get(urlPathEqualTo("/restcomm/gmlc/rest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(gmlcDiameterShUdrResponse)));

        // Define Immediate Geolocation attributes for this method
        String deviceIdentifier;

        // Create Immediate type of Geolocation via POST
        // Parameter values Assignment
        MultivaluedMap<String, String> geolocationParams = new MultivaluedMapImpl();
        geolocationParams.add("DeviceIdentifier", deviceIdentifier = msisdn);
        geolocationParams.add("StatusCallback", "http://192.1.1.19:8080/ACae6e420f425248d6a26948c17a9e2acf");
        geolocationParams.add("CoreNetwork", "IMS");
        geolocationParams.add("ResponseStatus", "successful");
        geolocationParams.add("MSISDN", "59899077937");
        geolocationParams.add("IMSI", "124356871012345");
        geolocationParams.add("IMEI", "01171400466105");
        geolocationParams.add("LMSI", "2915");
        geolocationParams.add("MobileCountryCode", "749");
        geolocationParams.add("MobileNetworkCode", "3");
        geolocationParams.add("LocationAreaCode", "321");
        geolocationParams.add("CellId", "3579");
        geolocationParams.add("ENodeBId", "50179");
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
