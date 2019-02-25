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

package org.restcomm.connect.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.spi.resource.Singleton;
import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.util.StringUtils;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.GeolocationDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Geolocation;
import org.restcomm.connect.dao.entities.Geolocation.GeolocationType;
import org.restcomm.connect.dao.entities.GeolocationList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.http.converter.ClientListConverter;
import org.restcomm.connect.http.converter.GeolocationConverter;
import org.restcomm.connect.http.converter.GeolocationListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.http.security.ContextUtil;
import org.restcomm.connect.http.security.PermissionEvaluator.SecuredType;
import org.restcomm.connect.identity.UserIdentityContext;

/**
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 *
 */
@Path("/Accounts/{accountSid}/Geolocation")
@ThreadSafe
@Singleton
public class GeolocationEndpoint extends AbstractEndpoint {

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected GeolocationDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;
    private static final Logger logger = Logger.getLogger(GeolocationEndpoint.class);
    private static final String ImmediateGT = Geolocation.GeolocationType.Immediate.toString();
    private static final String NotificationGT = Geolocation.GeolocationType.Notification.toString();
    private String cause;
    private String rStatus;
    private boolean httpBadRequest = false;

    private enum responseStatus {
        Successful("successful"), PartiallySuccessful("partially-successful"), LastKnown("last-known"), Failed("failed"),
        Unauthorized("unauthorized"), Rejected("rejected");

        private final String rs;

        responseStatus(final String rs) {
            this.rs = rs;
        }

        @Override
        public String toString() {
            return rs;
        }
    }

    public GeolocationEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getGeolocationDao();
        accountsDao = storage.getAccountsDao();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final GeolocationConverter converter = new GeolocationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Geolocation.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ClientListConverter(configuration));
        xstream.registerConverter(new GeolocationListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Response getGeolocation(final String accountSid, final String sid, final MediaType responseType,
            UserIdentityContext userIdentityContext) {
        Account account;
        try {
            account = accountsDao.getAccount(accountSid);
            permissionEvaluator.secure(account, "RestComm:Read:Geolocation", userIdentityContext);
        } catch (final Exception exception) {
            return status(UNAUTHORIZED).build();
        }
        // final Geolocation geolocation = dao.getGeolocation(new Sid(sid));
        Geolocation geolocation = null;
        if (Sid.pattern.matcher(sid).matches()) {
            geolocation = dao.getGeolocation(new Sid(sid));
        }
        if (geolocation == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                permissionEvaluator.secure(account, geolocation.getAccountSid(), SecuredType.SECURED_APP, userIdentityContext);
            } catch (final Exception exception) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE.equals(responseType)) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE.equals(responseType)) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getGeolocations(final String accountSid, final MediaType responseType,
            UserIdentityContext userIdentityContext) {
        Account account;
        try {
            account = accountsDao.getAccount(accountSid);
            permissionEvaluator.secure(account, "RestComm:Read:Geolocation", SecuredType.SECURED_APP, userIdentityContext);
        } catch (final Exception exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<Geolocation> geolocations = dao.getGeolocations(new Sid(accountSid));
        if (APPLICATION_XML_TYPE.equals(responseType)) {
            final RestCommResponse response = new RestCommResponse(new GeolocationList(geolocations));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE.equals(responseType)) {
            return ok(gson.toJson(geolocations), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response deleteGeolocation(final String accountSid, final String sid, UserIdentityContext userIdentityContext) {
        Account account;
        try {
            account = accountsDao.getAccount(accountSid);
            permissionEvaluator.secure(account, "RestComm:Delete:Geolocation", userIdentityContext);
            Geolocation geolocation = dao.getGeolocation(new Sid(sid));
            if (geolocation != null) {
                permissionEvaluator.secure(account, geolocation.getAccountSid(), SecuredType.SECURED_APP, userIdentityContext);
            }
        } catch (final Exception exception) {
            return status(UNAUTHORIZED).build();
        }
        dao.removeGeolocation(new Sid(sid));
        return ok().build();
    }

    public Response putGeolocation(final String accountSid, final MultivaluedMap<String, String> data,
            GeolocationType geolocationType, final MediaType responseType, UserIdentityContext userIdentityContext) {
        Account account;
        try {
            account = accountsDao.getAccount(accountSid);
            permissionEvaluator.secure(account, "RestComm:Create:Geolocation", SecuredType.SECURED_APP, userIdentityContext);
        } catch (final Exception exception) {
            return status(UNAUTHORIZED).build();
        }

        try {
            validate(data, geolocationType);
        } catch (final NullPointerException nullPointerException) {
            // API compliance check regarding missing mandatory parameters
            return status(BAD_REQUEST).entity(nullPointerException.getMessage()).build();
        } catch (final IllegalArgumentException illegalArgumentException) {
            // API compliance check regarding malformed parameters
            if (httpBadRequest) {
                return status(BAD_REQUEST).entity(illegalArgumentException.getMessage()).build();
            } else {
                cause = illegalArgumentException.getMessage();
                rStatus = responseStatus.Failed.toString();
            }
        } catch (final UnsupportedOperationException unsupportedOperationException) {
            // API compliance check regarding parameters not allowed for Immediate type of Geolocation
            return status(BAD_REQUEST).entity(unsupportedOperationException.getMessage()).build();
        }

        /***********************************************/
        /******* Query GMLC for Location Data  ********/
        /*********************************************/
        try {
            String targetMSISDN = data.getFirst("DeviceIdentifier");
            Configuration gmlcConf = configuration.subset("gmlc");
            String gmlcURI = gmlcConf.getString("gmlc-uri");
            // Authorization for further stage of the project
            String gmlcUser = gmlcConf.getString("gmlc-user");
            String gmlcPassword = gmlcConf.getString("gmlc-password");
            // Credentials credentials = new UsernamePasswordCredentials(gmlcUser, gmlcPassword);
            String domainType = data.getFirst("Domain");
            String psiService = data.getFirst("PsiService");
            String coreNetwork = data.getFirst("CoreNetwork");
            String httpRespType = data.getFirst("HttpRespType");
            String locationEstimateType = data.getFirst("LocationEstimateType");
            String lcsClientType = data.getFirst("ClientType");
            String lcsClientNameString = data.getFirst("ClientName");
            String lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
            String lcsServiceTypeID = data.getFirst("ServiceTypeID");
            String priority = data.getFirst("Priority");
            String horizontalAccuracy = data.getFirst("HorizontalAccuracy");
            String verticalAccuracy = data.getFirst("VerticalAccuracy");
            String verticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            String responseTimeCategory = data.getFirst("ResponseTime");
            String velocityRequested = data.getFirst("VelocityRequested");
            String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
            String areaType = data.getFirst("GeofenceType");
            String areaId = data.getFirst("GeofenceId");
            String occurrenceInfo = data.getFirst("GeofenceOccurrenceInfo");
            String intervalTime = data.getFirst("GeofenceIntervalTime");
            String motionEventLinearDistance = data.getFirst("MotionEventRange");
            String motionEventOccurrence = data.getFirst("MotionEventOccurrence");
            String motionEventInterval = data.getFirst("MotionEventInterval");
            String motionEventMaxInterval = data.getFirst("MotionEventMaxInterval");
            String motionEventSamplingInterval = data.getFirst("MotionEventSamplingInterval");
            String motionEventReportingDuration = data.getFirst("MotionEventReportingDuration");
            String lcsReferenceNumber = data.getFirst("ReferenceNumber");
            String reportingAmount = data.getFirst("EventReportingAmount");
            String reportingInterval = data.getFirst("EventReportingInterval");
            String callbackUrl = data.getFirst("StatusCallback");
            URIBuilder uriBuilder = new URIBuilder(gmlcURI);
            uriBuilder.addParameter("msisdn", targetMSISDN);

            if (coreNetwork != null)
                uriBuilder.addParameter("coreNetwork", coreNetwork);

            if (domainType != null)
                uriBuilder.addParameter("domain", domainType);

            if (psiService != null)
                uriBuilder.addParameter("psiService", psiService);

            if (httpRespType != null)
                uriBuilder.addParameter("httpRespType", httpRespType);

            if (locationEstimateType != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    if (locationEstimateType.equalsIgnoreCase("lastKnown"))
                        locationEstimateType = "currentOrLastKnownLocation";
                    if (locationEstimateType.equalsIgnoreCase("initial"))
                        locationEstimateType = "initialLocation";
                    if (locationEstimateType.equalsIgnoreCase("current"))
                        locationEstimateType = "currentLocation";
                    if (locationEstimateType.equalsIgnoreCase("activateDeferred"))
                        locationEstimateType = "activateDeferredLocation";
                    if (locationEstimateType.equalsIgnoreCase("cancelDeferred"))
                        locationEstimateType = "cancelDeferredLocation";
                    if (locationEstimateType.equalsIgnoreCase("notificationVerificationOnly"))
                        locationEstimateType = "notificationVerificationOnly";
                    uriBuilder.addParameter("locationEstimateType", locationEstimateType);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (locationEstimateType.equalsIgnoreCase("lastKnown"))
                        locationEstimateType = "0"; // CURRENT_OR_LAST_KNOWN_LOCATION
                    if (locationEstimateType.equalsIgnoreCase("initial"))
                        locationEstimateType = "1"; // INITIAL_LOCATION
                    if (locationEstimateType.equalsIgnoreCase("current"))
                        locationEstimateType = "2"; // CURRENT_LOCATION
                    if (locationEstimateType.equalsIgnoreCase("activateDeferred"))
                        locationEstimateType = "3"; // ACTIVATE_DEFERRED_LOCATION
                    if (locationEstimateType.equalsIgnoreCase("cancelDeferred"))
                        locationEstimateType = "4"; // CANCEL_DEFERRED_LOCATION
                    if (locationEstimateType.equalsIgnoreCase("notificationVerificationOnly"))
                        locationEstimateType = "5"; // NOTIFICATION_VERIFICATION_ONLY
                    uriBuilder.addParameter("slgLocationType", locationEstimateType);
                }
            }

            if (lcsClientType != null && coreNetwork != null) {
                if (lcsClientType.equalsIgnoreCase("emergency"))
                    lcsClientType = "0";
                if (lcsClientType.equalsIgnoreCase("vas"))
                    lcsClientType = "1";
                if (lcsClientType.equalsIgnoreCase("operator"))
                    lcsClientType = "2";
                if (lcsClientType.equalsIgnoreCase("lawful"))
                    lcsClientType = "3";
                if (coreNetwork.equalsIgnoreCase("LTE"))
                    uriBuilder.addParameter("slgClientType", lcsClientType);
                else if (coreNetwork.equalsIgnoreCase("UMTS"))
                    uriBuilder.addParameter("lcsClientType", lcsClientType);
            }

            if (lcsClientNameString != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsNameString", lcsClientNameString);
                }
            }

            if (lcsClientFormatIndicator != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (lcsClientFormatIndicator.equalsIgnoreCase("name"))
                        lcsClientFormatIndicator = "0";
                    if (lcsClientFormatIndicator.equalsIgnoreCase("email"))
                        lcsClientFormatIndicator = "1";
                    if (lcsClientFormatIndicator.equalsIgnoreCase("msisdn"))
                        lcsClientFormatIndicator = "2";
                    if (lcsClientFormatIndicator.equalsIgnoreCase("url"))
                        lcsClientFormatIndicator = "3";
                    if (lcsClientFormatIndicator.equalsIgnoreCase("sip"))
                        lcsClientFormatIndicator = "4";
                    uriBuilder.addParameter("lcsFormatIndicator", lcsClientFormatIndicator);
                }
            }

            if (lcsServiceTypeID != null && coreNetwork != null) {
                uriBuilder.addParameter("lcsServiceTypeId", lcsServiceTypeID);
            }

            if (priority != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    if (priority.equalsIgnoreCase("normal"))
                        priority = "normalPriority";
                    if (priority.equalsIgnoreCase("high") )
                        priority = "highestPriority";
                    uriBuilder.addParameter("priority", priority);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (priority.equalsIgnoreCase("normal"))
                        priority = "0";
                    if (priority.equalsIgnoreCase("high"))
                        priority = "1";
                    uriBuilder.addParameter("lcsPriority", priority);
                }
            }

            if (horizontalAccuracy != null)
                uriBuilder.addParameter("horizontalAccuracy", horizontalAccuracy);

            if (verticalAccuracy != null)
                uriBuilder.addParameter("verticalAccuracy", verticalAccuracy);

            if (verticalCoordinateRequest != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("verticalCoordinateRequest", verticalCoordinateRequest);
                }
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (verticalCoordinateRequest.equalsIgnoreCase("false"))
                        verticalCoordinateRequest = "0";
                    if (verticalCoordinateRequest.equalsIgnoreCase("true"))
                        verticalCoordinateRequest = "1";
                    uriBuilder.addParameter("verticalCoordinateRequest", verticalCoordinateRequest);
                }
            }

            if (responseTimeCategory != null  && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    if (responseTimeCategory.equalsIgnoreCase("low"))
                        responseTimeCategory = "lowdelay";
                    if (responseTimeCategory.equalsIgnoreCase("tolerant"))
                        responseTimeCategory = "delaytolerant";
                    uriBuilder.addParameter("responseTimeCategory", responseTimeCategory);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (responseTimeCategory.equalsIgnoreCase("low"))
                        responseTimeCategory = "0";
                    if (responseTimeCategory.equalsIgnoreCase("tolerant"))
                        responseTimeCategory = "1";
                    uriBuilder.addParameter("responseTime", responseTimeCategory);
                }
            }

            if (deferredLocationEventType != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("deferredLocationEventType", deferredLocationEventType);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (deferredLocationEventType.equalsIgnoreCase("available"))
                        deferredLocationEventType = "0";
                    if (deferredLocationEventType.equalsIgnoreCase("entering"))
                        deferredLocationEventType = "1";
                    if (deferredLocationEventType.equalsIgnoreCase("leaving"))
                        deferredLocationEventType = "2";
                    if (deferredLocationEventType.equalsIgnoreCase("inside"))
                        deferredLocationEventType = "3";
                    if (deferredLocationEventType.equalsIgnoreCase("periodic-ldr"))
                        deferredLocationEventType = "4";
                    if (deferredLocationEventType.equalsIgnoreCase("motion-event"))
                        deferredLocationEventType = "5";
                    if (deferredLocationEventType.equalsIgnoreCase("ldr-activated"))
                        deferredLocationEventType = "6";
                    if (deferredLocationEventType.equalsIgnoreCase("max-interval-expiration"))
                        deferredLocationEventType = "7";
                    uriBuilder.addParameter("lcsDeferredLocationType", deferredLocationEventType);

                }
            }

            if (areaType != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("areaType", areaType);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (areaType.equalsIgnoreCase("countryCode"))
                        areaType = "0";
                    if (areaType.equalsIgnoreCase("plmnId"))
                        areaType = "1";
                    if (areaType.equalsIgnoreCase("locationAreaId"))
                        areaType = "2";
                    if (areaType.equalsIgnoreCase("routingAreaId"))
                        areaType = "3";
                    if (areaType.equalsIgnoreCase("cellGlobalId"))
                        areaType = "4";
                    if (areaType.equalsIgnoreCase("utranCellId"))
                        areaType = "5";
                    if (areaType.equalsIgnoreCase("trackingAreaId"))
                        areaType = "6";
                    if (areaType.equalsIgnoreCase("eUtranCellId"))
                        areaType = "7";
                    uriBuilder.addParameter("lcsAreaType", areaType);
                }
            }

            if (areaId != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("areaId", areaId);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsAreaId", areaId);
                }
            }

            if (occurrenceInfo != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    if (occurrenceInfo.equalsIgnoreCase("once"))
                        occurrenceInfo = "oneTimeEvent";
                    if (occurrenceInfo.equalsIgnoreCase("multiple"))
                        occurrenceInfo = "multipleTimeEvent";
                    uriBuilder.addParameter("occurrenceInfo", occurrenceInfo);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (occurrenceInfo.equalsIgnoreCase("once"))
                        occurrenceInfo = "0";
                    if (occurrenceInfo.equalsIgnoreCase("multiple"))
                        occurrenceInfo = "1";
                    uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", occurrenceInfo);
                }
            }

            if (intervalTime != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("intervalTime", intervalTime);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsAreaEventIntervalTime", intervalTime);
                }
            }

            if (motionEventLinearDistance != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsMotionEventLinearDistance", motionEventLinearDistance);
                    uriBuilder.addParameter("lcsMotionEventReportLocationReqs", "0"); // set to Location Estimate
                    if (motionEventOccurrence != null) {
                        if (motionEventOccurrence.equalsIgnoreCase("once"))
                            motionEventOccurrence = "0";
                        if (motionEventOccurrence.equalsIgnoreCase("multiple"))
                            motionEventOccurrence = "1";
                        uriBuilder.addParameter("lcsMotionEventOccurrenceInfo", motionEventOccurrence);
                    }
                    if (motionEventInterval != null) {
                        uriBuilder.addParameter("lcsMotionEventIntervalTime", motionEventInterval);
                    }
                    if (motionEventMaxInterval != null) {
                        uriBuilder.addParameter("lcsMotionEventMaxInterval", motionEventMaxInterval);
                    }
                    if (motionEventSamplingInterval != null) {
                        uriBuilder.addParameter("lcsMotionEventSamplingInterval", motionEventSamplingInterval);
                    }
                    if (motionEventReportingDuration != null) {
                        uriBuilder.addParameter("lcsMotionEventReportingDuration", motionEventReportingDuration);
                    }
                }
            }

            if (velocityRequested != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (velocityRequested.equalsIgnoreCase("false"))
                        velocityRequested = "0";
                    if (velocityRequested.equalsIgnoreCase("true"))
                        velocityRequested = "1";
                    uriBuilder.addParameter("velocityRequested", velocityRequested);
                }
            }

            if (lcsReferenceNumber != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("lcsReferenceNumber", lcsReferenceNumber);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("slgLcsReferenceNumber", lcsReferenceNumber);
                }
            }


            if (reportingAmount != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("reportingAmount", reportingAmount);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsPeriodicReportingAmount", reportingAmount);
                }
            }

            if (reportingInterval != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("reportingInterval", reportingInterval);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsPeriodicReportingInterval", reportingInterval);
                }
            }

            if (callbackUrl != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("slrCallbackUrl", callbackUrl);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsCallbackUrl", callbackUrl);
                }
            }

            URL url = uriBuilder.build().toURL();
            HttpClient client = HttpClientBuilder.create().build();
            if (logger.isDebugEnabled())
                logger.debug("\ncURL URL: " + url);
            HttpGet request = new HttpGet(String.valueOf(url));
            // Authorization for further stage of the project
            request.addHeader("User-Agent", gmlcUser);
            request.addHeader("User-Password", gmlcPassword);
            HttpResponse response = client.execute(request);
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream stream = null;
                String gmlcResponse = null;
                try {
                    if (httpRespType != null && psiService == null && coreNetwork == null) {
                        // For retro-compatibility with Restcomm GMLC 1.0.0
                        stream = entity.getContent();
                        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                        while (null != (gmlcResponse = br.readLine())) {
                            List<String> items = Arrays.asList(gmlcResponse.split("\\s*,\\s*"));
                            if (logger.isDebugEnabled()) {
                                logger.debug("Data retrieved from GMLC via MAP ATI: " + items.toString());
                            }
                            for (String item : items) {
                                for (int i = 0; i < items.size(); i++) {
                                    if (item.contains("mcc")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("MobileCountryCode", token);
                                    }
                                    if (item.contains("mnc")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("MobileNetworkCode", token);
                                    }
                                    if (item.contains("lac")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("LocationAreaCode", token);
                                    }
                                    if (item.contains("cellid")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("CellId", token);
                                    }
                                    if (item.contains("aol")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("LocationAge", token);
                                    }
                                    if (item.contains("vlrNumber")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("NetworkEntityAddress", token);
                                    }
                                    if (item.contains("subscriberState")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("SubscriberState", token);
                                    }
                                }
                            }
                        }
                    } else {
                        gmlcResponse = EntityUtils.toString(entity, "UTF-8");
                        if (httpRespType == null) {
                            if (gmlcResponse != null) {
                                if (psiService == null && geolocationType.toString().equals(NotificationGT)) {
                                    if (coreNetwork.equalsIgnoreCase("UMTS")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via MAP SRIforLCS-PSL: " + gmlcResponse);
                                        HashMap<String, String> sriPslResponse = parsePslJsonString(gmlcResponse);
                                        putDataFromSriPslResponse(sriPslResponse, data);

                                    } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via Diameter RIR/RIA-PLR/PLA: " + gmlcResponse);
                                        HashMap<String, String> rirPlrResponse = parsePlrJsonString(gmlcResponse);
                                        putDataFromRirPlrResponse(rirPlrResponse, data);
                                    }
                                }
                                if (coreNetwork != null && psiService == null && !geolocationType.toString().equals(NotificationGT)) {
                                    if (coreNetwork.equalsIgnoreCase("IMS")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via Sh UDR/UDA: " + gmlcResponse);
                                        HashMap<String, String> shUdrResponse = parseUdaJsonString(gmlcResponse);
                                        putDataFromShUdrResponse(shUdrResponse, data);
                                    }
                                }
                                if (psiService == null && !geolocationType.toString().equals(NotificationGT) && coreNetwork == null) {
                                    if (logger.isDebugEnabled())
                                        logger.debug("Data retrieved from GMLC via MAP ATI: " + gmlcResponse);
                                    HashMap<String, String> atiResponse = parseAtiOrPsiJsonString(gmlcResponse);
                                    putDataFromAtiOrPsiResponse(atiResponse, data);

                                } else if (psiService != null) {
                                    if (psiService.equalsIgnoreCase("true") && !geolocationType.toString().equals(NotificationGT)) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via MAP PSI: " + gmlcResponse);
                                        HashMap<String, String> psiResponse = parseAtiOrPsiJsonString(gmlcResponse);
                                        putDataFromAtiOrPsiResponse(psiResponse, data);
                                    }
                                }
                            }
                        }
                    }
                    if (gmlcURI != null && gmlcResponse != null) {
                        // For debugging purposes only
                        if (logger.isDebugEnabled()) {
                            logger.debug("Geolocation data of " + targetMSISDN + " retrieved from GMLC at: " + gmlcURI);
                            logger.debug("\nDevice Identifier = " + data.getFirst("DeviceIdentifier"));
                            logger.debug("\nMSISDN = " + getLong("MSISDN", data));
                            logger.debug("\nIMSI = " + getLong("IMSI", data));
                            logger.debug("\nIMEI = " + data.getFirst("IMEI"));
                            logger.debug("\nLMSI = " + getLong("LMSI", data));
                            logger.debug("\nMCC = " + getInteger("MobileCountryCode", data));
                            logger.debug("\nMNC = " + data.getFirst("MobileNetworkCode"));
                            logger.debug("\nLAC  = " + data.getFirst("LocationAreaCode"));
                            logger.debug("\nCI = " + data.getFirst("CellId"));
                            logger.debug("\nSAC = " + data.getFirst("ServiceAreaCode"));
                            logger.debug("\nENodeBId = " + getInteger("ENodeBId", data));
                            logger.debug("\nTAC = " + data.getFirst("TrackingAreaCode"));
                            logger.debug("\nRAC = " + data.getFirst("RoutingAreaCode"));
                            logger.debug("\nLocation Number Address = " + data.getFirst("LocationNumberAddress"));
                            logger.debug("\nAOL = " + getInteger("LocationAge", data));
                            logger.debug("\nSubscriber State = " + data.getFirst("SubscriberState"));
                            logger.debug("\nNot Reachable Reason = " + data.getFirst("NotReachableReason"));
                            logger.debug("\nNetwork Entity Address = " + getLong("NetworkEntityAddress", data));
                            logger.debug("\nNetwork Entity Name = " + data.getFirst("NetworkEntityName"));
                            logger.debug("\nType of Shape = " + data.getFirst("TypeOfShape"));
                            logger.debug("\nDevice Latitude = " + data.getFirst("DeviceLatitude"));
                            logger.debug("\nDevice Longitude = " + data.getFirst("DeviceLongitude"));
                            logger.debug("\nUncertainty = " + data.getFirst("Uncertainty"));
                            logger.debug("\nUncertainty Semi Major Axis = " + data.getFirst("UncertaintySemiMajorAxis"));
                            logger.debug("\nUncertainty Semi Minor Axis = " + data.getFirst("UncertaintySemiMinorAxis"));
                            logger.debug("\nAngle Of Major Axis = " + data.getFirst("AngleOfMajorAxis"));
                            logger.debug("\nConfidence = " + data.getFirst("Confidence"));
                            logger.debug("\nDevice Altitude = " + data.getFirst("DeviceAltitude"));
                            logger.debug("\nUncertaintyAltitude = " + data.getFirst("UncertaintyAltitude"));
                            logger.debug("\nInner Radius = " + data.getFirst("InnerRadius"));
                            logger.debug("\nUncertainty Inner Radius = " + data.getFirst("UncertaintyInnerRadius"));
                            logger.debug("\nOffset Angle = " + data.getFirst("OffsetAngle"));
                            logger.debug("\nIncluded Angle = " + data.getFirst("IncludedAngle"));
                            logger.debug("\nHorizontal Speed = " + data.getFirst("HorizontalSpeed"));
                            logger.debug("\nVertical Speed = " + data.getFirst("VerticalSpeed"));
                            logger.debug("\nUncertainty Horizontal Speed = " + data.getFirst("UncertaintyHorizontalSpeed"));
                            logger.debug("\nUncertainty Vertical Speed = " + data.getFirst("UncertaintyVerticalSpeed"));
                            logger.debug("\nBearing = " + data.getFirst("Bearing"));
                            logger.debug("\nCivic Address = " + data.getFirst("CivicAddress"));
                            logger.debug("\nBarometric Pressure = " + getLong("BarometricPressure", data));
                            logger.debug("\nResponse Status = " + data.getFirst("ResponseStatus"));
                            logger.debug("\nCause = " + data.getFirst("Cause"));
                        }
                    }

                } finally {
                    if (stream != null)
                        stream.close();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            if (logger.isInfoEnabled()) {
                logger.info("Problem while trying to retrieve data from GMLC, exception: "+ex);
            }
            return status(INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }

        Geolocation geolocation = createFrom(new Sid(accountSid), data, geolocationType);

        if (geolocation.getResponseStatus() != null
                && geolocation.getResponseStatus().equals(responseStatus.Rejected.toString())) {
            if (APPLICATION_XML_TYPE.equals(responseType)) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE.equals(responseType)) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        } else {

            dao.addGeolocation(geolocation);

            if (APPLICATION_XML_TYPE.equals(responseType)) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE.equals(responseType)) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private void validate(final MultivaluedMap<String, String> data, Geolocation.GeolocationType glType)
            throws RuntimeException {

        // ** Validation of Geolocation POST requests with valid type **/
        if (!glType.toString().equals(ImmediateGT) && !glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: Geolocation Type can not be null, but either \"Immediate\" or \"Notification\".");
        }

        /*** DeviceIdentifier can not be null ***/
        if (!data.containsKey("DeviceIdentifier")) {
            throw new NullPointerException("Rejected: DeviceIdentifier value can not be null");
        }

        /*** StatusCallback can not be null ***/
        if (!data.containsKey("StatusCallback")) {
            throw new NullPointerException("Rejected: StatusCallback value can not be null");
        }

        /*** CoreNetwork must not be null or different than GSM or LTE for Notification type of Geolocation ***/
        if (!data.containsKey("CoreNetwork") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: CoreNetwork value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("CoreNetwork") && glType.toString().equals(NotificationGT)) {
            String coreNetwork = data.getFirst("CoreNetwork");
            if (!coreNetwork.equalsIgnoreCase("umts") && !coreNetwork.equalsIgnoreCase("lte")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: CoreNetwork value not API compliant, must be UMTS or LTE for Notification type of Geolocation");
            }
        }

        /*** Domain must be API compliant: cs or ps values only for Immediate type of Geolocation***/
        if (data.containsKey("Domain") && glType.toString().equals(ImmediateGT)) {
            String psiService = data.getFirst("Domain");
            if (!psiService.equalsIgnoreCase("cs") && !psiService.equalsIgnoreCase("ps")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Domain values can be only cs or ps for Circuit-Switched or Packet-Switched networks respectively");
            }
        }

        /*** PsiService must be API compliant: true or false values only for Immediate type of Geolocation***/
        if (data.containsKey("PsiService") && glType.toString().equals(ImmediateGT)) {
            String psiService = data.getFirst("PsiService");
            if (!psiService.equalsIgnoreCase("true") && !psiService.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PsiService values can be only true or false");
            }
        }

        /*** ClientType must be API compliant: emergency, vas, operator, lawful or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE") || network.equalsIgnoreCase("UMTS")) {
                throw new NullPointerException("Rejected: ClientType value can not be null for Notification type of Geolocation");
            }
        }
        if (data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE") || network.equalsIgnoreCase("UMTS")) {
                String locationEstimateType = data.getFirst("ClientType");
                if (!locationEstimateType.equalsIgnoreCase("emergency") && !locationEstimateType.equalsIgnoreCase("vas")
                    && !locationEstimateType.equalsIgnoreCase("operator") && !locationEstimateType.equalsIgnoreCase("lawful")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ClientType value not API compliant, must be emergency, vas, operator or lawful " +
                        "for Notification type of Geolocation");
                }
            }
        }

        /*** LocationEstimateType must be API compliant: fast or slow for Notification type of Geolocation only ***/
        if (data.containsKey("LocationEstimateType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: LocationEstimateType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: LocationEstimateType value can not be null for Notification type of Geolocation");
        }
        if (data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            String locationEstimateType = data.getFirst("LocationEstimateType");
            if (!locationEstimateType.equalsIgnoreCase("lastKnown") && !locationEstimateType.equalsIgnoreCase("initial")
                && !locationEstimateType.equalsIgnoreCase("current") && !locationEstimateType.equalsIgnoreCase("activateDeferred")
                && !locationEstimateType.equalsIgnoreCase("cancelDeferred") && !locationEstimateType.equalsIgnoreCase("notificationVerificationOnly")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: LocationEstimateType value not API compliant, must be lastKnown, initial, current, activateDeferred," +
                    " cancelDeferred or notificationVerificationOnly");
            }
        }

        /*** ClientName must be API compliant: not null for Notification type of Geolocation for LTE ***/
        if (data.containsKey("ClientName") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientName only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientName") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                throw new NullPointerException("Rejected: ClientName value can not be null for Notification type of Geolocation in LTE");
            }
        }

        /*** ClientNameFormat must be API compliant: name, email, msisdn, url or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientNameFormat") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientNameFormat only applies for Notification type of Geolocation in LTE");
        }
        if (!data.containsKey("ClientNameFormat") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                throw new NullPointerException("Rejected: ClientNameFormat value can not be null for Notification type of Geolocation in LTE");
            }
        }
        if (data.containsKey("ClientNameFormat") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                String locationEstimateType = data.getFirst("ClientNameFormat");
                if (!locationEstimateType.equalsIgnoreCase("name") && !locationEstimateType.equalsIgnoreCase("email")
                    && !locationEstimateType.equalsIgnoreCase("msisdn") && !locationEstimateType.equalsIgnoreCase("url")
                    && !locationEstimateType.equalsIgnoreCase("sip")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ClientNameFormat value not API compliant, must be name, email, msisdn, url or sip " +
                        "for Notification type of Geolocation in LTE");
                }
            }
        }

        /*** ReferenceNumber must be API compliant: not null and a positive integer value for Notification type of Geolocation only ***/
        if (data.containsKey("ReferenceNumber") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ReferenceNumber only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ReferenceNumber") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: ReferenceNumber value can not be null for Notification type of Geolocation");
        }
        if (data.containsKey("ReferenceNumber") && glType.toString().equals(NotificationGT)) {
            try {
                Integer lcsReferenceNumber = Integer.valueOf(data.getFirst("ReferenceNumber"));
                if (lcsReferenceNumber > Integer.MAX_VALUE || lcsReferenceNumber < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ReferenceNumber value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ReferenceNumber value not API compliant, must be a positive integer value");
            }
        }

        /**** LCS-QoS parameters ****/
        /***************************/
        /*** HorizontalAccuracy must be API compliant: a positive integer value for Notification type of Geolocation only ***/
        if (data.containsKey("HorizontalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: HorizontalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("HorizontalAccuracy") && glType.toString().equals(NotificationGT)) {
            try {
                Integer horizontalAccuracy = Integer.valueOf(data.getFirst("HorizontalAccuracy"));
                if (horizontalAccuracy > Integer.MAX_VALUE || horizontalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: HorizontalAccuracy value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: HorizontalAccuracy value not API compliant, must be a positive integer value");
            }
        }
        /*** VerticalAccuracy must be API compliant: a positive integer value for Notification type of Geolocation only***/
        if (data.containsKey("VerticalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VerticalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalAccuracy") && glType.toString().equals(NotificationGT)) {
            Integer verticalAccuracy = Integer.valueOf(data.getFirst("VerticalAccuracy"));
            try {
                if (verticalAccuracy > Integer.MAX_VALUE || verticalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: VerticalAccuracy value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalAccuracy value not API compliant, must be a positive integer value");
            }
        }
        /*** VerticalCoordinateRequest must be API compliant: a boolean value for Notification type of Geolocation only***/
        if (data.containsKey("VerticalCoordinateRequest") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VerticalCoordinateRequest only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalCoordinateRequest") && glType.toString().equals(NotificationGT)) {
            String verticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            if (!verticalCoordinateRequest.equalsIgnoreCase("true") && !verticalCoordinateRequest.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalCoordinateRequest value not API compliant, must be true or false");
            }
        }
        /*** ResponseTime must be API compliant: fast or slow for Notification type of Geolocation***/
        if (data.containsKey("ResponseTime") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ResponseTime only applies for Notification type of Geolocation");
        }
        if (data.containsKey("ResponseTime") && glType.toString().equals(NotificationGT)) {
            String priority = data.getFirst("ResponseTime");
            if (!priority.equalsIgnoreCase("low") && !priority.equalsIgnoreCase("tolerant")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ResponseTime value not API compliant, must be low or tolerant");
            }
        }

        /*** VelocityRequested must be API compliant: true of false for Notification type of Geolocation only ***/
        if (data.containsKey("VelocityRequested") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VelocityRequested only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VelocityRequested") && glType.toString().equals(NotificationGT)) {
            String priority = data.getFirst("VelocityRequested");
            if (!priority.equalsIgnoreCase("true") && !priority.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VelocityRequested value not API compliant, must be true or false");
            }
        }

        /*** Priority must be API compliant: normal or high for Notification type of Geolocation only ***/
        if (data.containsKey("Priority") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: Priority only applies for Notification type of Geolocation");
        }
        if (data.containsKey("Priority") && glType.toString().equals(NotificationGT)) {
            String priority = data.getFirst("Priority");
            if (!priority.equalsIgnoreCase("normal") && !priority.equalsIgnoreCase("high")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Priority value not API compliant, must be normal or high");
            }
        }

        /*** ServiceTypeID must belong to Notification type of Geolocation only and API compliant ***/
        if (data.containsKey("ServiceTypeID") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ServiceTypeID only applies for Notification type of Geolocation");
        }
        if (data.containsKey("ServiceTypeID") && glType.toString().equals(NotificationGT)) {
            try {
                Long lcsServiceTypeId = Long.valueOf(data.getFirst("ServiceTypeID"));
                if (lcsServiceTypeId > 127 || lcsServiceTypeId < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ServiceTypeID value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ServiceTypeID value not API compliant, must be a positive integer value");
            }
        }

        /*** DeviceLatitude must be API compliant***/
        if (data.containsKey("DeviceLatitude")) {
            String deviceLat = data.getFirst("DeviceLatitude");
            Boolean devLatWGS84 = validateGeoCoordinatesFormat(deviceLat);
            if (!devLatWGS84) {
                throw new IllegalArgumentException("Rejected: DeviceLatitude not API compliant");
            }
        }

        /*** DeviceLongitude must be API compliant ***/
        if (data.containsKey("DeviceLongitude")) {
            String deviceLong = data.getFirst("DeviceLongitude");
            Boolean devLongWGS84 = validateGeoCoordinatesFormat(deviceLong);
            if (!devLongWGS84) {
                throw new IllegalArgumentException("Rejected: DeviceLongitude not API compliant");
            }
        }

        /**** Deferred Location by Event parameters ****/
        /**********************************************/
        /*** DeferredLocationEventType must belong to Notification type of Geolocation and API compliant ***/
        if (data.containsKey("DeferredLocationEventType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: DeferredLocationEventType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("DeferredLocationEventType") && glType.toString().equals(NotificationGT)) {
            String eventGeofenceType = data.getFirst("DeferredLocationEventType");
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("UMTS")) {
                if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                    && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: DeferredLocationEventType value not API compliant, must be one of available, inside, " +
                        "entering or leaving for Notification type of Geolocation in UMTS");
                }
            } else if (network.equalsIgnoreCase("LTE")) {
                if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                    && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")
                    && !eventGeofenceType.equalsIgnoreCase("periodic-ldr") && !eventGeofenceType.equalsIgnoreCase("motion-event")
                    && !eventGeofenceType.equalsIgnoreCase("ldr-activated")
                    && !eventGeofenceType.equalsIgnoreCase("max-interval-expiration")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: DeferredLocationEventType value not API compliant, " +
                        "must be one of available, inside, entering, leaving, periodic-ldr, motion-event, ldr-activated or max-interval-expiration " +
                        "for Notification type of Geolocation in LTE");
                }
            }
        }

        /**** Geofence Area Event Info parameters ****/
        /*********************************************/
        /*** GeofenceType must belong to to Notification type of Geolocation and API compliant and specific DeferredLocationEventType values ***/
        if (data.containsKey("GeofenceType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: GeofenceType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceType") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String eventGeofenceType = data.getFirst("DeferredLocationEventType");
                if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                    && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, GeofenceType only applies when DeferredLocationEventType value equals " +
                        "available, inside, entering, leaving");
                }
            }
            String geofenceType = data.getFirst("GeofenceType");
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                if (!geofenceType.equalsIgnoreCase("locationAreaId") && !geofenceType.equalsIgnoreCase("cellGlobalId")
                    && !geofenceType.equalsIgnoreCase("countryCode") && !geofenceType.equalsIgnoreCase("plmnId")
                    && !geofenceType.equalsIgnoreCase("routingAreaId") && !geofenceType.equalsIgnoreCase("utranCellId")
                    && !geofenceType.equalsIgnoreCase("trackingAreaId") && !geofenceType.equalsIgnoreCase("eUtranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceType value not API compliant, must be one of locationAreaId, cellGlobalId, countryCode, " +
                        "plmnId, routingAreaId, utranCellId, trackingAreaId or eUtranCellId for Notification type of Geolocation in LTE");
                }
            } else if (network.equalsIgnoreCase("UMTS")) {
                if (!geofenceType.equalsIgnoreCase("locationAreaId") && !geofenceType.equalsIgnoreCase("cellGlobalId")
                    && !geofenceType.equalsIgnoreCase("countryCode") && !geofenceType.equalsIgnoreCase("plmnId")
                    && !geofenceType.equalsIgnoreCase("routingAreaId") && !geofenceType.equalsIgnoreCase("utranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceType value not API compliant, must be one of locationAreaId, cellGlobalId, countryCode, " +
                        "plmnId, routingAreaId or utranCellId for Notification type of Geolocation in UMTS");
                }
            }
            if (!data.containsKey("GeofenceId")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: GeofenceId argument must not be null when a valid GeofenceType is provided");
            }
        }
        /*** GeofenceId must belong to Notification type of Geolocation only ***/
        if (data.containsKey("GeofenceId") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: GeofenceId only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceId") && glType.toString().equals(NotificationGT)) {
            String geofenceIdString = data.getFirst("GeofenceId");
            int geofenceIdStringLength = geofenceIdString.length();
            try {
                Long geofenceId = Long.valueOf(geofenceIdString);
                if (geofenceIdStringLength < 2 && geofenceIdStringLength > 7) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceId value not API compliant, must be a positive integer value according to the GeofenceType");
                }
                if (!data.containsKey("GeofenceType")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceType argument must not be null when a valid GeofenceId is provided");
                } else {
                    String geofenceType = data.getFirst("GeofenceType");
                    if (geofenceType.equalsIgnoreCase("locationAreaId") || geofenceType.equalsIgnoreCase("cellGlobalId")
                        || geofenceType.equalsIgnoreCase("countryCode") || geofenceType.equalsIgnoreCase("plmnId")
                        || geofenceType.equalsIgnoreCase("utranCellId")) {
                        if (geofenceIdStringLength < 2 || geofenceIdStringLength > 7) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: Error: GeofenceId must be a positive integer value according to the GeofenceType");
                        }
                    } else if (geofenceType.equalsIgnoreCase("trackingAreaId")) {
                        if (geofenceIdStringLength != 5) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: Error: GeofenceId must be a positive integer value according to the GeofenceType");
                        }
                    } else if (geofenceType.equalsIgnoreCase("eUtranCellId")) {
                        if (geofenceIdStringLength != 7) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: Error: GeofenceId must be a positive integer value according to the GeofenceType");
                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: GeofenceId value not API compliant, must be a positive integer value");
            }
        }
        /*** GeofenceOccurrenceInfo ***/
        if (data.containsKey("GeofenceOccurrenceInfo") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: GeofenceOccurrenceInfo only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceOccurrenceInfo") && glType.toString().equals(NotificationGT)) {
            String occurrenceInfo = data.getFirst("GeofenceOccurrenceInfo");
            if (!occurrenceInfo.equalsIgnoreCase("once") && !occurrenceInfo.equalsIgnoreCase("multiple")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: GeofenceOccurrenceInfo value not API compliant, " +
                    "must be one of once (for one time event) or multiple (for multiple time events)");
            }
            if (!data.containsKey("GeofenceType") || !data.containsKey("GeofenceId") && !data.containsKey("GeofenceIntervalTime")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: GeofenceType and/or GeofenceId and/or GeofenceIntervalTime arguments must not be null when " +
                    "a valid GeofenceOccurrenceInfo is provided");
            }
        }
        /*** GeofenceIntervalTime must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("GeofenceIntervalTime") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: GeofenceIntervalTime only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceIntervalTime") && glType.toString().equals(NotificationGT)) {
            try {
                Long eventIntervalTime = Long.valueOf(data.getFirst("GeofenceIntervalTime"));
                if (eventIntervalTime > 32767 || eventIntervalTime < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceIntervalTime value not API compliant, must be a positive integer value not greater than 32767");
                }
                if (!data.containsKey("GeofenceOccurrenceInfo") && !data.containsKey("GeofenceType") || !data.containsKey("GeofenceId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: GeofenceType and/or GeofenceId and/or GeofenceOccurrenceInfo arguments must not be null when" +
                        " a valid GeofenceIntervalTime is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: GeofenceIntervalTime value not API compliant, must be a positive integer value not greater than 32767");
            }
        }

        /**** Motion Event parameters ****/
        /*********************************/
        /*** MotionEventRange must belong to Notification type of Geolocation only and specific DeferredLocationEventType value ***/
        if (data.containsKey("MotionEventRange") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: EventRange only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventRange") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, EventRange only applies when DeferredLocationEventType value equals " +
                            "motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, EventRange only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            try {
                Long eventRange = Long.valueOf(data.getFirst("MotionEventRange"));
                if (eventRange > 10000 || eventRange < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: EventRange value not API compliant, must be a positive integer value lower than 10000 (meters)");
                }
                if (!data.containsKey("MotionEventReportingDuration") && !data.containsKey("MotionEventOccurrence") && !data.containsKey("MotionEventInterval") &&
                    !data.containsKey("MotionEventMaxInterval") && !data.containsKey("MotionEventSamplingInterval")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventOccurrence and/or MotionEventInterval and/or MotionEventMaxInterval and/or " +
                        "MotionEventSamplingInterval and/or MotionEventSamplingInterval arguments must not be null when a valid " +
                        "MotionEventRange is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: EventRange value not API compliant, must be a positive integer value");
            }
        }
        /*** MotionEventOccurrence must belong to Notification type of Geolocation only ***/
        if (data.containsKey("MotionEventOccurrence") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventOccurrence only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventOccurrence") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventOccurrence only applies when DeferredLocationEventType value equals " +
                            "motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventOccurrence only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            String motionEventOccurrence = data.getFirst("MotionEventOccurrence");
            if (!motionEventOccurrence.equalsIgnoreCase("once") && !motionEventOccurrence.equalsIgnoreCase("multiple")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventOccurrence value not API compliant, " +
                    "must be one of once (for one time event) or multiple (for multiple time events)");
            }
            if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventReportingDuration") && !data.containsKey("MotionEventInterval") &&
                !data.containsKey("MotionEventMaxInterval") && !data.containsKey("MotionEventSamplingInterval")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventReportingDuration and/or MotionEventInterval " +
                    "and/or MotionEventMaxInterval and/or MotionEventSamplingInterval arguments must not be null when a valid " +
                    "MotionEventOccurrence is provided");
            }
        }
        /*** MotionEventInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventInterval") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventInterval only applies when DeferredLocationEventType value equals " +
                            "motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventInterval only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            try {
                Long motionEventInterval = Long.valueOf(data.getFirst("MotionEventInterval"));
                if (motionEventInterval > 3600 || motionEventInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventInterval value not API compliant, must be a positive integer value not greater than 3600");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and MotionEventOccurrence arguments must not be null when a valid " +
                        "MotionEventInterval is provided");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventReportingDuration") && !data.containsKey("MotionEventOccurrence") &&
                    !data.containsKey("MotionEventMaxInterval") && !data.containsKey("MotionEventSamplingInterval")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventReportingDuration and/or MotionEventOccurrence " +
                        "and/or MotionEventMaxInterval and/or MotionEventSamplingInterval arguments must not be null when a valid " +
                        "MotionEventInterval is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventInterval value not API compliant, must be a positive integer value not greater than 3600");
            }
        }
        /*** MotionEventMaxInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventMaxInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventMaxInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventMaxInterval") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventMaxInterval only applies when DeferredLocationEventType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventMaxInterval only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            try {
                Long motionEventMaxInterval = Long.valueOf(data.getFirst("MotionEventMaxInterval"));
                if (motionEventMaxInterval > 86400 || motionEventMaxInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventMaxInterval value not API compliant, must be a positive integer value " +
                        "not greater than 86400");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence") && !data.containsKey("MotionEventInterval")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventOccurrence and/or MotionEventInterval arguments must not be null " +
                        "when a valid MotionEventMaxInterval is provided");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventReportingDuration") && !data.containsKey("MotionEventOccurrence") &&
                    !data.containsKey("MotionEventInterval") && !data.containsKey("MotionEventSamplingInterval")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventReportingDuration and/or MotionEventOccurrence " +
                        "and/or MotionEventInterval and/or MotionEventSamplingInterval arguments must not be null when a valid " +
                        "MotionEventMaxInterval is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventMaxInterval value not API compliant, must be a positive integer value not greater than 86400");
            }
        }
        /*** MotionEventSamplingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventSamplingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventSamplingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventSamplingInterval") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventSamplingInterval only applies when DeferredLocationEventType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventSamplingInterval only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            try {
                Long motionEventSamplingInterval = Long.valueOf(data.getFirst("MotionEventSamplingInterval"));
                if (motionEventSamplingInterval > 32767 || motionEventSamplingInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventSamplingInterval value not API compliant, must be a positive integer value " +
                        "not greater than 32767");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence") && !data.containsKey("MotionEventInterval") &&
                    !data.containsKey("MotionEventMaxInterval") && !data.containsKey("MotionEventReportingDuration")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventOccurrence and/or MotionEventInterval " +
                        "and/or MotionEventMaxInterval and/or MotionEventReportingDuration arguments must not be null when a valid " +
                        "MotionEventSamplingInterval is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventSamplingInterval value not API compliant, must be a positive integer value " +
                    "not greater than 32767");
            }
        }
        /*** MotionEventReportingDuration must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventReportingDuration") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventReportingDuration only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventReportingDuration") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventReportingDuration only applies when DeferredLocationEventType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventReportingDuration only applies when DeferredLocationEventType value equals " +
                    "motion-event for Notification type of Geolocation in LTE");
            }
            try {
                Long motionEventReportingDuration = Long.valueOf(data.getFirst("MotionEventReportingDuration"));
                if (motionEventReportingDuration > 8640000 || motionEventReportingDuration < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventReportingDuration value not API compliant, must be a positive integer value " +
                        "not greater than 8640000");
                }
                if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence") && !data.containsKey("MotionEventInterval") &&
                    !data.containsKey("MotionEventMaxInterval") && !data.containsKey("MotionEventSamplingInterval")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventOccurrence and/or MotionEventInterval " +
                        "and/or MotionEventMaxInterval and/or MotionEventSamplingInterval arguments must not be null when a valid " +
                        "MotionEventReportingDuration is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventReportingDuration value not API compliant, must be a positive integer value " +
                    "not greater than 8640000");
            }
        }

        /**** Periodic Location Deferred Request parameters ****/
        /******************************************************/
        /*** EventReportingAmount must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("EventReportingAmount") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: EventReportingAmount only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventReportingAmount") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("periodic-ldr") && !deferredLocationEventType.equalsIgnoreCase("ldr-activated")
                        && !deferredLocationEventType.equalsIgnoreCase("max-interval-expiration")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, EventReportingAmount only applies when DeferredLocationEventType value " +
                            "equals periodic-ldr, ldr-activated or max-interval-expiration for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, EventReportingAmount only applies when DeferredLocationEventType value equals " +
                    "periodic-ldr, ldr-activated or max-interval-expiration for Notification type of Geolocation in LTE");
            }
            try {
                Long eventReportingAmount = Long.valueOf(data.getFirst("EventReportingAmount"));
                if (eventReportingAmount > 8639999 || eventReportingAmount < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: EventReportingAmount value not API compliant, must be a positive integer value not greater " +
                        "than 8639999");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: EventReportingAmount value not API compliant, must be a positive integer value not greater " +
                    "than 8639999");
            }
        }

        /*** EventReportingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("EventReportingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: EventReportingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventReportingInterval") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationEventType")) {
                String deferredLocationEventType = data.getFirst("DeferredLocationEventType");
                String network = data.getFirst("CoreNetwork");
                if (network.equalsIgnoreCase("LTE")) {
                    if (!deferredLocationEventType.equalsIgnoreCase("periodic-ldr") && !deferredLocationEventType.equalsIgnoreCase("ldr-activated")
                        && !deferredLocationEventType.equalsIgnoreCase("max-interval-expiration")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, EventReportingInterval only applies when DeferredLocationEventType value " +
                            "equals periodic-ldr, ldr-activated or max-interval-expiration for Notification type of Geolocation in LTE");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Not API compliant, EventReportingAmount only applies when DeferredLocationEventType value equals " +
                    "periodic-ldr, ldr-activated or max-interval-expiration for Notification type of Geolocation in LTE");
            }
            try {
                Long eventReportingInterval = Long.valueOf(data.getFirst("EventReportingInterval"));
                if (eventReportingInterval > 8639999 || eventReportingInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: EventReportingInterval value not API compliant, must be a positive integer value not greater " +
                        "than 8639999");
                }
                if (!data.containsKey("EventReportingAmount")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: EventReportingAmount argument must not be null when a valid EventReportingInterval is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: EventReportingInterval value not API compliant, must be a positive integer value not greater " +
                    "than 8639999");
            }
        }

        /*** LocationTimestamp must be API compliant: DateTime format only ***/
        try {
            if (data.containsKey("LocationTimestamp")) {
                @SuppressWarnings("unused")
                DateTime locationTimestamp = getDateTime("LocationTimestamp", data);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Rejected: LocationTimestamp value is not API compliant");
        }
    }

    private Geolocation createFrom(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {

        if (rStatus != null && rStatus.equals(responseStatus.Failed.toString())) {
            Geolocation gl = buildFailedGeolocation(accountSid, data, glType);
            return gl;
        } else {
            Geolocation gl = buildGeolocation(accountSid, data, glType);
            return gl;
        }
    }

    private Geolocation buildGeolocation(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        String geoloctype = glType.toString();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setMsisdn(getLong("MSISDN", data));
        builder.setImsi(getLong("IMSI", data));
        builder.setImei(data.getFirst("IMEI"));
        builder.setLmsi(getLong("LMSI", data));
        builder.setReferenceNumber(getLong("ReferenceNumber", data));
        builder.setGeolocationType(glType);
        if (glType.equals(GeolocationType.Immediate)) {
            if ((getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                (data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null)) && getInteger("LocationAge", data) < 2) {
                rStatus = responseStatus.Successful.toString();
            } else if ((getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                (data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null)) && (getInteger("LocationAge", data) > 1 ||
                data.getFirst("NotReachableReason") != null)) {
                rStatus = responseStatus.LastKnown.toString();
            } else if ((getInteger("CellId", data) == null || getInteger("ServiceAreaCode", data) == null ||
                (data.getFirst("DeviceLatitude") == null || data.getFirst("DeviceLongitude") == null)) &&
                (getInteger("TrackingAreaCode", data) != null || getInteger("RoutingAreaCode", data) != null ||
                    getInteger("LocationAreaCode", data) != null)) {
                rStatus = responseStatus.PartiallySuccessful.toString();
            }
        } else if (glType.equals(GeolocationType.Notification)) {
            if ((data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null) && getInteger("LocationAge", data) < 2) {
                rStatus = responseStatus.Successful.toString();
            } else if ((data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null) && getInteger("LocationAge", data) > 1) {
                rStatus = responseStatus.LastKnown.toString();
            } else if ((getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                getInteger("TrackingAreaCode", data) != null || getInteger("RoutingAreaCode", data) != null ||
                getInteger("LocationAreaCode", data) != null) && getInteger("LocationAge", data) < 2) {
                rStatus = responseStatus.PartiallySuccessful.toString();
            } else if ((getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                getInteger("TrackingAreaCode", data) != null || getInteger("RoutingAreaCode", data) != null ||
                getInteger("LocationAreaCode", data) != null) && getInteger("LocationAge", data) > 1) {
                rStatus = responseStatus.LastKnown.toString();
            }
        }
        builder.setResponseStatus(rStatus);
        builder.setCause(data.getFirst("Cause"));
        builder.setMobileCountryCode(getInteger("MobileCountryCode", data));
        builder.setMobileNetworkCode(getInteger("MobileNetworkCode", data));
        builder.setLocationAreaCode(getInteger("LocationAreaCode", data));
        builder.setCellId(getInteger("CellId", data));
        builder.setServiceAreaCode(getInteger("ServiceAreaCode", data));
        builder.setEnodebId(getInteger("ENodeBId", data));
        builder.setTrackingAreaCode(getInteger("TrackingAreaCode", data));
        builder.setRoutingAreaCode(getInteger("RoutingAreaCode", data));
        builder.setLocationNumberAddress(getLong("LocationNumberAddress", data));
        builder.setAgeOfLocationInfo(getInteger("LocationAge", data));
        builder.setSubscriberState(data.getFirst("SubscriberState"));
        builder.setNotReachableReason(data.getFirst("NotReachableReason"));
        builder.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        builder.setNetworkEntityName(data.getFirst("NetworkEntityName"));
        builder.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        builder.setTypeOfShape(data.getFirst("TypeOfShape"));
        builder.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        builder.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        if (data.getFirst("Uncertainty") != null)
            builder.setUncertainty(Double.valueOf(data.getFirst("Uncertainty")));
        if (data.getFirst("UncertaintySemiMajorAxis") != null)
            builder.setUncertaintySemiMajorAxis(Double.valueOf(data.getFirst("UncertaintySemiMajorAxis")));
        if (data.getFirst("UncertaintySemiMinorAxis") != null)
            builder.setUncertaintySemiMinorAxis(Double.valueOf(data.getFirst("UncertaintySemiMinorAxis")));
        if (data.getFirst("AngleOfMajorAxis") != null)
            builder.setAngleOfMajorAxis(Double.valueOf(data.getFirst("AngleOfMajorAxis")));
        builder.setConfidence(getInteger("Confidence", data));
        builder.setAltitude(getInteger("DeviceAltitude", data));
        if (data.getFirst("UncertaintyAltitude") != null)
            builder.setUncertaintyAltitude(Double.valueOf(data.getFirst("UncertaintyAltitude")));
        builder.setInnerRadius(getInteger("InnerRadius", data));
        if (data.getFirst("UncertaintyInnerRadius") != null)
            builder.setUncertaintyInnerRadius(Double.valueOf(data.getFirst("UncertaintyInnerRadius")));
        if (data.getFirst("OffsetAngle") != null)
            builder.setOffsetAngle(Double.valueOf(data.getFirst("OffsetAngle")));
        if (data.getFirst("IncludedAngle") != null)
            builder.setIncludedAngle(Double.valueOf(data.getFirst("IncludedAngle")));
        builder.setHorizontalSpeed(getInteger("HorizontalSpeed", data));
        builder.setVerticalSpeed(getInteger("VerticalSpeed", data));
        builder.setUncertaintyHorizontalSpeed(getInteger("UncertaintyHorizontalSpeed", data));
        builder.setUncertaintyVerticalSpeed(getInteger("UncertaintyVerticalSpeed", data));
        builder.setBearing(getInteger("Bearing", data));
        builder.setDeferredLocationEventType(data.getFirst("DeferredLocationEventType"));
        builder.setGeofenceType(data.getFirst("GeofenceType"));
        builder.setGeofenceId(data.getFirst("GeofenceId"));
        builder.setMotionEventRange(getLong("MotionEventRange", data));
        builder.setCivicAddress(data.getFirst("CivicAddress"));
        builder.setBarometricPressure(getLong("BarometricPressure", data));
        builder.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        builder.setInternetAddress(data.getFirst("InternetAddress"));
        builder.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation buildFailedGeolocation(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        String geoloctype = glType.toString();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setReferenceNumber(getLong("ReferenceNumber", data));
        builder.setResponseStatus(rStatus);
        rStatus = null;
        builder.setGeolocationType(glType);
        builder.setCause(cause);
        cause = null;
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    protected Response updateGeolocation(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType, UserIdentityContext userIdentityContext) {
        Account account;
        try {
            account = accountsDao.getAccount(accountSid);
            permissionEvaluator.secure(account, "RestComm:Modify:Geolocation", userIdentityContext);
        } catch (final Exception exception) {
            return status(UNAUTHORIZED).build();
        }
        Geolocation geolocation = dao.getGeolocation(new Sid(sid));
        if (geolocation == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                permissionEvaluator.secure(account, geolocation.getAccountSid(), SecuredType.SECURED_APP, userIdentityContext);
            } catch (final NullPointerException exception) {
                return status(BAD_REQUEST).entity(exception.getMessage()).build();
            } catch (final Exception exception) {
                return status(UNAUTHORIZED).build();
            }

            geolocation = update(geolocation, data);
            dao.updateGeolocation(geolocation);
            if (APPLICATION_XML_TYPE.equals(responseType)) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE.equals(responseType)) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private Geolocation update(final Geolocation geolocation, final MultivaluedMap<String, String> data) {

        Geolocation updatedGeolocation = geolocation;

        // *** Set of parameters with provided data for Geolocation update ***//

        if (data.containsKey("Source")) {
            updatedGeolocation = updatedGeolocation.setSource(data.getFirst("Source"));
        }

        if (data.containsKey("DeviceIdentifier")) {
            updatedGeolocation = updatedGeolocation.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        }

        if (data.containsKey("LocationTimestamp")) {
            updatedGeolocation = updatedGeolocation.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        }

        if (data.containsKey("MSISDN")) {
            String msisdn = data.getFirst("MSISDN");
            try {
                if (msisdn.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MSISDN amount of digits must not be greater than 15");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MSISDN must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setMsisdn(getLong("MSISDN", data));
        }

        if (data.containsKey("IMSI")) {
            String imsi = data.getFirst("IMSI");
            try {
                if (imsi.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: IMSI amount of digits must not be greater than 15");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: IMSI must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setImsi(getLong("IMSI", data));
        }

        if (data.containsKey("IMEI")) {
            String imei = data.getFirst("IMEI");
            try {
                if (imei.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: IMEI amount of digits must not be greater than 15");
                }
            } catch (Exception e) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: IMEI must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setImei(data.getFirst("IMEI"));
        }

        if (data.containsKey("LMSI")) {
            Long lmsi = Long.valueOf(data.getFirst("LMSI"));
            try {
                if (lmsi > 4294967295L) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: IMEI value must not be greater than 4294967295");
                }
            } catch (Exception e) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: IMEI must be a number with a value not greater than 4294967295");
            }
            updatedGeolocation = updatedGeolocation.setLmsi(getLong("LMSI", data));
        }

        if (data.containsKey("ReferenceNumber")) {
            updatedGeolocation = updatedGeolocation.setReferenceNumber(getLong("ReferenceNumber", data));
        }

        if (data.containsKey("ResponseStatus")) {
            updatedGeolocation = updatedGeolocation.setResponseStatus(data.getFirst("ResponseStatus"));
            updatedGeolocation = updatedGeolocation.setDateUpdated(DateTime.now());
            if (data.containsKey("Cause") && (updatedGeolocation.getResponseStatus().equals(responseStatus.Rejected.toString())
                    || updatedGeolocation.getResponseStatus().equals(responseStatus.Unauthorized.toString())
                    || updatedGeolocation.getResponseStatus().equals(responseStatus.Failed.toString()))) {
                updatedGeolocation = updatedGeolocation.setCause(data.getFirst("Cause"));
                // cause is set to null if responseStatus is not rejected, failed or unauthorized
            }
            if (!updatedGeolocation.getResponseStatus().equals(responseStatus.Rejected.toString())
                    || !updatedGeolocation.getResponseStatus().equals(responseStatus.Unauthorized.toString())
                    || !updatedGeolocation.getResponseStatus().equals(responseStatus.Failed.toString())) {
                updatedGeolocation = updatedGeolocation.setCause(null);
                // cause is set to null if responseStatus is not rejected, failed or unauthorized
            }
        }

        if (updatedGeolocation.getResponseStatus() != null
                && (!updatedGeolocation.getResponseStatus().equals(responseStatus.Unauthorized.toString())
                        || !updatedGeolocation.getResponseStatus().equals(responseStatus.Failed.toString()))) {
            updatedGeolocation = updatedGeolocation.setCause(null);
            // "Cause" is set to null if "ResponseStatus" is not null and is neither "rejected", "unauthorized" nor "failed"
        }

        if (data.containsKey("MobileCountryCode")) {
            String mcc = data.getFirst("MobileCountryCode");
            Integer digits = Integer.valueOf(mcc);
            try {
                if (mcc.length() > 3) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MobileCountryCode amount of digits must not be greater than 3");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MobileCountryCode must be a number with an amount of digits not greater than 3");
            }
            updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }

        if (data.containsKey("MobileNetworkCode")) {
            String mnc = data.getFirst("MobileNetworkCode");
            if (mnc.length() > 3) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MobileNetworkCode amount of digits must not be greater than 3");
            } else {
                updatedGeolocation = updatedGeolocation.setMobileNetworkCode(getInteger("MobileNetworkCode", data));

            }
        }

        if (data.containsKey("LocationAreaCode")) {
            String lac = data.getFirst("LocationAreaCode");
            Integer digits = Integer.valueOf(lac);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: LocationAreaCode must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: LocationAreaCode must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setLocationAreaCode(getInteger("LocationAreaCode", data));
        }

        if (data.containsKey("CellId")) {
            String ci = data.getFirst("CellId");
            Long digits = Long.valueOf(ci);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: CellId must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: CellId must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setCellId(getInteger("CellId", data));
        }

        if (data.containsKey("ServiceAreaCode")) {
            String sai = data.getFirst("ServiceAreaCode");
            Long digits = Long.valueOf(sai);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: SAI must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: SAI must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setServiceAreaCode(getInteger("ServiceAreaCode", data));
        }

        if (data.containsKey("ENodeBId")) {
            String ecid = data.getFirst("ENodeBId");
            Long digits = Long.valueOf(ecid);
            try {
                if (digits > 1048575) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ENodeBId must be a number not greater than 1048575");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ENodeBId must be a number not greater than 1048575");
            }
            updatedGeolocation = updatedGeolocation.setEnodebId(getInteger("ENodeBId", data));
        }

        if (data.containsKey("TrackingAreaCode")) {
            String tac = data.getFirst("TrackingAreaCode");
            Integer digits = Integer.valueOf(tac);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: TrackingAreaCode must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: TrackingAreaCode must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setTrackingAreaCode(getInteger("TrackingAreaCode", data));
        }

        if (data.containsKey("RoutingAreaCode")) {
            String routeingAreaCode = data.getFirst("RoutingAreaCode");
            Integer digits = Integer.valueOf(routeingAreaCode);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: RoutingAreaCode must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: TrackingAreaCode must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setRoutingAreaCode(getInteger("RoutingAreaCode", data));
        }

        if (data.containsKey("LocationNumberAddress")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: LocationNumberAddress value can not be updated");
        }

        if (data.containsKey("NetworkEntityAddress")) {
            String gt = data.getFirst("NetworkEntityAddress");
            Long digits = Long.valueOf(gt);
            try {
                if (digits > Long.MAX_VALUE) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: NetworkEntityAddress must be a number compliant with Long data type");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: NetworkEntityAddress must be a number compliant with Long data type");
            }
            updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        }

        if (data.containsKey("NetworkEntityName")) {
            String entityName = data.getFirst("NetworkEntityName");
            if (entityName.length() > 254) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: NetworkEntityName length must not be greater than 254 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setNetworkEntityName(data.getFirst("NetworkEntityName"));
            }
        }

        if (data.containsKey("LocationAge")) {
            String aol = data.getFirst("LocationAge");
            Integer digits = Integer.valueOf(aol);
            try {
                if (digits > Long.MAX_VALUE) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: LocationAge must be a number compliant with Integer data type");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: LocationAge must be a number compliant with Integer data type");
            }
            updatedGeolocation = updatedGeolocation.setAgeOfLocationInfo(getInteger("LocationAge", data));
        }

        if (data.containsKey("SubscriberState")) {
            String state = data.getFirst("SubscriberState");
            if (!state.equalsIgnoreCase("assumedIdle") && !state.equalsIgnoreCase("camelBusy") &&
                !state.equalsIgnoreCase("netDetNotReachable") && !state.equalsIgnoreCase("notProvidedFromVLR") &&
                !state.equalsIgnoreCase("psDetached") && !state.equalsIgnoreCase("psAttachedReachableForPaging") &&
                !state.equalsIgnoreCase("psAttachedNotReachableForPaging") && !state.equalsIgnoreCase("notProvidedFromSGSNorMME") &&
                !state.equalsIgnoreCase("psPDPActiveNotReachableForPaging") && !state.equalsIgnoreCase("psPDPActiveReachableForPaging")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: SubscriberState \""+state+"\" is not API compliant. " +
                    "It must be one of assumedIdle, camelBusy, notProvidedFromVLR (CS domain), " +
                    "psDetached, psAttachedReachableForPaging, psAttachedNotReachableForPaging, notProvidedFromSGSNorMME, " +
                    "psPDPActiveNotReachableForPaging, psPDPActiveReachableForPaging (PS domain), or netDetNotReachable");
            } else {
                updatedGeolocation = updatedGeolocation.setSubscriberState(data.getFirst("SubscriberState"));
            }
        }

        if (data.containsKey("NotReachableReason")) {
            String notReachableReason = data.getFirst("NotReachableReason");
            if (!notReachableReason.equalsIgnoreCase("msPurged") && !notReachableReason.equalsIgnoreCase("imsiDetached") &&
                !notReachableReason.equalsIgnoreCase("restrictedArea") && !notReachableReason.equalsIgnoreCase("notRegistered")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: NotReachableReason \""+notReachableReason+"\" is not API compliant. " +
                    "It must be one of msPurged, imsiDetached, restrictedArea or notRegistered");
            } else {
                updatedGeolocation = updatedGeolocation.setSubscriberState(data.getFirst("SubscriberState"));
            }
        }

        if (data.containsKey("TypeOfShape")) {
            String typeOfShape = data.getFirst("TypeOfShape");
            if (typeOfShape.equalsIgnoreCase("ellipsoidPoint") || typeOfShape.equalsIgnoreCase("ellipsoidPointWithUncertaintyCircle")
                || typeOfShape.equalsIgnoreCase("ellipsoidPointWithUncertaintyEllipse") || typeOfShape.equalsIgnoreCase("polygon")
                || typeOfShape.equalsIgnoreCase("ellipsoidPointWithAltitude")
                || typeOfShape.equalsIgnoreCase("getEllipsoidPointWithAltitudeAndUncertaintyEllipsoid")
                || typeOfShape.equalsIgnoreCase("ellipsoidArc"))
                updatedGeolocation = updatedGeolocation.setTypeOfShape(data.getFirst("TypeOfShape"));
            else
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(),
                    responseStatus.Failed.toString(), "TypeOfShape parameter not API compliant");
        }

        if (data.containsKey("DeviceLatitude")) {
            String deviceLat = data.getFirst("DeviceLatitude");
            Boolean deviceLatWGS84 = validateGeoCoordinatesFormat(deviceLat);
            if (!deviceLatWGS84) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(),
                        responseStatus.Failed.toString(), "DeviceLatitude format not API compliant");
            } else {
                updatedGeolocation = updatedGeolocation.setDeviceLatitude(deviceLat);
            }
        }

        if (data.containsKey("DeviceLongitude")) {
            updatedGeolocation = updatedGeolocation.setDeviceLongitude(data.getFirst("DeviceLongitude"));
            String deviceLong = data.getFirst("DeviceLongitude");
            Boolean deviceLongGS84 = validateGeoCoordinatesFormat(deviceLong);
            if (!deviceLongGS84) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(),
                        responseStatus.Failed.toString(), "DeviceLongitude format not API compliant");
            } else {
                updatedGeolocation = updatedGeolocation.setDeviceLongitude(deviceLong);
            }
        }

        if (data.containsKey("Uncertainty")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: Uncertainty value can not be updated");
        }

        if (data.containsKey("UncertaintySemiMajorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintySemiMajorAxis value can not be updated");
        }

        if (data.containsKey("UncertaintySemiMinorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintySemiMinorAxis value can not be updated");
        }

        if (data.containsKey("AngleOfMajorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AngleOfMajorAxis value can not be updated");
        }

        if (data.containsKey("Confidence")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: Confidence value can not be updated");
        }

        if (data.containsKey("DeviceAltitude")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: DeviceAltitude value can not be updated");
        }

        if (data.containsKey("UncertaintyAltitude")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintyAltitude value can not be updated");
        }

        if (data.containsKey("InnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: InnerRadius value can not be updated");
        }

        if (data.containsKey("UncertaintyInnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintyInnerRadius value can not be updated");
        }

        if (data.containsKey("UncertaintyInnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintyInnerRadius value can not be updated");
        }

        if (data.containsKey("OffsetAngle")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: OffsetAngle value can not be updated");
        }

        if (data.containsKey("IncludedAngle")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: IncludedAngle value can not be updated");
        }

        if (data.containsKey("HorizontalSpeed")) {
            String horizontalSpeed = data.getFirst("HorizontalSpeed");
            if (horizontalSpeed.length() > 30) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: HorizontalSpeed length must not be greater than 30 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setHorizontalSpeed(getInteger("HorizontalSpeed", data));
            }
        }

        if (data.containsKey("VerticalSpeed")) {
            String verticalSpeed = data.getFirst("VerticalSpeed");
            if (verticalSpeed.length() > 30) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalSpeed length must not be greater than 30 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setVerticalSpeed(getInteger("VerticalSpeed", data));
            }
        }

        if (data.containsKey("UncertaintyHorizontalSpeed")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintyHorizontalSpeed value can not be updated");
        }

        if (data.containsKey("UncertaintyVerticalSpeed")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: UncertaintyVerticalSpeed value can not be updated");
        }

        if (data.containsKey("Bearing")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: Bearing value can not be updated");
        }

        if (data.containsKey("DeferredLocationEventType")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: DeferredLocationEventType value can not be updated");
        }

        if (data.containsKey("GeofenceType")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: GeofenceType value can not be updated");
        }

        if (data.containsKey("GeofenceId")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: GeofenceId value can not be updated");
        }

        if (data.containsKey("GeofenceOccurrenceInfo")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: GeofenceType value can not be updated");
        }

        if (data.containsKey("GeofenceIntervalTime")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: GeofenceIntervalTime value can not be updated");
        }

        if (data.containsKey("MotionEventRange")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: EventRange value can not be updated");
        }

        if (data.containsKey("EventReportingAmount")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: EventReportingAmount value can not be updated");
        }

        if (data.containsKey("EventReportingInterval")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: EventReportingInterval value can not be updated");
        }

        if (data.containsKey("CivicAddress")) {
            String civicAddress = data.getFirst("CivicAddress");
            if (civicAddress.length() > 200) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: CivicAddress length must not be greater than 200 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setCivicAddress(data.getFirst("CivicAddress"));
            }
        }

        if (data.containsKey("BarometricPressure")) {
            Long barometricPressure = Long.valueOf(data.getFirst("BarometricPressure"));
            if (barometricPressure > Long.MAX_VALUE) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: BarometricPressure must not be a number not greater than maximum value for Long data type");
            } else {
                updatedGeolocation = updatedGeolocation.setBarometricPressure(getLong("BarometricPressure", data));
            }
        }

        if (data.containsKey("PhysicalAddress")) {
            String physicalAddress = data.getFirst("PhysicalAddress");
            if (physicalAddress.length() > 50) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PhysicalAddress length must not be greater than 50 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setPhysicalAddress(data.getFirst("PhysicalAddress"));
            }
        }

        if (data.containsKey("InternetAddress")) {
            String internetAddress = data.getFirst("InternetAddress");
            if (internetAddress.length() > 50) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: InternetAddress length must not be greater than 50 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setInternetAddress(data.getFirst("InternetAddress"));
            }
        }

        if (data.containsKey("LastGeolocationResponse")) {
            String lastGeolocationResponse = data.getFirst("LastGeolocationResponse");
            if (lastGeolocationResponse.equalsIgnoreCase("true") || lastGeolocationResponse.equalsIgnoreCase("false")) {
                updatedGeolocation = updatedGeolocation.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: LastGeolocationResponse value must be true or false");
            }
        }

        DateTime thisDateTime = DateTime.now();
        updatedGeolocation = updatedGeolocation.setDateUpdated(thisDateTime);
        return updatedGeolocation;
    }

    private Geolocation buildFailedGeolocationUpdate(Geolocation geolocation, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, String responseStatus, String wrongUpdateCause) {
        final Sid accountSid = geolocation.getAccountSid();
        final Sid sid = geolocation.getSid();
        final Geolocation.Builder builder = Geolocation.builder();
        String geoloctype = glType.toString();
        DateTime currentDateTime = DateTime.now();
        builder.setSid(sid);
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setResponseStatus(responseStatus);
        builder.setCause(wrongUpdateCause);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setGeolocationType(glType);
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private boolean validateGeoCoordinatesFormat(String coordinates) {

        String degrees = "\\u00b0";
        String minutes = "'";
        Boolean WGS84_validation;
        Boolean pattern1 = coordinates.matches("[NWSE]{1}\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern2 = coordinates.matches("\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}[NWSE]{1}$");
        Boolean pattern3 = coordinates.matches("\\d{1,3}[" + degrees + "]\\d{1,3}[" + minutes + "]\\d{1,2}\\.\\d{1,2}["
                + minutes + "][" + minutes + "][NWSE]{1}$");
        Boolean pattern4 = coordinates.matches("[NWSE]{1}\\d{1,3}[" + degrees + "]\\d{1,3}[" + minutes + "]\\d{1,2}\\.\\d{1,2}["
                + minutes + "][" + minutes + "]$");
        Boolean pattern5 = coordinates.matches("\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern6 = coordinates.matches("-?\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern7 = coordinates.matches("-?\\d+(\\.\\d+)?");

        if (pattern1 || pattern2 || pattern3 || pattern4 || pattern5 || pattern6 || pattern7) {
            WGS84_validation = true;
            return WGS84_validation;
        } else {
            WGS84_validation = false;
            return WGS84_validation;
        }
    }

    private HashMap<String, String> parseAtiOrPsiJsonString(String jsonLine) {
        HashMap<String, String> atiOrPsiResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        String network = jobject.get("network").getAsString();
        atiOrPsiResponse.put("network", network);
        String protocol = jobject.get("protocol").getAsString();
        atiOrPsiResponse.put("protocol", protocol);
        String operation = jobject.get("operation").getAsString();
        atiOrPsiResponse.put("operation", operation);

        JsonObject csLocationInformation = jelement.getAsJsonObject();
        csLocationInformation = csLocationInformation.getAsJsonObject("CSLocationInformation");
        if (csLocationInformation != null) {
            JsonObject locationNumber = csLocationInformation.getAsJsonObject("LocationNumber");
            if (locationNumber != null) {
                if (locationNumber.get("oddFlag") != null) {
                    String oF = locationNumber.get("oddFlag").getAsString();
                    if (!oF.isEmpty())
                        atiOrPsiResponse.put("oddFlag", oF);
                }
                if (locationNumber.get("natureOfAddressIndicator") != null) {
                    String nai = locationNumber.get("natureOfAddressIndicator").getAsString();
                    if (!nai.isEmpty())
                        atiOrPsiResponse.put("nai", nai);
                }
                if (locationNumber.get("internalNetworkNumberIndicator") != null) {
                    String inni = locationNumber.get("internalNetworkNumberIndicator").getAsString();
                    if (!inni.isEmpty())
                        atiOrPsiResponse.put("inni", inni);
                }
                if (locationNumber.get("numberingPlanIndicator") != null) {
                    String npi = locationNumber.get("numberingPlanIndicator").getAsString();
                    if (!npi.isEmpty())
                        atiOrPsiResponse.put("npi", npi);
                }
                if (locationNumber.get("addressRepresentationRestrictedIndicator") != null) {
                    String arpi = locationNumber.get("addressRepresentationRestrictedIndicator").getAsString();
                    if (!arpi.isEmpty())
                        atiOrPsiResponse.put("arpi", arpi);
                }
                if (locationNumber.get("screeningIndicator") != null) {
                    String si = locationNumber.get("screeningIndicator").getAsString();
                    if (!si.isEmpty())
                        atiOrPsiResponse.put("si", si);
                }
                if (locationNumber.get("address") != null) {
                    String address = locationNumber.get("address").getAsString();
                    if (!address.isEmpty())
                        atiOrPsiResponse.put("address", address);
                }
            }

            JsonObject csCgiSaiLai = csLocationInformation.getAsJsonObject("CGIorSAIorLAI");
            if (csCgiSaiLai != null) {
                if (csCgiSaiLai.get("mcc") != null) {
                    String csMcc = csCgiSaiLai.get("mcc").getAsString();
                    if (!csMcc.isEmpty())
                        atiOrPsiResponse.put("csMcc", csMcc);
                }
                if (csCgiSaiLai.get("mnc") != null) {
                    String csMnc = csCgiSaiLai.get("mnc").getAsString();
                    if (!csMnc.isEmpty())
                        atiOrPsiResponse.put("csMnc", csMnc);
                }
                if (csCgiSaiLai.get("lac") != null) {
                    String csLac = csCgiSaiLai.get("lac").getAsString();
                    if (!csLac.isEmpty())
                        atiOrPsiResponse.put("csLac", csLac);
                }
                if (csCgiSaiLai.get("ci") != null) {
                    String csCi = csCgiSaiLai.get("ci").getAsString();
                    if (!csCi.isEmpty())
                        atiOrPsiResponse.put("csCi", csCi);
                }
                if (csCgiSaiLai.get("sac") != null) {
                    String csSac = csCgiSaiLai.get("sac").getAsString();
                    if (!csSac.isEmpty())
                        atiOrPsiResponse.put("csSac", csSac);
                }
            }

            JsonObject geographicalInformation = csLocationInformation.getAsJsonObject("GeographicalInformation");
            if (geographicalInformation != null) {
                if (geographicalInformation.get("typeOfShape") != null) {
                    String csGeogTypeOfShape = geographicalInformation.get("typeOfShape").getAsString();
                    if (!csGeogTypeOfShape.isEmpty())
                        atiOrPsiResponse.put("csGeogTypeOfShape", csGeogTypeOfShape);
                }
                if (geographicalInformation.get("latitude") != null) {
                    String csGeogLatitude = geographicalInformation.get("latitude").getAsString();
                    if (!csGeogLatitude.isEmpty())
                        atiOrPsiResponse.put("csGeogLatitude", csGeogLatitude);
                }
                if (geographicalInformation.get("longitude") != null) {
                    String csGeogLongitude = geographicalInformation.get("longitude").getAsString();
                    if (!csGeogLongitude.isEmpty())
                        atiOrPsiResponse.put("csGeogLongitude", csGeogLongitude);
                }
                if (geographicalInformation.get("uncertainty") != null) {
                    String csGeogUncertainty = geographicalInformation.get("uncertainty").getAsString();
                    if (!csGeogUncertainty.isEmpty())
                        atiOrPsiResponse.put("csGeogUncertainty", csGeogUncertainty);
                }
            }

            JsonObject geodeticInformation = csLocationInformation.getAsJsonObject("GeodeticInformation");
            if (geodeticInformation != null) {
                if (geodeticInformation.get("typeOfShape") != null) {
                    String csGeodTypeOfShape = geodeticInformation.get("typeOfShape").getAsString();
                    if (!csGeodTypeOfShape.isEmpty())
                        atiOrPsiResponse.put("csGeodTypeOfShape", csGeodTypeOfShape);
                }
                if (geodeticInformation.get("latitude") != null) {
                    String csGeodLatitude = geodeticInformation.get("latitude").getAsString();
                    if (!csGeodLatitude.isEmpty())
                        atiOrPsiResponse.put("csGeodLatitude", csGeodLatitude);
                }
                if (geodeticInformation.get("longitude") != null) {
                    String csGeodLongitude = geodeticInformation.get("longitude").getAsString();
                    if (!csGeodLongitude.isEmpty())
                        atiOrPsiResponse.put("csGeodLongitude", csGeodLongitude);
                }
                if (geodeticInformation.get("uncertainty") != null) {
                    String csGeodUncertainty = geodeticInformation.get("uncertainty").getAsString();
                    if (!csGeodUncertainty.isEmpty())
                        atiOrPsiResponse.put("csGeodUncertainty", csGeodUncertainty);
                }
                if (geodeticInformation.get("confidence") != null) {
                    String csGeodConfidence = geodeticInformation.get("confidence").getAsString();
                    if (!csGeodConfidence.isEmpty())
                        atiOrPsiResponse.put("csGeodConfidence", csGeodConfidence);
                }
                if (geodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String csGeodScreeningAndPresentationIndicators = geodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    if (!csGeodScreeningAndPresentationIndicators.isEmpty())
                        atiOrPsiResponse.put("csGeodScreeningAndPresentationIndicators", csGeodScreeningAndPresentationIndicators);
                }
            }

            JsonObject epsLocationInformation = csLocationInformation.getAsJsonObject("EPSLocationInformation");
            if (epsLocationInformation != null) {
                JsonObject tai = epsLocationInformation.getAsJsonObject("TAI");
                if (tai != null) {
                    if (tai.get("mcc") != null) {
                        String taiMcc = tai.get("mcc").getAsString();
                        if (!taiMcc.isEmpty())
                            atiOrPsiResponse.put("taiMcc", taiMcc);
                    }
                    if (tai.get("mnc") != null) {
                        String taiMnc = tai.get("mnc").getAsString();
                        if (!taiMnc.isEmpty())
                            atiOrPsiResponse.put("taiMnc", taiMnc);
                    }
                    if (tai.get("tac") != null) {
                        String tac = tai.get("tac").getAsString();
                        if (!tac.isEmpty())
                            atiOrPsiResponse.put("tac", tac);
                    }
                }

                JsonObject ecgi = epsLocationInformation.getAsJsonObject("ECGI");
                if (ecgi != null) {
                    if (ecgi.get("mcc") != null) {
                        String ecgiMcc = ecgi.get("mcc").getAsString();
                        if (!ecgiMcc.isEmpty())
                            atiOrPsiResponse.put("ecgiMcc", ecgiMcc);
                    }
                    if (ecgi.get("mnc") != null) {
                        String ecgiMnc = ecgi.get("mnc").getAsString();
                        if (!ecgiMnc.isEmpty())
                            atiOrPsiResponse.put("ecgiMnc", ecgiMnc);
                    }
                    if (ecgi.get("eNBId") != null) {
                        String eNBId = ecgi.get("eNBId").getAsString();
                        if (!eNBId.isEmpty())
                            atiOrPsiResponse.put("ecgiENBId", eNBId);
                    }
                    if (ecgi.get("ci") != null) {
                        String ecgiCi = ecgi.get("ci").getAsString();
                        if (!ecgiCi.isEmpty())
                            atiOrPsiResponse.put("ecgiCi", ecgiCi);
                    }
                }

                geographicalInformation = epsLocationInformation.getAsJsonObject("GeographicalInformation");
                if (geographicalInformation != null) {
                    if (geographicalInformation.get("typeOfShape") != null) {
                        String epsGeogTypeOfShape = geographicalInformation.get("typeOfShape").getAsString();
                        if (!epsGeogTypeOfShape.isEmpty())
                            atiOrPsiResponse.put("epsGeogTypeOfShape", epsGeogTypeOfShape);
                    }
                    if (geographicalInformation.get("latitude") != null) {
                        String epsGeogLatitude = geographicalInformation.get("latitude").getAsString();
                        if (!epsGeogLatitude.isEmpty())
                            atiOrPsiResponse.put("epsGeogLatitude", epsGeogLatitude);
                    }
                    if (geographicalInformation.get("longitude") != null) {
                        String epsGeogLongitude = geographicalInformation.get("longitude").getAsString();
                        if (!epsGeogLongitude.isEmpty())
                            atiOrPsiResponse.put("epsGeogLongitude", epsGeogLongitude);
                    }

                    if (geographicalInformation.get("uncertainty") != null) {
                        String epsGeogUncertainty = geographicalInformation.get("uncertainty").getAsString();
                        if (!epsGeogUncertainty.isEmpty())
                            atiOrPsiResponse.put("epsGeogUncertainty", epsGeogUncertainty);
                    }
                }

                geodeticInformation = epsLocationInformation.getAsJsonObject("GeodeticInformation");
                if (geodeticInformation != null) {
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodTypeOfShape = geodeticInformation.get("typeOfShape").getAsString();
                        if (!epsGeodTypeOfShape.isEmpty())
                            atiOrPsiResponse.put("epsGeodTypeOfShape", epsGeodTypeOfShape);
                    }
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodLatitude = geodeticInformation.get("latitude").getAsString();
                        if (!epsGeodLatitude.isEmpty())
                            atiOrPsiResponse.put("epsGeodLatitude", epsGeodLatitude);
                    }
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodLongitude = geodeticInformation.get("longitude").getAsString();
                        if (!epsGeodLongitude.isEmpty())
                            atiOrPsiResponse.put("epsGeodLongitude", epsGeodLongitude);
                    }
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodUncertainty = geodeticInformation.get("uncertainty").getAsString();
                        if (!epsGeodUncertainty.isEmpty())
                            atiOrPsiResponse.put("epsGeodUncertainty", epsGeodUncertainty);
                    }
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodConfidence = geodeticInformation.get("confidence").getAsString();
                        if (!epsGeodConfidence.isEmpty())
                            atiOrPsiResponse.put("epsGeodConfidence", epsGeodConfidence);
                    }
                    if (geodeticInformation.get("typeOfShape") != null) {
                        String epsGeodScreeningAndPresentationIndicators = geodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                        if (!epsGeodScreeningAndPresentationIndicators.isEmpty())
                            atiOrPsiResponse.put("epsGeodScreeningAndPresentationIndicators", epsGeodScreeningAndPresentationIndicators);
                    }
                }
            }
        }


        JsonObject psLocationInformation = jelement.getAsJsonObject();
        psLocationInformation = psLocationInformation.getAsJsonObject("PSLocationInformation");
        if (psLocationInformation != null) {
            JsonObject lsa = psLocationInformation.getAsJsonObject("LSA");
            if (lsa != null) {
                if (lsa.get("lsaIdType") != null) {
                    String lsaIdType = lsa.get("lsaIdType").getAsString();
                    if (!lsaIdType.isEmpty())
                        atiOrPsiResponse.put("lsaIdType", lsaIdType);
                }
                if (lsa.get("lsaId") != null) {
                    String lsaId = lsa.get("lsaId").getAsString();
                    if (!lsaId.isEmpty())
                        atiOrPsiResponse.put("lsaId", lsaId);
                }
            }
            JsonObject rai = psLocationInformation.getAsJsonObject("RAI");
            if (rai != null) {
                if (rai.get("mcc") != null) {
                    String raiMcc = rai.get("mcc").getAsString();
                    if (!raiMcc.isEmpty())
                        atiOrPsiResponse.put("raiMcc", raiMcc);
                }
                if (rai.get("mnc") != null) {
                    String raiMnc = rai.get("mnc").getAsString();
                    if (!raiMnc.isEmpty())
                        atiOrPsiResponse.put("raiMnc", raiMnc);
                }
                if (rai.get("lac") != null) {
                    String raiLac = rai.get("lac").getAsString();
                    if (!raiLac.isEmpty())
                        atiOrPsiResponse.put("raiLac", raiLac);
                }
                if (rai.get("rac") != null) {
                    String rac = rai.get("rac").getAsString();
                    if (!rac.isEmpty())
                        atiOrPsiResponse.put("rac", rac);
                }
            }
            JsonObject psCgiSaiLai = psLocationInformation.getAsJsonObject("CGIorSAIorLAI");
            if (psCgiSaiLai != null) {
                if (psCgiSaiLai.get("mcc") != null) {
                    String psMcc = psCgiSaiLai.get("mcc").getAsString();
                    if (!psMcc.isEmpty())
                        atiOrPsiResponse.put("psMcc", psMcc);
                }
                if (psCgiSaiLai.get("mnc") != null) {
                    String psMnc = psCgiSaiLai.get("mnc").getAsString();
                    if (!psMnc.isEmpty())
                        atiOrPsiResponse.put("psMnc", psMnc);
                }
                if (psCgiSaiLai.get("lac") != null) {
                    String psLac = psCgiSaiLai.get("lac").getAsString();
                    if (!psLac.isEmpty())
                        atiOrPsiResponse.put("psLac", psLac);
                }
                if (psCgiSaiLai.get("ci") != null) {
                    String psCi = psCgiSaiLai.get("ci").getAsString();
                    if (!psCi.isEmpty())
                        atiOrPsiResponse.put("psCi", psCi);
                }
                if (psCgiSaiLai.get("sac") != null) {
                    String psSac = psCgiSaiLai.get("sac").getAsString();
                    if (!psSac.isEmpty())
                        atiOrPsiResponse.put("psSac", psSac);
                }

            }
            JsonObject psGeographicalInformation = psLocationInformation.getAsJsonObject("GeographicalInformation");
            if (psGeographicalInformation != null) {
                if (psGeographicalInformation.get("typeOfShape") != null) {
                    String psGeogTypeOfShape = psGeographicalInformation.get("typeOfShape").getAsString();
                    if (!psGeogTypeOfShape.isEmpty())
                        atiOrPsiResponse.put("psGeogTypeOfShape", psGeogTypeOfShape);
                }
                if (psGeographicalInformation.get("latitude") != null) {
                    String psGeogLatitude = psGeographicalInformation.get("latitude").getAsString();
                    if (!psGeogLatitude.isEmpty())
                        atiOrPsiResponse.put("psGeogLatitude", psGeogLatitude);
                }
                if (psGeographicalInformation.get("longitude") != null) {
                    String psGeogLongitude = psGeographicalInformation.get("longitude").getAsString();
                    if (!psGeogLongitude.isEmpty())
                        atiOrPsiResponse.put("psGeogLongitude", psGeogLongitude);
                }
                if (psGeographicalInformation.get("uncertainty") != null) {
                    String psGeogUncertainty = psGeographicalInformation.get("uncertainty").getAsString();
                    if (!psGeogUncertainty.isEmpty())
                        atiOrPsiResponse.put("psGeogUncertainty", psGeogUncertainty);
                }
            }
            JsonObject psGeodeticInformation = psLocationInformation.getAsJsonObject("GeodeticInformation");
            if (psGeographicalInformation != null) {
                if (psGeodeticInformation.get("typeOfShape") != null) {
                    String psGeodTypeOfShape = psGeodeticInformation.get("typeOfShape").getAsString();
                    if (!psGeodTypeOfShape.isEmpty())
                        atiOrPsiResponse.put("psGeodTypeOfShape", psGeodTypeOfShape);
                }

                if (psGeodeticInformation.get("latitude") != null) {
                    String psGeodLatitude = psGeodeticInformation.get("latitude").getAsString();
                    if (!psGeodLatitude.isEmpty())
                        atiOrPsiResponse.put("psGeodLatitude", psGeodLatitude);
                }
                if (psGeodeticInformation.get("longitude") != null) {
                    String psGeodLongitude = psGeodeticInformation.get("longitude").getAsString();
                    if (!psGeodLongitude.isEmpty())
                        atiOrPsiResponse.put("psGeodLongitude", psGeodLongitude);
                }
                if (psGeodeticInformation.get("uncertainty") != null) {
                    String psGeodUncertainty = psGeodeticInformation.get("uncertainty").getAsString();
                    if (!psGeodUncertainty.isEmpty())
                        atiOrPsiResponse.put("psGeodUncertainty", psGeodUncertainty);
                }

                if (psGeodeticInformation.get("confidence") != null) {
                    String psGeodConfidence = psGeodeticInformation.get("confidence").getAsString();
                    if (!psGeodConfidence.isEmpty())
                        atiOrPsiResponse.put("psGeodConfidence", psGeodConfidence);
                }
                if (psGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String psGeodScreeningAndPresentationIndicators = psGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    if (!psGeodScreeningAndPresentationIndicators.isEmpty())
                        atiOrPsiResponse.put("psGeodScreeningAndPresentationIndicators", psGeodScreeningAndPresentationIndicators);
                }
            }
        }

        JsonObject grpsMSClass = jelement.getAsJsonObject();
        grpsMSClass = grpsMSClass.getAsJsonObject("GPRSMSClass");
        if (grpsMSClass != null) {
            if (grpsMSClass.get("msNetworkCapability") != null) {
                String msNetworkCapability = grpsMSClass.get("msNetworkCapability").getAsString();
                if (!msNetworkCapability.isEmpty())
                    atiOrPsiResponse.put("msNetworkCapability", msNetworkCapability);
            }
            if (grpsMSClass.get("msNetworkCapability") != null) {
                String msRadioAccessCapability = grpsMSClass.get("msRadioAccessCapability").getAsString();
                if (!msRadioAccessCapability.isEmpty())
                    atiOrPsiResponse.put("msRadioAccessCapability", msRadioAccessCapability);
            }
        }

        JsonObject mnpInfoResult = jelement.getAsJsonObject();
        mnpInfoResult = mnpInfoResult.getAsJsonObject("MNPInfoResult");
        if (mnpInfoResult != null) {
            if (mnpInfoResult != null) {
                if (mnpInfoResult.get("mnpStatus") != null) {
                    String mnpStatus = mnpInfoResult.get("mnpStatus").getAsString();
                    if (!mnpStatus.isEmpty())
                        atiOrPsiResponse.put("mnpStatus", mnpStatus);
                }
                if (mnpInfoResult.get("mnpMsisdn") != null) {
                    String mnpMsisdn = mnpInfoResult.get("mnpMsisdn").getAsString();
                    if (!mnpMsisdn.isEmpty())
                        atiOrPsiResponse.put("mnpMsisdn", mnpMsisdn);
                }
                if (mnpInfoResult.get("mnpImsi") != null) {
                    String mnpImsi = mnpInfoResult.get("mnpImsi").getAsString();
                    if (!mnpImsi.isEmpty())
                        atiOrPsiResponse.put("mnpImsi", mnpImsi);
                }
                if (mnpInfoResult.get("mnpRouteingNumber") != null) {
                    String mnpRouteingNumber = mnpInfoResult.get("mnpRouteingNumber").getAsString();
                    if (!mnpRouteingNumber.isEmpty())
                        atiOrPsiResponse.put("mnpRouteingNumber", mnpRouteingNumber);
                }
            }
        }

        if (jobject.get("ageOfLocationInformation") != null) {
            String aol = jobject.get("ageOfLocationInformation").getAsString();
            if (!aol.isEmpty())
                atiOrPsiResponse.put("aol", aol);
        }
        if (jobject.get("currentLocationRetrieved") != null) {
            String current = jobject.get("currentLocationRetrieved").getAsString();
            if (!current.isEmpty())
                atiOrPsiResponse.put("currentLocationRetrieved", current);
        }
        if (jobject.get("msisdn") != null) {
            String msisdn = jobject.get("msisdn").getAsString();
            if (!msisdn.isEmpty())
                atiOrPsiResponse.put("msisdn", msisdn);
        }
        if (jobject.get("imsi") != null) {
            String imsi = jobject.get("imsi").getAsString();
            if (!imsi.isEmpty())
                atiOrPsiResponse.put("imsi", imsi);
        }
        if (jobject.get("imei") != null) {
            String imei = jobject.get("imei").getAsString();
            if (!imei.isEmpty())
                atiOrPsiResponse.put("imei", imei);
        }
        if (jobject.get("lmsi") != null) {
            String lmsi = jobject.get("lmsi").getAsString();
            if (!lmsi.isEmpty())
                atiOrPsiResponse.put("lmsi", lmsi);
        }
        if (jobject.get("vlrNumber") != null) {
            String vlrNumber = jobject.get("vlrNumber").getAsString();
            if (!vlrNumber.isEmpty())
                atiOrPsiResponse.put("vlrNumber", vlrNumber);
        }
        if (jobject.get("mscNumber") != null) {
            String mscNumber = jobject.get("mscNumber").getAsString();
            if (!mscNumber.isEmpty())
                atiOrPsiResponse.put("mscNumber", mscNumber);
        }
        if (jobject.get("sgsnNumber") != null) {
            String sgsnNumber = jobject.get("sgsnNumber").getAsString();
            if (!sgsnNumber.isEmpty())
                atiOrPsiResponse.put("sgsnNumber", sgsnNumber);
        }
        if (jobject.get("mmeName") != null) {
            String mmeName = jobject.get("mmeName").getAsString();
            if (!mmeName.isEmpty())
                atiOrPsiResponse.put("mmeName", mmeName);
        }
        if (jobject.get("subscriberState") != null) {
            String subscriberState = jobject.get("subscriberState").getAsString();
            if (!subscriberState.isEmpty())
                atiOrPsiResponse.put("subscriberState", subscriberState);
        }
        if (jobject.get("notReachableReason") != null) {
            String notReachableReason = jobject.get("notReachableReason").getAsString();
            if (!notReachableReason.isEmpty())
                atiOrPsiResponse.put("notReachableReason", notReachableReason);
        }
        if (jobject.get("msClassmark") != null) {
            String msClassmark = jobject.get("msClassmark").getAsString();
            if (!msClassmark.isEmpty())
                atiOrPsiResponse.put("msClassmark", msClassmark);
        }
        if (jobject.get("errorReason") != null) {
            String errorReason = jobject.get("errorReason").getAsString();
            if (!errorReason.isEmpty())
                atiOrPsiResponse.put("errorReason", errorReason);
        }

        return atiOrPsiResponse;
    }

    private void putDataFromAtiOrPsiResponse(HashMap<String, String> atiOrPsiResponse, MultivaluedMap<String, String> data) {

        // CS Subscriber Location Info
        // CGI
        String csMcc = atiOrPsiResponse.get("csMcc");
        if (csMcc != null)
            data.putSingle("MobileCountryCode", csMcc);

        String csMnc = atiOrPsiResponse.get("csMnc");
        if (csMnc != null)
            data.putSingle("MobileNetworkCode", csMnc);

        String csLac = atiOrPsiResponse.get("csLac");
        if (csLac != null)
            data.putSingle("LocationAreaCode", csLac);

        String csCi = atiOrPsiResponse.get("csCi");
        if (csCi != null)
            data.putSingle("CellId", csCi);

        String csSac = atiOrPsiResponse.get("csSac");
        if (csSac != null)
            data.putSingle("ServiceAreaCode", csSac);

        // Location Number
        String locationNumber = atiOrPsiResponse.get("address");
        if (locationNumber != null)
            data.putSingle("LocationNumberAddress", locationNumber);

        // Subscriber Geographic Location Info
        String csGeogTypeOfShape = atiOrPsiResponse.get("csGeogTypeOfShape");
        if (csGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", csGeogTypeOfShape);

        String csGeogLatitude = atiOrPsiResponse.get("csGeogLatitude");
        if (csGeogLatitude != null)
            data.putSingle("DeviceLatitude", csGeogLatitude);

        String csGeogLongitude = atiOrPsiResponse.get("csGeogLongitude");
        if (csGeogLongitude != null)
            data.putSingle("DeviceLongitude", csGeogLongitude);

        String csGeogUncertainty = atiOrPsiResponse.get("csGeogUncertainty");
        if (csGeogUncertainty != null)
            data.putSingle("Uncertainty", csGeogUncertainty);

        // Subscriber Geodetic Location Info
        String csGeodTypeOfShape = atiOrPsiResponse.get("csGeodTypeOfShape");
        if (csGeodTypeOfShape != null)
            data.putSingle("TypeOfShape", csGeodTypeOfShape);

        String csGeodLatitude = atiOrPsiResponse.get("csGeodLatitude");
        if (csGeodLatitude != null)
            data.putSingle("DeviceLatitude", csGeodLatitude);

        String csGeodLongitude = atiOrPsiResponse.get("csGeodLongitude");
        if (csGeodLongitude != null)
            data.putSingle("DeviceLongitude", csGeodLongitude);

        String csGeodUncertainty = atiOrPsiResponse.get("csGeodUncertainty");
        if (csGeodUncertainty != null)
            data.putSingle("Uncertainty", csGeodUncertainty);

        String csGeodConfidence = atiOrPsiResponse.get("csGeodConfidence");
        if (csGeodConfidence != null)
            data.putSingle("Confidence", csGeodConfidence);

        String csGeodScreeningAndPresentationIndicators = atiOrPsiResponse.get("csGeodScreeningAndPresentationIndicators");
        if (csGeodScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", csGeodScreeningAndPresentationIndicators);

        // EPS Location Info
        String taiMcc = atiOrPsiResponse.get("taiMcc");
        if (taiMcc != null)
            data.putSingle("MobileCountryCode", taiMcc);

        String taiMnc = atiOrPsiResponse.get("taiMnc");
        if (taiMnc != null)
            data.putSingle("MobileNetworkCode", taiMnc);

        String tac = atiOrPsiResponse.get("tac");
        if (tac != null)
            data.putSingle("TrackingAreaCode", tac);

        String ecgiMcc = atiOrPsiResponse.get("ecgiMcc");
        if (ecgiMcc != null)
            data.putSingle("MobileCountryCode", ecgiMcc);

        String ecgiMnc = atiOrPsiResponse.get("ecgiMnc");
        if (ecgiMnc != null)
            data.putSingle("MobileNetworkCode", ecgiMnc);

        String eNBId = atiOrPsiResponse.get("ecgiENBId");
        if (eNBId != null)
            data.putSingle("ENodeBId", eNBId);

        String ecgiCi = atiOrPsiResponse.get("ecgiCi");
        if (ecgiCi != null)
            data.putSingle("CellId", ecgiCi);

        // PS Subscriber Info
        // LSA ID
        String lsaId = atiOrPsiResponse.get("lsaId");
        if (lsaId != null)
            data.putSingle("LSAId", lsaId);

        // RAI
        String raiMcc = atiOrPsiResponse.get("raiMcc");
        if (raiMcc != null)
            data.putSingle("MobileCountryCode", raiMcc);

        String raiMnc = atiOrPsiResponse.get("raiMnc");
        if (raiMnc != null)
            data.putSingle("MobileNetworkCode", raiMnc);

        String raiLac = atiOrPsiResponse.get("raiLac");
        if (raiLac != null)
            data.putSingle("LocationAreaCode", raiLac);

        String rac = atiOrPsiResponse.get("rac");
        if (rac != null)
            data.putSingle("RoutingAreaCode", rac);

        // CGI
        String psMcc = atiOrPsiResponse.get("psMcc");
        if (psMcc != null)
            data.putSingle("MobileCountryCode", psMcc);

        String psMnc = atiOrPsiResponse.get("psMnc");
        if (psMnc != null)
            data.putSingle("MobileNetworkCode", psMnc);

        String psLac = atiOrPsiResponse.get("psLac");
        if (psLac != null)
            data.putSingle("LocationAreaCode", psLac);

        String psCi = atiOrPsiResponse.get("psCi");
        if (psCi != null)
            data.putSingle("CellId", psCi);

        String psSac = atiOrPsiResponse.get("psSac");
        if (psSac != null)
            data.putSingle("ServiceAreaCode", psSac);

        // PS Geographic info
        String psGeogTypeOfShape = atiOrPsiResponse.get("psGeogTypeOfShape");
        if (psGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", psGeogTypeOfShape);

        String psGeogLatitude = atiOrPsiResponse.get("psGeogLatitude");
        if (psGeogLatitude != null)
            data.putSingle("DeviceLatitude", psGeogLatitude);

        String psGeogLongitude = atiOrPsiResponse.get("psGeogLongitude");
        if (psGeogLongitude != null)
            data.putSingle("DeviceLongitude", psGeogLongitude);

        String psGeogUncertainty = atiOrPsiResponse.get("psGeogUncertainty");
        if (psGeogUncertainty != null)
            data.putSingle("Uncertainty", psGeogUncertainty);

        // PS Geodetic info
        String psGeodTypeOfShape = atiOrPsiResponse.get("psGeodTypeOfShape");
        if (psGeodTypeOfShape != null)
            data.putSingle("TypeOfShape", psGeodTypeOfShape);

        String psGeodLatitude = atiOrPsiResponse.get("psGeodLatitude");
        if (psGeodLatitude != null)
            data.putSingle("DeviceLatitude", psGeodLatitude);

        String psGeodLongitude = atiOrPsiResponse.get("psGeodLongitude");
        if (psGeodLongitude != null)
            data.putSingle("DeviceLongitude", psGeodLongitude);

        String psGeodUncertainty = atiOrPsiResponse.get("psGeodUncertainty");
        if (psGeodUncertainty != null)
            data.putSingle("Uncertainty", psGeodUncertainty);

        String psGeodConfidence = atiOrPsiResponse.get("psGeodConfidence");
        if (psGeodConfidence != null)
            data.putSingle("Confidence", psGeodConfidence);

        String psGeodScreeningAndPresentationIndicators = atiOrPsiResponse.get("psGeodScreeningAndPresentationIndicators");
        if (psGeodScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", psGeodScreeningAndPresentationIndicators);

        // MNP Status Info
        String mnpStatus = atiOrPsiResponse.get("mnpStatus");
        if (mnpStatus != null)
            data.putSingle("MNPStatus", mnpStatus);

        String mnpMsisdn = atiOrPsiResponse.get("mnpMsisdn");
        if (mnpMsisdn != null)
            data.putSingle("MNPMSISDN", mnpMsisdn);

        String mnpImsi = atiOrPsiResponse.get("mnpImsi");
        if (mnpImsi != null)
            data.putSingle("MNPIMSI", mnpImsi);

        String mnpRouteingNumber = atiOrPsiResponse.get("mnpRouteingNumber");
        if (mnpRouteingNumber != null)
            data.putSingle("MNPRouteingNumber", mnpRouteingNumber);

        // Rest of Subscriber Location Info
        String aol = atiOrPsiResponse.get("aol");
        if (aol != null)
            data.putSingle("LocationAge", aol);

        String currentLocationRetrieved = atiOrPsiResponse.get("currentLocationRetrieved");
        if (currentLocationRetrieved != null)
            data.putSingle("CurrentLocationRetrieved", currentLocationRetrieved);

        String msisdn = atiOrPsiResponse.get("msisdn");
        if (msisdn != null)
            data.putSingle("MSISDN", msisdn);

        String imsi = atiOrPsiResponse.get("imsi");
        if (imsi != null)
            data.putSingle("IMSI", imsi);

        String imei = atiOrPsiResponse.get("imei");
        if (imei != null)
            data.putSingle("IMEI", imei);

        String lmsi = atiOrPsiResponse.get("lmsi");
        if (lmsi != null)
            data.putSingle("LMSI", lmsi);

        String vlrNumber = atiOrPsiResponse.get("vlrNumber");
        if (vlrNumber != null)
            data.putSingle("NetworkEntityAddress", vlrNumber);

        String mscNumber = atiOrPsiResponse.get("mscNumber");
        if (mscNumber != null)
            data.putSingle("NetworkEntityAddress", mscNumber);

        String sgsnNumber = atiOrPsiResponse.get("sgsnNumber");
        if (sgsnNumber != null)
            data.putSingle("NetworkEntityAddress", sgsnNumber);

        String mmeName = atiOrPsiResponse.get("mmeName");
        if (mmeName != null)
            data.putSingle("NetworkEntityName", mmeName);

        String subscriberState = atiOrPsiResponse.get("subscriberState");
        if (subscriberState != null)
            data.putSingle("SubscriberState", subscriberState);

        String notReachableReason = atiOrPsiResponse.get("notReachableReason");
        if (notReachableReason != null)
            data.putSingle("NotReachableReason", notReachableReason);

        String errorReason = atiOrPsiResponse.get("errorReason");
        if (errorReason != null) {
            cause = errorReason;
            rStatus = responseStatus.Failed.toString();
            data.putSingle("Cause", cause);
            data.putSingle("ResponseStatus", rStatus);
        }
    }

    private HashMap<String, String> parsePslJsonString(String jsonLine) {
        HashMap<String, String> sriPslResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();

        if (jobject.get("lcsReferenceNumber") != null) {
            String lcsReferenceNumber = jobject.get("lcsReferenceNumber").getAsString();
            sriPslResponse.put("lcsReferenceNumber", lcsReferenceNumber);
        }
        if (jobject.get("errorReason") != null) {
            String errorReason = jobject.get("errorReason").getAsString();
            sriPslResponse.put("errorReason", errorReason);
        }

        JsonObject sri = jobject.getAsJsonObject("SRIforLCS");
        if (sri != null) {
            if (sri.get("msisdn") != null) {
                String msisdn = sri.get("msisdn").getAsString();
                if (!msisdn.isEmpty())
                    sriPslResponse.put("msisdn", msisdn);
            }
            if (sri.get("imsi") != null) {
                String imsi = sri.get("imsi").getAsString();
                if (!imsi.isEmpty())
                    sriPslResponse.put("imsi", imsi);
            }
            if (sri.get("lmsi") != null) {
                String lmsi = sri.get("lmsi").getAsString();
                if (!lmsi.isEmpty())
                    sriPslResponse.put("lmsi", lmsi);
            }
            if (sri.get("networkNodeNumber") != null) {
                String networkNodeNumber = sri.get("networkNodeNumber").getAsString();
                if (!networkNodeNumber.isEmpty())
                    sriPslResponse.put("networkNodeNumber", networkNodeNumber);
            }
            if (sri.get("gprsNodeIndicator") != null) {
                String gprsNodeIndicator = sri.get("gprsNodeIndicator").getAsString();
                if (!gprsNodeIndicator.isEmpty())
                    sriPslResponse.put("gprsNodeIndicator", gprsNodeIndicator);
            }
            if (sri.get("mmeName") != null) {
                String mmeName = sri.get("mmeName").getAsString();
                if (!mmeName.isEmpty())
                    sriPslResponse.put("mmeName", mmeName);
            }
            if (sri.get("sgsnName") != null) {
                String sgsnName = sri.get("sgsnName").getAsString();
                if (!sgsnName.isEmpty())
                    sriPslResponse.put("sgsnName", sgsnName);
            }
            if (sri.get("3GPPAAAServerName") != null) {
                String tgppAAAServerName = sri.get("3GPPAAAServerName").getAsString();
                if (!tgppAAAServerName.isEmpty())
                    sriPslResponse.put("tgppAAAServerName", tgppAAAServerName);
            }
            if (sri.get("hGmlcAddress") != null) {
                String hGmlcAddress = sri.get("hGmlcAddress").getAsString();
                if (!hGmlcAddress.isEmpty())
                    sriPslResponse.put("hGmlcAddress", hGmlcAddress);
            }
            if (sri.get("vGmlcAddress") != null) {
                String vGmlcAddress = sri.get("vGmlcAddress").getAsString();
                if (!vGmlcAddress.isEmpty())
                    sriPslResponse.put("vGmlcAddress", vGmlcAddress);
            }
            if (sri.get("pprAddress") != null) {
                String pprAddress = sri.get("pprAddress").getAsString();
                if (!pprAddress.isEmpty())
                    sriPslResponse.put("pprAddress", pprAddress);
            }
        }

        JsonObject psl = jelement.getAsJsonObject();
        psl = psl.getAsJsonObject("PSL");
        if (psl != null) {
            JsonObject locationEstimate = psl.getAsJsonObject("LocationEstimate");
            if (locationEstimate != null) {
                if (locationEstimate.get("typeOfShape") != null) {
                    String typeOfShape = locationEstimate.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        sriPslResponse.put("typeOfShape", typeOfShape);
                }
                if (locationEstimate.get("latitude") != null) {
                    String latitude = locationEstimate.get("latitude").getAsString();
                    if (!latitude.isEmpty())
                        sriPslResponse.put("latitude", latitude);
                }
                if (locationEstimate.get("longitude") != null) {
                    String longitude = locationEstimate.get("longitude").getAsString();
                    if (!longitude.isEmpty())
                        sriPslResponse.put("longitude", longitude);
                }
                if (locationEstimate.get("uncertainty") != null) {
                    String uncertainty = locationEstimate.get("uncertainty").getAsString();
                    if (!uncertainty.isEmpty())
                        sriPslResponse.put("uncertainty", uncertainty);
                }
                if (locationEstimate.get("uncertaintySemiMajorAxis") != null) {
                    String uncertaintySemiMajorAxis = locationEstimate.get("uncertaintySemiMajorAxis").getAsString();
                    if (!uncertaintySemiMajorAxis.isEmpty())
                        sriPslResponse.put("uncertaintySemiMajorAxis", uncertaintySemiMajorAxis);
                }
                if (locationEstimate.get("uncertaintySemiMinorAxis") != null) {
                    String uncertaintySemiMinorAxis = locationEstimate.get("uncertaintySemiMinorAxis").getAsString();
                    if (!uncertaintySemiMinorAxis.isEmpty())
                        sriPslResponse.put("uncertaintySemiMinorAxis", uncertaintySemiMinorAxis);
                }
                if (locationEstimate.get("angleOfMajorAxis") != null) {
                    String angleOfMajorAxis = locationEstimate.get("angleOfMajorAxis").getAsString();
                    if (!angleOfMajorAxis.isEmpty())
                        sriPslResponse.put("angleOfMajorAxis", angleOfMajorAxis);
                }
                if (locationEstimate.get("confidence") != null) {
                    String confidence = locationEstimate.get("confidence").getAsString();
                    if (!confidence.isEmpty())
                        sriPslResponse.put("confidence", confidence);
                }
                if (locationEstimate.get("altitude") != null) {
                    String altitude = locationEstimate.get("altitude").getAsString();
                    if (!altitude.isEmpty())
                        sriPslResponse.put("altitude", altitude);
                }
                if (locationEstimate.get("uncertaintyAltitude") != null) {
                    String uncertaintyAltitude = locationEstimate.get("uncertaintyAltitude").getAsString();
                    if (!uncertaintyAltitude.isEmpty())
                        sriPslResponse.put("uncertaintyAltitude", uncertaintyAltitude);
                }
                if (locationEstimate.get("innerRadius") != null) {
                    String innerRadius = locationEstimate.get("innerRadius").getAsString();
                    if (!innerRadius.isEmpty())
                        sriPslResponse.put("innerRadius", innerRadius);
                }
                if (locationEstimate.get("uncertaintyInnerRadius") != null) {
                    String uncertaintyInnerRadius = locationEstimate.get("uncertaintyInnerRadius").getAsString();
                    sriPslResponse.put("uncertaintyInnerRadius", uncertaintyInnerRadius);
                }
                if (locationEstimate.get("offsetAngle") != null) {
                    String offsetAngle = locationEstimate.get("offsetAngle").getAsString();
                    if (!offsetAngle.isEmpty())
                        sriPslResponse.put("offsetAngle", offsetAngle);
                }
                if (locationEstimate.get("includedAngle") != null) {
                    String includedAngle = locationEstimate.get("includedAngle").getAsString();
                    if (!includedAngle.isEmpty())
                        sriPslResponse.put("includedAngle", includedAngle);
                }
            }
            if (psl.get("ageOfLocationEstimate") != null) {
                String ageOfLocationEstimate = psl.get("ageOfLocationEstimate").getAsString();
                if (!ageOfLocationEstimate.isEmpty())
                    sriPslResponse.put("ageOfLocationEstimate", ageOfLocationEstimate);
            }
            if (psl.get("deferredMTLRresponseIndicator") != null) {
                String deferredMTLRresponseIndicator = psl.get("deferredMTLRresponseIndicator").getAsString();
                if (!deferredMTLRresponseIndicator.isEmpty())
                    sriPslResponse.put("deferredMTLRresponseIndicator", deferredMTLRresponseIndicator);
            }
            if (psl.get("moLrShortCircuitIndicator") != null) {
                String moLrShortCircuitIndicator = psl.get("moLrShortCircuitIndicator").getAsString();
                if (!moLrShortCircuitIndicator.isEmpty())
                    sriPslResponse.put("moLrShortCircuitIndicator", moLrShortCircuitIndicator);
            }
            JsonObject cgiSaiLai = psl.getAsJsonObject("CGIorSAIorLAI");
            if (cgiSaiLai != null) {
                if (cgiSaiLai.get("mcc") != null) {
                    String mcc = cgiSaiLai.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        sriPslResponse.put("mcc", mcc);
                }
                if (cgiSaiLai.get("mnc") != null) {
                    String mnc = cgiSaiLai.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        sriPslResponse.put("mnc", mnc);
                }
                if (cgiSaiLai.get("lac") != null) {
                    String lac = cgiSaiLai.get("lac").getAsString();
                    if (!lac.isEmpty())
                        sriPslResponse.put("lac", lac);
                }
                if (cgiSaiLai.get("ci") != null) {
                    String ci = cgiSaiLai.get("ci").getAsString();
                    if (!ci.isEmpty())
                        sriPslResponse.put("ci", ci);
                }
                if (cgiSaiLai.get("sac") != null) {
                    String sac = cgiSaiLai.get("sac").getAsString();
                    if (!sac.isEmpty())
                        sriPslResponse.put("sac", sac);
                }
            }
            JsonObject geranPosInfo = psl.getAsJsonObject("GERANPositioningInfo");
            if (geranPosInfo != null) {
                if (geranPosInfo.get("geranPositioningInfo") != null) {
                    String geranPositioningInfo = geranPosInfo.get("geranPositioningInfo").getAsString();
                    if (!geranPositioningInfo.isEmpty())
                        sriPslResponse.put("geranPositioningInfo", geranPositioningInfo);
                }
                if (geranPosInfo.get("geranGanssPositioningData") != null) {
                    String geranGanssPositioningData = geranPosInfo.get("geranGanssPositioningData").getAsString();
                    if (!geranGanssPositioningData.isEmpty())
                        sriPslResponse.put("geranGanssPositioningData", geranGanssPositioningData);
                }
            }
            JsonObject utranPosInfo = psl.getAsJsonObject("UTRANPositioningInfo");
            if (utranPosInfo != null) {
                if (utranPosInfo.get("utranPositioningInfo") != null) {
                    String utranPositioningInfo = utranPosInfo.get("utranPositioningInfo").getAsString();
                    if (!utranPositioningInfo.isEmpty())
                        sriPslResponse.put("utranPositioningInfo", utranPositioningInfo);
                }
                if (utranPosInfo.get("utranGanssPositioningData") != null) {
                    String utranGanssPositioningData = utranPosInfo.get("utranGanssPositioningData").getAsString();
                    if (!utranGanssPositioningData.isEmpty())
                        sriPslResponse.put("utranGanssPositioningData", utranGanssPositioningData);
                }
            }
            JsonObject velocityEstimate = psl.getAsJsonObject("VelocityEstimate");
            if (velocityEstimate != null) {
                if (velocityEstimate.get("horizontalSpeed") != null) {
                    String horizontalSpeed = velocityEstimate.get("horizontalSpeed").getAsString();
                    if (!horizontalSpeed.isEmpty())
                        sriPslResponse.put("horizontalSpeed", horizontalSpeed);
                }
                if (velocityEstimate.get("bearing") != null) {
                    String bearing = velocityEstimate.get("bearing").getAsString();
                    if (!bearing.isEmpty())
                        sriPslResponse.put("bearing", bearing);
                }
                if (velocityEstimate.get("verticalSpeed") != null) {
                    String verticalSpeed = velocityEstimate.get("verticalSpeed").getAsString();
                    if (!verticalSpeed.isEmpty())
                        sriPslResponse.put("verticalSpeed", verticalSpeed);
                }
                if (velocityEstimate.get("uncertaintyHorizontalSpeed") != null) {
                    String uncertaintyHorizontalSpeed = velocityEstimate.get("uncertaintyHorizontalSpeed").getAsString();
                    if (!uncertaintyHorizontalSpeed.isEmpty())
                        sriPslResponse.put("uncertaintyHorizontalSpeed", uncertaintyHorizontalSpeed);
                }
                if (velocityEstimate.get("uncertaintyVerticalSpeed") != null) {
                    String uncertaintyVerticalSpeed = velocityEstimate.get("uncertaintyVerticalSpeed").getAsString();
                    if (!uncertaintyVerticalSpeed.isEmpty())
                        sriPslResponse.put("uncertaintyVerticalSpeed", uncertaintyVerticalSpeed);
                }
                if (velocityEstimate.get("velocityType") != null) {
                    String velocityType = velocityEstimate.get("velocityType").getAsString();
                    if (!velocityType.isEmpty())
                        sriPslResponse.put("velocityType", velocityType);
                }
            }
        }
        return sriPslResponse;
    }

    private void putDataFromSriPslResponse(HashMap<String, String> sriPslResponse, MultivaluedMap<String, String> data) {
        String lcsReferenceNumber = sriPslResponse.get("lcsReferenceNumber");
        if (lcsReferenceNumber != null)
            data.putSingle("ReferenceNumber", lcsReferenceNumber);

        String msisdn = sriPslResponse.get("msisdn");
        if (msisdn != null)
            data.putSingle("MSISDN", msisdn);

        String imsi = sriPslResponse.get("imsi");
        if (imsi != null)
            data.putSingle("IMSI", imsi);

        String lmsi = sriPslResponse.get("lmsi");
        if (lmsi != null)
            data.putSingle("LMSI", lmsi);

        String networkNodeNumber = sriPslResponse.get("networkNodeNumber");
        if (networkNodeNumber != null)
            data.putSingle("NetworkEntityAddress", networkNodeNumber);

        String tgppAAAServerName = sriPslResponse.get("3GPPAAAServerName");
        if (tgppAAAServerName != null)
            data.putSingle("NetworkEntityName", tgppAAAServerName);

        String pprAddress = sriPslResponse.get("pprAddress");
        if (pprAddress != null)
            data.putSingle("NetworkEntityName", pprAddress);

        String sgsnName = sriPslResponse.get("sgsnName");
        if (sgsnName != null)
            data.putSingle("NetworkEntityName", sgsnName);

        String sgsnRealm = sriPslResponse.get("sgsnRealm");
        if (sgsnRealm != null) {
            sgsnName = sgsnName.concat("@").concat(sgsnRealm);
            data.putSingle("NetworkEntityName", sgsnName);
        }

        String mmeName = sriPslResponse.get("mmeName");
        if (mmeName != null)
            data.putSingle("NetworkEntityName", mmeName);

        String mmeRealm = sriPslResponse.get("mmeRealm");
        if (mmeRealm != null) {
            mmeName = mmeName.concat("@").concat(mmeRealm);
            data.putSingle("NetworkEntityName", mmeName);
        }

        String typeOfShape = sriPslResponse.get("typeOfShape");
        if (typeOfShape != null)
            data.putSingle("TypeOfShape", typeOfShape);

        String latitude = sriPslResponse.get("latitude");
        if (latitude != null)
            data.putSingle("DeviceLatitude", latitude);

        String longitude = sriPslResponse.get("longitude");
        if (longitude != null)
            data.putSingle("DeviceLongitude", longitude);

        String uncertainty = sriPslResponse.get("uncertainty");
        if (uncertainty != null)
            data.putSingle("Uncertainty", uncertainty);

        String uncertaintySemiMajorAxis = sriPslResponse.get("uncertaintySemiMajorAxis");
        if (uncertaintySemiMajorAxis != null)
            data.putSingle("UncertaintySemiMajorAxis", uncertaintySemiMajorAxis);

        String uncertaintySemiMinorAxis = sriPslResponse.get("uncertaintySemiMinorAxis");
        if (uncertaintySemiMinorAxis != null)
            data.putSingle("UncertaintySemiMinorAxis", uncertaintySemiMinorAxis);

        String angleOfMajorAxis = sriPslResponse.get("angleOfMajorAxis");
        if (angleOfMajorAxis != null)
            data.putSingle("AngleOfMajorAxis", angleOfMajorAxis);

        String confidence = sriPslResponse.get("confidence");
        if (confidence != null)
            data.putSingle("Confidence", confidence);

        String altitude = sriPslResponse.get("altitude");
        if (altitude != null)
            data.putSingle("DeviceAltitude", altitude);

        String uncertaintyAltitude = sriPslResponse.get("uncertaintyAltitude");
        if (uncertaintyAltitude != null)
            data.putSingle("UncertaintyAltitude", uncertaintyAltitude);

        String innerRadius = sriPslResponse.get("innerRadius");
        if (innerRadius != null)
            data.putSingle("InnerRadius", innerRadius);

        String uncertaintyInnerRadius = sriPslResponse.get("uncertaintyInnerRadius");
        if (uncertaintyInnerRadius != null)
            data.putSingle("UncertaintyInnerRadius", uncertaintyInnerRadius);

        String offsetAngle = sriPslResponse.get("offsetAngle");
        if (offsetAngle != null)
            data.putSingle("OffsetAngle", offsetAngle);

        String includedAngle = sriPslResponse.get("includedAngle");
        if (includedAngle != null)
            data.putSingle("IncludedAngle", includedAngle);

        String horizontalSpeed = sriPslResponse.get("horizontalSpeed");
        if (horizontalSpeed != null)
            data.putSingle("HorizontalSpeed", horizontalSpeed);

        String verticalSpeed = sriPslResponse.get("verticalSpeed");
        if (verticalSpeed != null)
            data.putSingle("VerticalSpeed", verticalSpeed);

        String uncertaintyHorizontalSpeed = sriPslResponse.get("uncertaintyHorizontalSpeed");
        if (uncertaintyHorizontalSpeed != null)
            data.putSingle("UncertaintyHorizontalSpeed", uncertaintyHorizontalSpeed);

        String uncertaintyVerticalSpeed = sriPslResponse.get("uncertaintyVerticalSpeed");
        if (uncertaintyVerticalSpeed != null)
            data.putSingle("UncertaintyVerticalSpeed", uncertaintyVerticalSpeed);

        String bearing = sriPslResponse.get("bearing");
        if (bearing != null)
            data.putSingle("Bearing", bearing);

        String ageOfLocationEstimate = sriPslResponse.get("ageOfLocationEstimate");
        if (ageOfLocationEstimate != null)
            data.putSingle("LocationAge", ageOfLocationEstimate);

        String mcc = sriPslResponse.get("mcc");
        if (mcc != null)
            data.putSingle("MobileCountryCode", mcc);

        String mnc = sriPslResponse.get("mnc");
        if (mnc != null)
            data.putSingle("MobileNetworkCode", mnc);

        String lac = sriPslResponse.get("lac");
        if (lac != null)
            data.putSingle("LocationAreaCode", lac);

        String ci = sriPslResponse.get("ci");
        if (ci != null)
            data.putSingle("CellId", ci);

        String sac = sriPslResponse.get("sac");
        if (sac != null)
            data.putSingle("ServiceAreaCode", sac);

        String errorReason = sriPslResponse.get("errorReason");
        if (errorReason != null) {
            cause = errorReason;
            rStatus = responseStatus.Failed.toString();
            data.putSingle("Cause", cause);
            data.putSingle("ResponseStatus", rStatus);
        }

        /*String gprsNodeIndicator = sriPslResponse.get("gprsNodeIndicator");
        String deferredMTLRresponseIndicator = sriPslResponse.get("deferredMTLRresponseIndicator");
        String geranPositioningInfo = sriPslResponse.get("geranPositioningInfo");
        String geranGanssPositioningData = sriPslResponse.get("geranGanssPositioningData");
        String utranPositioningInfo = sriPslResponse.get("utranPositioningInfo");
        String utranGanssPositioningData = sriPslResponse.get("utranGanssPositioningData");
        String velocityType = sriPslResponse.get("velocityType");*/

    }


    private HashMap<String, String> parsePlrJsonString(String jsonLine) {
        HashMap <String, String> rirPlrResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();

        if (jobject.get("lcsReferenceNumber") != null) {
            String lcsReferenceNumber = jobject.get("lcsReferenceNumber").getAsString();
            if (!lcsReferenceNumber.isEmpty())
                rirPlrResponse.put("lcsReferenceNumber", lcsReferenceNumber);
        }
        if (jobject.get("errorReason") != null) {
            String errorReason = jobject.get("errorReason").getAsString();
            rirPlrResponse.put("errorReason", errorReason);
        }

        JsonObject ria = jobject.getAsJsonObject("Routing-Info-Answer");
        if (ria != null) {
            if (ria.get("msisdn") != null) {
                String msisdn = ria.get("msisdn").getAsString();
                if (!msisdn.isEmpty())
                    rirPlrResponse.put("msisdn", msisdn);
            }
            if (ria.get("imsi") != null) {
                String imsi = ria.get("imsi").getAsString();
                if (!imsi.isEmpty())
                    rirPlrResponse.put("imsi", imsi);
            }
            if (ria.get("lmsi") != null) {
                String lmsi = ria.get("lmsi").getAsString();
                if (!lmsi.isEmpty())
                    rirPlrResponse.put("lmsi", lmsi);
            }
            if (ria.get("mmeName") != null) {
                String mmeName = ria.get("mmeName").getAsString();
                if (!mmeName.isEmpty())
                    rirPlrResponse.put("mmeName", mmeName);
            }
            if (ria.get("mmeRealm") != null) {
                String mmeRealm = ria.get("mmeRealm").getAsString();
                if (!mmeRealm.isEmpty())
                    rirPlrResponse.put("mmeRealm", mmeRealm);
            }
            if (ria.get("mscNumber") != null) {
                String mscNumber = ria.get("mscNumber").getAsString();
                if (!mscNumber.isEmpty())
                    rirPlrResponse.put("mscNumber", mscNumber);
            }
            if (ria.get("sgsnNumber") != null) {
                String sgsnNumber = ria.get("sgsnNumber").getAsString();
                if (!sgsnNumber.isEmpty())
                    rirPlrResponse.put("sgsnNumber", sgsnNumber);
            }
            if (ria.get("sgsnName") != null) {
                String sgsnName = ria.get("sgsnName").getAsString();
                if (!sgsnName.isEmpty())
                    rirPlrResponse.put("sgsnName", sgsnName);
            }
            if (ria.get("sgsnRealm") != null) {
                String sgsnRealm = ria.get("sgsnRealm").getAsString();
                if (!sgsnRealm.isEmpty())
                    rirPlrResponse.put("sgsnRealm", sgsnRealm);
            }
            if (ria.get("3GPPAAAServerName") != null) {
                String tgppAAAServerName = ria.get("3GPPAAAServerName").getAsString();
                rirPlrResponse.put("tgppAAAServerName", tgppAAAServerName);
            }
            if (ria.get("gmlcAddress") != null) {
                String gmlcAddress = ria.get("gmlcAddress").getAsString();
                if (!gmlcAddress.isEmpty())
                    rirPlrResponse.put("gmlcAddress", gmlcAddress);
            }
        }
        JsonObject pla = jelement.getAsJsonObject();
        pla = pla.getAsJsonObject("Provide-Location-Answer");
        if (pla != null) {
            JsonObject locationEstimate = pla.getAsJsonObject("LocationEstimate");
            if (locationEstimate != null) {
                if (locationEstimate.get("typeOfShape") != null) {
                    String typeOfShape = locationEstimate.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        rirPlrResponse.put("typeOfShape", typeOfShape);
                }
                if (locationEstimate.get("latitude") != null) {
                    String latitude = locationEstimate.get("latitude").getAsString();
                    if (!latitude.isEmpty())
                        rirPlrResponse.put("latitude", latitude);
                }
                if (locationEstimate.get("longitude") != null) {
                    String longitude = locationEstimate.get("longitude").getAsString();
                    if (!longitude.isEmpty())
                        rirPlrResponse.put("longitude", longitude);
                }
                if (locationEstimate.get("uncertainty") != null) {
                    String uncertainty = locationEstimate.get("uncertainty").getAsString();
                    if (!uncertainty.isEmpty())
                        rirPlrResponse.put("uncertainty", uncertainty);
                }
                if (locationEstimate.get("uncertaintySemiMajorAxis") != null) {
                    String uncertaintySemiMajorAxis = locationEstimate.get("uncertaintySemiMajorAxis").getAsString();
                    if (!uncertaintySemiMajorAxis.isEmpty())
                        rirPlrResponse.put("uncertaintySemiMajorAxis", uncertaintySemiMajorAxis);
                }
                if (locationEstimate.get("uncertaintySemiMinorAxis") != null) {
                    String uncertaintySemiMinorAxis = locationEstimate.get("uncertaintySemiMinorAxis").getAsString();
                    if (!uncertaintySemiMinorAxis.isEmpty())
                        rirPlrResponse.put("uncertaintySemiMinorAxis", uncertaintySemiMinorAxis);
                }
                if (locationEstimate.get("angleOfMajorAxis") != null) {
                    String angleOfMajorAxis = locationEstimate.get("angleOfMajorAxis").getAsString();
                    if (!angleOfMajorAxis.isEmpty())
                        rirPlrResponse.put("angleOfMajorAxis", angleOfMajorAxis);
                }
                if (locationEstimate.get("confidence") != null) {
                    String confidence = locationEstimate.get("confidence").getAsString();
                    if (!confidence.isEmpty())
                        rirPlrResponse.put("confidence", confidence);
                }
                if (locationEstimate.get("altitude") != null) {
                    String altitude = locationEstimate.get("altitude").getAsString();
                    if (!altitude.isEmpty())
                        rirPlrResponse.put("altitude", altitude);
                }
                if (locationEstimate.get("uncertaintyAltitude") != null) {
                    String uncertaintyAltitude = locationEstimate.get("uncertaintyAltitude").getAsString();
                    if (!uncertaintyAltitude.isEmpty())
                        rirPlrResponse.put("uncertaintyAltitude", uncertaintyAltitude);
                }
                if (locationEstimate.get("innerRadius") != null) {
                    String innerRadius = locationEstimate.get("innerRadius").getAsString();
                    if (!innerRadius.isEmpty())
                        rirPlrResponse.put("innerRadius", innerRadius);
                }
                if (locationEstimate.get("uncertaintyInnerRadius") != null) {
                    String uncertaintyInnerRadius = locationEstimate.get("uncertaintyInnerRadius").getAsString();
                    if (!uncertaintyInnerRadius.isEmpty())
                        rirPlrResponse.put("uncertaintyInnerRadius", uncertaintyInnerRadius);
                }
                if (locationEstimate.get("offsetAngle") != null) {
                    String offsetAngle = locationEstimate.get("offsetAngle").getAsString();
                    if (!offsetAngle.isEmpty())
                        rirPlrResponse.put("offsetAngle", offsetAngle);
                }
                if (locationEstimate.get("includedAngle") != null) {
                    String includedAngle = locationEstimate.get("includedAngle").getAsString();
                    if (!includedAngle.isEmpty())
                        rirPlrResponse.put("includedAngle", includedAngle);
                }
            }
            if (pla.get("accuracyFulfilmentIndicator") != null) {
                String accuracyFulfilmentIndicator = pla.get("accuracyFulfilmentIndicator").getAsString();
                if (!accuracyFulfilmentIndicator.isEmpty())
                    rirPlrResponse.put("accuracyFulfilmentIndicator", accuracyFulfilmentIndicator);
            }
            if (pla.get("ageOfLocationEstimate") != null) {
                String ageOfLocationEstimate = pla.get("ageOfLocationEstimate").getAsString();
                if (!ageOfLocationEstimate.isEmpty())
                    rirPlrResponse.put("ageOfLocationEstimate", ageOfLocationEstimate);
            }

            JsonObject cgiSaiEsmlcCell = pla.getAsJsonObject("CGIorSAIorESMLCCellInfo");
            JsonObject cgi = cgiSaiEsmlcCell.getAsJsonObject("CGI");
            if (cgi.get("mcc") != null) {
                String lteCgiMcc = cgi.get("mcc").getAsString();
                if (!lteCgiMcc.isEmpty())
                    rirPlrResponse.put("mcc", lteCgiMcc);
            }
            if (cgi.get("mnc") != null) {
                String lteCgiMnc = cgi.get("mnc").getAsString();
                if (!lteCgiMnc.isEmpty())
                    rirPlrResponse.put("mnc", lteCgiMnc);
            }
            if (cgi.get("lac") != null) {
                String lteCgiLac = cgi.get("lac").getAsString();
                if (!lteCgiLac.isEmpty())
                    rirPlrResponse.put("lac", lteCgiLac);
            }
            if (cgi.get("ci") != null) {
                String lteCgiCi = cgi.get("ci").getAsString();
                if (!lteCgiCi.isEmpty())
                    rirPlrResponse.put("ci", lteCgiCi);
            }

            JsonObject sai = cgiSaiEsmlcCell.getAsJsonObject("SAI");
            if (sai.get("mcc") != null) {
                String eSaiMcc = sai.get("mcc").getAsString();
                if (!eSaiMcc.isEmpty())
                    rirPlrResponse.put("mcc", eSaiMcc);
            }
            if (sai.get("mnc") != null) {
                String eSaiMnc = sai.get("mnc").getAsString();
                if (!eSaiMnc.isEmpty())
                    rirPlrResponse.put("mnc", eSaiMnc);
            }
            if (sai.get("lac") != null) {
                String eSaiLac = sai.get("lac").getAsString();
                if (!eSaiLac.isEmpty())
                    rirPlrResponse.put("lac", eSaiLac);
            }
            if (sai.get("sac") != null) {
                String eSaiSac = sai.get("sac").getAsString();
                if (!eSaiSac.isEmpty())
                    rirPlrResponse.put("sac", eSaiSac);
            }

            JsonObject ecgi = cgiSaiEsmlcCell.getAsJsonObject("ECGI");
            if (ecgi.get("mcc") != null) {
                String ecgiMcc = ecgi.get("mcc").getAsString();
                if (!ecgiMcc.isEmpty())
                    rirPlrResponse.put("mcc", ecgiMcc);
            }
            if (ecgi.get("mnc") != null) {
                String ecgiMnc = ecgi.get("mnc").getAsString();
                if (!ecgiMnc.isEmpty())
                    rirPlrResponse.put("mnc", ecgiMnc);
            }
            if (ecgi.get("eNBId") != null) {
                String ecgiENBId = ecgi.get("eNBId").getAsString();
                if (!ecgiENBId.isEmpty())
                rirPlrResponse.put("eNBId", ecgiENBId);
            }
            if (ecgi.get("ci") != null) {
                String ecgiCi = ecgi.get("ci").getAsString();
                if (!ecgiCi.isEmpty())
                    rirPlrResponse.put("ecgiCi", ecgiCi);
            }
            if (ecgi.get("cellPortionId") != null) {
                String cellPortionId = ecgi.get("cellPortionId").getAsString();
                if (!cellPortionId.isEmpty())
                    rirPlrResponse.put("cellPortionId", cellPortionId);
            }

            JsonObject geranPosInfo = pla.getAsJsonObject("GERANPositioningInfo");
            if (geranPosInfo != null) {
                if (geranPosInfo.get("geranPositioningInfo") != null) {
                    String geranPositioningInfo = geranPosInfo.get("geranPositioningInfo").getAsString();
                    if (!geranPositioningInfo.isEmpty())
                        rirPlrResponse.put("geranPositioningInfo", geranPositioningInfo);
                }
                if (geranPosInfo.get("geranGanssPositioningData") != null) {
                    String geranGanssPositioningData = geranPosInfo.get("geranGanssPositioningData").getAsString();
                    if (!geranGanssPositioningData.isEmpty())
                        rirPlrResponse.put("geranGanssPositioningData", geranGanssPositioningData);
                }
            }
            JsonObject utranPosInfo = pla.getAsJsonObject("UTRANPositioningInfo");
            if (utranPosInfo != null) {
                if (utranPosInfo.get("utranPositioningInfo") != null) {
                    String utranPositioningInfo = utranPosInfo.get("utranPositioningInfo").getAsString();
                    if (!utranPositioningInfo.isEmpty())
                        rirPlrResponse.put("utranPositioningInfo", utranPositioningInfo);
                }
                if (utranPosInfo.get("utranGanssPositioningData") != null) {
                    String utranGanssPositioningData = utranPosInfo.get("utranGanssPositioningData").getAsString();
                    if (!utranGanssPositioningData.isEmpty())
                        rirPlrResponse.put("utranGanssPositioningData", utranGanssPositioningData);
                }
            }
            JsonObject eUtranPosInfo = pla.getAsJsonObject("E-UTRANPositioningInfo");
            if (eUtranPosInfo != null) {
                String eUtranPositioningData = eUtranPosInfo.get("eUtranPositioningData").getAsString();
                if (!eUtranPositioningData.isEmpty())
                    rirPlrResponse.put("eUtranPositioningData", eUtranPositioningData);
            }
            JsonObject velocityEstimate = pla.getAsJsonObject("VelocityEstimate");
            if (velocityEstimate != null) {
                if (velocityEstimate.get("horizontalSpeed") != null) {
                    String horizontalSpeed = velocityEstimate.get("horizontalSpeed").getAsString();
                    if (!horizontalSpeed.isEmpty())
                        rirPlrResponse.put("horizontalSpeed", horizontalSpeed);
                }
                if (velocityEstimate.get("bearing") != null) {
                    String bearing = velocityEstimate.get("bearing").getAsString();
                    if (!bearing.isEmpty())
                        rirPlrResponse.put("bearing", bearing);
                }
                if (velocityEstimate.get("verticalSpeed") != null) {
                    String verticalSpeed = velocityEstimate.get("verticalSpeed").getAsString();
                    if (!verticalSpeed.isEmpty())
                        rirPlrResponse.put("verticalSpeed", verticalSpeed);
                }
                if (velocityEstimate.get("uncertaintyHorizontalSpeed") != null) {
                    String uncertaintyHorizontalSpeed = velocityEstimate.get("uncertaintyHorizontalSpeed").getAsString();
                    if (!uncertaintyHorizontalSpeed.isEmpty())
                        rirPlrResponse.put("uncertaintyHorizontalSpeed", uncertaintyHorizontalSpeed);
                }
                if (velocityEstimate.get("uncertaintyVerticalSpeed") != null) {
                    String uncertaintyVerticalSpeed = velocityEstimate.get("uncertaintyVerticalSpeed").getAsString();
                    if (!uncertaintyVerticalSpeed.isEmpty())
                        rirPlrResponse.put("uncertaintyVerticalSpeed", uncertaintyVerticalSpeed);
                }
                if (velocityEstimate.get("velocityType") != null) {
                    String velocityType = velocityEstimate.get("velocityType").getAsString();
                    if (!velocityType.isEmpty())
                        rirPlrResponse.put("velocityType", velocityType);
                }
            }
            if (pla.get("civicAddress") != null) {
                String civicAddress = pla.get("civicAddress").getAsString();
                if (!civicAddress.isEmpty())
                    rirPlrResponse.put("civicAddress", civicAddress);
            }
            if (pla.get("barometricPressure") != null) {
                String barometricPressure = pla.get("barometricPressure").getAsString();
                if (!barometricPressure.isEmpty())
                    rirPlrResponse.put("barometricPressure", barometricPressure);
            }
        }
        return rirPlrResponse;
    }

    private void putDataFromRirPlrResponse(HashMap<String, String> rirPlrResponse, MultivaluedMap<String, String> data) {
        String lcsReferenceNumber = rirPlrResponse.get("lcsReferenceNumber");
        if (lcsReferenceNumber != null)
            data.putSingle("ReferenceNumber", lcsReferenceNumber);

        String msisdn = rirPlrResponse.get("msisdn");
        if (msisdn != null)
            data.putSingle("MSISDN", msisdn);

        String imsi = rirPlrResponse.get("imsi");
        if (imsi != null)
            data.putSingle("IMSI", imsi);

        String lmsi = rirPlrResponse.get("lmsi");
        if (lmsi != null)
            data.putSingle("LMSI", lmsi);

        String sgsnName = rirPlrResponse.get("sgsnName");
        if (sgsnName != null)
            data.putSingle("NetworkEntityName", sgsnName);

        String sgsnRealm = rirPlrResponse.get("sgsnRealm");
        if (sgsnRealm != null) {
            sgsnName = sgsnName.concat("@").concat(sgsnRealm);
            data.putSingle("NetworkEntityName", sgsnName);
        }

        String mmeName = rirPlrResponse.get("mmeName");
        if (mmeName != null)
            data.putSingle("NetworkEntityName", mmeName);

        String mmeRealm = rirPlrResponse.get("mmeRealm");
        if (mmeRealm != null) {
            mmeName = mmeName.concat("@").concat(mmeRealm);
            data.putSingle("NetworkEntityName", mmeName);
        }

        String mscNumber = rirPlrResponse.get("mscNumber");
        if (mscNumber != null)
            data.putSingle("NetworkEntityAddress", mscNumber);

        String sgsnNumber = rirPlrResponse.get("sgsnNumber");
        if (sgsnNumber != null)
            data.putSingle("NetworkEntityAddress", sgsnNumber);

        String typeOfShape = rirPlrResponse.get("typeOfShape");
        if (typeOfShape != null)
            data.putSingle("TypeOfShape", typeOfShape);

        String latitude = rirPlrResponse.get("latitude");
        if (latitude != null)
            data.putSingle("DeviceLatitude", latitude);

        String longitude = rirPlrResponse.get("longitude");
        if (longitude != null)
            data.putSingle("DeviceLongitude", longitude);

        String uncertainty = rirPlrResponse.get("uncertainty");
        if (uncertainty != null)
            data.putSingle("Uncertainty", uncertainty);

        String uncertaintySemiMajorAxis = rirPlrResponse.get("uncertaintySemiMajorAxis");
        if (uncertaintySemiMajorAxis != null)
            data.putSingle("UncertaintySemiMajorAxis", uncertaintySemiMajorAxis);

        String uncertaintySemiMinorAxis = rirPlrResponse.get("uncertaintySemiMinorAxis");
        if (uncertaintySemiMinorAxis != null)
            data.putSingle("UncertaintySemiMinorAxis", uncertaintySemiMinorAxis);

        String angleOfMajorAxis = rirPlrResponse.get("angleOfMajorAxis");
        if (angleOfMajorAxis != null)
            data.putSingle("AngleOfMajorAxis", angleOfMajorAxis);

        String confidence = rirPlrResponse.get("confidence");
        if (confidence != null)
            data.putSingle("Confidence", confidence);

        String altitude = rirPlrResponse.get("altitude");
        if (altitude != null)
            data.putSingle("DeviceAltitude", altitude);

        String uncertaintyAltitude = rirPlrResponse.get("uncertaintyAltitude");
        if (uncertaintyAltitude != null)
            data.putSingle("UncertaintyAltitude", uncertaintyAltitude);

        String innerRadius = rirPlrResponse.get("innerRadius");
        if (innerRadius != null)
            data.putSingle("InnerRadius", innerRadius);

        String uncertaintyInnerRadius = rirPlrResponse.get("uncertaintyInnerRadius");
        if (uncertaintyInnerRadius != null)
            data.putSingle("UncertaintyInnerRadius", uncertaintyInnerRadius);

        String offsetAngle = rirPlrResponse.get("offsetAngle");
        if (offsetAngle != null)
            data.putSingle("OffsetAngle", offsetAngle);

        String includedAngle = rirPlrResponse.get("includedAngle");
        if (includedAngle != null)
            data.putSingle("IncludedAngle", includedAngle);

        String horizontalSpeed = rirPlrResponse.get("horizontalSpeed");
        if (horizontalSpeed != null)
            data.putSingle("HorizontalSpeed", horizontalSpeed);

        String verticalSpeed = rirPlrResponse.get("verticalSpeed");
        if (verticalSpeed != null)
            data.putSingle("VerticalSpeed", verticalSpeed);

        String uncertaintyHorizontalSpeed = rirPlrResponse.get("uncertaintyHorizontalSpeed");
        if (uncertaintyHorizontalSpeed != null)
            data.putSingle("UncertaintyHorizontalSpeed", uncertaintyHorizontalSpeed);

        String uncertaintyVerticalSpeed = rirPlrResponse.get("uncertaintyVerticalSpeed");
        if (uncertaintyVerticalSpeed != null)
            data.putSingle("UncertaintyVerticalSpeed", uncertaintyVerticalSpeed);

        String bearing = rirPlrResponse.get("bearing");
        if (bearing != null)
            data.putSingle("Bearing", bearing);

        String ageOfLocationEstimate = rirPlrResponse.get("ageOfLocationEstimate");
        if (ageOfLocationEstimate != null)
            data.putSingle("LocationAge", ageOfLocationEstimate);

        String mcc = rirPlrResponse.get("mcc");
        if (mcc != null)
            data.putSingle("MobileCountryCode", mcc);

        String mnc = rirPlrResponse.get("mnc");
        if (mnc != null)
            data.putSingle("MobileNetworkCode", mnc);

        String lac = rirPlrResponse.get("lac");
        if (lac != null)
            data.putSingle("LocationAreaCode", lac);

        String ci = rirPlrResponse.get("ci");
        if (ci != null)
            data.putSingle("CellId", ci);

        String sac = rirPlrResponse.get("sac");
        if (sac != null)
            data.putSingle("ServiceAreaCode", sac);

        String eUtranCgiCi = rirPlrResponse.get("ecgiCi");
        if (eUtranCgiCi != null)
            data.putSingle("CellId", eUtranCgiCi);

        String eNodeBId = rirPlrResponse.get("eNBId");
        if (eNodeBId != null)
            data.putSingle("ENodeBId", eNodeBId);

        String civicAddress = rirPlrResponse.get("civicAddress");
        if (civicAddress != null)
            data.putSingle("CivicAddress", civicAddress);

        String barometricPressure = rirPlrResponse.get("barometricPressure");
        if (barometricPressure != null)
            data.putSingle("BarometricPressure", barometricPressure);

        String errorReason = rirPlrResponse.get("errorReason");
        if (errorReason != null) {
            cause = errorReason;
            rStatus = responseStatus.Failed.toString();
            data.putSingle("Cause", cause);
            data.putSingle("ResponseStatus", rStatus);
        }
    }

    private HashMap<String, String> parseUdaJsonString(String jsonLine) {
        HashMap <String, String> udrResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        String network = jobject.get("network").getAsString();
        udrResponse.put("network", network);
        String protocol = jobject.get("protocol").getAsString();
        udrResponse.put("protocol", protocol);
        String operation = jobject.get("operation").getAsString();
        udrResponse.put("operation", operation);

        if (jobject.get("errorReason") != null) {
            String errorReason = jobject.get("errorReason").getAsString();
            udrResponse.put("errorReason", errorReason);
        }

        JsonObject publicIdentifiers = jelement.getAsJsonObject();
        publicIdentifiers = publicIdentifiers.getAsJsonObject("PublicIdentifiers");
        if (publicIdentifiers != null) {
            if (publicIdentifiers.get("msisdn") != null) {
                String msisdn = publicIdentifiers.get("msisdn").getAsString();
                udrResponse.put("msisdn", msisdn);
            }
            if (publicIdentifiers.get("imsPublicIdentity") != null) {
                String imsPublicIdentity = publicIdentifiers.get("imsPublicIdentity").getAsString();
                udrResponse.put("imsPublicIdentity", imsPublicIdentity);
            }

        }

        JsonObject csLocationInformation = jelement.getAsJsonObject();
        csLocationInformation = csLocationInformation.getAsJsonObject("CSLocationInformation");
        if (csLocationInformation != null) {
            JsonObject locationNumber = csLocationInformation.getAsJsonObject("LocationNumber");
            if (locationNumber != null) {
                if (locationNumber.get("oddFlag") != null) {
                    String oF = locationNumber.get("oddFlag").getAsString();
                    udrResponse.put("oddFlag", oF);
                }
                if (locationNumber.get("natureOfAddressIndicator") != null) {
                    String nai = locationNumber.get("natureOfAddressIndicator").getAsString();
                    udrResponse.put("nai", nai);
                }
                if (locationNumber.get("internalNetworkNumberIndicator") != null) {
                    String inni = locationNumber.get("internalNetworkNumberIndicator").getAsString();
                    udrResponse.put("inni", inni);
                }
                if (locationNumber.get("numberingPlanIndicator") != null) {
                    String npi = locationNumber.get("numberingPlanIndicator").getAsString();
                    udrResponse.put("npi", npi);
                }
                if (locationNumber.get("addressRepresentationRestrictedIndicator") != null) {
                    String arpi = locationNumber.get("addressRepresentationRestrictedIndicator").getAsString();
                    udrResponse.put("arpi", arpi);
                }
                if (locationNumber.get("screeningIndicator") != null) {
                    String si = locationNumber.get("screeningIndicator").getAsString();
                    udrResponse.put("si", si);
                }
                if (locationNumber.get("address") != null) {
                    String address = locationNumber.get("address").getAsString();
                    udrResponse.put("address", address);
                }
            }

            JsonObject csCgi = csLocationInformation.getAsJsonObject("CGI");
            if (csCgi != null) {
                if (csCgi.get("mcc") != null) {
                    String csMcc = csCgi.get("mcc").getAsString();
                    udrResponse.put("csMcc", csMcc);
                }
                if (csCgi.get("mnc") != null) {
                    String csMnc = csCgi.get("mnc").getAsString();
                    udrResponse.put("csMnc", csMnc);
                }
                if (csCgi.get("lac") != null) {
                    String csLac = csCgi.get("lac").getAsString();
                    udrResponse.put("csLac", csLac);
                }
                if (csCgi.get("ci") != null) {
                    String csCi = csCgi.get("ci").getAsString();
                    udrResponse.put("csCi", csCi);
                }
            }

            JsonObject csGeographicalInformation = csLocationInformation.getAsJsonObject("GeographicalInformation");
            if (csGeographicalInformation != null) {
                if (csGeographicalInformation.get("typeOfShape") != null) {
                    String csTypeOfShape = csGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("csGeogTypeOfShape", csTypeOfShape);
                }
                if (csGeographicalInformation.get("latitude") != null) {
                    String csLatitude = csGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("csGeogLatitude", csLatitude);
                }
                if (csGeographicalInformation.get("longitude") != null) {
                    String csLongitude = csGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("csGeogLongitude", csLongitude);
                }
                if (csGeographicalInformation.get("uncertainty") != null) {
                    String csUncertainty = csGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("csGeogUncertainty", csUncertainty);
                }
            }

            JsonObject csGeodeticInformation = csLocationInformation.getAsJsonObject("GeodeticInformation");
            if (csGeodeticInformation != null) {
                if (csGeodeticInformation.get("typeOfShape") != null) {
                    String csGeodTypeOfShape = csGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("csGeodTypeOfShape", csGeodTypeOfShape);
                }
                logger.info("csGeodeticInformation.get(\"latitude\")= "+csGeodeticInformation);
                if (csGeodeticInformation.get("latitude") != null) {
                    String csGeodLatitude = csGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("csGeodLatitude", csGeodLatitude);
                }
                if (csGeodeticInformation.get("longitude") != null) {
                    String csGeodLongitude = csGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("csGeodLongitude", csGeodLongitude);
                }
                if (csGeodeticInformation.get("uncertainty") != null) {
                    String csGeodUncertainty = csGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("csGeodUncertainty", csGeodUncertainty);
                }
                if (csGeodeticInformation.get("confidence") != null) {
                    String csConfidence = csGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("csGeodConfidence", csConfidence);
                }
                if (csGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String csScreeningAndPresentationIndicators = csGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("csGeodScreeningAndPresentationIndicators", csScreeningAndPresentationIndicators);
                }
            }

            if (csLocationInformation.get("mscNumber") != null) {
                String mscNumber = csLocationInformation.get("mscNumber").getAsString();
                if (mscNumber != null) {
                    udrResponse.put("mscNumber", mscNumber);
                }
            }

            if (csLocationInformation.get("vlrNumber") != null) {
                String vlrNumber = csLocationInformation.get("vlrNumber").getAsString();
                if (vlrNumber != null) {
                    udrResponse.put("vlrNumber", vlrNumber);
                }
            }

            if (csLocationInformation.get("ageOfLocationInformation") != null) {
                String csAgeOfLocationInfo = csLocationInformation.get("ageOfLocationInformation").getAsString();
                if (csAgeOfLocationInfo != null) {
                    udrResponse.put("csAgeOfLocationInformation", csAgeOfLocationInfo);
                }
            }

            if (csLocationInformation.get("currentLocationRetrieved") != null) {
                String csCurrentLocationRetrieved = csLocationInformation.get("currentLocationRetrieved").getAsString();
                if (csCurrentLocationRetrieved != null) {
                    udrResponse.put("csCurrentLocationRetrieved", csCurrentLocationRetrieved);
                }
            }
        }

        JsonObject psLocationInformation = jelement.getAsJsonObject();
        psLocationInformation = psLocationInformation.getAsJsonObject("PSLocationInformation");
        if (psLocationInformation != null) {
            JsonObject routingAreaId = psLocationInformation.getAsJsonObject("RAI");
            if (routingAreaId != null) {
                if (routingAreaId.get("mcc") != null) {
                    String raiMcc = routingAreaId.get("mcc").getAsString();
                    udrResponse.put("raiMcc", raiMcc);
                }
                if (routingAreaId.get("mnc") != null) {
                    String raiMnc = routingAreaId.get("mnc").getAsString();
                    udrResponse.put("raiMnc", raiMnc);
                }
                if (routingAreaId.get("lac") != null) {
                    String raiLac = routingAreaId.get("lac").getAsString();
                    udrResponse.put("raiLac", raiLac);
                }
                if (routingAreaId.get("rac") != null) {
                    String rac = routingAreaId.get("rac").getAsString();
                    udrResponse.put("rac", rac);
                }

            }

            JsonObject psCgi = psLocationInformation.getAsJsonObject("CGI");
            if (psCgi != null) {
                if (psCgi.get("mcc") != null) {
                    String psMcc = psCgi.get("mcc").getAsString();
                    udrResponse.put("psMcc", psMcc);
                }
                if (psCgi.get("mnc") != null) {
                    String psMnc = psCgi.get("mnc").getAsString();
                    udrResponse.put("psMnc", psMnc);
                }
                if (psCgi.get("lac") != null) {
                    String psLac = psCgi.get("lac").getAsString();
                    udrResponse.put("psLac", psLac);
                }
                if (psCgi.get("ci") != null) {
                    String psCi = psCgi.get("ci").getAsString();
                    udrResponse.put("psCi", psCi);
                }
                if (psCgi.get("sac") != null) {
                    String psSac = psCgi.get("psSac").getAsString();
                    udrResponse.put("psSac", psSac);
                }
            }

            JsonObject psGeographicalInformation = psLocationInformation.getAsJsonObject("GeographicalInformation");
            if (psGeographicalInformation != null) {
                if (psGeographicalInformation.get("typeOfShape") != null) {
                    String psTypeOfShape = psGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("psGeogTypeOfShape", psTypeOfShape);
                }
                if (psGeographicalInformation.get("latitude") != null) {
                    String psLatitude = psGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("psGeogLatitude", psLatitude);
                }
                if (psGeographicalInformation.get("longitude") != null) {
                    String psLongitude = psGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("psGeogLongitude", psLongitude);
                }
                if (psGeographicalInformation.get("uncertainty") != null) {
                    String psUncertainty = psGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("psGeogUncertainty", psUncertainty);
                }
            }

            JsonObject psGeodeticInformation = psLocationInformation.getAsJsonObject("GeodeticInformation");
            if (psGeodeticInformation != null) {
                if (psGeodeticInformation.get("typeOfShape") != null) {
                    String psGeodTypeOfShape = psGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("psGeodTypeOfShape", psGeodTypeOfShape);
                }
                if (psGeodeticInformation.get("latitude") != null) {
                    String psGeodLatitude = psGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("psGeodLatitude", psGeodLatitude);
                }
                if (psGeodeticInformation.get("longitude") != null) {
                    String psGeodLongitude = psGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("psGeodLongitude", psGeodLongitude);
                }
                if (psGeodeticInformation.get("uncertainty") != null) {
                    String psGeodUncertainty = psGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("psGeodUncertainty", psGeodUncertainty);
                }
                if (psGeodeticInformation.get("confidence") != null) {
                    String psConfidence = psGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("psGeodConfidence", psConfidence);
                }
                if (psGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String psScreeningAndPresentationIndicators = psGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("psGeodScreeningAndPresentationIndicators", psScreeningAndPresentationIndicators);
                }
            }

            if (psLocationInformation.get("sgsnNumber") != null) {
                String sgsnNumber = psLocationInformation.get("sgsnNumber").getAsString();
                if (sgsnNumber != null) {
                    udrResponse.put("sgsnNumber", sgsnNumber);
                }
            }

            if (psLocationInformation.get("ageOfLocationInformation") != null) {
                String psAgeOfLocationInfo = psLocationInformation.get("ageOfLocationInformation").getAsString();
                if (psAgeOfLocationInfo != null) {
                    udrResponse.put("psAgeOfLocationInformation", psAgeOfLocationInfo);
                }
            }

            if (psLocationInformation.get("currentLocationRetrieved") != null) {
                String psCurrentLocationRetrieved = psLocationInformation.get("currentLocationRetrieved").getAsString();
                if (psCurrentLocationRetrieved != null) {
                    udrResponse.put("psCurrentLocationRetrieved", psCurrentLocationRetrieved);
                }
            }
        }

        JsonObject epsLocationInformation = jelement.getAsJsonObject();
        epsLocationInformation = epsLocationInformation.getAsJsonObject("EPSLocationInformation");
        if (epsLocationInformation != null) {
            JsonObject trackingAreaId = epsLocationInformation.getAsJsonObject("TAI");
            if (trackingAreaId != null) {
                if (trackingAreaId.get("mcc") != null) {
                    String taiMcc = trackingAreaId.get("mcc").getAsString();
                    udrResponse.put("taiMcc", taiMcc);
                }
                if (trackingAreaId.get("mnc") != null) {
                    String taiMnc = trackingAreaId.get("mnc").getAsString();
                    udrResponse.put("taiMnc", taiMnc);
                }
                if (trackingAreaId.get("tac") != null) {
                    String tac = trackingAreaId.get("tac").getAsString();
                    udrResponse.put("tac", tac);
                }
            }

            JsonObject epsCgi = epsLocationInformation.getAsJsonObject("ECGI");
            if (epsCgi != null) {
                if (epsCgi.get("mcc") != null) {
                    String epsMcc = epsCgi.get("mcc").getAsString();
                    udrResponse.put("epsMcc", epsMcc);
                }
                if (epsCgi.get("mnc") != null) {
                    String epsMnc = epsCgi.get("mnc").getAsString();
                    udrResponse.put("epsMnc", epsMnc);
                }
                if (epsCgi.get("eNBId") != null) {
                    String eNBId = epsCgi.get("eNBId").getAsString();
                    udrResponse.put("eNBId", eNBId);
                }
                if (epsCgi.get("ci") != null) {
                    String epsCi = epsCgi.get("ci").getAsString();
                    udrResponse.put("epsCi", epsCi);
                }
            }

            JsonObject epsGeographicalInformation = epsLocationInformation.getAsJsonObject("GeographicalInformation");
            if (epsGeographicalInformation != null) {
                if (epsGeographicalInformation.get("typeOfShape") != null) {
                    String epsTypeOfShape = epsGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("epsGeogTypeOfShape", epsTypeOfShape);
                }
                if (epsGeographicalInformation.get("latitude") != null) {
                    String epsLatitude = epsGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("epsGeogLatitude", epsLatitude);
                }
                if (epsGeographicalInformation.get("longitude") != null) {
                    String epsLongitude = epsGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("epsGeogLongitude", epsLongitude);
                }
                if (epsGeographicalInformation.get("uncertainty") != null) {
                    String epsUncertainty = epsGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("epsGeogUncertainty", epsUncertainty);
                }
            }

            JsonObject epsGeodeticInformation = epsLocationInformation.getAsJsonObject("GeodeticInformation");
            if (epsGeodeticInformation != null) {
                if (epsGeodeticInformation.get("typeOfShape") != null) {
                    String epsGeodTypeOfShape = epsGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("epsGeodTypeOfShape", epsGeodTypeOfShape);
                }
                if (epsGeodeticInformation.get("latitude") != null) {
                    String epsGeodLatitude = epsGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("epsGeodLatitude", epsGeodLatitude);
                }
                if (epsGeodeticInformation.get("longitude") != null) {
                    String epsGeodLongitude = epsGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("epsGeodLongitude", epsGeodLongitude);
                }
                if (epsGeodeticInformation.get("uncertainty") != null) {
                    String epsGeodUncertainty = epsGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("epsGeodUncertainty", epsGeodUncertainty);
                }
                if (epsGeodeticInformation.get("confidence") != null) {
                    String epsConfidence = epsGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("epsGeodConfidence", epsConfidence);
                }
                if (epsGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String epsScreeningAndPresentationIndicators = epsGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("epsGeodScreeningAndPresentationIndicators", epsScreeningAndPresentationIndicators);
                }
            }

            if (epsLocationInformation.get("mmeName") != null) {
                String mmeName = epsLocationInformation.get("mmeName").getAsString();
                if (mmeName != null) {
                    udrResponse.put("mmeName", mmeName);
                }
            }

            if (epsLocationInformation.get("ageOfLocationInformation") != null) {
                String epsAgeOfLocationInfo = epsLocationInformation.get("ageOfLocationInformation").getAsString();
                if (epsAgeOfLocationInfo != null) {
                    udrResponse.put("epsAgeOfLocationInformation", epsAgeOfLocationInfo);
                }
            }

            if (epsLocationInformation.get("currentLocationRetrieved") != null) {
                String epsCurrentLocationRetrieved = epsLocationInformation.get("currentLocationRetrieved").getAsString();
                if (epsCurrentLocationRetrieved != null) {
                    udrResponse.put("epsCurrentLocationRetrieved", epsCurrentLocationRetrieved);
                }
            }
        }

        return udrResponse;
    }

    private void putDataFromShUdrResponse(HashMap<String, String> udrResponse, MultivaluedMap<String, String> data) {

        // Public Identifiers
        String msisdn = udrResponse.get("msisdn");
        if (msisdn != null)
            data.putSingle("MSISDN", msisdn);

        String imsPublicIdentity = udrResponse.get("imsPublicIdentity");
        if (msisdn != null)
            data.putSingle("ImsPublicIdentity", imsPublicIdentity);

        // CS Subscriber Location Info
        // CGI
        String csMcc = udrResponse.get("csMcc");
        if (csMcc != null)
            data.putSingle("MobileCountryCode", csMcc);

        String csMnc = udrResponse.get("csMnc");
        if (csMnc != null)
            data.putSingle("MobileNetworkCode", csMnc);

        String csLac = udrResponse.get("csLac");
        if (csLac != null)
            data.putSingle("LocationAreaCode", csLac);

        String csCi = udrResponse.get("csCi");
        if (csCi != null)
            data.putSingle("CellId", csCi);

        // Location Number
        String locationNumber = udrResponse.get("address");
        if (locationNumber != null)
            data.putSingle("LocationNumberAddress", locationNumber);

        // Subscriber Geographic Location Info
        String csGeogTypeOfShape = udrResponse.get("csGeogTypeOfShape");
        if (csGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", csGeogTypeOfShape);

        String csGeogLatitude = udrResponse.get("csGeogLatitude");
        if (csGeogLatitude != null)
            data.putSingle("DeviceLatitude", csGeogLatitude);

        String csGeogLongitude = udrResponse.get("csGeogLongitude");
        if (csGeogLongitude != null)
            data.putSingle("DeviceLongitude", csGeogLongitude);

        String csGeogUncertainty = udrResponse.get("csGeogUncertainty");
        if (csGeogUncertainty != null)
            data.putSingle("Uncertainty", csGeogUncertainty);

        // Subscriber Geodetic Location Info
        String csGeodTypeOfShape = udrResponse.get("csGeodTypeOfShape");
        if (csGeodTypeOfShape != null)
            data.putSingle("TypeOfShape", csGeodTypeOfShape);

        String csGeodLatitude = udrResponse.get("csGeodLatitude");
        if (csGeodLatitude != null)
            data.putSingle("DeviceLatitude", csGeodLatitude);

        String csGeodLongitude = udrResponse.get("csGeodLongitude");
        if (csGeodLongitude != null)
            data.putSingle("DeviceLongitude", csGeodLongitude);

        String csGeodUncertainty = udrResponse.get("csGeodUncertainty");
        if (csGeodUncertainty != null)
            data.putSingle("Uncertainty", csGeodUncertainty);

        String csGeodConfidence = udrResponse.get("csGeodConfidence");
        if (csGeodConfidence != null)
            data.putSingle("Confidence", csGeodConfidence);

        String csGeodScreeningAndPresentationIndicators = udrResponse.get("csGeodScreeningAndPresentationIndicators");
        if (csGeodScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", csGeodScreeningAndPresentationIndicators);

        String vlrNumber = udrResponse.get("vlrNumber");
        if (vlrNumber != null)
            data.putSingle("NetworkEntityAddress", vlrNumber);

        String mscNumber = udrResponse.get("mscNumber");
        if (mscNumber != null)
            data.putSingle("NetworkEntityAddress", mscNumber);

        String csAgeOfLocationInfo = udrResponse.get("csAgeOfLocationInformation");
        if (csAgeOfLocationInfo != null)
            data.putSingle("LocationAge", csAgeOfLocationInfo);

        String csCurrentLocationRetrieved = udrResponse.get("csCurrentLocationRetrieved");
        if (csCurrentLocationRetrieved != null)
            data.putSingle("CurrentLocationRetrieved", csCurrentLocationRetrieved);

        // PS Subscriber Info
        // RAI
        String raiMcc = udrResponse.get("raiMcc");
        if (raiMcc != null)
            data.putSingle("MobileCountryCode", raiMcc);

        String raiMnc = udrResponse.get("raiMnc");
        if (raiMnc != null)
            data.putSingle("MobileNetworkCode", raiMnc);

        String raiLac = udrResponse.get("raiLac");
        if (raiLac != null)
            data.putSingle("LocationAreaCode", raiLac);

        String rac = udrResponse.get("rac");
        if (rac != null)
            data.putSingle("RoutingAreaCode", rac);

        // CGI
        String psMcc = udrResponse.get("psMcc");
        if (psMcc != null)
            data.putSingle("MobileCountryCode", psMcc);

        String psMnc = udrResponse.get("psMnc");
        if (psMnc != null)
            data.putSingle("MobileNetworkCode", psMnc);

        String psLac = udrResponse.get("psLac");
        if (psLac != null)
            data.putSingle("LocationAreaCode", psLac);

        String psCi = udrResponse.get("psCi");
        if (psCi != null)
            data.putSingle("CellId", psCi);

        // PS Geographic info
        String psGeogTypeOfShape = udrResponse.get("psGeogTypeOfShape");
        if (psGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", psGeogTypeOfShape);

        String psGeogLatitude = udrResponse.get("psGeogLatitude");
        if (psGeogLatitude != null)
            data.putSingle("DeviceLatitude", psGeogLatitude);

        String psGeogLongitude = udrResponse.get("psGeogLongitude");
        if (psGeogLongitude != null)
            data.putSingle("DeviceLongitude", psGeogLongitude);

        String psGeogUncertainty = udrResponse.get("psGeogUncertainty");
        if (psGeogUncertainty != null)
            data.putSingle("Uncertainty", psGeogUncertainty);

        // PS Geodetic info
        String psGeodTypeOfShape = udrResponse.get("psGeodTypeOfShape");
        if (psGeodTypeOfShape != null)
            data.putSingle("TypeOfShape", psGeodTypeOfShape);

        String psGeodLatitude = udrResponse.get("psGeodLatitude");
        if (psGeodLatitude != null)
            data.putSingle("DeviceLatitude", psGeodLatitude);

        String psGeodLongitude = udrResponse.get("psGeodLongitude");
        if (psGeodLongitude != null)
            data.putSingle("DeviceLongitude", psGeodLongitude);

        String psGeodUncertainty = udrResponse.get("psGeodUncertainty");
        if (psGeodUncertainty != null)
            data.putSingle("Uncertainty", psGeodUncertainty);

        String psGeodConfidence = udrResponse.get("psGeodConfidence");
        if (psGeodConfidence != null)
            data.putSingle("Confidence", psGeodConfidence);

        String psGeodScreeningAndPresentationIndicators = udrResponse.get("psGeodScreeningAndPresentationIndicators");
        if (psGeodScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", psGeodScreeningAndPresentationIndicators);

        //SGSN Number
        String sgsnNumber = udrResponse.get("sgsnNumber");
        if (sgsnNumber != null)
            data.putSingle("NetworkEntityAddress", sgsnNumber);

        String psAgeOfLocationInfo = udrResponse.get("psAgeOfLocationInformation");
        if (psAgeOfLocationInfo != null)
            data.putSingle("LocationAge", psAgeOfLocationInfo);

        String psCurrentLocationRetrieved = udrResponse.get("psCurrentLocationRetrieved");
        if (psCurrentLocationRetrieved != null)
            data.putSingle("CurrentLocationRetrieved", psCurrentLocationRetrieved);

        // EPS Location Info
        // TAI
        String taiMcc = udrResponse.get("taiMcc");
        if (taiMcc != null)
            data.putSingle("MobileCountryCode", taiMcc);

        String taiMnc = udrResponse.get("epsMnc");
        if (taiMnc != null)
            data.putSingle("MobileNetworkCode", taiMnc);

        String tac = udrResponse.get("tac");
        if (tac != null)
            data.putSingle("TrackingAreaCode", tac);

        // E-UTRAN CGI
        String epsMcc = udrResponse.get("epsMcc");
        if (epsMcc != null)
            data.putSingle("MobileCountryCode", epsMcc);

        String epsMnc = udrResponse.get("epsMnc");
        if (epsMnc != null)
            data.putSingle("MobileNetworkCode", epsMnc);

        String eNBId = udrResponse.get("eNBId");
        if (eNBId != null)
            data.putSingle("ENodeBId", eNBId);

        String epsCi = udrResponse.get("epsCi");
        if (epsCi != null)
            data.putSingle("CellId", epsCi);

        // EPS Subscriber Geographic Location Info
        String epsGeogTypeOfShape = udrResponse.get("epsGeogTypeOfShape");
        if (epsGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", epsGeogTypeOfShape);

        String epsGeogLatitude = udrResponse.get("epsGeogLatitude");
        if (epsGeogLatitude != null)
            data.putSingle("DeviceLatitude", epsGeogLatitude);

        String epsGeogLongitude = udrResponse.get("epsGeogLongitude");
        if (epsGeogLongitude != null)
            data.putSingle("DeviceLongitude", epsGeogLongitude);

        String epsGeogUncertainty = udrResponse.get("epsGeogUncertainty");
        if (epsGeogUncertainty != null)
            data.putSingle("Uncertainty", epsGeogUncertainty);

        // Subscriber Geodetic Location Info
        String epsGeodTypeOfShape = udrResponse.get("epsGeodTypeOfShape");
        if (epsGeodTypeOfShape != null)
            data.putSingle("TypeOfShape", epsGeodTypeOfShape);

        String epsGeodLatitude = udrResponse.get("epsGeodLatitude");
        if (epsGeodLatitude != null)
            data.putSingle("DeviceLatitude", epsGeodLatitude);

        String epsGeodLongitude = udrResponse.get("epsGeodLongitude");
        if (epsGeodLongitude != null)
            data.putSingle("DeviceLongitude", epsGeodLongitude);

        String epsGeodUncertainty = udrResponse.get("epsGeodUncertainty");
        if (epsGeodUncertainty != null)
            data.putSingle("Uncertainty", epsGeodUncertainty);

        String epsGeodConfidence = udrResponse.get("epsGeodConfidence");
        if (epsGeodConfidence != null)
            data.putSingle("Confidence", epsGeodConfidence);

        String epsGeodScreeningAndPresentationIndicators = udrResponse.get("epsGeodScreeningAndPresentationIndicators");
        if (epsGeodScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", epsGeodScreeningAndPresentationIndicators);

        // MME Name
        String mmeName = udrResponse.get("mmeName");
        if (mmeName != null)
            data.putSingle("NetworkEntityName", mmeName);

        String epsAgeOfLocationInfo = udrResponse.get("epsAgeOfLocationInformation");
        if (epsAgeOfLocationInfo != null)
            data.putSingle("LocationAge", epsAgeOfLocationInfo);

        String epsCurrentLocationRetrieved = udrResponse.get("psCurrentLocationRetrieved");
        if (epsCurrentLocationRetrieved != null)
            data.putSingle("CurrentLocationRetrieved", epsCurrentLocationRetrieved);

        String errorReason = udrResponse.get("errorReason");
        if (errorReason != null) {
            cause = errorReason;
            rStatus = responseStatus.Failed.toString();
            data.putSingle("Cause", cause);
            data.putSingle("ResponseStatus", rStatus);
        }
    }


    /*********************************************/
    // ***   Immediate type of Geolocation   ***//
    /*******************************************/

    @Path("/Immediate/{sid}")
    @DELETE
    public Response deleteImmediateGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, @Context SecurityContext sec) {
        return deleteGeolocation(accountSid, sid, ContextUtil.convert(sec));
    }

    @Path("/Immediate/{sid}")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getImmediateGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, @HeaderParam("Accept") String accept, @Context SecurityContext sec) {
        return getGeolocation(accountSid, sid, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    @Path("/Immediate")
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response putImmediateGeolocationXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept, @Context SecurityContext sec) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Immediate, retrieveMediaType(accept),
                ContextUtil.convert(sec));
    }

    @Path("/Immediate/{sid}")
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response putImmediateGeolocationAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept,
            @Context SecurityContext sec) {
        return updateGeolocation(accountSid, sid, data, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    @Path("/Immediate/{sid}")
    @PUT
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response updateImmediateGeolocationAsXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept,
            @Context SecurityContext sec) {
        return updateGeolocation(accountSid, sid, data, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    /*******************************************/
    // *** Notification type of Geolocation ***//
    /*******************************************/

    @Path("/Notification/{sid}")
    @DELETE
    public Response deleteNotificationGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, @Context SecurityContext sec) {
        return deleteGeolocation(accountSid, sid, ContextUtil.convert(sec));
    }

    @Path("/Notification/{sid}")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getNotificationGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, @HeaderParam("Accept") String accept, @Context SecurityContext sec) {
        return getGeolocation(accountSid, sid, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    @Path("/Notification")
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response putNotificationGeolocationXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept, @Context SecurityContext sec) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Notification, retrieveMediaType(accept),
                ContextUtil.convert(sec));
    }

    @Path("/Notification/{sid}")
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response putNotificationGeolocationAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept,
            @Context SecurityContext sec) {
        return updateGeolocation(accountSid, sid, data, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    @Path("/Notification/{sid}")
    @PUT
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response updateNotificationGeolocationAsXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data, @HeaderParam("Accept") String accept,
            @Context SecurityContext sec) {
        return updateGeolocation(accountSid, sid, data, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getGeolocationsAsXml(@PathParam("accountSid") final String accountSid, @HeaderParam("Accept") String accept,
            @Context SecurityContext sec) {
        return getGeolocations(accountSid, retrieveMediaType(accept), ContextUtil.convert(sec));
    }

}
