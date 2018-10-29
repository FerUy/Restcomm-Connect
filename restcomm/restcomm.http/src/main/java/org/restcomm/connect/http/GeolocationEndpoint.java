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
        Queued("queued"), Sent("sent"), Processing("processing"), Successful("successful"), PartiallySuccessful(
                "partially-successful"), LastKnown(
                        "last-known"), Failed("failed"), Unauthorized("unauthorized"), Rejected("rejected");

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
            String psiService = data.getFirst("PsiService");
            String coreNetwork = data.getFirst("CoreNetwork");
            String httpRespType = data.getFirst("HttpRespType");
            String priority = data.getFirst("Priority");
            String horizontalAccuracy = data.getFirst("HorizontalAccuracy");
            String verticalAccuracy = data.getFirst("VerticalAccuracy");
            String verticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            String responseTimeCategory = data.getFirst("ResponseTime");
            String locationEstimateType = data.getFirst("LocationEstimateType");
            String deferredLocationEventType = data.getFirst("GeofenceEventType");
            String areaType = data.getFirst("GeofenceType");
            String areaId = data.getFirst("GeofenceId");
            String occurrenceInfo = data.getFirst("OccurrenceInfo");
            String lcsReferenceNumber = data.getFirst("ReferenceNumber");
            String lcsServiceTypeID = data.getFirst("ServiceTypeID");
            String intervalTime = data.getFirst("EventIntervalTime");
            String reportingAmount = data.getFirst("EventReportingAmount");
            String reportingInterval = data.getFirst("EventReportingInterval");
            String lcsClientNameString = data.getFirst("ClientName");
            String lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
            String lcsClientType = data.getFirst("ClientType");
            String callbackUrl = data.getFirst("StatusCallback");
            URIBuilder uriBuilder = new URIBuilder(gmlcURI);
            uriBuilder.addParameter("msisdn", targetMSISDN);

            if (coreNetwork != null)
                uriBuilder.addParameter("coreNetwork", coreNetwork);

            if (psiService != null)
                uriBuilder.addParameter("psiService", psiService);

            if (httpRespType != null)
                uriBuilder.addParameter("httpRespType", httpRespType);

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
                    uriBuilder.addParameter("vertCoordinateRequest", verticalCoordinateRequest);
                }
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (verticalCoordinateRequest.equalsIgnoreCase("false"))
                        verticalCoordinateRequest = "0";
                    if (verticalCoordinateRequest.equalsIgnoreCase("true"))
                        verticalCoordinateRequest = "1";
                    uriBuilder.addParameter("vertCoordinateRequest", verticalCoordinateRequest);
                }
            }

            if (responseTimeCategory != null  && coreNetwork != null)
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

            if (lcsReferenceNumber != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("lcsReferenceNumber", lcsReferenceNumber);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("slgLcsReferenceNumber", lcsReferenceNumber);
                }
            }

            if (lcsServiceTypeID != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("lcsServiceTypeID", lcsServiceTypeID);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsServiceTypeId", lcsServiceTypeID);
                }
            }

            if (intervalTime != null && coreNetwork != null)
                if (coreNetwork.equalsIgnoreCase("UMTS")) {
                    uriBuilder.addParameter("intervalTime", intervalTime);
                } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                    uriBuilder.addParameter("lcsAreaEventIntervalTime", intervalTime);
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

            if (lcsClientType != null && coreNetwork != null) {
                if (coreNetwork.equalsIgnoreCase("LTE")) {
                    if (lcsClientType.equalsIgnoreCase("emergency"))
                        lcsClientType = "0";
                    if (lcsClientType.equalsIgnoreCase("vas"))
                        lcsClientType = "1";
                    if (lcsClientType.equalsIgnoreCase("operator"))
                        lcsClientType = "2";
                    if (lcsClientType.equalsIgnoreCase("lawful"))
                        lcsClientType = "3";
                    uriBuilder.addParameter("slgClientType", lcsClientType);
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
            logger.info("\ncURL URL: " + url);
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
                        stream = entity.getContent();
                        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                        while (null != (gmlcResponse = br.readLine())) {
                            List<String> items = Arrays.asList(gmlcResponse.split("\\s*,\\s*"));
                            if (logger.isInfoEnabled()) {
                                logger.info("Data retrieved from GMLC via MAP ATI: " + items.toString());
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
                                        HashMap<String, String> sriPslResponse = parsePslJsonString(gmlcResponse);
                                        logger.info("Data retrieved from GMLC via MAP SRIforLCS-PSL: " + gmlcResponse);

                                        lcsReferenceNumber = sriPslResponse.get("lcsReferenceNumber");
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

                                        /*String gprsNodeIndicator = sriPslResponse.get("gprsNodeIndicator");
                                        String deferredMTLRresponseIndicator = sriPslResponse.get("deferredMTLRresponseIndicator");
                                        String geranPositioningInfo = sriPslResponse.get("geranPositioningInfo");
                                        String geranGanssPositioningData = sriPslResponse.get("geranGanssPositioningData");
                                        String utranPositioningInfo = sriPslResponse.get("utranPositioningInfo");
                                        String utranGanssPositioningData = sriPslResponse.get("utranGanssPositioningData");
                                        String velocityType = sriPslResponse.get("velocityType");*/

                                    } else if (coreNetwork.equalsIgnoreCase("LTE")) {
                                        HashMap<String, String> rirPlrResponse = parsePlrJsonString(gmlcResponse);
                                        logger.info("Data retrieved from GMLC via Diameter RIR/RIA-PLR/PLA: " + gmlcResponse);

                                        lcsReferenceNumber = rirPlrResponse.get("lcsReferenceNumber");
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

                                        String ci = rirPlrResponse.get("cellGlobalIdentity");
                                        if (ci != null)
                                            data.putSingle("CellId", ci);

                                        String sai = rirPlrResponse.get("serviceAreaIdentity");
                                        if (sai != null)
                                            data.putSingle("Sai", sai);

                                        String eUtranCgi = rirPlrResponse.get("eUtranCgi");
                                        if (eUtranCgi != null)
                                            data.putSingle("LteCellId", eUtranCgi);

                                        String civicAddress = rirPlrResponse.get("civicAddress");
                                        if (civicAddress != null)
                                            data.putSingle("CivicAddress", civicAddress);

                                        String barometricPressure = rirPlrResponse.get("barometricPressure");
                                        if (barometricPressure != null)
                                            data.putSingle("BarometricPressure", barometricPressure);

                                    }
                                }
                                if (psiService == null && !geolocationType.toString().equals(NotificationGT)) {
                                    logger.info("Data retrieved from GMLC via MAP ATI: " + gmlcResponse);
                                    HashMap<String, String> atiResponse = parseAtiJsonString(gmlcResponse);

                                    String mcc = atiResponse.get("mcc");
                                    if (mcc != null)
                                        data.putSingle("MobileCountryCode", mcc);

                                    String mnc = atiResponse.get("mnc");
                                    if (mnc != null)
                                        data.putSingle("MobileNetworkCode", mnc);

                                    String lac = atiResponse.get("lac");
                                    if (lac != null)
                                        data.putSingle("LocationAreaCode", lac);

                                    String ci = atiResponse.get("ci");
                                    if (ci != null)
                                        data.putSingle("CellId", ci);

                                    String aol = atiResponse.get("aol");
                                    if (aol != null)
                                        data.putSingle("LocationAge", aol);

                                    String vlrNumber = atiResponse.get("vlrNumber");
                                    if (vlrNumber != null)
                                        data.putSingle("NetworkEntityAddress", vlrNumber);

                                    String subscriberState = atiResponse.get("subscriberState");
                                    if (subscriberState != null)
                                        data.putSingle("SubscriberState", subscriberState);

                                } else if (psiService != null) {
                                    if (psiService.equalsIgnoreCase("true") && !geolocationType.toString().equals(NotificationGT)) {
                                        logger.info("Data retrieved from GMLC via MAP PSI: " + gmlcResponse);
                                        HashMap<String, String> psiResponse = parsePsiJsonString(gmlcResponse);

                                        String imsi = psiResponse.get("imsi");
                                        if (imsi != null)
                                            data.putSingle("IMSI", imsi);

                                        String imei = psiResponse.get("imei");
                                        if (imei != null)
                                            data.putSingle("IMEI", imei);

                                        String lmsi = psiResponse.get("lmsi");
                                        if (lmsi != null)
                                            data.putSingle("LMSI", lmsi);

                                        String subscriberState = psiResponse.get("subscriberState");
                                        if (subscriberState != null)
                                            data.putSingle("SubscriberState", subscriberState);

                                        // Subscriber GPRS Location Info
                                        // GPRS CGI
                                        String mcc = psiResponse.get("gprsMcc");
                                        if (mcc != null)
                                            data.putSingle("MobileCountryCode", mcc);

                                        String mnc = psiResponse.get("gprsMnc");
                                        if (mnc != null)
                                            data.putSingle("MobileNetworkCode", mnc);

                                        String lac = psiResponse.get("gprsLac");
                                        if (lac != null)
                                            data.putSingle("LocationAreaCode", lac);

                                        String ci = psiResponse.get("gprsCi");
                                        if (ci != null)
                                            data.putSingle("CellId", ci);

                                        // GPRS Geographic info
                                        String latitude = psiResponse.get("gprsLatitude");
                                        if (latitude != null)
                                            data.putSingle("DeviceLatitude", latitude);

                                        String longitude = psiResponse.get("gprsLongitude");
                                        if (longitude != null)
                                            data.putSingle("DeviceLongitude", longitude);

                                        String typeOfShape = psiResponse.get("gprsTypeOfShape");
                                        if (typeOfShape != null)
                                            data.putSingle("TypeOfShape", typeOfShape);

                                        String uncertainty = psiResponse.get("gprsUncertainty");
                                        if (uncertainty != null)
                                            data.putSingle("Uncertainty", uncertainty);

                                        // GPRS Geodetic info
                                        latitude = psiResponse.get("gprsGeodLatitude");
                                        if (latitude != null)
                                            data.putSingle("DeviceLatitude", latitude);

                                        longitude = psiResponse.get("gprsGeodLongitude");
                                        if (longitude != null)
                                            data.putSingle("DeviceLongitude", longitude);

                                        typeOfShape = psiResponse.get("gprsGeodTypeOfShape");
                                        if (typeOfShape != null)
                                            data.putSingle("TypeOfShape", typeOfShape);

                                        uncertainty = psiResponse.get("gprsGeodUncertainty");
                                        if (uncertainty != null)
                                            data.putSingle("Uncertainty", uncertainty);

                                        String confidence = psiResponse.get("gprsConfidence");
                                        if (confidence != null)
                                            data.putSingle("Confidence", confidence);

                                        String screeningAndPresInd = psiResponse.get("gprsScreeningAndPresentationIndicators");
                                        if (screeningAndPresInd != null)
                                            data.putSingle("ScreeningAndPresentationIndicators", screeningAndPresInd);

                                        // GPRS Location info
                                        String sgsnNumber = psiResponse.get("sgsnNumber");
                                        if (sgsnNumber != null)
                                            data.putSingle("NetworkEntityAddress", sgsnNumber);

                                        String lsaId = psiResponse.get("lsaId");
                                        if (lsaId != null)
                                            data.putSingle("LSAId", lsaId);

                                        String routeingAreaId = psiResponse.get("routeingAreaId");
                                        if (routeingAreaId != null)
                                            data.putSingle("RouteingAreaId", routeingAreaId);

                                        // Subscriber Location Info
                                        // CGI
                                        mcc = psiResponse.get("mcc");
                                        if (mcc != null)
                                            data.putSingle("MobileCountryCode", mcc);

                                        mnc = psiResponse.get("mnc");
                                        if (mnc != null)
                                            data.putSingle("MobileNetworkCode", mnc);

                                        lac = psiResponse.get("lac");
                                        if (lac != null)
                                            data.putSingle("LocationAreaCode", lac);

                                        ci = psiResponse.get("ci");
                                        if (ci != null)
                                            data.putSingle("CellId", ci);

                                        // Subscriber Location info
                                        String locationNumber = psiResponse.get("locationNumber");
                                        if (locationNumber != null)
                                            data.putSingle("LocationNumber", locationNumber);

                                        String aol = psiResponse.get("aol");
                                        if (aol != null)
                                            data.putSingle("LocationAge", aol);

                                        String vlrNumber = psiResponse.get("vlrNumber");
                                        if (vlrNumber != null)
                                            data.putSingle("NetworkEntityAddress", vlrNumber);

                                        String mscNumber = psiResponse.get("mscNumber");
                                        if (mscNumber != null)
                                            data.putSingle("NetworkEntityAddress", mscNumber);

                                        // Subscriber Geographic Location Info
                                        latitude = psiResponse.get("latitude");
                                        if (latitude != null)
                                            data.putSingle("DeviceLatitude", latitude);

                                        longitude = psiResponse.get("longitude");
                                        if (longitude != null)
                                            data.putSingle("DeviceLongitude", longitude);

                                        typeOfShape = psiResponse.get("typeOfShape");
                                        if (typeOfShape != null)
                                            data.putSingle("TypeOfShape", typeOfShape);

                                        uncertainty = psiResponse.get("uncertainty");
                                        if (uncertainty != null)
                                            data.putSingle("Uncertainty", uncertainty);

                                        // Subscriber Geodetic Location Info
                                        latitude = psiResponse.get("geodLatitude");
                                        if (latitude != null)
                                            data.putSingle("DeviceLatitude", latitude);

                                        longitude = psiResponse.get("geodLongitude");
                                        if (longitude != null)
                                            data.putSingle("DeviceLongitude", longitude);

                                        typeOfShape = psiResponse.get("geodTypeOfShape");
                                        if (typeOfShape != null)
                                            data.putSingle("TypeOfShape", typeOfShape);

                                        uncertainty = psiResponse.get("geodUncertainty");
                                        if (uncertainty != null)
                                            data.putSingle("Uncertainty", uncertainty);

                                        confidence = psiResponse.get("confidence");
                                        if (confidence != null)
                                            data.putSingle("Confidence", confidence);

                                        screeningAndPresInd = psiResponse.get("screeningAndPresentationIndicators");
                                        if (screeningAndPresInd != null)
                                            data.putSingle("ScreeningAndPresentationIndicators", screeningAndPresInd);

                                        String currentLocationRetrieved = psiResponse.get("currentLocationRetrieved");
                                        if (currentLocationRetrieved != null)
                                            data.putSingle("CurrentLocationRetrieved", currentLocationRetrieved);

                                        // EPS Location Info
                                        String mmeName = psiResponse.get("mmeName");
                                        if (mmeName != null)
                                            data.putSingle("NetworkEntityName", mmeName);

                                        String eUtranCgi = psiResponse.get("eUtranCgi");
                                        if (eUtranCgi != null)
                                            data.putSingle("LteCellId", eUtranCgi);

                                        String trackingAreaId = psiResponse.get("trackingAreaId");
                                        if (trackingAreaId != null)
                                            data.putSingle("TrackingAreaCode", trackingAreaId);

                                        // MNP Stastus Info
                                        String mnpStatus = psiResponse.get("mnpStatus");
                                        if (mnpStatus != null)
                                            data.putSingle("MNPStatus", mnpStatus);

                                        String mnpMsisdn = psiResponse.get("mnpMsisdn");
                                        if (mnpMsisdn != null)
                                            data.putSingle("MNPMSISDN", mnpMsisdn);

                                        String mnpImsi = psiResponse.get("mnpImsi");
                                        if (mnpImsi != null)
                                            data.putSingle("MNPIMSI", mnpImsi);

                                        String mnpRouteingNumber = psiResponse.get("mnpRouteingNumber");
                                        if (mnpRouteingNumber != null)
                                            data.putSingle("MNPRouteingNumber", mnpRouteingNumber);

                                    }
                                }
                            }
                        }
                    }
                    if (gmlcURI != null && gmlcResponse != null) {
                        // For debugging/logging purposes only
                        if (logger.isInfoEnabled()) {
                            logger.info("Geolocation data of " + targetMSISDN + " retrieved from GMLC at: " + gmlcURI);
                            logger.info("\nDevice Identifier = " + data.getFirst("DeviceIdentifier"));
                            logger.info("\nMSISDN = " + getLong("MSISDN", data));
                            logger.info("\nIMSI = " + getLong("IMSI", data));
                            logger.info("\nIMEI = " + data.getFirst("IMEI"));
                            logger.info("\nLMSI = " + getLong("LMSI", data));
                            logger.info("\nMCC = " + getInteger("MobileCountryCode", data));
                            logger.info("\nMNC = " + data.getFirst("MobileNetworkCode"));
                            logger.info("\nLAC  = " + data.getFirst("LocationAreaCode"));
                            logger.info("\nCI = " + data.getFirst("CellId"));
                            logger.info("\nSAI = " + data.getFirst("Sai"));
                            logger.info("\nECID = " + getLong("LteCellId", data));
                            logger.info("\nAOL = " + getInteger("LocationAge", data));
                            logger.info("\nSubscriber State = " + data.getFirst("SubscriberState"));
                            logger.info("\nNetwork Entity Address = " + getLong("NetworkEntityAddress", data));
                            logger.info("\nNetwork Entity Name = " + data.getFirst("NetworkEntityName"));
                            logger.info("\nTracking Area Code = " + data.getFirst("TrackingAreaCode"));
                            logger.info("\nRouteing Area Id = " + data.getFirst("RouteingAreaId"));
                            logger.info("\nType of Shape = " + data.getFirst("TypeOfShape"));
                            logger.info("\nDevice Latitude = " + data.getFirst("DeviceLatitude"));
                            logger.info("\nDevice Longitude = " + data.getFirst("DeviceLongitude"));
                            logger.info("\nUncertainty = " + data.getFirst("Uncertainty"));
                            logger.info("\nUncertainty Semi Major Axis = " + data.getFirst("UncertaintySemiMajorAxis"));
                            logger.info("\nUncertainty Semi Minor Axis = " + data.getFirst("UncertaintySemiMinorAxis"));
                            logger.info("\nAngle Of Major Axis = " + data.getFirst("AngleOfMajorAxis"));
                            logger.info("\nConfidence = " + data.getFirst("Confidence"));
                            logger.info("\nDevice Altitude = " + data.getFirst("DeviceAltitude"));
                            logger.info("\nUncertaintyAltitude = " + data.getFirst("UncertaintyAltitude"));
                            logger.info("\nInner Radius = " + data.getFirst("InnerRadius"));
                            logger.info("\nUncertainty Inner Radius = " + data.getFirst("UncertaintyInnerRadius"));
                            logger.info("\nOffset Angle = " + data.getFirst("OffsetAngle"));
                            logger.info("\nIncluded Angle = " + data.getFirst("IncludedAngle"));
                            logger.info("\nHorizontal Speed = " + data.getFirst("HorizontalSpeed"));
                            logger.info("\nVertical Speed = " + data.getFirst("VerticalSpeed"));
                            logger.info("\nUncertainty Horizontal Speed = " + data.getFirst("UncertaintyHorizontalSpeed"));
                            logger.info("\nUncertainty Vertical Speed = " + data.getFirst("UncertaintyVerticalSpeed"));
                            logger.info("\nBearing = " + data.getFirst("Bearing"));
                            logger.info("\nCivic Address = " + data.getFirst("CivicAddress"));
                            logger.info("\nBarometric Pressure = " + getLong("BarometricPressure", data));
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
            throw new NullPointerException("Geolocation Type can not be null, but either \"Immediate\" or \"Notification\".");
        }

        /*** DeviceIdentifier can not be null ***/
        if (!data.containsKey("DeviceIdentifier")) {
            throw new NullPointerException("DeviceIdentifier value can not be null");
        }

        /*** StatusCallback can not be null ***/
        if (!data.containsKey("StatusCallback")) {
            throw new NullPointerException("StatusCallback value can not be null");
        }

        /*** CoreNetwork must not be null or different than GSM or LTE for Notification type of Geolocation ***/
        if (!data.containsKey("CoreNetwork") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("CoreNetwork value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("CoreNetwork") && glType.toString().equals(NotificationGT)) {
            String coreNetwork = data.getFirst("CoreNetwork");
            if (!coreNetwork.equalsIgnoreCase("umts") && !coreNetwork.equalsIgnoreCase("lte")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("CoreNetwork value not API compliant, must be UMTS or LTE for Notification type of Geolocation");
            }
        }

        /*** Priority must be API compliant: normal or high for Notification type of Geolocation only ***/
        if (data.containsKey("Priority") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Priority only applies for Notification type of Geolocation");
        }
        if (data.containsKey("Priority") && glType.toString().equals(NotificationGT)) {
            String priority = data.getFirst("Priority");
            if (!priority.equalsIgnoreCase("normal") && !priority.equalsIgnoreCase("high")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Priority value not API compliant, must be normal or high");
            }
        }

        /*** HorizontalAccuracy must be API compliant: a positive integer value for Notification type of Geolocation only ***/
        if (data.containsKey("HorizontalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("HorizontalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("HorizontalAccuracy") && glType.toString().equals(NotificationGT)) {
            Integer horizontalAccuracy = Integer.valueOf(data.getFirst("HorizontalAccuracy"));
            try {
                if (horizontalAccuracy > Integer.MAX_VALUE || horizontalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("HorizontalAccuracy value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("HorizontalAccuracy value not API compliant, must be a positive integer value");
            }
        }

        /*** VerticalAccuracy must be API compliant: a positive integer value for Notification type of Geolocation only***/
        if (data.containsKey("VerticalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("VerticalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalAccuracy") && glType.toString().equals(NotificationGT)) {
            Integer verticalAccuracy = Integer.valueOf(data.getFirst("VerticalAccuracy"));
            try {
                if (verticalAccuracy > Integer.MAX_VALUE || verticalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("VerticalAccuracy value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("VerticalAccuracy value not API compliant, must be a positive integer value");
            }
        }

        /*** VerticalCoordinateRequest must be API compliant: a boolean value for Notification type of Geolocation only***/
        if (data.containsKey("VerticalCoordinateRequest") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("VerticalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalCoordinateRequest") && glType.toString().equals(NotificationGT)) {
            String verticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            if (!verticalCoordinateRequest.equalsIgnoreCase("true") && !verticalCoordinateRequest.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("VerticalCoordinateRequest value not API compliant, must be true or false");
            }
        }

        /*** ResponseTime must be API compliant: fast or slow for Notification type of Geolocation***/
        if (data.containsKey("ResponseTime") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("ResponseTime only applies for Notification type of Geolocation");
        }
        if (data.containsKey("ResponseTime") && glType.toString().equals(NotificationGT)) {
            String priority = data.getFirst("ResponseTime");
            if (!priority.equalsIgnoreCase("low") && !priority.equalsIgnoreCase("tolerant")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("ResponseTime value not API compliant, must be low or tolerant");
            }
        }

        /*** LocationEstimateType must be API compliant: fast or slow for Notification type of Geolocation only ***/
        if (data.containsKey("LocationEstimateType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("LocationEstimateType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("LocationEstimateType value can not be null for Notification type of Geolocation");
        }
        if (data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            String locationEstimateType = data.getFirst("LocationEstimateType");
            if (!locationEstimateType.equalsIgnoreCase("lastKnown") && !locationEstimateType.equalsIgnoreCase("initial")
                && !locationEstimateType.equalsIgnoreCase("current") && !locationEstimateType.equalsIgnoreCase("activateDeferred")
                && !locationEstimateType.equalsIgnoreCase("cancelDeferred") && !locationEstimateType.equalsIgnoreCase("notificationVerificationOnly")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("LocationEstimateType value not API compliant, must be lastKnown, initial, current, activateDeferred," +
                    " cancelDeferred or notificationVerificationOnly");
            }
        }

        /*** ClientName must be API compliant: not null for Notification type of Geolocation for LTE ***/
        if (data.containsKey("ClientName") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("ClientName only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientName") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                throw new NullPointerException("ClientName value can not be null for Notification type of Geolocation in LTE");
            }
        }

        /*** ClientNameFormat must be API compliant: name, email, msisdn, url or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientNameFormat") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("ClientNameFormat only applies for Notification type of Geolocation in LTE");
        }
        if (!data.containsKey("ClientNameFormat") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                throw new NullPointerException("ClientNameFormat value can not be null for Notification type of Geolocation in LTE");
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
                    throw new IllegalArgumentException("ClientNameFormat value not API compliant, must be name, email, msisdn, url or sip " +
                        "for Notification type of Geolocation in LTE");
                }
            }
        }

        /*** ClientType must be API compliant: emergency, vas, operator, lawful or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("ClientType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                throw new NullPointerException("ClientType value can not be null for Notification type of Geolocation in LTE");
            }
        }
        if (data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                String locationEstimateType = data.getFirst("ClientType");
                if (!locationEstimateType.equalsIgnoreCase("emergency") && !locationEstimateType.equalsIgnoreCase("vas")
                    && !locationEstimateType.equalsIgnoreCase("operator") && !locationEstimateType.equalsIgnoreCase("lawful")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("ClientType value not API compliant, must be emergency, vas, operator or lawful " +
                        "for Notification type of Geolocation in LTE");
                }
            }
        }


        /*** DeviceLatitude must be API compliant***/
        if (data.containsKey("DeviceLatitude")) {
            String deviceLat = data.getFirst("DeviceLatitude");
            Boolean devLatWGS84 = validateGeoCoordinatesFormat(deviceLat);
            if (!devLatWGS84) {
                throw new IllegalArgumentException("DeviceLatitude not API compliant");
            }
        }

        /*** DeviceLongitude must be API compliant ***/
        if (data.containsKey("DeviceLongitude")) {
            String deviceLong = data.getFirst("DeviceLongitude");
            Boolean devLongWGS84 = validateGeoCoordinatesFormat(deviceLong);
            if (!devLongWGS84) {
                throw new IllegalArgumentException("DeviceLongitude not API compliant");
            }
        }

        /*** GeofenceType must belong to to Notification type of Geolocation and API compliant ***/
        if (data.containsKey("GeofenceType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceType") && glType.toString().equals(NotificationGT)) {
            String eventGeofenceType = data.getFirst("GeofenceType");
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("LTE")) {
                if (!eventGeofenceType.equalsIgnoreCase("locationAreaId") && !eventGeofenceType.equalsIgnoreCase("cellGlobalId")
                    && !eventGeofenceType.equalsIgnoreCase("countryCode") && !eventGeofenceType.equalsIgnoreCase("plmnId")
                    && !eventGeofenceType.equalsIgnoreCase("routingAreaId") && !eventGeofenceType.equalsIgnoreCase("utranCellId")
                    && !eventGeofenceType.equalsIgnoreCase("trackingAreaId") && !eventGeofenceType.equalsIgnoreCase("eUtranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("GeofenceType value not API compliant, " +
                        "must be one of locationAreaId, cellGlobalId, countryCode, plmnId, routingAreaId, utranCellId, trackingAreaId or eUtranCellId " +
                        "for Notification type of Geolocation in LTE");
                }
            } else if (network.equalsIgnoreCase("UMTS")) {
                if (!eventGeofenceType.equalsIgnoreCase("locationAreaId") && !eventGeofenceType.equalsIgnoreCase("cellGlobalId")
                    && !eventGeofenceType.equalsIgnoreCase("countryCode") && !eventGeofenceType.equalsIgnoreCase("plmnId")
                    && !eventGeofenceType.equalsIgnoreCase("routingAreaId") && !eventGeofenceType.equalsIgnoreCase("utranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("GeofenceType value not API compliant, " +
                        "must be one of locationAreaId, cellGlobalId, countryCode, plmnId, routingAreaId or utranCellId " +
                        "for Notification type of Geolocation in UMTS");
                }
            }

        }


        /*** GeofenceId must belong to Notification type of Geolocation only ***/
        if (data.containsKey("GeofenceId") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceId only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceId") && !glType.toString().equals(NotificationGT)) {
            Long geofenceId = Long.valueOf(data.getFirst("GeofenceId"));
            try {
                if (geofenceId > Long.MAX_VALUE || geofenceId < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("GeofenceId value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("GeofenceId value not API compliant, must be a positive integer value");
            }
        }

        /*** GeofenceEventType must belong to Notification type of Geolocation and API compliant ***/
        if (data.containsKey("GeofenceEventType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceEventType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("GeofenceEventType") && glType.toString().equals(NotificationGT)) {
            String eventGeofenceType = data.getFirst("GeofenceEventType");
            String network = data.getFirst("CoreNetwork");
            if (network.equalsIgnoreCase("UMTS")) {
                if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                    && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("GeofenceEventType value not API compliant, " +
                        "must be one of available, inside, entering or leaving for " +
                        "Notification type of Geolocation in UMTS");
                }
            } else if (network.equalsIgnoreCase("LTE")) {
                if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                    && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")
                    && !eventGeofenceType.equalsIgnoreCase("periodic-ldr") && !eventGeofenceType.equalsIgnoreCase("motion-event")
                    && !eventGeofenceType.equalsIgnoreCase("ldr-activated") && !eventGeofenceType.equalsIgnoreCase("max-interval-expiration")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("GeofenceEventType value not API compliant, " +
                        "must be one of available, inside, entering, leaving, periodic-ldr, motion-event, ldr-activated or " +
                        "max-interval-expiration for Notification type of Geolocation in LTE");
                }
            }
        }

        /*** EventRange must belong to Notification type of Geolocation only ***/
        if (data.containsKey("EventRange") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventRange only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventRange") && !glType.toString().equals(NotificationGT)) {
            Long eventRange = Long.valueOf(data.getFirst("EventRange"));
            try {
                if (eventRange > Long.MAX_VALUE || eventRange < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("EventRange value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("EventRange value not API compliant, must be a positive integer value");
            }
        }

        /*** OccurrenceInfo ***/
        if (data.containsKey("OccurrenceInfo") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceEventType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("OccurrenceInfo") && glType.toString().equals(NotificationGT)) {
            String occurrenceInfo = data.getFirst("OccurrenceInfo");
            if (!occurrenceInfo.equalsIgnoreCase("once") && !occurrenceInfo.equalsIgnoreCase("multiple")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("OccurrenceInfo value not API compliant, " +
                    "must be one of once (for one time event) or multiple (for multiple time event)");
            }
        }

        /*** ServiceTypeID must belong to Notification type of Geolocation only and API compliant ***/
        if (data.containsKey("ServiceTypeID") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("ServiceTypeID only applies for Notification type of Geolocation");
        }
        if (data.containsKey("ServiceTypeID") && glType.toString().equals(NotificationGT)) {
            Long lcsServiceTypeId = Long.valueOf(data.getFirst("ServiceTypeID"));
            try {
                if (lcsServiceTypeId > Long.MAX_VALUE || lcsServiceTypeId < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("ServiceTypeID value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("ServiceTypeID value not API compliant, must be a positive integer value");
            }
        }


        /*** EventIntervalTime must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("EventIntervalTime") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventIntervalTime only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventIntervalTime") && glType.toString().equals(NotificationGT)) {
            Long eventIntervalTime = Long.valueOf(data.getFirst("EventIntervalTime"));
            try {
                if (eventIntervalTime > 32767 || eventIntervalTime < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("EventIntervalTime value not API compliant, must be a positive integer value not greater than 32767");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("EventIntervalTime value not API compliant, must be a positive integer value not greater than 32767");
            }
        }

        /*** EventReportingAmount must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("EventReportingAmount") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventReportingAmount only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventReportingAmount") && glType.toString().equals(NotificationGT)) {
            Long eventReportingAmount = Long.valueOf(data.getFirst("EventReportingAmount"));
            try {
                if (eventReportingAmount > 8639999 || eventReportingAmount < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("EventReportingAmount value not API compliant, must be a positive integer value not greater than 8639999");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("EventReportingAmount value not API compliant, must be a positive integer value not greater than 8639999");
            }
        }

        /*** EventReportingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("EventReportingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventReportingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("EventReportingInterval") && glType.toString().equals(NotificationGT)) {
            Long eventReportingInterval = Long.valueOf(data.getFirst("EventReportingInterval"));
            try {
                if (eventReportingInterval > 8639999 || eventReportingInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("EventReportingInterval value not API compliant, must be a positive integer value not greater than 8639999");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("EventReportingInterval value not API compliant, must be a positive integer value not greater than 8639999");
            }
        }

        /*** LocationTimestamp must be API compliant: DateTime format only ***/
        try {
            if (data.containsKey("LocationTimestamp")) {
                @SuppressWarnings("unused")
                DateTime locationTimestamp = getDateTime("LocationTimestamp", data);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("LocationTimestamp value is not API compliant");
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
        builder.setResponseStatus(data.getFirst("ResponseStatus"));
        builder.setCause(data.getFirst("Cause"));
        builder.setMobileCountryCode(getInteger("MobileCountryCode", data));
        builder.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        builder.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        builder.setCellId(data.getFirst("CellId"));
        builder.setSai(data.getFirst("Sai"));
        builder.setEcid(getLong("LteCellId", data));
        builder.setAgeOfLocationInfo(getInteger("LocationAge", data));
        builder.setSubscriberState(data.getFirst("SubscriberState"));
        builder.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        builder.setNetworkEntityName(data.getFirst("NetworkEntityName"));
        builder.setTac(data.getFirst("TrackingAreaCode"));
        builder.setRai(data.getFirst("RouteingAreaId"));
        builder.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        builder.setTypeOfShape(data.getFirst("TypeOfShape"));
        builder.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        builder.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        builder.setUncertainty(data.getFirst("Uncertainty"));
        builder.setUncertaintySemiMajorAxis(data.getFirst("UncertaintySemiMajorAxis"));
        builder.setUncertaintySemiMinorAxis(data.getFirst("UncertaintySemiMinorAxis"));
        builder.setAngleOfMajorAxis(data.getFirst("AngleOfMajorAxis"));
        builder.setConfidence(data.getFirst("Confidence"));
        builder.setAltitude(data.getFirst("DeviceAltitude"));
        builder.setUncertaintyAltitude(data.getFirst("UncertaintyAltitude"));
        builder.setInnerRadius(data.getFirst("InnerRadius"));
        builder.setUncertaintyInnerRadius(data.getFirst("UncertaintyInnerRadius"));
        builder.setOffsetAngle(data.getFirst("OffsetAngle"));
        builder.setIncludedAngle(data.getFirst("IncludedAngle"));
        builder.setHorizontalSpeed(data.getFirst("HorizontalSpeed"));
        builder.setVerticalSpeed(data.getFirst("VerticalSpeed"));
        builder.setUncertaintyHorizontalSpeed(data.getFirst("UncertaintyHorizontalSpeed"));
        builder.setUncertaintyVerticalSpeed(data.getFirst("UncertaintyVerticalSpeed"));
        builder.setBearing(data.getFirst("Bearing"));
        builder.setGeofenceType(data.getFirst("GeofenceType"));
        builder.setGeofenceId(data.getFirst("GeofenceId"));
        builder.setGeofenceEventType(data.getFirst("GeofenceEventType"));
        builder.setEventRange(getLong("EventRange", data));
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
        // *** Set of parameters with provided data for Geolocation update***//
        if (data.containsKey("Source")) {
            updatedGeolocation = updatedGeolocation.setSource(data.getFirst("Source"));
        }

        if (data.containsKey("DeviceIdentifier")) {
            updatedGeolocation = updatedGeolocation.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        }

        if (data.containsKey("MSISDN")) {
            String msisdn = data.getFirst("MSISDN");
            try {
                if (msisdn.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("MSISDN amount of digits must not be greater than 15");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("MSISDN must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setMsisdn(getLong("MSISDN", data));
        }

        if (data.containsKey("IMSI")) {
            String imsi = data.getFirst("IMSI");
            try {
                if (imsi.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("IMSI amount of digits must not be greater than 15");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("IMSI must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setImsi(getLong("IMSI", data));
        }

        if (data.containsKey("IMEI")) {
            String imei = data.getFirst("IMEI");
            try {
                if (imei.length() > 15) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("IMEI amount of digits must not be greater than 15");
                }
            } catch (Exception e) {
                httpBadRequest = true;
                throw new IllegalArgumentException("IMEI must be a number with an amount of digits not greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setImei(data.getFirst("IMEI"));
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
                    throw new IllegalArgumentException("MobileCountryCode amount of digits must not be greater than 3");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("MobileCountryCode must be a number with an amount of digits not greater than 3");
            }
            updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }

        if (data.containsKey("MobileNetworkCode")) {
            String mnc = data.getFirst("MobileNetworkCode");
            if (mnc.length() > 3) {
                httpBadRequest = true;
                throw new IllegalArgumentException("MobileNetworkCode amount of digits must not be greater than 3");
            } else {
                updatedGeolocation = updatedGeolocation.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));

            }
        }

        if (data.containsKey("LocationAreaCode")) {
            String lac = data.getFirst("LocationAreaCode");
            Integer digits = Integer.valueOf(lac);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("LocationAreaCode must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("LocationAreaCode must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        }

        if (data.containsKey("CellId")) {
            String ci = data.getFirst("CellId");
            Long digits = Long.valueOf(ci);
            try {
                if (digits > 268435455) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("CellId must be a number not greater than 268435455");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("CellId must be a number not greater than 268435455");
            }
            updatedGeolocation = updatedGeolocation.setCellId(data.getFirst("CellId"));
        }

        if (data.containsKey("Sai")) {
            String sai = data.getFirst("SZai");
            Long digits = Long.valueOf(sai);
            try {
                if (digits > 268435455) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("SAI must be a number not greater than 268435455");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("SAI must be a number not greater than 268435455");
            }
            updatedGeolocation = updatedGeolocation.setSai(data.getFirst("Sai"));
        }

        if (data.containsKey("LteCellId")) {
            String ecid = data.getFirst("LteCellId");
            Long digits = Long.valueOf(ecid);
            try {
                if (digits > 268435455) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("LteCellId must be a number not greater than 268435455");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("LteCellId must be a number not greater than 268435455");
            }
            updatedGeolocation = updatedGeolocation.setCellId(data.getFirst("LteCellId"));
        }

        if (data.containsKey("NetworkEntityAddress")) {
            String gt = data.getFirst("NetworkEntityAddress");
            Long digits = Long.valueOf(gt);
            try {
                if (digits > Long.MAX_VALUE) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("NetworkEntityAddress must be a number compliant with Long data type");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("NetworkEntityAddress must be a number compliant with Long data type");
            }
            updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        }

        if (data.containsKey("NetworkEntityName")) {
            String entityName = data.getFirst("NetworkEntityName");
            if (entityName.length() > 20) {
                httpBadRequest = true;
                throw new IllegalArgumentException("NetworkEntityName length must not be greater than 20 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
            }
        }

        if (data.containsKey("LocationAge")) {
            String aol = data.getFirst("LocationAge");
            Integer digits = Integer.valueOf(aol);
            try {
                if (digits > Long.MAX_VALUE) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("LocationAge must be a number compliant with Integer data type");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("LocationAge must be a number compliant with Integer data type");
            }
            updatedGeolocation = updatedGeolocation.setAgeOfLocationInfo(getInteger("LocationAge", data));
        }

        if (data.containsKey("SubscriberState")) {
            String state = data.getFirst("SubscriberState");
            if (!state.equalsIgnoreCase("assumedIdle") || !state.equalsIgnoreCase("camelBusy") ||
                !state.equalsIgnoreCase("netDetNotReachable") || !state.equalsIgnoreCase("notProvidedFromVLR")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("SubscriberState must be one of assumedIdle, camelBusy, netDetNotReachable, notProvidedFromVLR");
            } else {
                updatedGeolocation = updatedGeolocation.setSubscriberState(data.getFirst("SubscriberState"));

            }
        }

        if (data.containsKey("TrackingAreaCode")) {
            String tac = data.getFirst("TrackingAreaCode");
            Integer digits = Integer.valueOf(tac);
            try {
                if (digits > 65535) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("TrackingAreaCode must be a number not greater than 65535");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("TrackingAreaCode must be a number not greater than 65535");
            }
            updatedGeolocation = updatedGeolocation.setSubscriberState(data.getFirst("TrackingAreaCode"));
        }

        if (data.containsKey("RouteingAreaId")) {
            String routeingAreaId = data.getFirst("RouteingAreaId");
            if (routeingAreaId.length() > 20) {
                httpBadRequest = true;
                throw new IllegalArgumentException("RouteingAreaId length must not be greater than 20 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setSubscriberState(data.getFirst("RouteingAreaId"));
            }
        }

        if (data.containsKey("TypeOfShape")) {
            String typeOfShape = data.getFirst("TypeOfShape");
            if (typeOfShape.equalsIgnoreCase("ellipsoidPoint") || typeOfShape.equalsIgnoreCase("ellipsoidPointWithUncertaintyCircle")
                || typeOfShape.equalsIgnoreCase("ellipsoidPointWithUncertaintyEllipse") || typeOfShape.equalsIgnoreCase("polygon")
                || typeOfShape.equalsIgnoreCase("ellipsoidPointWithAltitude") || typeOfShape.equalsIgnoreCase("getEllipsoidPointWithAltitudeAndUncertaintyEllipsoid")
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
            throw new IllegalArgumentException("Uncertainty value can not be updated");
        }

        if (data.containsKey("UncertaintySemiMajorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintySemiMajorAxis value can not be updated");
        }

        if (data.containsKey("UncertaintySemiMinorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintySemiMinorAxis value can not be updated");
        }

        if (data.containsKey("AngleOfMajorAxis")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("AngleOfMajorAxis value can not be updated");
        }

        if (data.containsKey("Confidence")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Confidence value can not be updated");
        }

        if (data.containsKey("DeviceAltitude")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("DeviceAltitude value can not be updated");
        }

        if (data.containsKey("UncertaintyAltitude")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintyAltitude value can not be updated");
        }

        if (data.containsKey("InnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("InnerRadius value can not be updated");
        }

        if (data.containsKey("UncertaintyInnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintyInnerRadius value can not be updated");
        }

        if (data.containsKey("UncertaintyInnerRadius")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintyInnerRadius value can not be updated");
        }

        if (data.containsKey("OffsetAngle")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("OffsetAngle value can not be updated");
        }

        if (data.containsKey("IncludedAngle")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("IncludedAngle value can not be updated");
        }

        if (data.containsKey("HorizontalSpeed")) {
            String horizontalSpeed = data.getFirst("HorizontalSpeed");
            if (horizontalSpeed.length() > 30) {
                httpBadRequest = true;
                throw new IllegalArgumentException("HorizontalSpeed length must not be greater than 30 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setHorizontalSpeed(data.getFirst("HorizontalSpeed"));
            }
        }

        if (data.containsKey("VerticalSpeed")) {
            String verticalSpeed = data.getFirst("VerticalSpeed");
            if (verticalSpeed.length() > 30) {
                httpBadRequest = true;
                throw new IllegalArgumentException("VerticalSpeed length must not be greater than 30 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setHorizontalSpeed(data.getFirst("VerticalSpeed"));
            }
        }

        if (data.containsKey("UncertaintyHorizontalSpeed")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintyHorizontalSpeed value can not be updated");
        }

        if (data.containsKey("UncertaintyVerticalSpeed")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("UncertaintyVerticalSpeed value can not be updated");
        }

        if (data.containsKey("Bearing")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Bearing value can not be updated");
        }

        if (data.containsKey("LocationTimestamp")) {
            updatedGeolocation = updatedGeolocation.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        }

        if (data.containsKey("GeofenceType") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            String eventGeofenceType = data.getFirst("GeofenceType");
            if (!eventGeofenceType.equalsIgnoreCase("locationAreaId") && !eventGeofenceType.equalsIgnoreCase("cellGlobalId")
                && !eventGeofenceType.equalsIgnoreCase("countryCode") && !eventGeofenceType.equalsIgnoreCase("plmnId")
                && !eventGeofenceType.equalsIgnoreCase("routingAreaId") && !eventGeofenceType.equalsIgnoreCase("utranCellId")
                && !eventGeofenceType.equalsIgnoreCase("trackingAreaId") && !eventGeofenceType.equalsIgnoreCase("eUtranCellId")) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(), responseStatus.Failed.toString(),
                    "GeofenceEventType value not API compliant, must be one of available, inside, entering, leaving, periodic-ldr, " +
                        "motion-event, ldr-activated or max-interval-expiration");
            } else {
                updatedGeolocation = updatedGeolocation.setGeofenceType(data.getFirst("GeofenceType"));
            }
        }

        if (data.containsKey("GeofenceId") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            String eventGeofenceId = data.getFirst("GeofenceId");
            updatedGeolocation = updatedGeolocation.setGeofenceId(eventGeofenceId);
        }

        if (data.containsKey("GeofenceEventType") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            String eventGeofenceType = data.getFirst("GeofenceEventType");
            if (!eventGeofenceType.equalsIgnoreCase("inside") && !eventGeofenceType.equalsIgnoreCase("entering")
                && !eventGeofenceType.equalsIgnoreCase("leaving") && !eventGeofenceType.equalsIgnoreCase("available")
                && !eventGeofenceType.equalsIgnoreCase("periodic-ldr") && !eventGeofenceType.equalsIgnoreCase("motion-event")
                && !eventGeofenceType.equalsIgnoreCase("ldr-activated") && !eventGeofenceType.equalsIgnoreCase("max-interval-expiration")) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(), responseStatus.Failed.toString(),
                    "GeofenceEventType value not API compliant, must be one of available, inside, entering, leaving, periodic-ldr, motion-event, " +
                        "ldr-activated or max-interval-expiration");
            } else {
                updatedGeolocation = updatedGeolocation.setGeofenceEventType(eventGeofenceType);
            }
        }

        if (data.containsKey("EventRange") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            updatedGeolocation = updatedGeolocation.setEventRange(getLong("EventRange", data));
        }

        if (data.containsKey("CivicAddress")) {
            String civicAddress = data.getFirst("CivicAddress");
            if (civicAddress.length() > 200) {
                httpBadRequest = true;
                throw new IllegalArgumentException("CivicAddress length must not be greater than 200 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setCivicAddress(data.getFirst("CivicAddress"));
            }
        }

        if (data.containsKey("BarometricPressure")) {
            Long barometricPressure = Long.valueOf(data.getFirst("BarometricPressure"));
            if (barometricPressure > Long.MAX_VALUE) {
                httpBadRequest = true;
                throw new IllegalArgumentException("BarometricPressure must not be a number not greater than maximum value for Long data type");
            } else {
                updatedGeolocation = updatedGeolocation.setBarometricPressure(getLong("BarometricPressure", data));
            }
        }

        if (data.containsKey("PhysicalAddress")) {
            String physicalAddress = data.getFirst("PhysicalAddress");
            if (physicalAddress.length() > 50) {
                httpBadRequest = true;
                throw new IllegalArgumentException("PhysicalAddress length must not be greater than 50 characters");
            } else {
                updatedGeolocation = updatedGeolocation.setPhysicalAddress(data.getFirst("PhysicalAddress"));
            }
        }

        if (data.containsKey("InternetAddress")) {
            String internetAddress = data.getFirst("InternetAddress");
            if (internetAddress.length() > 50) {
                httpBadRequest = true;
                throw new IllegalArgumentException("InternetAddress length must not be greater than 50 characters");
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
                throw new IllegalArgumentException("LastGeolocationResponse value must be true or false");
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

    private static HashMap<String, String> parseAtiJsonString(String jsonLine) {
        HashMap<String, String> atiResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonObject cgiSaiLai = jelement.getAsJsonObject();
        cgiSaiLai = cgiSaiLai.getAsJsonObject("CGIorSAIorLAI");
        if (cgiSaiLai != null) {
            if (cgiSaiLai.get("mcc") != null) {
                String mcc = cgiSaiLai.get("mcc").getAsString();
                if (!mcc.isEmpty())
                    atiResponse.put("mcc", mcc);
            }
            if (cgiSaiLai.get("mnc") != null) {
                String mnc = cgiSaiLai.get("mnc").getAsString();
                if (!mnc.isEmpty())
                    atiResponse.put("mnc", mnc);
            }
            if (cgiSaiLai.get("lac") != null) {
                String lac = cgiSaiLai.get("lac").getAsString();
                if (!lac.isEmpty())
                    atiResponse.put("lac", lac);
            }
            if (cgiSaiLai.get("cellid") != null) {
                String ci = cgiSaiLai.get("cellid").getAsString();
                if (!ci.isEmpty())
                    atiResponse.put("ci", ci);
            }
        }
        if (jobject.get("aol") != null) {
            String aol = jobject.get("aol").getAsString();
            if (!aol.isEmpty())
                atiResponse.put("aol", aol);
        }
        if (jobject.get("vlrNumber") != null) {
            String vlrNumber = jobject.get("vlrNumber").getAsString();
            if (!vlrNumber.isEmpty())
                atiResponse.put("vlrNumber", vlrNumber);
        }
        if (jobject.get("subscriberState") != null) {
            String subscriberState = jobject.get("subscriberState").getAsString();
            if (!subscriberState.isEmpty())
                atiResponse.put("subscriberState", subscriberState);
        }
        return atiResponse;
    }

    private static HashMap<String, String> parsePslJsonString(String jsonLine) {
        HashMap<String, String> sriPslResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();

        if (jobject.get("lcsReferenceNumber") != null) {
            String lcsReferenceNumber = jobject.get("lcsReferenceNumber").getAsString();
            sriPslResponse.put("lcsReferenceNumber", lcsReferenceNumber);
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
                if (cgiSaiLai.get("cellid") != null) {
                    String ci = cgiSaiLai.get("cellid").getAsString();
                    if (!ci.isEmpty())
                        sriPslResponse.put("ci", ci);
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

    private static HashMap<String, String> parsePsiJsonString(String jsonLine) {
        HashMap<String, String> psiResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        if (jobject.get("imsi") != null) {
            String imsi = jobject.get("imsi").getAsString();
            if (!imsi.isEmpty())
                psiResponse.put("imsi", imsi);
        }
        if (jobject.get("imei") != null) {
            String imei = jobject.get("imei").getAsString();
            if (!imei.isEmpty())
                psiResponse.put("imei", imei);
        }
        if (jobject.get("lmsi") != null) {
            String lmsi = jobject.get("lmsi").getAsString();
            if (!lmsi.isEmpty())
                psiResponse.put("lmsi", lmsi);
        }
        if (jobject.get("subscriberState") != null) {
            String subscriberState = jobject.get("subscriberState").getAsString();
            if (!subscriberState.isEmpty())
                psiResponse.put("subscriberState", subscriberState);
        }

        JsonObject locationInfo = jelement.getAsJsonObject();
        if (locationInfo != null) {
            locationInfo = locationInfo.getAsJsonObject("LocationInformation");
            JsonObject cgiSaiLai = locationInfo.getAsJsonObject("CGIorSAIorLAI");
            if (cgiSaiLai != null) {
                if (cgiSaiLai.get("mcc") != null) {
                    String mcc = cgiSaiLai.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        psiResponse.put("mcc", mcc);
                }
                if (cgiSaiLai.get("mnc") != null) {
                    String mnc = cgiSaiLai.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        psiResponse.put("mnc", mnc);
                }
                if (cgiSaiLai.get("lac") != null) {
                    String lac = cgiSaiLai.get("lac").getAsString();
                    if (!lac.isEmpty())
                        psiResponse.put("lac", lac);
                }
                if (cgiSaiLai.get("cellid") != null) {
                    String ci = cgiSaiLai.get("cellid").getAsString();
                    if (!ci.isEmpty())
                        psiResponse.put("ci", ci);
                }
            }
            if (locationInfo.get("saiPresent") != null) {
                String saiPresent = locationInfo.get("saiPresent").getAsString();
                if (!saiPresent.isEmpty())
                    psiResponse.put("saiPresent", saiPresent);
            }
            if (locationInfo.get("locationNumber") != null) {
                String locationNumber = locationInfo.get("locationNumber").getAsString();
                if (!locationNumber.isEmpty())
                    psiResponse.put("locationNumber", locationNumber);
            }
            if (locationInfo.get("aol") != null) {
                String aol = locationInfo.get("aol").getAsString();
                if (!aol.isEmpty())
                    psiResponse.put("aol", aol);
            }
            if (locationInfo.get("vlrNumber") != null) {
                String vlrNumber = locationInfo.get("vlrNumber").getAsString();
                if (!vlrNumber.isEmpty())
                    psiResponse.put("vlrNumber", vlrNumber);
            }
            if (locationInfo.get("mscNumber") != null) {
                String mscNumber = locationInfo.get("mscNumber").getAsString();
                if (!mscNumber.isEmpty())
                    psiResponse.put("mscNumber", mscNumber);
            }
            JsonObject geographicalInformation = locationInfo.getAsJsonObject("GeographicalInformation");
            if (geographicalInformation != null) {
                if (geographicalInformation.get("latitude") != null) {
                    String latitude = geographicalInformation.get("latitude").getAsString();
                    if (!latitude.isEmpty())
                        psiResponse.put("latitude", latitude);
                }
                if (geographicalInformation.get("longitude") != null) {
                    String longitude = geographicalInformation.get("longitude").getAsString();
                    if (!longitude.isEmpty())
                        psiResponse.put("longitude", longitude);
                }
                if (geographicalInformation.get("typeOfShape") != null) {
                    String typeOfShape = geographicalInformation.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        psiResponse.put("typeOfShape", typeOfShape);
                }
                if (geographicalInformation.get("uncertainty") != null) {
                    String uncertainty = geographicalInformation.get("uncertainty").getAsString();
                    if (!uncertainty.isEmpty())
                        psiResponse.put("uncertainty", uncertainty);
                }
            }

            JsonObject geodeticInformation = locationInfo.getAsJsonObject("GeodeticInformation");
            if (geodeticInformation != null) {
                if (geodeticInformation.get("latitude") != null) {
                    String geodLatitude = geodeticInformation.get("latitude").getAsString();
                    if (!geodLatitude.isEmpty())
                        psiResponse.put("geodLatitude", geodLatitude);
                }
                if (geodeticInformation.get("longitude") != null) {
                    String geodLongitude = geodeticInformation.get("longitude").getAsString();
                    if (!geodLongitude.isEmpty())
                        psiResponse.put("geodLongitude", geodLongitude);
                }
                if (geodeticInformation.get("typeOfShape") != null) {
                    String geodTypeOfShape = geodeticInformation.get("typeOfShape").getAsString();
                    if (!geodTypeOfShape.isEmpty())
                        psiResponse.put("geodTypeOfShape", geodTypeOfShape);
                }
                if (geodeticInformation.get("uncertainty") != null) {
                    String geodUncertainty = geodeticInformation.get("uncertainty").getAsString();
                    if (!geodUncertainty.isEmpty())
                        psiResponse.put("geodUncertainty", geodUncertainty);
                }
                if (geodeticInformation.get("confidence") != null) {
                    String confidence = geodeticInformation.get("confidence").getAsString();
                    if (!confidence.isEmpty())
                        psiResponse.put("confidence", confidence);
                }
                if (geodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String screeningAndPresentationIndicators = geodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    if (!screeningAndPresentationIndicators.isEmpty())
                        psiResponse.put("screeningAndPresentationIndicators", screeningAndPresentationIndicators);
                }
            }

            if (locationInfo.get("currentLocationRetrieved") != null) {
                String currentLocationRetrieved = locationInfo.get("currentLocationRetrieved").getAsString();
                if (!currentLocationRetrieved.isEmpty())
                    psiResponse.put("currentLocationRetrieved", currentLocationRetrieved);
            }

            JsonObject locationInfoEPS = locationInfo.getAsJsonObject("LocationInformationEPS");
            if (locationInfoEPS != null) {
                if (locationInfoEPS.get("mmeName") != null) {
                    String mmeName = locationInfoEPS.get("mmeName").getAsString();
                    if (!mmeName.isEmpty())
                        psiResponse.put("mmeName", mmeName);
                }
                if (locationInfoEPS.get("eUtranCgi") != null) {
                    String eUtranCgi = locationInfoEPS.get("eUtranCgi").getAsString();
                    if (!eUtranCgi.isEmpty())
                        psiResponse.put("eUtranCgi", eUtranCgi);
                }
                if (locationInfoEPS.get("trackingAreaId") != null) {
                    String trackingAreaId = locationInfoEPS.get("trackingAreaId").getAsString();
                    if (!trackingAreaId.isEmpty())
                        psiResponse.put("trackingAreaId", trackingAreaId);
                }
            }
        }

        JsonObject locationInfoGPRS = jelement.getAsJsonObject();
        if (locationInfoGPRS != null) {
            locationInfoGPRS = locationInfoGPRS.getAsJsonObject("LocationInformationGPRS");
            JsonObject cgiSaiLai = locationInfoGPRS.getAsJsonObject("CGIorSAIorLAI");
            if (cgiSaiLai != null) {
                if (cgiSaiLai.get("mcc") != null) {
                    String mcc = cgiSaiLai.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        psiResponse.put("gprsMcc", mcc);
                }
                if (cgiSaiLai.get("mnc") != null) {
                    String mnc = cgiSaiLai.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        psiResponse.put("gprsMnc", mnc);
                }
                if (cgiSaiLai.get("lac") != null) {
                    String lac = cgiSaiLai.get("lac").getAsString();
                    if (!lac.isEmpty())
                        psiResponse.put("gprsLac", lac);
                }
                if (cgiSaiLai.get("cellid") != null) {
                    String ci = cgiSaiLai.get("cellid").getAsString();
                    if (!ci.isEmpty())
                        psiResponse.put("gprsCi", ci);
                }
            }

            JsonObject geoInfoGPRS = locationInfoGPRS.getAsJsonObject("GeographicalInformationGPRS");
            if (geoInfoGPRS != null) {
                if (geoInfoGPRS.get("latitude") != null) {
                    String latitude = geoInfoGPRS.get("latitude").getAsString();
                    if (!latitude.isEmpty())
                        psiResponse.put("gprsLatitude", latitude);
                }
                if (geoInfoGPRS.get("longitude") != null) {
                    String longitude = geoInfoGPRS.get("longitude").getAsString();
                    if (!longitude.isEmpty())
                        psiResponse.put("gprsLongitude", longitude);
                }
                if (geoInfoGPRS.get("typeOfShape") != null) {
                    String typeOfShape = geoInfoGPRS.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        psiResponse.put("gprsTypeOfShape", typeOfShape);
                }
                if (geoInfoGPRS.get("uncertainty") != null) {
                    String uncertainty = geoInfoGPRS.get("uncertainty").getAsString();
                    if (!uncertainty.isEmpty())
                        psiResponse.put("gprsUncertainty", uncertainty);
                }
            }

            JsonObject geodInfoGPRS = locationInfoGPRS.getAsJsonObject("GeodeticInformationGPRS");
            if (geodInfoGPRS != null) {
                if (geodInfoGPRS.get("latitude") != null) {
                    String latitude = geodInfoGPRS.get("latitude").getAsString();
                    if (!latitude.isEmpty())
                        psiResponse.put("gprsGeodLatitude", latitude);
                }
                if (geodInfoGPRS.get("longitude") != null) {
                    String longitude = geodInfoGPRS.get("longitude").getAsString();
                    if (!longitude.isEmpty())
                        psiResponse.put("gprsGeodLongitude", longitude);
                }
                if (geodInfoGPRS.get("typeOfShape") != null) {
                    String typeOfShape = geodInfoGPRS.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        psiResponse.put("gprsGeodTypeOfShape", typeOfShape);
                }
                if (geodInfoGPRS.get("uncertainty") != null) {
                    String uncertainty = geodInfoGPRS.get("uncertainty").getAsString();
                    if (!uncertainty.isEmpty())
                        psiResponse.put("gprsGeodUncertainty", uncertainty);
                }
                if (geodInfoGPRS.get("confidence") != null) {
                    String confidence = geodInfoGPRS.get("confidence").getAsString();
                    if (!confidence.isEmpty())
                        psiResponse.put("gprsConfidence", confidence);
                }
                if (geodInfoGPRS.get("screeningAndPresentationIndicators") != null) {
                    String screeningAndPresentationIndicators = geodInfoGPRS.get("screeningAndPresentationIndicators").getAsString();
                    if (!screeningAndPresentationIndicators.isEmpty())
                        psiResponse.put("gprsScreeningAndPresentationIndicators", screeningAndPresentationIndicators);
                }
            }

            if (locationInfoGPRS.get("sgsnNumber") != null) {
                String sgsnNumber = locationInfoGPRS.get("sgsnNumber").getAsString();
                if (!sgsnNumber.isEmpty())
                    psiResponse.put("sgsnNumber", sgsnNumber);
            }
            if (locationInfoGPRS.get("lsaId") != null) {
                String lsaId = locationInfoGPRS.get("lsaId").getAsString();
                if (!lsaId.isEmpty())
                    psiResponse.put("lsaId", lsaId);
            }
            if (locationInfoGPRS.get("routeingAreaId") != null) {
                String routeingAreaId = locationInfoGPRS.get("routeingAreaId").getAsString();
                if (!routeingAreaId.isEmpty())
                    psiResponse.put("routeingAreaId", routeingAreaId);
            }
        }

        JsonObject mnpInfoResult = jelement.getAsJsonObject();
        mnpInfoResult = mnpInfoResult.getAsJsonObject("MNPInfoResult");
        if (mnpInfoResult != null) {
            if (mnpInfoResult.get("mnpStatus") != null) {
                String mnpStatus = mnpInfoResult.get("mnpStatus").getAsString();
                if (!mnpStatus.isEmpty())
                    psiResponse.put("mnpStatus", mnpStatus);
            }
            if (mnpInfoResult.get("mnpMsisdn") != null) {
                String mnpMsisdn = mnpInfoResult.get("mnpMsisdn").getAsString();
                if (!mnpMsisdn.isEmpty())
                    psiResponse.put("mnpMsisdn", mnpMsisdn);
            }
            if (mnpInfoResult.get("mnpImsi") != null) {
                String mnpImsi = mnpInfoResult.get("mnpImsi").getAsString();
                if (!mnpImsi.isEmpty())
                    psiResponse.put("mnpImsi", mnpImsi);
            }
            if (mnpInfoResult.get("mnpRouteingNumber") != null) {
                String mnpRouteingNumber = mnpInfoResult.get("mnpRouteingNumber").getAsString();
                if (!mnpRouteingNumber.isEmpty())
                    psiResponse.put("mnpRouteingNumber", mnpRouteingNumber);
            }
        }
        return psiResponse;
    }

    public static HashMap<String, String> parsePlrJsonString(String jsonLine) {
        HashMap <String, String> rirPlrResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        if (jobject.get("lcsReferenceNumber") != null) {
            String lcsReferenceNumber = jobject.get("lcsReferenceNumber").getAsString();
            if (!lcsReferenceNumber.isEmpty())
                rirPlrResponse.put("lcsReferenceNumber", lcsReferenceNumber);
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
            JsonObject cgiSaiEci = pla.getAsJsonObject("CGIorSAIorESMLCCellInfo");
            if (cgiSaiEci != null) {
                if (cgiSaiEci.get("cellGlobalIdentity") != null) {
                    String cgi = cgiSaiEci.get("cellGlobalIdentity").getAsString();
                    if (!cgi.isEmpty())
                        rirPlrResponse.put("cellGlobalIdentity", cgi);
                }
                if (cgiSaiEci.get("serviceAreaIdentity") != null) {
                    String sai = cgiSaiEci.get("serviceAreaIdentity").getAsString();
                    if (!sai.isEmpty())
                        rirPlrResponse.put("serviceAreaIdentity", sai);
                }
                if (cgiSaiEci.get("eUtranCgi") != null) {
                    String eUtranCgi = cgiSaiEci.get("eUtranCgi").getAsString();
                    if (!eUtranCgi.isEmpty())
                        rirPlrResponse.put("eUtranCgi", eUtranCgi);
                }
                if (cgiSaiEci.get("cellPortionId") != null) {
                    String cellPortionId = cgiSaiEci.get("cellPortionId").getAsString();
                    if (!cellPortionId.isEmpty())
                        rirPlrResponse.put("cellPortionId", cellPortionId);
                }
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
