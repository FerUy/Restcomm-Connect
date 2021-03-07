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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            Configuration gmlcConf = configuration.subset("gmlc");
            validate(data, geolocationType, gmlcConf);
        } catch (final NullPointerException nullPointerException) {
            // API compliance check regarding missing mandatory parameters
            logger.warn(nullPointerException.getMessage());
            return status(BAD_REQUEST).entity(nullPointerException.getMessage()).build();
        } catch (final IllegalArgumentException illegalArgumentException) {
            logger.warn(illegalArgumentException.getMessage());
            // API compliance check regarding malformed parameters
            if (httpBadRequest) {
                return status(BAD_REQUEST).entity(illegalArgumentException.getMessage()).build();
            } else {
                cause = illegalArgumentException.getMessage();
                rStatus = responseStatus.Failed.toString();
            }
        } catch (final UnsupportedOperationException unsupportedOperationException) {
            logger.warn(unsupportedOperationException.getMessage());
            // API compliance check regarding parameters not allowed for Immediate type of Geolocation
            return status(BAD_REQUEST).entity(unsupportedOperationException.getMessage()).build();
        }

        /***********************************************/
        /******* Query GMLC for Location Data  ********/
        /*********************************************/
        try {
            Configuration gmlcConf = configuration.subset("gmlc");
            String gmlcURI = gmlcConf.getString("gmlc-uri");
            // Authorization for further stage of the project
            // Credentials credentials = new UsernamePasswordCredentials(gmlcUser, gmlcPassword);

            String targetMSISDN = data.getFirst("DeviceIdentifier");
            String token;
            if (data.getFirst("Token") != null) {
                token = data.getFirst("Token");
            } else {
                token = gmlcConf.getString("token");
            }
            String domainType = data.getFirst("Domain");
            String operation = data.getFirst("Operation");
            String httpRespType = data.getFirst("HttpRespType");

            // LCS
            String lcsLocationType = data.getFirst("LocationEstimateType");
            String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
            String lcsClientType, lcsClientName, lcsClientFormatIndicator, lcsClientExternalID, lcsClientInternalID;
            if (data.getFirst("ClientType") != null) {
                lcsClientType = data.getFirst("ClientType");
            } else {
                lcsClientType = gmlcConf.getString("lcs-client-type");
            }
            if (data.getFirst("ClientName") != null) {
                lcsClientName = data.getFirst("ClientName");
            } else {
                lcsClientName = gmlcConf.getString("lcs-client-name");
            }
            if (data.getFirst("ClientNameFormat") != null) {
                lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
            } else {
                lcsClientFormatIndicator = gmlcConf.getString("lcs-client-fi");
            }
            if (data.getFirst("ClientExternalID") != null) {
                lcsClientExternalID = data.getFirst("ClientExternalID");
            } else {
                lcsClientExternalID = gmlcConf.getString("lcs-client-external-id");
            }
            if (data.getFirst("ClientInternalID") != null) {
                lcsClientInternalID = data.getFirst("ClientInternalID");
            } else {
                lcsClientInternalID = gmlcConf.getString("lcs-client-internal-id");
            }
            String lcsRequestorId = data.getFirst("RequestorID");
            String lcsRequestorFormatIndicator = data.getFirst("RequestorIDFormat");

            // LCS QoS
            String qosClass = data.getFirst("QoSClass");
            String qosHorizontalAccuracy = data.getFirst("HorizontalAccuracy");
            String qosVerticalAccuracy = data.getFirst("VerticalAccuracy");
            String qosVerticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            String qosResponseTime = data.getFirst("ResponseTime");
            String velocityRequested = data.getFirst("VelocityRequested");

            String lcsServiceTypeID = data.getFirst("ServiceTypeID");
            String lcsPriority = data.getFirst("Priority");

            // LCS Area Event Info (Geofencing)
            String lcsAreaEventType = data.getFirst("AreaEventType");
            String lcsAreaEventId = data.getFirst("AreaEventId");
            String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
            String lcsAreaEventIntervalTime = data.getFirst("AreaEventInterval");
            String lcsAreaEventMaxInterval = data.getFirst("AreaEventMaxInterval");
            String lcsAreaEventSamplingInterval = data.getFirst("AreaEventSamplingInterval");
            String lcsAreaEventReportingDuration = data.getFirst("AreaEventReportingDuration");
            String lcsAdditionalAreaEventType = data.getFirst("AdditionalAreaEventType");
            String lcsAdditionalAreaEventId = data.getFirst("AdditionalAreaEventId");

            // LCS Motion Event Info
            String motionEventLinearDistance = data.getFirst("MotionEventRange");
            String motionEventOccurrence = data.getFirst("MotionEventOccurrence");
            String motionEventInterval = data.getFirst("MotionEventInterval");
            String motionEventMaxInterval = data.getFirst("MotionEventMaxInterval");
            String motionEventSamplingInterval = data.getFirst("MotionEventSamplingInterval");
            String motionEventReportingDuration = data.getFirst("MotionEventReportingDuration");

            // LCS Periodic LDR
            String lcsPeriodicReportingAmount = data.getFirst("PeriodicReportingAmount");
            String lcsPeriodicReportingInterval = data.getFirst("PeriodicReportingInterval");

            String referenceNumber = data.getFirst("ReferenceNumber");
            String callbackUrl;
            if (data.getFirst("StatusCallback") != null) {
                callbackUrl = data.getFirst("StatusCallback");
            } else {
                callbackUrl = gmlcConf.getString("lcs-callback-url");
            }

            // ATI or PSI optionals
            String locationInfoEps = data.getFirst("RequestLteLocation");
            String extraRequestedInfo = data.getFirst("RequestExtraInfo");

            // Sh UDR optionals
            String ratTypeRequested = data.getFirst("RequestRatType");
            String activeLocation = data.getFirst("RequestActiveLocation");
            String locationInfo5gs = data.getFirst("Request5GLocation");

            if (logger.isTraceEnabled()) {
                logger.trace("Geolocation API request. " +
                    "Arguments: token: " + token + ", msisdn: " + targetMSISDN + ", operation: " + operation +
                    "lcsClientType: " + lcsClientType + ", lcsClientName: " + lcsClientName + ", lcsClientFormatIndicator: " + lcsClientFormatIndicator +
                    ", lcsClientExternalID: " + lcsClientExternalID + ", lcsClientInternalID : " + lcsClientInternalID +
                    ", lcsRequestorId: " + lcsRequestorId + ", lcsRequestorFormatIndicator: " + lcsRequestorFormatIndicator +
                    ", QoSClass: " + qosClass + ", qosHorizontalAccuracy: " + qosHorizontalAccuracy +
                    ", qosVerticalAccuracy: " + qosVerticalAccuracy + ", qosVerticalCoordinateRequest:" + qosVerticalCoordinateRequest +
                    ", qosResponseTime: " + qosResponseTime + ", velocityRequested: " + velocityRequested +
                    ", lcsServiceTypeID: " + lcsServiceTypeID + ", lcsPriority:" + lcsPriority +
                    ", lcsAreaEventType: " + lcsAreaEventType + ", lcsAreaEventId:" + lcsAreaEventId +
                    ", lcsAreaEventOccurrenceInfo: " + lcsAreaEventOccurrenceInfo + ", lcsAreaEventIntervalTime: " + lcsAreaEventIntervalTime +
                    ", lcsAreaEventMaxInterval: " + lcsAreaEventMaxInterval + ", lcsAreaEventSamplingInterval:" + lcsAreaEventSamplingInterval +
                    ", lcsAreaEventReportingDuration: " + lcsAreaEventReportingDuration +
                    ", lcsAdditionalAreaEventType: " + lcsAdditionalAreaEventType + ", lcsAdditionalAreaEventId: " + lcsAdditionalAreaEventId +
                    ", motionEventLinearDistance:" + motionEventLinearDistance + ", motionEventOccurrence: " + motionEventOccurrence +
                    ", motionEventInterval:" + motionEventInterval + ", motionEventMaxInterval: " + motionEventMaxInterval +
                    ", motionEventSamplingInterval:" + motionEventSamplingInterval + ", motionEventReportingDuration: " + motionEventReportingDuration +
                    ", lcsPeriodicReportingAmount:" + lcsPeriodicReportingAmount + ", lcsPeriodicReportingInterval: " + lcsPeriodicReportingInterval +
                    ", referenceNumber:" + referenceNumber + ", callbackUrl: " + callbackUrl +
                    ", locationInfoEps:" + locationInfoEps + ", extraRequestedInfo: " + extraRequestedInfo +
                    ", ratTypeRequested:" + ratTypeRequested + ", activeLocation: " + activeLocation + ", locationInfo5gs: " + locationInfo5gs);
            }

            URIBuilder uriBuilder = new URIBuilder(gmlcURI);
            uriBuilder.addParameter("msisdn", targetMSISDN);
            uriBuilder.addParameter("token", token);

            if (operation != null)
                uriBuilder.addParameter("operation", operation);

            if (domainType != null)
                uriBuilder.addParameter("domain", domainType);

            if (httpRespType != null)
                uriBuilder.addParameter("httpRespType", httpRespType);

            if (locationInfoEps != null)
                uriBuilder.addParameter("locationInfoEps", locationInfoEps);

            if (extraRequestedInfo != null)
                uriBuilder.addParameter("extraRequestedInfo", extraRequestedInfo);

            if (ratTypeRequested != null)
                uriBuilder.addParameter("ratTypeRequested", ratTypeRequested);

            if (activeLocation != null)
                uriBuilder.addParameter("activeLocation", activeLocation);

            if (locationInfo5gs != null)
                uriBuilder.addParameter("locationInfo5gs", locationInfo5gs);

            if (lcsLocationType != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL")) {
                    if (lcsLocationType.equalsIgnoreCase("lastKnown"))
                        lcsLocationType = "currentOrLastKnownLocation";
                    if (lcsLocationType.equalsIgnoreCase("initial"))
                        lcsLocationType = "initialLocation";
                    if (lcsLocationType.equalsIgnoreCase("current"))
                        lcsLocationType = "currentLocation";
                    if (lcsLocationType.equalsIgnoreCase("activateDeferred"))
                        lcsLocationType = "activateDeferredLocation";
                    if (lcsLocationType.equalsIgnoreCase("cancelDeferred"))
                        lcsLocationType = "cancelDeferredLocation";
                    if (lcsLocationType.equalsIgnoreCase("notificationVerificationOnly"))
                        lcsLocationType = "notificationVerificationOnly";
                    uriBuilder.addParameter("lcsLocationType", lcsLocationType);
                } else if (operation.equalsIgnoreCase("PLR")) {
                    if (lcsLocationType.equalsIgnoreCase("lastKnown"))
                        lcsLocationType = "0"; // CURRENT_OR_LAST_KNOWN_LOCATION
                    if (lcsLocationType.equalsIgnoreCase("initial"))
                        lcsLocationType = "1"; // INITIAL_LOCATION
                    if (lcsLocationType.equalsIgnoreCase("current"))
                        lcsLocationType = "2"; // CURRENT_LOCATION
                    if (lcsLocationType.equalsIgnoreCase("activateDeferred"))
                        lcsLocationType = "3"; // ACTIVATE_DEFERRED_LOCATION
                    if (lcsLocationType.equalsIgnoreCase("cancelDeferred"))
                        lcsLocationType = "4"; // CANCEL_DEFERRED_LOCATION
                    if (lcsLocationType.equalsIgnoreCase("notificationVerificationOnly"))
                        lcsLocationType = "5"; // NOTIFICATION_VERIFICATION_ONLY
                    uriBuilder.addParameter("lcsLocationType", lcsLocationType);
                }
            }

            if (lcsDeferredLocationType != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL")) {
                    if (lcsDeferredLocationType.equalsIgnoreCase("periodic-ldr"))
                        uriBuilder.addParameter("lcsDeferredLocationType", "periodicLDR");
                    else
                        uriBuilder.addParameter("lcsDeferredLocationType", lcsDeferredLocationType);
                } else if (operation.equalsIgnoreCase("PLR")) {
                    if (lcsDeferredLocationType.equalsIgnoreCase("available"))
                        lcsDeferredLocationType = "1";
                    if (lcsDeferredLocationType.equalsIgnoreCase("entering"))
                        lcsDeferredLocationType = "2";
                    if (lcsDeferredLocationType.equalsIgnoreCase("leaving"))
                        lcsDeferredLocationType = "4";
                    if (lcsDeferredLocationType.equalsIgnoreCase("inside"))
                        lcsDeferredLocationType = "8";
                    if (lcsDeferredLocationType.equalsIgnoreCase("periodic-ldr"))
                        lcsDeferredLocationType = "16";
                    if (lcsDeferredLocationType.equalsIgnoreCase("motion-event"))
                        lcsDeferredLocationType = "32";
                    if (lcsDeferredLocationType.equalsIgnoreCase("ldr-activated"))
                        lcsDeferredLocationType = "64";
                    if (lcsDeferredLocationType.equalsIgnoreCase("max-interval-expiration"))
                        lcsDeferredLocationType = "128";
                    uriBuilder.addParameter("lcsDeferredLocationType", lcsDeferredLocationType);
                }
            }

            if (lcsClientType != null && operation != null) {
                if (operation.equalsIgnoreCase("PLR") || operation.equalsIgnoreCase("PSL")) {
                    if (lcsClientType.equalsIgnoreCase("emergency"))
                        lcsClientType = "0";
                    if (lcsClientType.equalsIgnoreCase("vas"))
                        lcsClientType = "1";
                    if (lcsClientType.equalsIgnoreCase("operator"))
                        lcsClientType = "2";
                    if (lcsClientType.equalsIgnoreCase("lawful"))
                        lcsClientType = "3";
                    uriBuilder.addParameter("lcsClientType", lcsClientType);
                }
            }

            if (lcsClientName != null && operation != null && lcsClientType != null) {
                // lcsClientName and lcsClientFormatIndicator are mandatory for lcsClientType 1 (vas)) for either PSL or PLR
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    if (lcsClientFormatIndicator != null) {
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
                        if (operation.equalsIgnoreCase("PSL") && lcsClientType.equals("1")) {
                            // lcsClientName and lcsClientFormatIndicator are mandatory for lcsClientType 1 (vas)) for PSL
                            uriBuilder.addParameter("lcsClientName", lcsClientName);
                            uriBuilder.addParameter("lcsClientFormatIndicator", lcsClientFormatIndicator);
                        } else if (operation.equalsIgnoreCase("PLR")) {
                            // PLR AVP: { LCS-EPS-Client-Name } (M)
                            uriBuilder.addParameter("lcsClientName", lcsClientName);
                            uriBuilder.addParameter("lcsClientFormatIndicator", lcsClientFormatIndicator);
                        }
                    }
                }
            }

            if (lcsClientExternalID != null && operation != null && lcsClientType != null) {
                // lcsClientExternalID is mandatory for lcsClientType 0 (emergency) and 1 (vas) for PSL
                if (operation.equalsIgnoreCase("PSL") && (lcsClientType.equals("0") || (lcsClientType.equals("1")))) {
                    uriBuilder.addParameter("lcsClientExternalID", lcsClientExternalID);
                }
            }

            if (lcsClientInternalID != null && operation != null && lcsClientType != null) {
                // HTTP param: lcsClientInternalID (mandatory for lcsClientType 2 (plmnOperatorServices)) for PSL
                if (operation.equalsIgnoreCase("PSL") && lcsClientType.equals("2")) {
                    uriBuilder.addParameter("lcsClientInternalID", lcsClientInternalID);
                }
            }

            if (lcsRequestorId != null && operation != null && lcsClientType != null) {
                // lcsRequestorId and lcsRequestorFormatIndicator are optional for either PSL or PLR
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    if (lcsRequestorFormatIndicator != null || lcsRequestorFormatIndicator != null) {
                        if (lcsRequestorFormatIndicator.equalsIgnoreCase("name"))
                            lcsRequestorFormatIndicator = "0";
                        if (lcsRequestorFormatIndicator.equalsIgnoreCase("email"))
                            lcsRequestorFormatIndicator = "1";
                        if (lcsRequestorFormatIndicator.equalsIgnoreCase("msisdn"))
                            lcsRequestorFormatIndicator = "2";
                        if (lcsRequestorFormatIndicator.equalsIgnoreCase("url"))
                            lcsRequestorFormatIndicator = "3";
                        if (lcsRequestorFormatIndicator.equalsIgnoreCase("sip"))
                            lcsRequestorFormatIndicator = "4";
                        if (operation.equalsIgnoreCase("PSL") && lcsClientType.equals("1")) {
                            // lcsRequestorId and lcsRequestorFormatIndicator are optional for lcsClientType 1 (vas)) for PSL or PLR
                            uriBuilder.addParameter("lcsRequestorId", lcsRequestorId);
                            uriBuilder.addParameter("lcsRequestorFormatIndicator", lcsRequestorFormatIndicator);
                        } else {
                            // PLR AVP: [ LCS-Requestor-Name ] (O)
                            uriBuilder.addParameter("lcsRequestorId", lcsRequestorId);
                            uriBuilder.addParameter("lcsRequestorFormatIndicator", lcsRequestorFormatIndicator);
                        }
                    }
                }
            }

            if (qosClass != null && operation != null) {
                if (operation.equalsIgnoreCase("PLR")) {
                    if (qosClass.equalsIgnoreCase("assured"))
                        qosClass = "0";
                    if (qosClass.equalsIgnoreCase("best-effort"))
                        qosClass = "1";
                    uriBuilder.addParameter("lcsQoSClass", qosClass);
                }
            }

            if (qosHorizontalAccuracy != null)
                uriBuilder.addParameter("horizontalAccuracy", qosHorizontalAccuracy);

            if (qosVerticalAccuracy != null)
                uriBuilder.addParameter("verticalAccuracy", qosVerticalAccuracy);

            if (qosVerticalCoordinateRequest != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL")) {
                    uriBuilder.addParameter("verticalCoordinateRequest", qosVerticalCoordinateRequest);
                }
                if (operation.equalsIgnoreCase("PLR")) {
                    if (qosVerticalCoordinateRequest.equalsIgnoreCase("false"))
                        qosVerticalCoordinateRequest = "0";
                    if (qosVerticalCoordinateRequest.equalsIgnoreCase("true"))
                        qosVerticalCoordinateRequest = "1";
                    uriBuilder.addParameter("verticalCoordinateRequest", qosVerticalCoordinateRequest);
                }
            }

            if (qosResponseTime != null  && operation != null) {
                if (operation.equalsIgnoreCase("PSL")) {
                    if (qosResponseTime.equalsIgnoreCase("low"))
                        qosResponseTime = "lowdelay";
                    if (qosResponseTime.equalsIgnoreCase("tolerant"))
                        qosResponseTime = "delaytolerant";
                    uriBuilder.addParameter("responseTime", qosResponseTime);
                } else if (operation.equalsIgnoreCase("PLR")) {
                    if (qosResponseTime.equalsIgnoreCase("low"))
                        qosResponseTime = "0";
                    if (qosResponseTime.equalsIgnoreCase("tolerant"))
                        qosResponseTime = "1";
                    uriBuilder.addParameter("responseTime", qosResponseTime);
                }
            }

            if (lcsServiceTypeID != null && operation != null) {
                uriBuilder.addParameter("lcsServiceTypeId", lcsServiceTypeID);
            }

            if (lcsPriority != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL")) {
                    if (lcsPriority.equalsIgnoreCase("normal"))
                        lcsPriority = "normalPriority";
                    if (lcsPriority.equalsIgnoreCase("high") )
                        lcsPriority = "highestPriority";
                    uriBuilder.addParameter("lcsPriority", lcsPriority);
                } else if (operation.equalsIgnoreCase("PLR")) {
                    if (lcsPriority.equalsIgnoreCase("normal"))
                        lcsPriority = "0";
                    if (lcsPriority.equalsIgnoreCase("high"))
                        lcsPriority = "1";
                    uriBuilder.addParameter("lcsPriority", lcsPriority);
                }
            }

            if (lcsAreaEventType != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    if (operation.equalsIgnoreCase("PSL")) {
                        uriBuilder.addParameter("lcsAreaType", lcsAreaEventType);
                    } else if (operation.equalsIgnoreCase("PLR")) {
                        if (lcsAreaEventType.equalsIgnoreCase("countryCode"))
                            lcsAreaEventType = "0";
                        if (lcsAreaEventType.equalsIgnoreCase("plmnId"))
                            lcsAreaEventType = "1";
                        if (lcsAreaEventType.equalsIgnoreCase("locationAreaId"))
                            lcsAreaEventType = "2";
                        if (lcsAreaEventType.equalsIgnoreCase("routingAreaId"))
                            lcsAreaEventType = "3";
                        if (lcsAreaEventType.equalsIgnoreCase("cellGlobalId"))
                            lcsAreaEventType = "4";
                        if (lcsAreaEventType.equalsIgnoreCase("utranCellId"))
                            lcsAreaEventType = "5";
                        if (lcsAreaEventType.equalsIgnoreCase("trackingAreaId"))
                            lcsAreaEventType = "6";
                        if (lcsAreaEventType.equalsIgnoreCase("eUtranCellId"))
                            lcsAreaEventType = "7";
                        uriBuilder.addParameter("lcsAreaType", lcsAreaEventType);

                        if (lcsAdditionalAreaEventType != null) {
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("countryCode"))
                                lcsAdditionalAreaEventType = "0";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("plmnId"))
                                lcsAdditionalAreaEventType = "1";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("locationAreaId"))
                                lcsAdditionalAreaEventType = "2";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("routingAreaId"))
                                lcsAdditionalAreaEventType = "3";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("cellGlobalId"))
                                lcsAdditionalAreaEventType = "4";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("utranCellId"))
                                lcsAdditionalAreaEventType = "5";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("trackingAreaId"))
                                lcsAdditionalAreaEventType = "6";
                            if (lcsAdditionalAreaEventType.equalsIgnoreCase("eUtranCellId"))
                                lcsAdditionalAreaEventType = "7";
                            uriBuilder.addParameter("lcsAdditionalAreaType", lcsAdditionalAreaEventType);
                        }
                    }
                    if (lcsAreaEventId != null) {
                            uriBuilder.addParameter("lcsAreaId", lcsAreaEventId);
                            if (lcsAdditionalAreaEventId != null) {
                                uriBuilder.addParameter("lcsAdditionalAreaId", lcsAdditionalAreaEventId);
                            }

                        if (lcsAreaEventOccurrenceInfo != null) {
                            if (operation.equalsIgnoreCase("PSL")) {
                                if (lcsAreaEventOccurrenceInfo.equalsIgnoreCase("once"))
                                    lcsAreaEventOccurrenceInfo = "oneTimeEvent";
                                if (lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple"))
                                    lcsAreaEventOccurrenceInfo = "multipleTimeEvent";
                                uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", lcsAreaEventOccurrenceInfo);
                            } else if (operation.equalsIgnoreCase("PLR")) {
                                if (lcsAreaEventOccurrenceInfo.equalsIgnoreCase("once"))
                                    lcsAreaEventOccurrenceInfo = "0";
                                if (lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple"))
                                    lcsAreaEventOccurrenceInfo = "1";
                                uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", lcsAreaEventOccurrenceInfo);
                            } else {
                                if (operation.equalsIgnoreCase("PSL")) {
                                    lcsAreaEventOccurrenceInfo = "oneTimeEvent";
                                    uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", lcsAreaEventOccurrenceInfo);
                                } else if (operation.equalsIgnoreCase("PLR")) {
                                    lcsAreaEventOccurrenceInfo = "0";
                                    uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", lcsAreaEventOccurrenceInfo);
                                }
                            }
                            // Interval-Time (minimum interval time between area reports in seconds) and Maximum-Interval are only applicable
                            // when the Occurrence-Info is set to "MULTIPLE_TIME_EVENT" (1).
                            if (lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multipleTimeEvent") ||
                                lcsAreaEventOccurrenceInfo.equals("1")) {
                                if (lcsAreaEventIntervalTime != null) {
                                    uriBuilder.addParameter("lcsAreaEventIntervalTime", lcsAreaEventIntervalTime);
                                } else {
                                    // If not included, the default value of Interval-Time shall be considered as one
                                    uriBuilder.addParameter("lcsAreaEventIntervalTime", "1");
                                }
                                if (lcsAreaEventMaxInterval != null) {
                                    uriBuilder.addParameter("lcsAreaEventMaxInterval", lcsAreaEventMaxInterval);
                                } else {
                                    // If not included, the default value of Maximum-Interval shall be considered as the maximum value.
                                    uriBuilder.addParameter("lcsAreaEventMaxInterval", "86400");
                                }
                            }
                            if (operation.equalsIgnoreCase("PLR")) {
                                if (lcsAreaEventSamplingInterval != null) {
                                    uriBuilder.addParameter("lcsAreaEventSamplingInterval", lcsAreaEventSamplingInterval);
                                } else {
                                    // If not included, the default value of Sampling-Interval shall be considered as the maximum value.
                                    uriBuilder.addParameter("lcsAreaEventSamplingInterval", "3600");
                                }
                                if (lcsAreaEventReportingDuration != null) {
                                    uriBuilder.addParameter("lcsAreaEventReportingDuration", lcsAreaEventReportingDuration);
                                } else {
                                    // If not included, the default value of Reporting-Duration shall be considered as the maximum value.
                                    uriBuilder.addParameter("lcsAreaEventReportingDuration", "8640000");
                                }
                            }
                        } else {
                            // If not included, the default value of Occurrence-Info shall be considered as "ONE_TIME_EVENT" (0).
                            if (operation.equalsIgnoreCase("PSL")) {
                                lcsAreaEventOccurrenceInfo = "oneTimeEvent";
                            } else if (operation.equalsIgnoreCase("PLR")) {
                                lcsAreaEventOccurrenceInfo = "0";
                            }
                            uriBuilder.addParameter("lcsAreaEventOccurrenceInfo", lcsAreaEventOccurrenceInfo);
                        }
                    }
                }
            }

            if (motionEventLinearDistance != null && operation != null) {
                if (operation.equalsIgnoreCase("PLR")) {
                    uriBuilder.addParameter("lcsMotionEventLinearDistance", motionEventLinearDistance);
                    if (motionEventOccurrence != null) {
                        if (motionEventOccurrence.equalsIgnoreCase("once"))
                            motionEventOccurrence = "0";
                        if (motionEventOccurrence.equalsIgnoreCase("multiple"))
                            motionEventOccurrence = "1";
                        uriBuilder.addParameter("lcsMotionEventOccurrenceInfo", motionEventOccurrence);
                    } else {
                        // If not included, the default value of Occurrence-Info shall be considered as "ONE_TIME_EVENT" (0).
                        motionEventOccurrence = "0";
                        uriBuilder.addParameter("lcsMotionEventOccurrenceInfo", motionEventOccurrence);
                    }
                    // Interval-Time and Maximum-Interval AVPs are only applicable when the Occurrence-Info is set to "MULTIPLE_TIME_EVENT" (1).
                    if (motionEventOccurrence.equals("1")) {
                        if (motionEventInterval != null) {
                            uriBuilder.addParameter("lcsMotionEventIntervalTime", motionEventInterval);
                        } else {
                            // If not included, the default value of Interval-Time shall be considered as one
                            uriBuilder.addParameter("lcsMotionEventIntervalTime", "1");
                        }
                        if (motionEventMaxInterval != null) {
                            uriBuilder.addParameter("lcsMotionEventMaxInterval", motionEventMaxInterval);
                        } else {
                            // If not included, the default value of Maximum-Interval shall be considered as the maximum value.
                            uriBuilder.addParameter("lcsMotionEventMaxInterval", "86400");
                        }
                    }
                    if (motionEventSamplingInterval != null) {
                        uriBuilder.addParameter("lcsMotionEventSamplingInterval", motionEventSamplingInterval);
                    } else {
                        // If not included, the default value of Sampling-Interval shall be considered as the maximum value.
                        uriBuilder.addParameter("lcsMotionEventSamplingInterval", "3600");
                    }
                    if (motionEventReportingDuration != null) {
                        uriBuilder.addParameter("lcsMotionEventReportingDuration", motionEventReportingDuration);
                    } else {
                        // If not included, the default value of Reporting-Duration shall be considered as the maximum value.
                        uriBuilder.addParameter("lcsMotionEventReportingDuration", "8640000");
                    }
                }
            }

            if (velocityRequested != null && operation != null) {
                if (operation.equalsIgnoreCase("PLR")) {
                    if (velocityRequested.equalsIgnoreCase("false"))
                        velocityRequested = "0";
                    if (velocityRequested.equalsIgnoreCase("true"))
                        velocityRequested = "1";
                    uriBuilder.addParameter("velocityRequested", velocityRequested);
                }
            }

            if (referenceNumber != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    uriBuilder.addParameter("clientReferenceNumber", referenceNumber);
                }
            }

            if (lcsPeriodicReportingAmount != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    uriBuilder.addParameter("lcsPeriodicReportingAmount", lcsPeriodicReportingAmount);
                }
            }

            if (lcsPeriodicReportingInterval != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    uriBuilder.addParameter("lcsPeriodicReportingInterval", lcsPeriodicReportingInterval);
                }
            }

            if (callbackUrl != null && operation != null) {
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    uriBuilder.addParameter("lcsCallbackUrl", callbackUrl);
                }
            }

            URL url = uriBuilder.build().toURL();
            HttpClient client = HttpClientBuilder.create().build();
            if (logger.isDebugEnabled())
                logger.debug("\ncURL URL: " + url);
            HttpGet request = new HttpGet(String.valueOf(url));
            // Authorization for further stage of the project
            //request.addHeader("User-Agent", gmlcUser);
            //request.addHeader("User-Password", gmlcPassword);
            HttpResponse response = client.execute(request);
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream stream = null;
                String gmlcResponse;
                try {
                    if (httpRespType != null) {
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
                                        String mcc = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("MobileCountryCode", mcc);
                                    }
                                    if (item.contains("mnc")) {
                                        String mnc = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("MobileNetworkCode", mnc);
                                    }
                                    if (item.contains("lac")) {
                                        String lac = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("LocationAreaCode", lac);
                                    }
                                    if (item.contains("cellid")) {
                                        String ci = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("CellId", ci);
                                    }
                                    if (item.contains("aol")) {
                                        String aol = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("LocationAge", aol);
                                    }
                                    if (item.contains("vlrNumber")) {
                                        String nnn = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("NetworkEntityAddress", nnn);
                                    }
                                    if (item.contains("subscriberState")) {
                                        String state = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("SubscriberState", state);
                                    }
                                }
                            }
                        }
                    } else {
                        gmlcResponse = EntityUtils.toString(entity, "UTF-8");
                        if (gmlcResponse != null) {
                            if (operation != null) {
                                if (geolocationType.toString().equals(NotificationGT)) {
                                    if (operation.equalsIgnoreCase("PSL")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via MAP SRILCS-PSL: " + gmlcResponse);
                                        HashMap<String, String> sriPslResponse = parsePslJsonString(gmlcResponse);
                                        putDataFromSriPslResponse(sriPslResponse, data);
                                    } else if (operation.equalsIgnoreCase("PLR")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via Diameter RIR/RIA-PLR/PLA: " + gmlcResponse);
                                        HashMap<String, String> rirPlrResponse = parsePlrJsonString(gmlcResponse);
                                        putDataFromRirPlrResponse(rirPlrResponse, data);
                                    }
                                } else {
                                    if (operation.equalsIgnoreCase("UDR")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via Sh UDR/UDA: " + gmlcResponse);
                                        HashMap<String, String> shUdrResponse = parseUdaJsonString(gmlcResponse);
                                        putDataFromShUdrResponse(shUdrResponse, data);
                                    } else if (operation.equalsIgnoreCase("ATI")) {
                                        if (logger.isDebugEnabled())
                                            logger.debug("Data retrieved from GMLC via MAP ATI: " + gmlcResponse);
                                        HashMap<String, String> atiResponse = parseAtiOrPsiJsonString(gmlcResponse);
                                        putDataFromAtiOrPsiResponse(atiResponse, data);

                                    } else if (operation.equalsIgnoreCase("PSI")) {
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
                            logger.debug("\nMCC = " + getInteger("MobileCountryCode", data));
                            logger.debug("\nMNC = " + getInteger("MobileNetworkCode", data));
                            logger.debug("\nLAC  = " + getInteger("LocationAreaCode", data));
                            logger.debug("\nCI = " + getInteger("CellId", data));
                            logger.debug("\nSAC = " + getInteger("ServiceAreaCode", data));
                            logger.debug("\nENodeBId = " + getInteger("ENodeBId", data));
                            logger.debug("\nECellId = " + getLong("ECellId", data));
                            logger.debug("\nNCI = " + getLong("NRCellId", data));
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
                            logger.debug("\nRadio Access Type = " + data.getFirst("RadioAccessType"));
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
            logger.error("Problem while trying to retrieve data from GMLC, exception: "+ex);
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

    private void validate(final MultivaluedMap<String, String> data, Geolocation.GeolocationType glType, Configuration gmlcConf)
        throws RuntimeException {

        /** Validation of Geolocation POST requests with valid type **/
        if (!glType.toString().equals(ImmediateGT) && !glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: Geolocation Type can not be null, but either \"Immediate\" or \"Notification\".");
        }

        /*** DeviceIdentifier can not be null ***/
        if (!data.containsKey("DeviceIdentifier")) {
            throw new NullPointerException("Rejected: DeviceIdentifier value can not be null");
        }

        /*** Operation must not be null ***/
        if (!data.containsKey("Operation")) {
            throw new NullPointerException("Rejected: Operation value con not be null");
        }

        /*** Token can not be null ***/
        if (!data.containsKey("Token")) {
            if (gmlcConf.getString("token").equals("")) {
                throw new NullPointerException("Rejected: Token argument or token configuration value can not be null");
            }
        }

        /*** StatusCallback can not be null for Notification type of Geolocation ***/
        if (!data.containsKey("StatusCallback") && glType.toString().equals(NotificationGT)) {
            String callbackUrl = gmlcConf.getString("lcs-callback-url");
            if (callbackUrl != null) {
                if (callbackUrl.equalsIgnoreCase("")) {
                    throw new NullPointerException("Rejected: StatusCallback value can not be null for Notification type of Geolocation");
                }
            }
        } else if (data.containsKey("StatusCallback") && glType.toString().equals(ImmediateGT)) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: StatusCallback only applies for Notification type of Geolocation");
        }

        /*** Operation must not be different than PSL or PLR for Notification type of Geolocation ***/
        if (glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (!operation.equalsIgnoreCase("PSL") && !operation.equalsIgnoreCase("PLR")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Operation value not API compliant, must be PSL or PLR for Notification type of Geolocation");
            }
        }

        /*** Domain must be API compliant: cs or ps values only for Immediate type of Geolocation ***/
        if (data.containsKey("Domain") && glType.toString().equals(ImmediateGT)) {
            String domain = data.getFirst("Domain");
            if (!domain.equalsIgnoreCase("cs") && !domain.equalsIgnoreCase("ps")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Domain values can be only cs or ps for Circuit-Switched or Packet-Switched networks respectively");
            }
        }

        /*** RequestLteLocation must be API compliant: true or false values only for Immediate type of Geolocation ***/
        if (data.containsKey("RequestLteLocation") && glType.toString().equals(ImmediateGT)) {
            String locationInfoEps = data.getFirst("RequestLteLocation");
            if (!locationInfoEps.equalsIgnoreCase("true") && !locationInfoEps.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: RequestLteLocation values can be only true or false");
            }
        }

        /*** RequestExtraInfo must be API compliant: true or false values only for Immediate type of Geolocation ***/
        if (data.containsKey("RequestExtraInfo") && glType.toString().equals(ImmediateGT)) {
            String extraRequestedInfo = data.getFirst("RequestExtraInfo");
            if (!extraRequestedInfo.equalsIgnoreCase("true") && !extraRequestedInfo.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: RequestExtraInfo values can be only true or false");
            }
        }

        /*** RequestRatType must be API compliant: true or false values only for Immediate type of Geolocation ***/
        if (data.containsKey("RequestRatType") && glType.toString().equals(ImmediateGT)) {
            String ratTypeRequested = data.getFirst("RequestRatType");
            if (!ratTypeRequested.equalsIgnoreCase("true") && !ratTypeRequested.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: RequestRatType values can be only true or false");
            }
        }

        /*** RequestActiveLocation must be API compliant: true or false values only for Immediate type of Geolocation ***/
        if (data.containsKey("RequestActiveLocation") && glType.toString().equals(ImmediateGT)) {
            String activeLocation = data.getFirst("RequestActiveLocation");
            if (!activeLocation.equalsIgnoreCase("true") && !activeLocation.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: RequestActiveLocation values can be only true or false");
            }
        }

        /*** Request5GLocation must be API compliant: true or false values only for Immediate type of Geolocation ***/
        if (data.containsKey("Request5GLocation") && glType.toString().equals(ImmediateGT)) {
            String locationInfo5gs = data.getFirst("Request5GLocation");
            if (!locationInfo5gs.equalsIgnoreCase("true") && !locationInfo5gs.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: Request5GLocation values can be only true or false");
            }
        }

        /*** ClientType must be API compliant: emergency, vas, operator, lawful or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR") || operation.equalsIgnoreCase("PSL")) {
                String lcs_client_type = gmlcConf.getString("lcs-client-type");
                if (lcs_client_type != null) {
                    if (lcs_client_type.equals("")) {
                        throw new NullPointerException("Rejected: ClientType parameter or lcs-client-type value can not be null or empty " +
                            "for Notification type of Geolocation");
                    }
                }
            }
        }
        if (data.containsKey("ClientType") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR") || operation.equalsIgnoreCase("PSL")) {
                String lcsLocationType = data.getFirst("ClientType");
                if (!lcsLocationType.equalsIgnoreCase("emergency") && !lcsLocationType.equalsIgnoreCase("vas")
                    && !lcsLocationType.equalsIgnoreCase("operator") && !lcsLocationType.equalsIgnoreCase("lawful")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ClientType value not API compliant, must be emergency, vas, operator or lawful " +
                        "for Notification type of Geolocation");
                }
            }
        }

        /*** LocationEstimateType must be API compliant: Mandatory parameter for Notification type of Geolocation
         * accepted values: lastKnown, initial, current, activateDeferred, cancelDeferred or notificationVerificationOnly ***/
        if (data.containsKey("LocationEstimateType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: LocationEstimateType only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("Rejected: LocationEstimateType value can not be null for Notification type of Geolocation");
        }
        if (data.containsKey("LocationEstimateType") && glType.toString().equals(NotificationGT)) {
            String lcsLocationType = data.getFirst("LocationEstimateType");
            if (!lcsLocationType.equalsIgnoreCase("lastKnown") && !lcsLocationType.equalsIgnoreCase("initial")
                && !lcsLocationType.equalsIgnoreCase("current") && !lcsLocationType.equalsIgnoreCase("activateDeferred")
                && !lcsLocationType.equalsIgnoreCase("cancelDeferred") && !lcsLocationType.equalsIgnoreCase("notificationVerificationOnly")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: LocationEstimateType value not API compliant, must be lastKnown, initial, current, activateDeferred, " +
                    "cancelDeferred or notificationVerificationOnly");
            }
        }

        /*** DeferredLocationType must belong to Notification type of Geolocation and API compliant
         * Valid only when LocationEstimateType equals activateDeferred or cancelDeferred
         * accepted values: available, entering, leaving, inside, periodic-ldr, motion-event, ldr-activated, max-interval-expiration ***/
        if (data.containsKey("DeferredLocationType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: DeferredLocationType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("DeferredLocationType") && glType.toString().equals(NotificationGT)) {
            String lcsLocationType = data.getFirst("LocationEstimateType");
            if (!lcsLocationType.equalsIgnoreCase("activateDeferred") && !lcsLocationType.equalsIgnoreCase("cancelDeferred")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: DeferredLocationType parameter is only valid when LocationEstimateType parameter " +
                    "equals activateDeferred or cancelDeferred");
            }
            String deferredLocationType = data.getFirst("DeferredLocationType");
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PSL")) {
                if (!deferredLocationType.equalsIgnoreCase("available") && !deferredLocationType.equalsIgnoreCase("entering")
                    && !deferredLocationType.equalsIgnoreCase("leaving") && !deferredLocationType.equalsIgnoreCase("inside")
                    && !deferredLocationType.equalsIgnoreCase("periodic-ldr")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: DeferredLocationType value not API compliant, must be one of available, inside, " +
                        "entering, leaving or periodic-ldr for Notification type of Geolocation in UMTS via MAP PSL");
                }
            } else if (operation.equalsIgnoreCase("PLR")) {
                if (!deferredLocationType.equalsIgnoreCase("available") && !deferredLocationType.equalsIgnoreCase("entering")
                    && !deferredLocationType.equalsIgnoreCase("leaving") && !deferredLocationType.equalsIgnoreCase("inside")
                    && !deferredLocationType.equalsIgnoreCase("periodic-ldr") && !deferredLocationType.equalsIgnoreCase("motion-event")
                    && !deferredLocationType.equalsIgnoreCase("ldr-activated")
                    && !deferredLocationType.equalsIgnoreCase("max-interval-expiration")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: DeferredLocationType value not API compliant, " +
                        "must be one of available, inside, entering, leaving, periodic-ldr, motion-event, ldr-activated or max-interval-expiration " +
                        "for Notification type of Geolocation in LTE via Diameter PLR");
                }
            }
        }

        /*** ClientName must be API compliant: not null for Notification type of Geolocation in LTE or UMTS when Client Type is vas ***/
        if (data.containsKey("ClientName") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientName only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("ClientName") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                String lcs_client_name = gmlcConf.getString("lcs-client-name");
                if (lcs_client_name != null) {
                    if (lcs_client_name.equals("")) {
                        throw new NullPointerException("Rejected: ClientName parameter or lcs-client-name configuration values can not be null " +
                            "or empty for Notification type of Geolocation in LTE via Diameter PLR");
                    }
                }
            }
            if (operation.equalsIgnoreCase("PSL")) {
                String clientType = data.getFirst("ClientType");
                if (clientType != null) {
                    if (clientType.equalsIgnoreCase("vas")) {
                        throw new NullPointerException("Rejected: ClientName value can not be null for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter equals vas (value-added services)");
                    }
                }
                String lcs_client_name = gmlcConf.getString("lcs-client-name");
                if (lcs_client_name != null) {
                    if (lcs_client_name.equals("")) {
                        String lcs_client_type = gmlcConf.getString("lcs-client-type");
                        if (lcs_client_type != null) {
                            if (lcs_client_type.equalsIgnoreCase("vas")) {
                                throw new NullPointerException("Rejected: ClientName value can not be null for Notification type of Geolocation in UMTS via MAP PSL when " +
                                    "lcs-client-name configuration value equals vas (value-added services)");
                            }
                        }
                    }
                }
            }
        }
        if (data.containsKey("ClientName") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (!data.containsKey("ClientNameFormat")) {
                    throw new NullPointerException("Rejected: ClientNameFormat value can not be null for Notification type of Geolocation in LTE " +
                        "via Diameter PLR");
                } else {
                    String lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
                    if (!lcsClientFormatIndicator.equalsIgnoreCase("name") && !lcsClientFormatIndicator.equalsIgnoreCase("email") &&
                        !lcsClientFormatIndicator.equalsIgnoreCase("msisdn") && !lcsClientFormatIndicator.equalsIgnoreCase("url") &&
                        !lcsClientFormatIndicator.equalsIgnoreCase("sip")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: ClientNameFormat value not API compliant, must be one of name, email, msisdn, url or sip " +
                            "for Notification type of Geolocation");
                    }
                }
            }
            if (operation.equalsIgnoreCase("PSL")) {
                String clientType = data.getFirst("ClientType");
                if (clientType != null) {
                    if (!clientType.equalsIgnoreCase("vas")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: ClientName is only valid for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter equals vas (value-added services)");
                    }
                }
                if (!data.containsKey("ClientNameFormat")) {
                    throw new NullPointerException("Rejected: ClientNameFormat value can not be null for Notification type of Geolocation in UMTS " +
                        "via MAP PSL when ClientName is provided and ClientType parameter equals vas (value-added services)");
                } else {
                    String lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
                    if (!lcsClientFormatIndicator.equalsIgnoreCase("name") && !lcsClientFormatIndicator.equalsIgnoreCase("email") &&
                        !lcsClientFormatIndicator.equalsIgnoreCase("msisdn") && !lcsClientFormatIndicator.equalsIgnoreCase("url") &&
                        !lcsClientFormatIndicator.equalsIgnoreCase("sip")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: ClientNameFormat value not API compliant, must be one of name, email, msisdn, url or sip " +
                            "for Notification type of Geolocation");
                    }
                }
            }
        }

        /*** ClientNameFormat must be API compliant: name, email, msisdn, url or sip for Notification type of Geolocation only ***/
        if (data.containsKey("ClientNameFormat") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientNameFormat only applies for Notification type of Geolocation in LTE");
        }
        if (data.containsKey("ClientNameFormat") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR") || operation.equalsIgnoreCase("PSL")) {
                String lcsClientFormatIndicator = data.getFirst("ClientNameFormat");
                if (!lcsClientFormatIndicator.equalsIgnoreCase("name") && !lcsClientFormatIndicator.equalsIgnoreCase("email")
                    && !lcsClientFormatIndicator.equalsIgnoreCase("msisdn") && !lcsClientFormatIndicator.equalsIgnoreCase("url")
                    && !lcsClientFormatIndicator.equalsIgnoreCase("sip")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ClientNameFormat value not API compliant, must be one of name, email, msisdn, url or sip " +
                        "for Notification type of Geolocation");
                }
            }
        }

        /*** ClientExternalID must be API compliant: 16 digits number for Notification type of Geolocation only in UMTS when Location Type is vas or emergency***/
        if (data.containsKey("ClientExternalID") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientExternalID only applies for Notification type of Geolocation in UMTS via MAP PSL");
        }
        if (data.containsKey("ClientExternalID") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PSL")) {
                String clientType = data.getFirst("ClientType");
                if (clientType != null) {
                    if (!clientType.equalsIgnoreCase("vas") && !clientType.equalsIgnoreCase("emergency")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: ClientExternalID is only valid for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter value equals vas (value-added services) or emergency");
                    } else {
                        String clientExternalID = data.getFirst("ClientExternalID");
                        if (!isStringNumericRange(clientExternalID, 1, 16)) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: ClientExternalID must be a number with no more than 16 digits");
                        }
                    }
                } else {
                    String lcs_client_type = gmlcConf.getString("lcs-client-type");
                    if (lcs_client_type.equals("")) {
                        throw new NullPointerException("Rejected: ClientType can not be null when ClientExternalID is provided");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ClientExternalID only applies for Notification type of Geolocation in UMTS via MAP PSL");
            }
        }
        if (!data.containsKey("ClientExternalID") && glType.toString().equals(NotificationGT)) {
            String lcs_client_external_id = gmlcConf.getString("lcs-client-external-id");
            if (lcs_client_external_id.equals("")) {
                String operation = data.getFirst("Operation");
                if (operation.equalsIgnoreCase("PSL")) {
                    String lcsClientType = data.getFirst("ClientType");
                    if (lcsClientType != null) {
                        if (lcsClientType.equalsIgnoreCase("vas") || lcsClientType.equalsIgnoreCase("emergency")) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: ClientExternalID can not be null for Notification type of Geolocation in UMTS via MAP PSL when " +
                                "ClientType parameter value equals vas (value-added services) or emergency");
                        }
                    }
                    String lcs_client_type = gmlcConf.getString("lcs-client-type");
                    if (lcs_client_type.equalsIgnoreCase("vas") || lcs_client_type.equalsIgnoreCase("emergency")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: lcs_client_external_id can not be empty for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter value equals vas (value-added services) or emergency");
                    }
                }
            } else {
                if (!isStringNumericRange(lcs_client_external_id, 1, 16)) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: lcs_client_external_id must be a number with no more than 16 digits");
                }
            }
        }

        /*** ClientInternalID must be API compliant: 16 digits number for Notification type of Geolocation only in UMTS when Client Type is operator ***/
        if (data.containsKey("ClientInternalID") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientInternalID only applies for Notification type of Geolocation in UMTS via MAP PSL");
        }
        if (data.containsKey("ClientInternalID") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PSL")) {
                String lcsClientType = data.getFirst("ClientType");
                if (lcsClientType != null) {
                    if (!lcsClientType.equalsIgnoreCase("operator")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: ClientInternalID is only valid for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter equals operator (PLMN operator services)");
                    } else {
                        try {
                            String clientInternalID = data.getFirst("ClientInternalID");
                            int lcsClientInternalID = Integer.parseInt(clientInternalID);
                            if (lcsClientInternalID < 0 || lcsClientInternalID > 4) {
                                httpBadRequest = true;
                                throw new IllegalArgumentException("Rejected: ClientInternalID must be a number for one of the following services: " +
                                    "0 (broadcastService), 1 (oandMHPLMN), 2 (oandMVPLMN), 3 (anonymousLocation) or 4 (targetMSsubscribedServiceSIP)");
                            }
                        } catch (NumberFormatException nfe) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: ClientInternalID must be a number for one of the following services: " +
                                "0 (broadcastService), 1 (oandMHPLMN), 2 (oandMVPLMN), 3 (anonymousLocation) or 4 (targetMSsubscribedServiceSIP)");
                        }
                    }
                } else {
                    String lcs_client_type = gmlcConf.getString("lcs-client-type");
                    if (lcs_client_type.equals("")) {
                        throw new NullPointerException("Rejected: ClientType can not be null when ClientInternalID is provided");
                    }
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ClientInternalID only applies for Notification type of Geolocation in UMTS via MAP PSL");
            }
        }
        if (!data.containsKey("ClientInternalID") && glType.toString().equals(NotificationGT)) {
            String lcs_client_internal_id = gmlcConf.getString("lcs-client-internal-id");
            if (lcs_client_internal_id.equals("")) {
                String operation = data.getFirst("Operation");
                if (operation.equalsIgnoreCase("PSL")) {
                    String clientType = data.getFirst("ClientType");
                    if (clientType != null) {
                        if (clientType.equalsIgnoreCase("operator")) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: ClientInternalID can not be null for Notification type of Geolocation in UMTS via MAP PSL when " +
                                "ClientType parameter value equals operator (PLMN operator services)");
                        }
                    }
                    String lcs_client_type = gmlcConf.getString("lcs-client-type");
                    if (lcs_client_type.equalsIgnoreCase("operator")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: lcs_client_internal_id can not be empty for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter value equals operator (PLMN operator services)");
                    }
                }
            } else {
                try {
                    int clientInternalId = Integer.parseInt(lcs_client_internal_id);
                    if (clientInternalId < 0 && clientInternalId > 4) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: cs-client-external-id must be a number for one of the following services: " +
                            "0 (broadcastService), 1 (oandMHPLMN), 2 (oandMVPLMN), 3 (anonymousLocation) or 4 (targetMSsubscribedServiceSIP)");
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: cs-client-external-id must be a number for one of the following services: " +
                        "0 (broadcastService), 1 (oandMHPLMN), 2 (oandMVPLMN), 3 (anonymousLocation) or 4 (targetMSsubscribedServiceSIP)");
                }
            }
        }


        /*** RequestorID must be API compliant: only for Notification type of Geolocation in LTE or UMTS when Client Type is vas ***/
        if (data.containsKey("RequestorID") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ClientName only applies for Notification type of Geolocation");
        }
        if (data.containsKey("RequestorID") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (!data.containsKey("RequestorIDFormat")) {
                    throw new NullPointerException("Rejected: RequestorIDFormat value can not be null for Notification type of Geolocation if " +
                        "RequestorID is provided");
                }
            }
            if (operation.equalsIgnoreCase("PSL")) {
                String lcsClientType = data.getFirst("ClientType");
                if (lcsClientType == null) {
                    lcsClientType = data.getFirst("ClientType");
                }
                if (lcsClientType != null) {
                    if (!lcsClientType.equalsIgnoreCase("vas")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: RequestorID only applies for Notification type of Geolocation in UMTS via MAP PSL when " +
                            "ClientType parameter equals vas (value-added services)");
                    } else {
                        if (!data.containsKey("RequestorIDFormat")) {
                            throw new NullPointerException("Rejected: RequestorIDFormat value can not be null for Notification type of Geolocation in UMTS " +
                                "via MAP PSL when RequestorID is provided and ClientType parameter equals vas (value-added services)");
                        } else {
                            String lcsRequestorFormatIndicator = data.getFirst("RequestorIDFormat");
                            if (!lcsRequestorFormatIndicator.equalsIgnoreCase("name") && !lcsRequestorFormatIndicator.equalsIgnoreCase("email") &&
                                !lcsRequestorFormatIndicator.equalsIgnoreCase("msisdn") && !lcsRequestorFormatIndicator.equalsIgnoreCase("url") &&
                                !lcsRequestorFormatIndicator.equalsIgnoreCase("sip")) {
                                httpBadRequest = true;
                                throw new IllegalArgumentException("Rejected: RequestorIDFormat values can be only name, email, msisdn, url or sip");
                            }
                        }
                    }
                }
            }
        }

        /**** LCS-QoS parameters ****/
        /***************************/
        /*** QoSClass must be API compliant: a positive integer value (0 or 1) for Notification type of Geolocation only ***/
        if (data.containsKey("QoSClass") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: QoSClass only applies for Notification type of Geolocation");
        }
        if (data.containsKey("QoSClass") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                String qosClass = data.getFirst("QoSClass");
                if (!qosClass.equalsIgnoreCase("assured") && !qosClass.equalsIgnoreCase("best-effort")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: QoSClass value not API compliant, must be one of assured or best-effort");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: QoSClass argument only applies for Notification type of Geolocation via Diameter SLg PLR ");
            }
        }
        /*** HorizontalAccuracy must be API compliant: a positive integer value (0-127) for Notification type of Geolocation only ***/
        if (data.containsKey("HorizontalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: HorizontalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("HorizontalAccuracy") && glType.toString().equals(NotificationGT)) {
            try {
                int qosHorizontalAccuracy = Integer.parseInt(data.getFirst("HorizontalAccuracy"));
                if (qosHorizontalAccuracy > 127 || qosHorizontalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: HorizontalAccuracy value not API compliant, must be a must be a positive number " +
                        "not greater than 127 (corresponding to 1800 Km)");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: HorizontalAccuracy value not API compliant, must be a must be a positive number " +
                    "not greater than 127 (corresponding to 1800 Km)");
            }
        }
        /*** VerticalAccuracy must be API compliant: a positive integer value (0-127) for Notification type of Geolocation only ***/
        if (data.containsKey("VerticalAccuracy") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VerticalAccuracy only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalAccuracy") && glType.toString().equals(NotificationGT)) {
            int qosVerticalAccuracy = Integer.parseInt(data.getFirst("VerticalAccuracy"));
            try {
                if (qosVerticalAccuracy > 127 || qosVerticalAccuracy < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: VerticalAccuracy value not API compliant, must be a positive integer value " +
                        "not greater than 127 (corresponding to 990.5 metres)");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalAccuracy value not API compliant, must be a positive positive integer value " +
                    "not greater than 127 (corresponding to 990.5 metres)");
            }
        }
        /*** VerticalCoordinateRequest must be API compliant: a boolean value for Notification type of Geolocation only ***/
        if (data.containsKey("VerticalCoordinateRequest") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VerticalCoordinateRequest only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VerticalCoordinateRequest") && glType.toString().equals(NotificationGT)) {
            String qosVerticalCoordinateRequest = data.getFirst("VerticalCoordinateRequest");
            if (!qosVerticalCoordinateRequest.equalsIgnoreCase("true") &&
                !qosVerticalCoordinateRequest.equalsIgnoreCase("false")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalCoordinateRequest value not API compliant, must be true or false");
            }
        }
        /*** ResponseTime must be API compliant: fast or slow for Notification type of Geolocation***/
        if (data.containsKey("ResponseTime") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: ResponseTime only applies for Notification type of Geolocation");
        }
        if (data.containsKey("ResponseTime") && glType.toString().equals(NotificationGT)) {
            String qosResponseTime = data.getFirst("ResponseTime");
            if (!qosResponseTime.equalsIgnoreCase("low") && !qosResponseTime.equalsIgnoreCase("tolerant")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ResponseTime value not API compliant, must be low or tolerant");
            }
        }

        /*** VelocityRequested must be API compliant: true of false for Notification type of Geolocation only ***/
        if (data.containsKey("VelocityRequested") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: VelocityRequested only applies for Notification type of Geolocation");
        }
        if (data.containsKey("VelocityRequested") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                String velocityRequested = data.getFirst("VelocityRequested");
                if (!velocityRequested.equalsIgnoreCase("true") && !velocityRequested.equalsIgnoreCase("false")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: VelocityRequested value not API compliant, must be true or false");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VelocityRequested argument only applies for Notification type of Geolocation via Diameter SLg PLR ");
            }
        }

        /*** Priority must be API compliant: normal or high for Notification type of Geolocation only ***/
        if (data.containsKey("Priority") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: Priority only applies for Notification type of Geolocation");
        }
        if (data.containsKey("Priority") && glType.toString().equals(NotificationGT)) {
            String lcsPriority = data.getFirst("Priority");
            if (!lcsPriority.equalsIgnoreCase("normal") && !lcsPriority.equalsIgnoreCase("high")) {
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
                long lcsServiceTypeId = Long.parseLong(data.getFirst("ServiceTypeID"));
                if (lcsServiceTypeId > 127 || lcsServiceTypeId < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ServiceTypeID value not API compliant, must be a positive integer value not greater than 127");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ServiceTypeID value not API compliant, must be a positive integer value not greater than 127");
            }
        }

        /**********************************************/
        /**** Area Event Info parameters ****/
        /*********************************************/
        /*** AreaEventType must belong to to Notification type of Geolocation and API compliant and specific DeferredLocationType values ***/
        if (data.containsKey("AreaEventType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventType") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationType")) {
                String deferredLocationType = data.getFirst("DeferredLocationType");
                if (!deferredLocationType.equalsIgnoreCase("entering") && !deferredLocationType.equalsIgnoreCase("leaving")
                    && !deferredLocationType.equalsIgnoreCase("inside")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, AreaEventType only applies when DeferredLocationType value equals " +
                        "entering, leaving or inside");
                }
            } else {
                throw new NullPointerException("Rejected: Not API compliant, AreaEventType only applies when DeferredLocationType is not null and equals " +
                    "entering, leaving or inside");
            }
            String geofenceType = data.getFirst("AreaEventType");
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (!geofenceType.equalsIgnoreCase("locationAreaId") && !geofenceType.equalsIgnoreCase("cellGlobalId")
                    && !geofenceType.equalsIgnoreCase("countryCode") && !geofenceType.equalsIgnoreCase("plmnId")
                    && !geofenceType.equalsIgnoreCase("routingAreaId") && !geofenceType.equalsIgnoreCase("utranCellId")
                    && !geofenceType.equalsIgnoreCase("trackingAreaId") && !geofenceType.equalsIgnoreCase("eUtranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventType value not API compliant, must be one of locationAreaId, cellGlobalId, " +
                        "countryCode, plmnId, routingAreaId, utranCellId, trackingAreaId or eUtranCellId for Notification type of Geolocation in LTE via " +
                        "Diameter SLg PLR");
                }
            } else if (operation.equalsIgnoreCase("PSL")) {
                if (!geofenceType.equalsIgnoreCase("locationAreaId") && !geofenceType.equalsIgnoreCase("cellGlobalId")
                    && !geofenceType.equalsIgnoreCase("countryCode") && !geofenceType.equalsIgnoreCase("plmnId")
                    && !geofenceType.equalsIgnoreCase("routingAreaId") && !geofenceType.equalsIgnoreCase("utranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventType value not API compliant, must be one of locationAreaId, cellGlobalId, " +
                        "countryCode, plmnId, routingAreaId or utranCellId for Notification type of Geolocation in UMTS via MAP PSL");
                }
            }
        }
        if (!data.containsKey("AreaEventType")) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String deferredLocationType = data.getFirst("DeferredLocationType");
                    if (deferredLocationType.equalsIgnoreCase("entering") || deferredLocationType.equalsIgnoreCase("leaving")
                        || deferredLocationType.equalsIgnoreCase("inside")) {
                        throw new NullPointerException("Rejected: AreaEventType must not be null when DeferredLocationType equals entering, leaving or inside for " +
                            "Notification type of Geolocation in LTE via Diameter Slg PLR");
                    }
                }
            }
        }
        /*** AreaEventId must belong to Notification type of Geolocation only ***/
        if (data.containsKey("AreaEventId") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventId only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("AreaEventId")) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String deferredLocationType = data.getFirst("DeferredLocationType");
                    if (deferredLocationType.equalsIgnoreCase("entering") || deferredLocationType.equalsIgnoreCase("leaving")
                        || deferredLocationType.equalsIgnoreCase("inside")) {
                        throw new NullPointerException("Rejected: AreaEventId must not be null when DeferredLocationType equals entering, leaving or inside for " +
                            "Notification type of Geolocation in LTE via Diameter Slg PLR");
                    }
                }
            }
        }
        /*** AreaEventOccurrence ***/
        if (data.containsKey("AreaEventOccurrence") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventOccurrence only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventOccurrence") && glType.toString().equals(NotificationGT)) {
            String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
            if (!lcsAreaEventOccurrenceInfo.equalsIgnoreCase("once") && !lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventOccurrence value not API compliant, " +
                    "must be one of once (one time event) or multiple (multiple time events)");
            }
            if (!data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventType and AreaEventId arguments must not be null when " +
                    "a valid AreaEventOccurrence is provided");
            }
        }
        /*** AreaEventInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("AreaEventInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventInterval") && glType.toString().equals(NotificationGT)) {
            try {
                long areaEventIntervalTime = Long.parseLong(data.getFirst("AreaEventInterval"));
                if (areaEventIntervalTime > 32767 || areaEventIntervalTime < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventInterval value not API compliant, must be a positive integer value " +
                        "not greater than 32767 (seconds)");
                }
                if (!data.containsKey("AreaEventOccurrence") && !data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventInterval does not apply if AreaEventId, AreaEventType and " +
                        "AreaEventOccurrence are not provided");
                }
                if (data.containsKey("AreaEventOccurrence")) {
                    String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
                    if (!lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventInterval does not apply when, " +
                            "AreaEventOccurrence is not multiple (multiple time events)");
                    }
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventInterval value not API compliant, must be a positive integer value " +
                    "not greater than 32767 (seconds)");
            }
        }
        /*** AreaEventMaxInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("AreaEventMaxInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventMaxInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventMaxInterval") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                try {
                    long areaEventMaxIntervalTime = Long.parseLong(data.getFirst("AreaEventMaxInterval"));
                    if (areaEventMaxIntervalTime > 86400 || areaEventMaxIntervalTime < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventMaxInterval value not API compliant, must be a positive integer value " +
                            "not greater than 86400 (seconds)");
                    }
                    if (!data.containsKey("AreaEventOccurrence") && !data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventType, AreaEventId and AreaEventOccurrence arguments must not be null " +
                            "when a valid AreaEventMaxInterval is provided");
                    }
                    if (data.containsKey("AreaEventOccurrence")) {
                        String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
                        if (!lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple")) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: AreaEventMaxInterval does not apply when, " +
                                "AreaEventOccurrence is not multiple (multiple time events)");
                        }
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventMaxInterval value not API compliant, must be a positive integer value " +
                        "not greater than 86400 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventMaxInterval only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
        }
        /*** AreaEventSamplingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 3600 ***/
        if (data.containsKey("AreaEventSamplingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventSamplingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventSamplingInterval") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                try {
                    long areaEventSamplingIntervalTime = Long.parseLong(data.getFirst("AreaEventSamplingInterval"));
                    if (areaEventSamplingIntervalTime > 3600 || areaEventSamplingIntervalTime < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventSamplingInterval value not API compliant, must be a positive integer value " +
                            "not greater than 3600 (seconds)");
                    }
                    if (!data.containsKey("AreaEventOccurrence") && !data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventType, AreaEventId and AreaEventOccurrence arguments must not be null " +
                            "when a valid AreaEventSamplingInterval is provided");
                    }
                    if (data.containsKey("AreaEventOccurrence")) {
                        String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
                        if (!lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple")) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: AreaEventSamplingInterval does not apply when, " +
                                "AreaEventOccurrence is not multiple (multiple time events)");
                        }
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventSamplingInterval value not API compliant, must be a positive integer value " +
                        "not greater than 3600 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventSamplingInterval only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
        }
        /*** AreaEventReportingDuration must be API compliant if present for Notification Geolocation only: integer value between 1 and 8640000 ***/
        if (data.containsKey("AreaEventReportingDuration") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AreaEventReportingDuration only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AreaEventReportingDuration") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                try {
                    long areaEventReportingDuration = Long.parseLong(data.getFirst("AreaEventReportingDuration"));
                    if (areaEventReportingDuration > 8640000 || areaEventReportingDuration < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventReportingDuration value not API compliant, must be a positive integer value " +
                            "not greater than 8640000 (seconds)");
                    }
                    if (!data.containsKey("AreaEventOccurrence") && !data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: AreaEventType, AreaEventId and AreaEventOccurrence arguments must not be null " +
                            "when a valid AreaEventReportingDuration is provided");
                    }
                    if (data.containsKey("AreaEventOccurrence")) {
                        String lcsAreaEventOccurrenceInfo = data.getFirst("AreaEventOccurrence");
                        if (!lcsAreaEventOccurrenceInfo.equalsIgnoreCase("multiple")) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: AreaEventReportingDuration does not apply when, " +
                                "AreaEventOccurrence is not multiple (multiple time events)");
                        }
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AreaEventReportingDuration value not API compliant, must be a positive integer value " +
                        "not greater than 8640000 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AreaEventReportingDuration only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
        }
        /*** AdditionalAreaEventType must belong to to Notification type of Geolocation and API compliant and specific DeferredLocationType values ***/
        if (data.containsKey("AdditionalAreaEventType") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AdditionalAreaEventType only applies for Notification type of Geolocation");
        }
        if (data.containsKey("AdditionalAreaEventType") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationType")) {
                String deferredLocationType = data.getFirst("DeferredLocationType");
                if (!deferredLocationType.equalsIgnoreCase("entering") && !deferredLocationType.equalsIgnoreCase("leaving")
                    && !deferredLocationType.equalsIgnoreCase("inside")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AdditionalAreaEventType only applies when DeferredLocationType value equals " +
                        "entering, leaving or inside");
                }
                if (!data.containsKey("AreaEventType") && !data.containsKey("AreaEventId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AdditionalAreaEventType only applies when valid AreaEventType and AreaEventType are provided");
                }
            } else {
                throw new NullPointerException("Rejected: AdditionalAreaEventType only applies when DeferredLocationType is not null and equals " +
                    "entering, leaving or inside");
            }
            String geofenceType = data.getFirst("AdditionalAreaEventType");
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (!geofenceType.equalsIgnoreCase("locationAreaId") && !geofenceType.equalsIgnoreCase("cellGlobalId")
                    && !geofenceType.equalsIgnoreCase("countryCode") && !geofenceType.equalsIgnoreCase("plmnId")
                    && !geofenceType.equalsIgnoreCase("routingAreaId") && !geofenceType.equalsIgnoreCase("utranCellId")
                    && !geofenceType.equalsIgnoreCase("trackingAreaId") && !geofenceType.equalsIgnoreCase("eUtranCellId")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: AdditionalAreaEventType value not API compliant, must be one of locationAreaId, cellGlobalId, " +
                        "countryCode, plmnId, routingAreaId, utranCellId, trackingAreaId or eUtranCellId for Notification type of Geolocation in LTE");
                }
            }
            if (!data.containsKey("AdditionalAreaEventId")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: AdditionalAreaEventId argument must not be null when a valid AdditionalAreaEventType is provided");
            }
        }
        if (!data.containsKey("AdditionalAreaEventType") && data.containsKey("AdditionalAreaEventId")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AdditionalAreaEventType argument must not be null when valid AdditionalAreaEventId is provided");
        }
        /*** AdditionalAreaEventId must belong to Notification type of Geolocation only ***/
        if (data.containsKey("AdditionalAreaEventId") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: AdditionalAreaEventId only applies for Notification type of Geolocation");
        }
        if (!data.containsKey("AdditionalAreaEventId") && data.containsKey("AdditionalAreaEventType")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AdditionalAreaEventId argument must not be null when a valid AdditionalAreaEventType is provided");
        }

        /**********************************/
        /**** Motion Event parameters ****/
        /*********************************/
        /*** MotionEventRange must belong to Notification type of Geolocation only and specific DeferredLocationType value ***/
        if (data.containsKey("MotionEventRange") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: EventRange only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventRange") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventRange only applies when DeferredLocationType value equals " +
                            "motion-event for Notification type of Geolocation in LTE via Diameter Slg PLR");
                    }
                    try {
                        long eventRange = Long.parseLong(data.getFirst("MotionEventRange"));
                        if (eventRange > 10000 || eventRange < 0) {
                            httpBadRequest = true;
                            throw new IllegalArgumentException("Rejected: EventRange value not API compliant, must be a positive integer value lower than 10000 (meters)");
                        }
                    } catch (NumberFormatException nfe) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: EventRange value not API compliant, must be a positive integer value");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, EventRange only applies when DeferredLocationType value equals " +
                        "motion-event for Notification type of Geolocation in LTE");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
        }
        if (!data.containsKey("MotionEventRange")) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        throw new NullPointerException("Rejected: MotionEventRange must not be null when DeferredLocationType equals motion-event for " +
                            "Notification type of Geolocation in LTE via Diameter Slg PLR");
                    }
                }
            }
        }
        /*** MotionEventOccurrence must belong to Notification type of Geolocation only ***/
        if (data.containsKey("MotionEventOccurrence") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventOccurrence only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventOccurrence") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventOccurrence only applies when DeferredLocationType " +
                            "value equals motion-event for Notification type of Geolocation in LTE via Diameter SLg PLR");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventOccurrence only applies when DeferredLocationType value " +
                        "equals motion-event for Notification type of Geolocation in LTE");
                }
                String motionEventOccurrence = data.getFirst("MotionEventOccurrence");
                if (!motionEventOccurrence.equalsIgnoreCase("once") && !motionEventOccurrence.equalsIgnoreCase("multiple")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventOccurrence value not API compliant, " +
                        "must be one of once (for one time event) or multiple (for multiple time events)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("MotionEventOccurrence only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
            if (!data.containsKey("MotionEventRange")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange argument must not be null when a valid " +
                    "MotionEventOccurrence is provided");
            }
        }
        /*** MotionEventInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventInterval") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventInterval only applies when DeferredLocationType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventInterval only applies when DeferredLocationType value " +
                        "equals motion-event for Notification type of Geolocation in LTE");
                }
                try {
                    long motionEventInterval = Long.parseLong(data.getFirst("MotionEventInterval"));
                    if (motionEventInterval > 32767 || motionEventInterval < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventInterval value not API compliant, must be a positive integer value not " +
                            "greater than 32767 (seconds)");
                    }
                    if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventRange and MotionEventOccurrence arguments must not be null when a valid " +
                            "MotionEventInterval is provided");
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventInterval value not API compliant, must be a positive integer value not " +
                        "greater than 32767 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventInterval only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
            if (!data.containsKey("MotionEventRange")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange argument must not be null when a valid " +
                    "MotionEventInterval is provided");
            }
        }
        /*** MotionEventMaxInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 86400 ***/
        if (data.containsKey("MotionEventMaxInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventMaxInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventMaxInterval") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventMaxInterval only applies when DeferredLocationType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventMaxInterval only applies when DeferredLocationType " +
                        "value equals motion-event for Notification type of Geolocation in LTE");
                }
                try {
                    long motionEventMaxInterval = Long.parseLong(data.getFirst("MotionEventMaxInterval"));
                    if (motionEventMaxInterval > 86400 || motionEventMaxInterval < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventMaxInterval value not API compliant, must be a positive integer value " +
                            "not greater than 86400 (seconds");
                    }
                    if (!data.containsKey("MotionEventRange") && !data.containsKey("MotionEventOccurrence") && !data.containsKey("MotionEventInterval")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventRange and/or MotionEventOccurrence and/or MotionEventInterval arguments " +
                            "must not be null when a valid MotionEventMaxInterval is provided");
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventMaxInterval value not API compliant, must be a positive integer value " +
                        "not greater than 86400 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventMaxInterval only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
            if (!data.containsKey("MotionEventRange")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange argument must not be null when a valid " +
                    "MotionEventMaxInterval is provided");
            }
        }
        /*** MotionEventSamplingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 3600 ***/
        if (data.containsKey("MotionEventSamplingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventSamplingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventSamplingInterval") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (operation.equalsIgnoreCase("PLR")) {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventSamplingInterval only applies when DeferredLocationType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventSamplingInterval only applies when DeferredLocationType " +
                        "value equals motion-event for Notification type of Geolocation in LTE");
                }
                try {
                    long motionEventSamplingInterval = Long.parseLong(data.getFirst("MotionEventSamplingInterval"));
                    if (motionEventSamplingInterval > 3600 || motionEventSamplingInterval < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventSamplingInterval value not API compliant, must be a positive integer value " +
                            "not greater than 3600 (seconds)");
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventSamplingInterval value not API compliant, must be a positive integer value " +
                        "not greater than 3600 (seconds)");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventSamplingInterval only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            }
            if (!data.containsKey("MotionEventRange")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange argument must not be null when a valid " +
                    "MotionEventSamplingInterval is provided");
            }
        }
        /*** MotionEventReportingDuration must be API compliant if present for Notification Geolocation only: integer value between 1 and 32767 ***/
        if (data.containsKey("MotionEventReportingDuration") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: MotionEventReportingDuration only applies for Notification type of Geolocation");
        }
        if (data.containsKey("MotionEventReportingDuration") && glType.toString().equals(NotificationGT)) {
            String operation = data.getFirst("Operation");
            if (!operation.equalsIgnoreCase("PLR")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventReportingDuration only applies to Notification type of Geolocation in LTE via Diameter Slg PLR");
            } else {
                if (data.containsKey("DeferredLocationType")) {
                    String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                    if (!lcsDeferredLocationType.equalsIgnoreCase("motion-event")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventReportingDuration only applies when DeferredLocationType " +
                            "value equals motion-event for Notification type of Geolocation in LTE");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: Not API compliant, MotionEventReportingDuration only applies when DeferredLocationType " +
                        "value equals motion-event for Notification type of Geolocation in LTE");
                }
                try {
                    long motionEventReportingDuration = Long.parseLong(data.getFirst("MotionEventReportingDuration"));
                    if (motionEventReportingDuration > 8640000 || motionEventReportingDuration < 0) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: MotionEventReportingDuration value not API compliant, must be a positive integer value " +
                            "not greater than 8640000 (seconds)");
                    }
                } catch (NumberFormatException nfe) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: MotionEventReportingDuration value not API compliant, must be a positive integer value " +
                        "not greater than 8640000 (seconds)");
                }
            }
            if (!data.containsKey("MotionEventRange")) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MotionEventRange argument must not be null when a valid " +
                    "MotionEventReportingDuration is provided");
            }
        }

        /********************************************************/
        /**** Periodic Location Deferred Request parameters ****/
        /******************************************************/
        /*** PeriodicReportingAmount must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("PeriodicReportingAmount") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: PeriodicReportingAmount only applies for Notification type of Geolocation");
        }
        if (data.containsKey("PeriodicReportingAmount") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationType")) {
                String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                String operation = data.getFirst("Operation");
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    if (!lcsDeferredLocationType.equalsIgnoreCase("periodic-ldr")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: PeriodicReportingAmount only applies when DeferredLocationType value " +
                            "equals periodic-ldr for Notification type of Geolocation");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: PeriodicReportingAmount only applies for Notification type of Geolocation in UMTS via MAP PSL " +
                        "or in LTE via Diameter SLg PLR");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PeriodicReportingAmount only applies when DeferredLocationType value equals " +
                    "periodic-ldr for Notification type of Geolocation");
            }
            try {
                long eventReportingAmount = Long.parseLong(data.getFirst("PeriodicReportingAmount"));
                if (eventReportingAmount > 8639999 || eventReportingAmount < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: PeriodicReportingAmount value not API compliant, must be a positive integer value not greater " +
                        "than 8639999");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PeriodicReportingAmount value not API compliant, must be a positive integer value not greater " +
                    "than 8639999");
            }
        }
        if (!data.containsKey("PeriodicReportingAmount") && data.containsKey("PeriodicReportingInterval")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: PeriodicReportingAmount must not be null when valid PeriodicReportingInterval is provided");
        }

        /*** PeriodicReportingInterval must be API compliant if present for Notification Geolocation only: integer value between 1 and 8639999 ***/
        if (data.containsKey("PeriodicReportingInterval") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("Rejected: PeriodicReportingInterval only applies for Notification type of Geolocation");
        }
        if (data.containsKey("PeriodicReportingInterval") && glType.toString().equals(NotificationGT)) {
            if (data.containsKey("DeferredLocationType")) {
                String lcsDeferredLocationType = data.getFirst("DeferredLocationType");
                String operation = data.getFirst("Operation");
                if (operation.equalsIgnoreCase("PSL") || operation.equalsIgnoreCase("PLR")) {
                    if (!lcsDeferredLocationType.equalsIgnoreCase("periodic-ldr")) {
                        httpBadRequest = true;
                        throw new IllegalArgumentException("Rejected: PeriodicReportingInterval only applies when DeferredLocationType value " +
                            "equals periodic-ldr for Notification type of Geolocation in LTE");
                    }
                } else {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: PeriodicReportingInterval only applies for Notification type of Geolocation in UMTS via MAP PSL " +
                        "or in LTE via Diameter SLg PLR");
                }
            } else {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PeriodicReportingAmount only applies when DeferredLocationType value equals " +
                    "periodic-ldr for Notification type of Geolocation");
            }
            try {
                long eventReportingInterval = Long.parseLong(data.getFirst("PeriodicReportingInterval"));
                if (eventReportingInterval > 8639999 || eventReportingInterval < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: PeriodicReportingInterval value not API compliant, must be a positive integer value not greater " +
                        "than 8639999");
                }
                if (!data.containsKey("PeriodicReportingAmount")) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: PeriodicReportingAmount argument must not be null when a valid PeriodicReportingInterval is provided");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: PeriodicReportingInterval value not API compliant, must be a positive integer value not greater " +
                    "than 8639999");
            }
        }
        if (!data.containsKey("PeriodicReportingInterval") && data.containsKey("PeriodicReportingAmount")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: PeriodicReportingInterval must not be null when valid PeriodicReportingAmount is provided");
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
                int refNumber = Integer.parseInt(data.getFirst("ReferenceNumber"));
                if (refNumber > Integer.MAX_VALUE || refNumber < 0) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ReferenceNumber value not API compliant, must be a positive integer value");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ReferenceNumber value not API compliant, must be a positive integer value");
            }
        }

        /*** DeviceLatitude must be API compliant***/
        // this is not used for now
        if (data.containsKey("DeviceLatitude")) {
            String deviceLat = data.getFirst("DeviceLatitude");
            Boolean devLatWGS84 = validateGeoCoordinatesFormat(deviceLat);
            if (!devLatWGS84) {
                throw new IllegalArgumentException("Rejected: DeviceLatitude not API compliant");
            }
        }

        /*** DeviceLongitude must be API compliant ***/
        // this is not used for now
        if (data.containsKey("DeviceLongitude")) {
            String deviceLong = data.getFirst("DeviceLongitude");
            Boolean devLongWGS84 = validateGeoCoordinatesFormat(deviceLong);
            if (!devLongWGS84) {
                throw new IllegalArgumentException("Rejected: DeviceLongitude not API compliant");
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
        String geolocType = glType.toString();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setMsisdn(getLong("MSISDN", data));
        builder.setImsi(getLong("IMSI", data));
        builder.setImei(data.getFirst("IMEI"));
        builder.setReferenceNumber(getLong("ReferenceNumber", data));
        builder.setGeolocationType(glType);
        if (glType.equals(GeolocationType.Immediate)) {
            if (data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null) {
                rStatus = responseStatus.Successful.toString();
                if (data.getFirst("NotReachableReason") != null)
                    rStatus = responseStatus.LastKnown.toString();
                if (getInteger("LocationAge", data) != null) {
                    if (getInteger("LocationAge", data) < 2)
                        rStatus = responseStatus.Successful.toString();
                    else
                        rStatus = responseStatus.LastKnown.toString();
                } else {
                    rStatus = responseStatus.LastKnown.toString();
                }
            } else if (getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                getLong("ECellId", data) != null || getLong("NRCellId", data) != null ||
                getInteger("TrackingAreaCode", data) != null || getInteger("RoutingAreaCode", data) != null) {
                rStatus = responseStatus.PartiallySuccessful.toString();
                if (data.getFirst("NotReachableReason") != null)
                    rStatus = responseStatus.LastKnown.toString();
                if (getInteger("LocationAge", data) != null) {
                    if (getInteger("LocationAge", data) < 2)
                        rStatus = responseStatus.PartiallySuccessful.toString();
                    else
                        rStatus = responseStatus.LastKnown.toString();
                } else {
                    rStatus = responseStatus.LastKnown.toString();
                }
            }
        } else if (glType.equals(GeolocationType.Notification)) {
            if (data.getFirst("DeviceLatitude") != null && data.getFirst("DeviceLongitude") != null) {
                rStatus = responseStatus.Successful.toString();
                if (getInteger("LocationAge", data) != null) {
                    if (getInteger("LocationAge", data) < 2)
                        rStatus = responseStatus.Successful.toString();
                    else
                        rStatus = responseStatus.LastKnown.toString();
                } else {
                    rStatus = responseStatus.LastKnown.toString();
                }
            } else if (getInteger("CellId", data) != null || getInteger("ServiceAreaCode", data) != null ||
                getLong("ECellId", data) != null || getLong("NRCellId", data) != null ||
                getInteger("TrackingAreaCode", data) != null || getInteger("RoutingAreaCode", data) != null ||
                getInteger("LocationAreaCode", data) != null) {
                rStatus = responseStatus.PartiallySuccessful.toString();
                if (getInteger("LocationAge", data) != null) {
                    if (getInteger("LocationAge", data) < 2)
                        rStatus = responseStatus.PartiallySuccessful.toString();
                    else
                        rStatus = responseStatus.LastKnown.toString();
                } else {
                    rStatus = responseStatus.LastKnown.toString();
                }
            }
        }
        builder.setResponseStatus(rStatus);
        builder.setCause(data.getFirst("Cause"));
        builder.setMobileCountryCode(getInteger("MobileCountryCode", data));
        builder.setMobileNetworkCode(getInteger("MobileNetworkCode", data));
        builder.setLocationAreaCode(getInteger("LocationAreaCode", data));
        builder.setCellId(getInteger("CellId", data));
        builder.setECellId(getLong("ECellId", data));
        builder.setNrCellId(getLong("NRCellId", data));
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
        builder.setDeferredLocationEventType(data.getFirst("DeferredLocationType"));
        builder.setGeofenceType(data.getFirst("AreaEventType"));
        builder.setGeofenceId(data.getFirst("AreaEventId"));
        builder.setMotionEventRange(getLong("MotionEventRange", data));
        builder.setCivicAddress(data.getFirst("CivicAddress"));
        builder.setBarometricPressure(getLong("BarometricPressure", data));
        builder.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        builder.setInternetAddress(data.getFirst("InternetAddress"));
        builder.setRadioAccessType(data.getFirst("RadioAccessType"));
        builder.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
            .append("/Geolocation/" + geolocType + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation buildFailedGeolocation(final Sid accountSid, final MultivaluedMap<String, String> data,
                                               Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        String geolocType = glType.toString();
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
            .append("/Geolocation/" + geolocType + "/").append(sid.toString());
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
            if (!isStringNumericRange(msisdn, 5, 15))  {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MSISDN amount of digits must not be greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setMsisdn(getLong("MSISDN", data));
        }

        if (data.containsKey("IMSI")) {
            String imsi = data.getFirst("IMSI");
            if (!isStringNumericRange(imsi, 14, 15))  {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: IMSI amount of digits must not be greater than 15");
            }
            updatedGeolocation = updatedGeolocation.setImsi(getLong("IMSI", data));
        }

        if (data.containsKey("IMEI")) {
            String imei = data.getFirst("IMEI");
            if (!isStringNumericRange(imei, 14, 15))  {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: IMEI amount of digits must not be greater than 15");
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
            if (!isStringNumericRange(mcc, 1, 3))  {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MobileCountryCode amount of digits must not be greater than 3");
            }
            updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }

        if (data.containsKey("MobileNetworkCode")) {
            String mnc = data.getFirst("MobileNetworkCode");
            if (!isStringNumericRange(mnc, 1, 3)) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: MobileNetworkCode amount of digits must not be greater than 3 (value set" +
                    " to "+mnc+")");
            } else {
                updatedGeolocation = updatedGeolocation.setMobileNetworkCode(getInteger("MobileNetworkCode", data));

            }
        }

        if (data.containsKey("LocationAreaCode")) {
            String lac = data.getFirst("LocationAreaCode");
            int digits = Integer.parseInt(lac);
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

        if (data.containsKey("ServiceAreaCode")) {
            String sai = data.getFirst("ServiceAreaCode");
            long digits = Long.parseLong(sai);
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

        if (data.containsKey("CellId")) {
            String ci = data.getFirst("CellId");
            long digits = Long.parseLong(ci);
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

        if (data.containsKey("ECellId")) {
            String ci = data.getFirst("ECellId");
            long digits = Long.parseLong(ci);
            try {
                if (digits > 268435455L) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: ECellId must be a number not greater than 268435455 (28 bits)");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: ECellId must be a number not greater than 268435455 (28 bits)");
            }
            updatedGeolocation = updatedGeolocation.setECellId(getLong("ECellId", data));
        }

        if (data.containsKey("NRCellId")) {
            String ci = data.getFirst("NRCellId");
            long digits = Long.parseLong(ci);
            try {
                if (digits > 68719476735L) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: NRCellId must be a number not greater than 68719476735 (36 bits)");
                }
            } catch (NumberFormatException nfe) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: NRCellId must be a number not greater than 68719476735 (36 bits)");
            }
            updatedGeolocation = updatedGeolocation.setNrCellId(getLong("NRCellId", data));
        }

        if (data.containsKey("ENodeBId")) {
            String ecid = data.getFirst("ENodeBId");
            long digits = Long.parseLong(ecid);
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
            int digits = Integer.parseInt(tac);
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
            int digits = Integer.parseInt(routeingAreaCode);
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
            if (!isStringNumericRange(gt, 1, 16)) {
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
            long digits = Integer.parseInt(aol);
            try {
                if (digits > 4294967295L) {
                    httpBadRequest = true;
                    throw new IllegalArgumentException("Rejected: LocationAge must be a number containing the " +
                        "elapsed time in minutes since the last network contact of the user equipment, with a value no longer than 4294967295");
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
            int hs = Integer.parseInt(horizontalSpeed);
            if (hs > 65535) {
                // 3GPP TS 23.032
                // Horizontal speed is encoded in increments of 1 kilometre per hour using a 16 bit binary coded number N
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: HorizontalSpeed must be a number not greater than 65535");
            } else {
                updatedGeolocation = updatedGeolocation.setHorizontalSpeed(getInteger("HorizontalSpeed", data));
            }
        }

        if (data.containsKey("VerticalSpeed")) {
            String verticalSpeed = data.getFirst("VerticalSpeed");
            int vs = Integer.parseInt(verticalSpeed);
            if (vs > 255) {
                // 3GPP TS 23.032
                // Vertical speed is encoded in increments of 1 kilometre per hour using 8 bits giving a number N between 0 and 28-1
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: VerticalSpeed must be a number not greater than 255");
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

        if (data.containsKey("DeferredLocationType")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: DeferredLocationType value can not be updated");
        }

        if (data.containsKey("AreaEventType")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AreaEventType value can not be updated");
        }

        if (data.containsKey("AreaEventId")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AreaEventId value can not be updated");
        }

        if (data.containsKey("AreaEventOccurrence")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AreaEventOccurrence value can not be updated");
        }

        if (data.containsKey("AreaEventInterval")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: AreaEventInterval value can not be updated");
        }

        if (data.containsKey("MotionEventRange")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: EventRange value can not be updated");
        }

        if (data.containsKey("PeriodicReportingAmount")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: PeriodicReportingAmount value can not be updated");
        }

        if (data.containsKey("PeriodicReportingInterval")) {
            httpBadRequest = true;
            throw new IllegalArgumentException("Rejected: PeriodicReportingInterval value can not be updated");
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
            long barometricPressure = Long.parseLong(data.getFirst("BarometricPressure"));
            if (barometricPressure > 4294967295L) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: BarometricPressure must not be a number not greater than maximum value of 4294967295");
            } else {
                updatedGeolocation = updatedGeolocation.setBarometricPressure(getLong("BarometricPressure", data));
            }
        }

        if (data.containsKey("RadioAccessType")) {
            String radioAccessType = data.getFirst("RadioAccessType");
            if (radioAccessType.length() > 20) {
                httpBadRequest = true;
                throw new IllegalArgumentException("Rejected: RadioAccessType length must not be greater than 20 digits");
            } else {
                updatedGeolocation = updatedGeolocation.setRadioAccessType(data.getFirst("RadioAccessType"));
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
        String geolocType = glType.toString();
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
            .append("/Geolocation/" + geolocType + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private HashMap<String, String> parseAtiOrPsiJsonString(String jsonLine) {
        HashMap<String, String> atiOrPsiResponse = new HashMap<>();
        JsonElement jsonElement = new JsonParser().parse(jsonLine);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String network = jsonObject.get("network").getAsString();
        atiOrPsiResponse.put("network", network);
        String protocol = jsonObject.get("protocol").getAsString();
        atiOrPsiResponse.put("protocol", protocol);
        String operation = jsonObject.get("operation").getAsString();
        atiOrPsiResponse.put("operation", operation);

        JsonObject csLocationInformation = jsonElement.getAsJsonObject();
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

            JsonObject cgi = csLocationInformation.getAsJsonObject("CGI");
            if (cgi != null) {
                if (cgi.get("mcc") != null) {
                    String csMcc = cgi.get("mcc").getAsString();
                    if (!csMcc.isEmpty())
                        atiOrPsiResponse.put("csMcc", csMcc);
                }
                if (cgi.get("mnc") != null) {
                    String csMnc = cgi.get("mnc").getAsString();
                    if (!csMnc.isEmpty())
                        atiOrPsiResponse.put("csMnc", csMnc);
                }
                if (cgi.get("lac") != null) {
                    String csLac = cgi.get("lac").getAsString();
                    if (!csLac.isEmpty())
                        atiOrPsiResponse.put("csLac", csLac);
                }
                if (cgi.get("ci") != null) {
                    String csCi = cgi.get("ci").getAsString();
                    if (!csCi.isEmpty())
                        atiOrPsiResponse.put("csCi", csCi);
                }
            }

            JsonObject sai = csLocationInformation.getAsJsonObject("SAI");
            if (sai != null) {
                if (sai.get("mcc") != null) {
                    String csMcc = sai.get("mcc").getAsString();
                    if (!csMcc.isEmpty())
                        atiOrPsiResponse.put("csMcc", csMcc);
                }
                if (sai.get("mnc") != null) {
                    String csMnc = sai.get("mnc").getAsString();
                    if (!csMnc.isEmpty())
                        atiOrPsiResponse.put("csMnc", csMnc);
                }
                if (sai.get("lac") != null) {
                    String csLac = sai.get("lac").getAsString();
                    if (!csLac.isEmpty())
                        atiOrPsiResponse.put("csLac", csLac);
                }
                if (sai.get("sac") != null) {
                    String csSac = sai.get("sac").getAsString();
                    if (!csSac.isEmpty())
                        atiOrPsiResponse.put("csSac", csSac);
                }
            }

            JsonObject lai = csLocationInformation.getAsJsonObject("LAI");
            if (lai != null) {
                if (lai.get("mcc") != null) {
                    String csMcc = lai.get("mcc").getAsString();
                    if (!csMcc.isEmpty())
                        atiOrPsiResponse.put("csMcc", csMcc);
                }
                if (lai.get("mnc") != null) {
                    String csMnc = lai.get("mnc").getAsString();
                    if (!csMnc.isEmpty())
                        atiOrPsiResponse.put("csMnc", csMnc);
                }
                if (lai.get("lac") != null) {
                    String csLac = lai.get("lac").getAsString();
                    if (!csLac.isEmpty())
                        atiOrPsiResponse.put("csLac", csLac);
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
                    if (ecgi.get("eci") != null) {
                        String ecgiEci = ecgi.get("eci").getAsString();
                        if (!ecgiEci.isEmpty())
                            atiOrPsiResponse.put("ecgiEci", ecgiEci);
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
                    if (geodeticInformation.get("latitude") != null) {
                        String epsGeodLatitude = geodeticInformation.get("latitude").getAsString();
                        if (!epsGeodLatitude.isEmpty())
                            atiOrPsiResponse.put("epsGeodLatitude", epsGeodLatitude);
                    }
                    if (geodeticInformation.get("longitude") != null) {
                        String epsGeodLongitude = geodeticInformation.get("longitude").getAsString();
                        if (!epsGeodLongitude.isEmpty())
                            atiOrPsiResponse.put("epsGeodLongitude", epsGeodLongitude);
                    }
                    if (geodeticInformation.get("uncertainty") != null) {
                        String epsGeodUncertainty = geodeticInformation.get("uncertainty").getAsString();
                        if (!epsGeodUncertainty.isEmpty())
                            atiOrPsiResponse.put("epsGeodUncertainty", epsGeodUncertainty);
                    }
                    if (geodeticInformation.get("confidence") != null) {
                        String epsGeodConfidence = geodeticInformation.get("confidence").getAsString();
                        if (!epsGeodConfidence.isEmpty())
                            atiOrPsiResponse.put("epsGeodConfidence", epsGeodConfidence);
                    }
                    if (geodeticInformation.get("screeningAndPresentationIndicators") != null) {
                        String epsGeodScreeningAndPresentationIndicators = geodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                        if (!epsGeodScreeningAndPresentationIndicators.isEmpty())
                            atiOrPsiResponse.put("epsGeodScreeningAndPresentationIndicators", epsGeodScreeningAndPresentationIndicators);
                    }
                }
                if (epsLocationInformation.get("mmeName") != null) {
                    String mmeName = epsLocationInformation.get("mmeName").getAsString();
                    if (!mmeName.isEmpty())
                        atiOrPsiResponse.put("mmeName", mmeName);
                }
                if (csLocationInformation.get("ageOfLocationInformation") != null || epsLocationInformation.get("ageOfLocationInformation") != null) {
                    String aol = epsLocationInformation.get("ageOfLocationInformation") != null ?
                        epsLocationInformation.get("ageOfLocationInformation").getAsString() :
                        csLocationInformation.get("ageOfLocationInformation").getAsString();
                    if (!aol.isEmpty())
                        atiOrPsiResponse.put("aol", aol);
                }
                if (csLocationInformation.get("currentLocationRetrieved") != null || epsLocationInformation.get("currentLocationRetrieved") != null) {
                    String current = epsLocationInformation.get("currentLocationRetrieved") != null ?
                        epsLocationInformation.get("currentLocationRetrieved").getAsString() :
                        csLocationInformation.get("currentLocationRetrieved").getAsString();
                    if (!current.isEmpty())
                        atiOrPsiResponse.put("currentLocationRetrieved", current);
                }
            }
            if (csLocationInformation.get("vlrNumber") != null) {
                String vlrNumber = csLocationInformation.get("vlrNumber").getAsString();
                if (!vlrNumber.isEmpty())
                    atiOrPsiResponse.put("vlrNumber", vlrNumber);
            }
            if (csLocationInformation.get("mscNumber") != null) {
                String mscNumber = csLocationInformation.get("mscNumber").getAsString();
                if (!mscNumber.isEmpty())
                    atiOrPsiResponse.put("mscNumber", mscNumber);
            }
        }


        JsonObject psLocationInformation = jsonElement.getAsJsonObject();
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
            JsonObject psCgi = psLocationInformation.getAsJsonObject("CGI");
            if (psCgi != null) {
                if (psCgi.get("mcc") != null) {
                    String psMcc = psCgi.get("mcc").getAsString();
                    if (!psMcc.isEmpty())
                        atiOrPsiResponse.put("psMcc", psMcc);
                }
                if (psCgi.get("mnc") != null) {
                    String psMnc = psCgi.get("mnc").getAsString();
                    if (!psMnc.isEmpty())
                        atiOrPsiResponse.put("psMnc", psMnc);
                }
                if (psCgi.get("lac") != null) {
                    String psLac = psCgi.get("lac").getAsString();
                    if (!psLac.isEmpty())
                        atiOrPsiResponse.put("psLac", psLac);
                }
                if (psCgi.get("ci") != null) {
                    String psCi = psCgi.get("ci").getAsString();
                    if (!psCi.isEmpty())
                        atiOrPsiResponse.put("psCi", psCi);
                }
            }
            JsonObject psSai = psLocationInformation.getAsJsonObject("SAI");
            if (psSai != null) {
                if (psSai.get("mcc") != null) {
                    String psMcc = psSai.get("mcc").getAsString();
                    if (!psMcc.isEmpty())
                        atiOrPsiResponse.put("psMcc", psMcc);
                }
                if (psSai.get("mnc") != null) {
                    String psMnc = psSai.get("mnc").getAsString();
                    if (!psMnc.isEmpty())
                        atiOrPsiResponse.put("psMnc", psMnc);
                }
                if (psSai.get("lac") != null) {
                    String psLac = psSai.get("lac").getAsString();
                    if (!psLac.isEmpty())
                        atiOrPsiResponse.put("psLac", psLac);
                }
                if (psSai.get("sac") != null) {
                    String psSac = psSai.get("sac").getAsString();
                    if (!psSac.isEmpty())
                        atiOrPsiResponse.put("psSac", psSac);
                }
            }
            JsonObject psLai = psLocationInformation.getAsJsonObject("LAI");
            if (psLai != null) {
                if (psLai.get("mcc") != null) {
                    String psMcc = psLai.get("mcc").getAsString();
                    if (!psMcc.isEmpty())
                        atiOrPsiResponse.put("psMcc", psMcc);
                }
                if (psLai.get("mnc") != null) {
                    String psMnc = psLai.get("mnc").getAsString();
                    if (!psMnc.isEmpty())
                        atiOrPsiResponse.put("psMnc", psMnc);
                }
                if (psLai.get("lac") != null) {
                    String psLac = psLai.get("lac").getAsString();
                    if (!psLac.isEmpty())
                        atiOrPsiResponse.put("psLac", psLac);
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
            if (psLocationInformation.get("ageOfLocationInformation") != null) {
                String aol = psLocationInformation.get("ageOfLocationInformation").getAsString();
                if (!aol.isEmpty())
                    atiOrPsiResponse.put("aol", aol);
            }
            if (psLocationInformation.get("currentLocationRetrieved") != null) {
                String current = psLocationInformation.get("currentLocationRetrieved").getAsString();
                if (!current.isEmpty())
                    atiOrPsiResponse.put("currentLocationRetrieved", current);
            }
            if (psLocationInformation.get("sgsnNumber") != null) {
                String sgsnNumber = psLocationInformation.get("sgsnNumber").getAsString();
                if (!sgsnNumber.isEmpty())
                    atiOrPsiResponse.put("sgsnNumber", sgsnNumber);
            }
        }

        JsonObject grpsMSClass = jsonElement.getAsJsonObject();
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

        JsonObject mnpInfoResult = jsonElement.getAsJsonObject();
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

        if (jsonObject.get("msisdn") != null) {
            String msisdn = jsonObject.get("msisdn").getAsString();
            if (!msisdn.isEmpty())
                atiOrPsiResponse.put("msisdn", msisdn);
        }
        if (jsonObject.get("imsi") != null) {
            String imsi = jsonObject.get("imsi").getAsString();
            if (!imsi.isEmpty())
                atiOrPsiResponse.put("imsi", imsi);
        }
        if (jsonObject.get("imei") != null) {
            String imei = jsonObject.get("imei").getAsString();
            if (!imei.isEmpty())
                atiOrPsiResponse.put("imei", imei);
        }
        if (jsonObject.get("lmsi") != null) {
            String lmsi = jsonObject.get("lmsi").getAsString();
            if (!lmsi.isEmpty())
                atiOrPsiResponse.put("lmsi", lmsi);
        }
        if (jsonObject.get("subscriberState") != null) {
            String subscriberState = jsonObject.get("subscriberState").getAsString();
            if (!subscriberState.isEmpty())
                atiOrPsiResponse.put("subscriberState", subscriberState);
        }
        if (jsonObject.get("notReachableReason") != null) {
            String notReachableReason = jsonObject.get("notReachableReason").getAsString();
            if (!notReachableReason.isEmpty())
                atiOrPsiResponse.put("notReachableReason", notReachableReason);
        }
        if (jsonObject.get("msClassmark") != null) {
            String msClassmark = jsonObject.get("msClassmark").getAsString();
            if (!msClassmark.isEmpty())
                atiOrPsiResponse.put("msClassmark", msClassmark);
        }
        if (jsonObject.get("errorReason") != null) {
            String errorReason = jsonObject.get("errorReason").getAsString();
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

        String eCi = atiOrPsiResponse.get("ecgiEci");
        if (eCi != null)
            data.putSingle("ECellId", eCi);

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
        JsonElement jsonElement = new JsonParser().parse(jsonLine);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject.get("clientReferenceNumber") != null) {
            String lcsReferenceNumber = jsonObject.get("clientReferenceNumber").getAsString();
            sriPslResponse.put("lcsReferenceNumber", lcsReferenceNumber);
        }
        if (jsonObject.get("errorReason") != null) {
            String errorReason = jsonObject.get("errorReason").getAsString();
            sriPslResponse.put("errorReason", errorReason);
        }

        JsonObject sri = jsonObject.getAsJsonObject("SRILCS");
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

        JsonObject psl = jsonElement.getAsJsonObject();
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

            JsonObject additionalLocationEstimate = psl.getAsJsonObject("AdditionalLocationEstimate");
            if (additionalLocationEstimate != null) {
                if (additionalLocationEstimate.get("typeOfShape") != null) {
                    String typeOfShape = additionalLocationEstimate.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        sriPslResponse.put("typeOfShape", typeOfShape);
                    if (typeOfShape.equalsIgnoreCase("Polygon")) {
                        if (additionalLocationEstimate.get("numberOfPoints") != null) {
                            int numberOfPoints = additionalLocationEstimate.get("numberOfPoints").getAsInt();
                            sriPslResponse.put("numberOfPoints", String.valueOf(numberOfPoints));
                        }
                        JsonObject additionalLocationPolygonCentroid = additionalLocationEstimate.getAsJsonObject("polygonCentroid");
                        if (additionalLocationPolygonCentroid != null) {
                            if (additionalLocationPolygonCentroid.get("latitude") != null) {
                                String latitude = additionalLocationPolygonCentroid.get("latitude").getAsString();
                                if (!latitude.isEmpty())
                                    sriPslResponse.put("latitude", latitude);
                            }
                            if (additionalLocationPolygonCentroid.get("longitude") != null) {
                                String longitude = additionalLocationPolygonCentroid.get("longitude").getAsString();
                                if (!longitude.isEmpty())
                                    sriPslResponse.put("longitude", longitude);
                            }
                        }
                    } else {
                        if (additionalLocationEstimate.get("latitude") != null) {
                            String latitude = additionalLocationEstimate.get("latitude").getAsString();
                            if (!latitude.isEmpty())
                                sriPslResponse.put("latitude", latitude);
                        }
                        if (additionalLocationEstimate.get("longitude") != null) {
                            String longitude = additionalLocationEstimate.get("longitude").getAsString();
                            if (!longitude.isEmpty())
                                sriPslResponse.put("longitude", longitude);
                        }
                        if (additionalLocationEstimate.get("altitude") != null) {
                            String altitude = additionalLocationEstimate.get("altitude").getAsString();
                            if (!altitude.isEmpty())
                                sriPslResponse.put("altitude", altitude);
                        }
                    }
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
            JsonObject cgi = psl.getAsJsonObject("CGI");
            if (cgi != null) {
                if (cgi.get("mcc") != null) {
                    String mcc = cgi.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        sriPslResponse.put("mcc", mcc);
                }
                if (cgi.get("mnc") != null) {
                    String mnc = cgi.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        sriPslResponse.put("mnc", mnc);
                }
                if (cgi.get("lac") != null) {
                    String lac = cgi.get("lac").getAsString();
                    if (!lac.isEmpty())
                        sriPslResponse.put("lac", lac);
                }
                if (cgi.get("ci") != null) {
                    String ci = cgi.get("ci").getAsString();
                    if (!ci.isEmpty())
                        sriPslResponse.put("ci", ci);
                }
            }
            JsonObject sai = psl.getAsJsonObject("SAI");
            if (sai != null) {
                if (sai.get("mcc") != null) {
                    String mcc = sai.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        sriPslResponse.put("mcc", mcc);
                }
                if (sai.get("mnc") != null) {
                    String mnc = sai.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        sriPslResponse.put("mnc", mnc);
                }
                if (sai.get("lac") != null) {
                    String lac = sai.get("lac").getAsString();
                    if (!lac.isEmpty())
                        sriPslResponse.put("lac", lac);
                }
                if (sai.get("sac") != null) {
                    String sac = sai.get("sac").getAsString();
                    if (!sac.isEmpty())
                        sriPslResponse.put("sac", sac);
                }
            }
            JsonObject lai = psl.getAsJsonObject("LAI");
            if (lai != null) {
                if (lai.get("mcc") != null) {
                    String mcc = lai.get("mcc").getAsString();
                    if (!mcc.isEmpty())
                        sriPslResponse.put("mcc", mcc);
                }
                if (lai.get("mnc") != null) {
                    String mnc = lai.get("mnc").getAsString();
                    if (!mnc.isEmpty())
                        sriPslResponse.put("mnc", mnc);
                }
                if (lai.get("lac") != null) {
                    String lac = lai.get("lac").getAsString();
                    if (!lac.isEmpty())
                        sriPslResponse.put("lac", lac);
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

        /* Parameters retrieved from GMLC but not used for now in Geolocation API
        String gprsNodeIndicator = sriPslResponse.get("gprsNodeIndicator");
        String deferredMTLRresponseIndicator = sriPslResponse.get("deferredMTLRresponseIndicator");
        String geranPositioningInfo = sriPslResponse.get("geranPositioningInfo");
        String geranGanssPositioningData = sriPslResponse.get("geranGanssPositioningData");
        String utranPositioningInfo = sriPslResponse.get("utranPositioningInfo");
        String utranGanssPositioningData = sriPslResponse.get("utranGanssPositioningData");
        String velocityType = sriPslResponse.get("velocityType");*/

    }


    private HashMap<String, String> parsePlrJsonString(String jsonLine) {
        HashMap <String, String> rirPlrResponse = new HashMap<>();
        JsonElement jsonElement = new JsonParser().parse(jsonLine);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject.get("clientReferenceNumber") != null) {
            String lcsReferenceNumber = jsonObject.get("clientReferenceNumber").getAsString();
            if (!lcsReferenceNumber.isEmpty())
                rirPlrResponse.put("lcsReferenceNumber", lcsReferenceNumber);
        }
        if (jsonObject.get("errorReason") != null) {
            String errorReason = jsonObject.get("errorReason").getAsString();
            rirPlrResponse.put("errorReason", errorReason);
        }

        JsonObject ria = jsonObject.getAsJsonObject("Routing-Info-Answer");
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
        JsonObject pla = jsonElement.getAsJsonObject();
        pla = pla.getAsJsonObject("Provide-Location-Answer");
        if (pla != null) {
            JsonObject locationEstimate = pla.getAsJsonObject("LocationEstimate");
            if (locationEstimate != null) {
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
                if (locationEstimate.get("typeOfShape") != null) {
                    String typeOfShape = locationEstimate.get("typeOfShape").getAsString();
                    if (!typeOfShape.isEmpty())
                        rirPlrResponse.put("typeOfShape", typeOfShape);
                    if (typeOfShape.equalsIgnoreCase("Polygon")) {
                        if (locationEstimate.get("numberOfPoints") != null) {
                            int numberOfPoints = locationEstimate.get("numberOfPoints").getAsInt();
                            rirPlrResponse.put("numberOfPoints", String.valueOf(numberOfPoints));
                        }
                        JsonObject locationEstimatePolygonCentroid = locationEstimate.getAsJsonObject("polygonCentroid");
                        if (locationEstimatePolygonCentroid != null) {
                            if (locationEstimatePolygonCentroid.get("latitude") != null) {
                                String latitude = locationEstimatePolygonCentroid.get("latitude").getAsString();
                                if (!latitude.isEmpty())
                                    rirPlrResponse.put("latitude", latitude);
                            }
                            if (locationEstimatePolygonCentroid.get("longitude") != null) {
                                String longitude = locationEstimatePolygonCentroid.get("longitude").getAsString();
                                if (!longitude.isEmpty())
                                    rirPlrResponse.put("longitude", longitude);
                            }
                        }
                    }
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

            JsonObject cgi = pla.getAsJsonObject("CGI");
            if (cgi != null) {
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
            }
            JsonObject sai = pla.getAsJsonObject("SAI");
            if (sai != null) {
                if (sai.get("mcc") != null) {
                    String saiMcc = sai.get("mcc").getAsString();
                    if (!saiMcc.isEmpty())
                        rirPlrResponse.put("mcc", saiMcc);
                }
                if (sai.get("mnc") != null) {
                    String saiMnc = sai.get("mnc").getAsString();
                    if (!saiMnc.isEmpty())
                        rirPlrResponse.put("mnc", saiMnc);
                }
                if (sai.get("lac") != null) {
                    String saiLac = sai.get("lac").getAsString();
                    if (!saiLac.isEmpty())
                        rirPlrResponse.put("lac", saiLac);
                }
                if (sai.get("sac") != null) {
                    String saiSac = sai.get("sac").getAsString();
                    if (!saiSac.isEmpty())
                        rirPlrResponse.put("sac", saiSac);
                }
            }

            JsonObject ecgi = pla.getAsJsonObject("ECGI");
            if (ecgi != null) {
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
                if (ecgi.get("eci") != null) {
                    String ecgiCi = ecgi.get("eci").getAsString();
                    if (!ecgiCi.isEmpty())
                        rirPlrResponse.put("lteCi", ecgiCi);
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
                if (eUtranPosInfo.get("eUtranPositioningData") != null) {
                    String eUtranPositioningData = eUtranPosInfo.get("eUtranPositioningData").getAsString();
                    if (!eUtranPositioningData.isEmpty())
                        rirPlrResponse.put("eUtranPositioningData", eUtranPositioningData);
                }
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

        String lteCi = rirPlrResponse.get("lteCi");
        if (lteCi != null)
            data.putSingle("ECellId", lteCi);

        String eNodeBId = rirPlrResponse.get("eNBId");
        if (eNodeBId != null)
            data.putSingle("ENodeBId", eNodeBId);

        String eUtranCgiCi = rirPlrResponse.get("ecgiCi");
        if (eUtranCgiCi != null)
            data.putSingle("CellId", eUtranCgiCi);

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
        JsonElement jsonElement = new JsonParser().parse(jsonLine);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String network = jsonObject.get("network").getAsString();
        udrResponse.put("network", network);
        String protocol = jsonObject.get("protocol").getAsString();
        udrResponse.put("protocol", protocol);
        String operation = jsonObject.get("operation").getAsString();
        udrResponse.put("operation", operation);

        if (jsonObject.get("errorReason") != null) {
            String errorReason = jsonObject.get("errorReason").getAsString();
            udrResponse.put("errorReason", errorReason);
        }

        JsonObject publicIdentifiers = jsonElement.getAsJsonObject();
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

        JsonObject csLocationInformation = jsonElement.getAsJsonObject();
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
                    udrResponse.put("locationNumberAddress", address);
                }
            }

            JsonObject csLai = csLocationInformation.getAsJsonObject("LAI");
            if (csLai != null) {
                if (csLai.get("mcc") != null) {
                    String csMcc = csLai.get("mcc").getAsString();
                    udrResponse.put("shMcc", csMcc);
                }
                if (csLai.get("mnc") != null) {
                    String csMnc = csLai.get("mnc").getAsString();
                    udrResponse.put("shMnc", csMnc);
                }
                if (csLai.get("lac") != null) {
                    String csLac = csLai.get("lac").getAsString();
                    udrResponse.put("shLac", csLac);
                }
            }

            JsonObject csCgi = csLocationInformation.getAsJsonObject("CGI");
            if (csCgi != null) {
                if (csCgi.get("mcc") != null) {
                    String csMcc = csCgi.get("mcc").getAsString();
                    udrResponse.put("shMcc", csMcc);
                }
                if (csCgi.get("mnc") != null) {
                    String csMnc = csCgi.get("mnc").getAsString();
                    udrResponse.put("shMnc", csMnc);
                }
                if (csCgi.get("lac") != null) {
                    String csLac = csCgi.get("lac").getAsString();
                    udrResponse.put("shLac", csLac);
                }
                if (csCgi.get("ci") != null) {
                    String csCi = csCgi.get("ci").getAsString();
                    udrResponse.put("shCi", csCi);
                }
            }

            JsonObject csSai = csLocationInformation.getAsJsonObject("SAI");
            if (csSai != null) {
                if (csSai.get("mcc") != null) {
                    String csMcc = csSai.get("mcc").getAsString();
                    udrResponse.put("shMcc", csMcc);
                }
                if (csSai.get("mnc") != null) {
                    String csMnc = csSai.get("mnc").getAsString();
                    udrResponse.put("shMnc", csMnc);
                }
                if (csSai.get("lac") != null) {
                    String csLac = csSai.get("lac").getAsString();
                    udrResponse.put("shLac", csLac);
                }
                if (csSai.get("sac") != null) {
                    String csSac = csSai.get("sac").getAsString();
                    udrResponse.put("shSac", csSac);
                }
            }

            JsonObject csGeographicalInformation = csLocationInformation.getAsJsonObject("GeographicalInformation");
            if (csGeographicalInformation != null) {
                if (csGeographicalInformation.get("typeOfShape") != null) {
                    String csTypeOfShape = csGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeogTypeOfShape", csTypeOfShape);
                }
                if (csGeographicalInformation.get("latitude") != null) {
                    String csLatitude = csGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("shGeogLatitude", csLatitude);
                }
                if (csGeographicalInformation.get("longitude") != null) {
                    String csLongitude = csGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("shGeogLongitude", csLongitude);
                }
                if (csGeographicalInformation.get("uncertainty") != null) {
                    String csUncertainty = csGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeogUncertainty", csUncertainty);
                }
            }

            JsonObject csGeodeticInformation = csLocationInformation.getAsJsonObject("GeodeticInformation");
            if (csGeodeticInformation != null) {
                if (csGeodeticInformation.get("typeOfShape") != null) {
                    String csGeodTypeOfShape = csGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeodeticTypeOfShape", csGeodTypeOfShape);
                }
                if (csGeodeticInformation.get("latitude") != null) {
                    String csGeodLatitude = csGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("shGeodeticLatitude", csGeodLatitude);
                }
                if (csGeodeticInformation.get("longitude") != null) {
                    String csGeodLongitude = csGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("shGeodeticLongitude", csGeodLongitude);
                }
                if (csGeodeticInformation.get("uncertainty") != null) {
                    String csGeodUncertainty = csGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeodeticUncertainty", csGeodUncertainty);
                }
                if (csGeodeticInformation.get("confidence") != null) {
                    String csConfidence = csGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("shGeodeticConfidence", csConfidence);
                }
                if (csGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String csScreeningAndPresentationIndicators = csGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("shGeodeticScreeningAndPresentationIndicators", csScreeningAndPresentationIndicators);
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
                    udrResponse.put("shAgeOfLocationInformation", csAgeOfLocationInfo);
                }
            }

            if (csLocationInformation.get("currentLocationRetrieved") != null) {
                String csCurrentLocationRetrieved = csLocationInformation.get("currentLocationRetrieved").getAsString();
                if (csCurrentLocationRetrieved != null) {
                    udrResponse.put("shCurrentLocationRetrieved", csCurrentLocationRetrieved);
                }
            }

            if (csLocationInformation.get("ratType") != null) {
                String ratType = csLocationInformation.get("ratType").getAsString();
                if (ratType != null) {
                    udrResponse.put("shRatType", ratType);
                }
            }

            JsonObject csEpsLocationInformation = csLocationInformation.getAsJsonObject("EPSLocationInformation");
            if (csEpsLocationInformation != null) {
                JsonObject trackingAreaId = csEpsLocationInformation.getAsJsonObject("TAI");
                if (trackingAreaId != null) {
                    if (trackingAreaId.get("mcc") != null) {
                        String taiMcc = trackingAreaId.get("mcc").getAsString();
                        udrResponse.put("shMcc", taiMcc);
                    }
                    if (trackingAreaId.get("mnc") != null) {
                        String taiMnc = trackingAreaId.get("mnc").getAsString();
                        udrResponse.put("shMnc", taiMnc);
                    }
                    if (trackingAreaId.get("tac") != null) {
                        String tac = trackingAreaId.get("tac").getAsString();
                        udrResponse.put("shTac", tac);
                    }
                }

                JsonObject epsCgi = csEpsLocationInformation.getAsJsonObject("ECGI");
                if (epsCgi != null) {
                    if (epsCgi.get("mcc") != null) {
                        String epsMcc = epsCgi.get("mcc").getAsString();
                        udrResponse.put("shMcc", epsMcc);
                    }
                    if (epsCgi.get("mnc") != null) {
                        String epsMnc = epsCgi.get("mnc").getAsString();
                        udrResponse.put("shMnc", epsMnc);
                    }
                    if (epsCgi.get("eci") != null) {
                        String epsCi = epsCgi.get("eci").getAsString();
                        udrResponse.put("shEci", epsCi);
                    }
                    if (epsCgi.get("eNBId") != null) {
                        String eNBId = epsCgi.get("eNBId").getAsString();
                        udrResponse.put("eNBId", eNBId);
                    }
                    if (epsCgi.get("ci") != null) {
                        String epsCi = epsCgi.get("ci").getAsString();
                        udrResponse.put("shCi", epsCi);
                    }
                }

                JsonObject epsGeographicalInformation = csEpsLocationInformation.getAsJsonObject("GeographicalInformation");
                if (epsGeographicalInformation != null) {
                    if (epsGeographicalInformation.get("typeOfShape") != null) {
                        String epsTypeOfShape = epsGeographicalInformation.get("typeOfShape").getAsString();
                        udrResponse.put("shGeogTypeOfShape", epsTypeOfShape);
                    }
                    if (epsGeographicalInformation.get("latitude") != null) {
                        String epsLatitude = epsGeographicalInformation.get("latitude").getAsString();
                        udrResponse.put("shGeogLatitude", epsLatitude);
                    }
                    if (epsGeographicalInformation.get("longitude") != null) {
                        String epsLongitude = epsGeographicalInformation.get("longitude").getAsString();
                        udrResponse.put("shGeogLongitude", epsLongitude);
                    }
                    if (epsGeographicalInformation.get("uncertainty") != null) {
                        String epsUncertainty = epsGeographicalInformation.get("uncertainty").getAsString();
                        udrResponse.put("shGeogUncertainty", epsUncertainty);
                    }
                }

                JsonObject epsGeodeticInformation = csEpsLocationInformation.getAsJsonObject("GeodeticInformation");
                if (epsGeodeticInformation != null) {
                    if (epsGeodeticInformation.get("typeOfShape") != null) {
                        String epsGeodTypeOfShape = epsGeodeticInformation.get("typeOfShape").getAsString();
                        udrResponse.put("shGeodeticTypeOfShape", epsGeodTypeOfShape);
                    }
                    if (epsGeodeticInformation.get("latitude") != null) {
                        String epsGeodLatitude = epsGeodeticInformation.get("latitude").getAsString();
                        udrResponse.put("shGeodeticLatitude", epsGeodLatitude);
                    }
                    if (epsGeodeticInformation.get("longitude") != null) {
                        String epsGeodLongitude = epsGeodeticInformation.get("longitude").getAsString();
                        udrResponse.put("shGeodeticLongitude", epsGeodLongitude);
                    }
                    if (epsGeodeticInformation.get("uncertainty") != null) {
                        String epsGeodUncertainty = epsGeodeticInformation.get("uncertainty").getAsString();
                        udrResponse.put("shGeodeticUncertainty", epsGeodUncertainty);
                    }
                    if (epsGeodeticInformation.get("confidence") != null) {
                        String epsConfidence = epsGeodeticInformation.get("confidence").getAsString();
                        udrResponse.put("shGeodeticConfidence", epsConfidence);
                    }
                    if (epsGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                        String epsScreeningAndPresentationIndicators = epsGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                        udrResponse.put("shGeodeticScreeningAndPresentationIndicators", epsScreeningAndPresentationIndicators);
                    }
                }

                if (csEpsLocationInformation.get("mmeName") != null) {
                    String mmeName = csEpsLocationInformation.get("mmeName").getAsString();
                    if (mmeName != null) {
                        udrResponse.put("mmeName", mmeName);
                    }
                }

                if (csEpsLocationInformation.get("ageOfLocationInformation") != null) {
                    String epsAgeOfLocationInfo = csEpsLocationInformation.get("ageOfLocationInformation").getAsString();
                    if (epsAgeOfLocationInfo != null) {
                        udrResponse.put("shAgeOfLocationInformation", epsAgeOfLocationInfo);
                    }
                }

                if (csEpsLocationInformation.get("currentLocationRetrieved") != null) {
                    String epsCurrentLocationRetrieved = csEpsLocationInformation.get("currentLocationRetrieved").getAsString();
                    if (epsCurrentLocationRetrieved != null) {
                        udrResponse.put("shCurrentLocationRetrieved", epsCurrentLocationRetrieved);
                    }
                }

                if (csEpsLocationInformation.get("ratType") != null) {
                    String ratType = csEpsLocationInformation.get("ratType").getAsString();
                    if (ratType != null) {
                        udrResponse.put("shRatType", ratType);
                    }
                }
            }
        }

        JsonObject psLocationInformation = jsonElement.getAsJsonObject();
        psLocationInformation = psLocationInformation.getAsJsonObject("PSLocationInformation");
        if (psLocationInformation != null) {
            JsonObject routingAreaId = psLocationInformation.getAsJsonObject("RAI");
            if (routingAreaId != null) {
                if (routingAreaId.get("mcc") != null) {
                    String raiMcc = routingAreaId.get("mcc").getAsString();
                    udrResponse.put("shMcc", raiMcc);
                }
                if (routingAreaId.get("mnc") != null) {
                    String raiMnc = routingAreaId.get("mnc").getAsString();
                    udrResponse.put("shMnc", raiMnc);
                }
                if (routingAreaId.get("lac") != null) {
                    String raiLac = routingAreaId.get("lac").getAsString();
                    udrResponse.put("shLac", raiLac);
                }
                if (routingAreaId.get("rac") != null) {
                    String rac = routingAreaId.get("rac").getAsString();
                    udrResponse.put("shRac", rac);
                }
            }

            JsonObject psLai = psLocationInformation.getAsJsonObject("LAI");
            if (psLai != null) {
                if (psLai.get("mcc") != null) {
                    String psMcc = psLai.get("mcc").getAsString();
                    udrResponse.put("shMcc", psMcc);
                }
                if (psLai.get("mnc") != null) {
                    String psMnc = psLai.get("mnc").getAsString();
                    udrResponse.put("shMnc", psMnc);
                }
                if (psLai.get("lac") != null) {
                    String psLac = psLai.get("lac").getAsString();
                    udrResponse.put("shLac", psLac);
                }
            }

            JsonObject psCgi = psLocationInformation.getAsJsonObject("CGI");
            if (psCgi != null) {
                if (psCgi.get("mcc") != null) {
                    String psMcc = psCgi.get("mcc").getAsString();
                    udrResponse.put("shMcc", psMcc);
                }
                if (psCgi.get("mnc") != null) {
                    String psMnc = psCgi.get("mnc").getAsString();
                    udrResponse.put("shMnc", psMnc);
                }
                if (psCgi.get("lac") != null) {
                    String psLac = psCgi.get("lac").getAsString();
                    udrResponse.put("shLac", psLac);
                }
                if (psCgi.get("ci") != null) {
                    String psCi = psCgi.get("ci").getAsString();
                    udrResponse.put("shCi", psCi);
                }
            }

            JsonObject psSai = psLocationInformation.getAsJsonObject("SAI");
            if (psSai != null) {
                if (psSai.get("mcc") != null) {
                    String psMcc = psSai.get("mcc").getAsString();
                    udrResponse.put("shMcc", psMcc);
                }
                if (psSai.get("mnc") != null) {
                    String psMnc = psSai.get("mnc").getAsString();
                    udrResponse.put("shMnc", psMnc);
                }
                if (psSai.get("lac") != null) {
                    String psLac = psSai.get("lac").getAsString();
                    udrResponse.put("shLac", psLac);
                }
                if (psSai.get("sac") != null) {
                    String psSac = psSai.get("sac").getAsString();
                    udrResponse.put("shSac", psSac);
                }
            }

            JsonObject psGeographicalInformation = psLocationInformation.getAsJsonObject("GeographicalInformation");
            if (psGeographicalInformation != null) {
                if (psGeographicalInformation.get("typeOfShape") != null) {
                    String psTypeOfShape = psGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeogTypeOfShape", psTypeOfShape);
                }
                if (psGeographicalInformation.get("latitude") != null) {
                    String psLatitude = psGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("shGeogLatitude", psLatitude);
                }
                if (psGeographicalInformation.get("longitude") != null) {
                    String psLongitude = psGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("shGeogLongitude", psLongitude);
                }
                if (psGeographicalInformation.get("uncertainty") != null) {
                    String psUncertainty = psGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeogUncertainty", psUncertainty);
                }
            }

            JsonObject psGeodeticInformation = psLocationInformation.getAsJsonObject("GeodeticInformation");
            if (psGeodeticInformation != null) {
                if (psGeodeticInformation.get("typeOfShape") != null) {
                    String psGeodTypeOfShape = psGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeodeticTypeOfShape", psGeodTypeOfShape);
                }
                if (psGeodeticInformation.get("latitude") != null) {
                    String psGeodLatitude = psGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("shGeodeticLatitude", psGeodLatitude);
                }
                if (psGeodeticInformation.get("longitude") != null) {
                    String psGeodLongitude = psGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("shGeodeticLongitude", psGeodLongitude);
                }
                if (psGeodeticInformation.get("uncertainty") != null) {
                    String psGeodUncertainty = psGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeodeticUncertainty", psGeodUncertainty);
                }
                if (psGeodeticInformation.get("confidence") != null) {
                    String psConfidence = psGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("shGeodeticConfidence", psConfidence);
                }
                if (psGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String psScreeningAndPresentationIndicators = psGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("shGeodeticScreeningAndPresentationIndicators", psScreeningAndPresentationIndicators);
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
                    udrResponse.put("shAgeOfLocationInformation", psAgeOfLocationInfo);
                }
            }

            if (psLocationInformation.get("currentLocationRetrieved") != null) {
                String psCurrentLocationRetrieved = psLocationInformation.get("currentLocationRetrieved").getAsString();
                if (psCurrentLocationRetrieved != null) {
                    udrResponse.put("shCurrentLocationRetrieved", psCurrentLocationRetrieved);
                }
            }

            if (psLocationInformation.get("ratType") != null) {
                String ratType = psLocationInformation.get("ratType").getAsString();
                if (ratType != null) {
                    udrResponse.put("shRatType", ratType);
                }
            }
        }

        JsonObject epsLocationInformation = jsonElement.getAsJsonObject();
        epsLocationInformation = epsLocationInformation.getAsJsonObject("EPSLocationInformation");
        if (epsLocationInformation != null) {
            JsonObject trackingAreaId = epsLocationInformation.getAsJsonObject("TAI");
            if (trackingAreaId != null) {
                if (trackingAreaId.get("mcc") != null) {
                    String taiMcc = trackingAreaId.get("mcc").getAsString();
                    udrResponse.put("shMcc", taiMcc);
                }
                if (trackingAreaId.get("mnc") != null) {
                    String taiMnc = trackingAreaId.get("mnc").getAsString();
                    udrResponse.put("shMnc", taiMnc);
                }
                if (trackingAreaId.get("tac") != null) {
                    String tac = trackingAreaId.get("tac").getAsString();
                    udrResponse.put("shTac", tac);
                }
            }

            JsonObject epsCgi = epsLocationInformation.getAsJsonObject("ECGI");
            if (epsCgi != null) {
                if (epsCgi.get("mcc") != null) {
                    String epsMcc = epsCgi.get("mcc").getAsString();
                    udrResponse.put("shMcc", epsMcc);
                }
                if (epsCgi.get("mnc") != null) {
                    String epsMnc = epsCgi.get("mnc").getAsString();
                    udrResponse.put("shMnc", epsMnc);
                }
                if (epsCgi.get("eci") != null) {
                    String epsCi = epsCgi.get("eci").getAsString();
                    udrResponse.put("shEci", epsCi);
                }
                if (epsCgi.get("eNBId") != null) {
                    String eNBId = epsCgi.get("eNBId").getAsString();
                    udrResponse.put("eNBId", eNBId);
                }
                if (epsCgi.get("ci") != null) {
                    String epsCi = epsCgi.get("ci").getAsString();
                    udrResponse.put("shCi", epsCi);
                }
            }

            JsonObject epsGeographicalInformation = epsLocationInformation.getAsJsonObject("GeographicalInformation");
            if (epsGeographicalInformation != null) {
                if (epsGeographicalInformation.get("typeOfShape") != null) {
                    String epsTypeOfShape = epsGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeogTypeOfShape", epsTypeOfShape);
                }
                if (epsGeographicalInformation.get("latitude") != null) {
                    String epsLatitude = epsGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("shGeogLatitude", epsLatitude);
                }
                if (epsGeographicalInformation.get("longitude") != null) {
                    String epsLongitude = epsGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("shGeogLongitude", epsLongitude);
                }
                if (epsGeographicalInformation.get("uncertainty") != null) {
                    String epsUncertainty = epsGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeogUncertainty", epsUncertainty);
                }
            }

            JsonObject epsGeodeticInformation = epsLocationInformation.getAsJsonObject("GeodeticInformation");
            if (epsGeodeticInformation != null) {
                if (epsGeodeticInformation.get("typeOfShape") != null) {
                    String epsGeodTypeOfShape = epsGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeodeticTypeOfShape", epsGeodTypeOfShape);
                }
                if (epsGeodeticInformation.get("latitude") != null) {
                    String epsGeodLatitude = epsGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("shGeodeticLatitude", epsGeodLatitude);
                }
                if (epsGeodeticInformation.get("longitude") != null) {
                    String epsGeodLongitude = epsGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("shGeodeticLongitude", epsGeodLongitude);
                }
                if (epsGeodeticInformation.get("uncertainty") != null) {
                    String epsGeodUncertainty = epsGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeodeticUncertainty", epsGeodUncertainty);
                }
                if (epsGeodeticInformation.get("confidence") != null) {
                    String epsConfidence = epsGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("shGeodeticConfidence", epsConfidence);
                }
                if (epsGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String epsScreeningAndPresentationIndicators = epsGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("shGeodeticScreeningAndPresentationIndicators", epsScreeningAndPresentationIndicators);
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
                    udrResponse.put("shAgeOfLocationInformation", epsAgeOfLocationInfo);
                }
            }

            if (epsLocationInformation.get("currentLocationRetrieved") != null) {
                String epsCurrentLocationRetrieved = epsLocationInformation.get("currentLocationRetrieved").getAsString();
                if (epsCurrentLocationRetrieved != null) {
                    udrResponse.put("shCurrentLocationRetrieved", epsCurrentLocationRetrieved);
                }
            }

            if (epsLocationInformation.get("ratType") != null) {
                String ratType = epsLocationInformation.get("ratType").getAsString();
                if (ratType != null) {
                    udrResponse.put("shRatType", ratType);
                }
            }
        }

        JsonObject sh5gsLocationInformation = jsonElement.getAsJsonObject();
        sh5gsLocationInformation = sh5gsLocationInformation.getAsJsonObject("5GSLocationInformation");
        if (sh5gsLocationInformation != null) {
            JsonObject trackingAreaId = sh5gsLocationInformation.getAsJsonObject("TAI");
            if (trackingAreaId != null) {
                if (trackingAreaId.get("mcc") != null) {
                    String taiMcc = trackingAreaId.get("mcc").getAsString();
                    udrResponse.put("shMcc", taiMcc);
                }
                if (trackingAreaId.get("mnc") != null) {
                    String taiMnc = trackingAreaId.get("mnc").getAsString();
                    udrResponse.put("shMnc", taiMnc);
                }
                if (trackingAreaId.get("tac") != null) {
                    String tac = trackingAreaId.get("tac").getAsString();
                    udrResponse.put("shTac", tac);
                }
            }

            JsonObject epsCgi = sh5gsLocationInformation.getAsJsonObject("ECGI");
            if (epsCgi != null) {
                if (epsCgi.get("mcc") != null) {
                    String epsMcc = epsCgi.get("mcc").getAsString();
                    udrResponse.put("shMcc", epsMcc);
                }
                if (epsCgi.get("mnc") != null) {
                    String epsMnc = epsCgi.get("mnc").getAsString();
                    udrResponse.put("shMnc", epsMnc);
                }
                if (epsCgi.get("eci") != null) {
                    String epsCi = epsCgi.get("eci").getAsString();
                    udrResponse.put("shEci", epsCi);
                }
                if (epsCgi.get("eNBId") != null) {
                    String eNBId = epsCgi.get("eNBId").getAsString();
                    udrResponse.put("eNBId", eNBId);
                }
                if (epsCgi.get("ci") != null) {
                    String epsCi = epsCgi.get("ci").getAsString();
                    udrResponse.put("shCi", epsCi);
                }
            }

            JsonObject sh5gNcgi = sh5gsLocationInformation.getAsJsonObject("NCGI");
            if (sh5gNcgi != null) {
                if (sh5gNcgi.get("mcc") != null) {
                    String ncgiMcc = sh5gNcgi.get("mcc").getAsString();
                    udrResponse.put("shMcc", ncgiMcc);
                }
                if (sh5gNcgi.get("mnc") != null) {
                    String ncgiMnc = sh5gNcgi.get("mnc").getAsString();
                    udrResponse.put("shMnc", ncgiMnc);
                }
                if (sh5gNcgi.get("nci") != null) {
                    String nci = sh5gNcgi.get("nci").getAsString();
                    udrResponse.put("shNci", nci);
                }
            }

            JsonObject sh5gsGeographicalInformation = sh5gsLocationInformation.getAsJsonObject("GeographicalInformation");
            if (sh5gsGeographicalInformation != null) {
                if (sh5gsGeographicalInformation.get("typeOfShape") != null) {
                    String sh5gsTypeOfShape = sh5gsGeographicalInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeogTypeOfShape", sh5gsTypeOfShape);
                }
                if (sh5gsGeographicalInformation.get("latitude") != null) {
                    String sh5gsLatitude = sh5gsGeographicalInformation.get("latitude").getAsString();
                    udrResponse.put("shGeogLatitude", sh5gsLatitude);
                }
                if (sh5gsGeographicalInformation.get("longitude") != null) {
                    String sh5gsLongitude = sh5gsGeographicalInformation.get("longitude").getAsString();
                    udrResponse.put("shGeogLongitude", sh5gsLongitude);
                }
                if (sh5gsGeographicalInformation.get("uncertainty") != null) {
                    String sh5gsUncertainty = sh5gsGeographicalInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeogUncertainty", sh5gsUncertainty);
                }
            }

            JsonObject sh5gsGeodeticInformation = sh5gsLocationInformation.getAsJsonObject("GeodeticInformation");
            if (sh5gsGeodeticInformation != null) {
                if (sh5gsGeodeticInformation.get("typeOfShape") != null) {
                    String sh5gsGeodTypeOfShape = sh5gsGeodeticInformation.get("typeOfShape").getAsString();
                    udrResponse.put("shGeodeticTypeOfShape", sh5gsGeodTypeOfShape);
                }
                if (sh5gsGeodeticInformation.get("latitude") != null) {
                    String sh5gsGeodLatitude = sh5gsGeodeticInformation.get("latitude").getAsString();
                    udrResponse.put("shGeodeticLatitude", sh5gsGeodLatitude);
                }
                if (sh5gsGeodeticInformation.get("longitude") != null) {
                    String sh5gsGeodLongitude = sh5gsGeodeticInformation.get("longitude").getAsString();
                    udrResponse.put("shGeodeticLongitude", sh5gsGeodLongitude);
                }
                if (sh5gsGeodeticInformation.get("uncertainty") != null) {
                    String sh5gsGeodUncertainty = sh5gsGeodeticInformation.get("uncertainty").getAsString();
                    udrResponse.put("shGeodeticUncertainty", sh5gsGeodUncertainty);
                }
                if (sh5gsGeodeticInformation.get("confidence") != null) {
                    String sh5gsConfidence = sh5gsGeodeticInformation.get("confidence").getAsString();
                    udrResponse.put("shGeodeticConfidence", sh5gsConfidence);
                }
                if (sh5gsGeodeticInformation.get("screeningAndPresentationIndicators") != null) {
                    String sh5gsScreeningAndPresentationIndicators = sh5gsGeodeticInformation.get("screeningAndPresentationIndicators").getAsString();
                    udrResponse.put("shGeodeticScreeningAndPresentationIndicators", sh5gsScreeningAndPresentationIndicators);
                }
            }

            if (sh5gsLocationInformation.get("amfAddress") != null) {
                String amfAddress = sh5gsLocationInformation.get("amfAddress").getAsString();
                if (amfAddress != null) {
                    udrResponse.put("amfAddress", amfAddress);
                }
            }

            if (sh5gsLocationInformation.get("ageOfLocationInformation") != null) {
                String sh5gsAgeOfLocationInfo = sh5gsLocationInformation.get("ageOfLocationInformation").getAsString();
                if (sh5gsAgeOfLocationInfo != null) {
                    udrResponse.put("shAgeOfLocationInformation", sh5gsAgeOfLocationInfo);
                }
            }

            if (sh5gsLocationInformation.get("currentLocationRetrieved") != null) {
                String sh5gsCurrentLocationRetrieved = sh5gsLocationInformation.get("currentLocationRetrieved").getAsString();
                if (sh5gsCurrentLocationRetrieved != null) {
                    udrResponse.put("shCurrentLocationRetrieved", sh5gsCurrentLocationRetrieved);
                }
            }

            if (sh5gsLocationInformation.get("ratType") != null) {
                String ratType = sh5gsLocationInformation.get("ratType").getAsString();
                if (ratType != null) {
                    udrResponse.put("shRatType", ratType);
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

        // LAI, CGI or SAI
        String shMcc = udrResponse.get("shMcc");
        if (shMcc != null)
            data.putSingle("MobileCountryCode", shMcc);

        String shMnc = udrResponse.get("shMnc");
        if (shMnc != null)
            data.putSingle("MobileNetworkCode", shMnc);

        String shLac = udrResponse.get("shLac");
        if (shLac != null)
            data.putSingle("LocationAreaCode", shLac);

        String shCi = udrResponse.get("shCi");
        if (shCi != null)
            data.putSingle("CellId", shCi);

        String shSac = udrResponse.get("shSac");
        if (shSac != null)
            data.putSingle("ServiceAreaCode", shSac);

        // Location Number
        String locationNumber = udrResponse.get("locationNumberAddress");
        if (locationNumber != null)
            data.putSingle("LocationNumberAddress", locationNumber);

        // Subscriber Geographic Location Info
        String shGeogTypeOfShape = udrResponse.get("shGeogTypeOfShape");
        if (shGeogTypeOfShape != null)
            data.putSingle("TypeOfShape", shGeogTypeOfShape);

        String shGeogLatitude = udrResponse.get("shGeogLatitude");
        if (shGeogLatitude != null)
            data.putSingle("DeviceLatitude", shGeogLatitude);

        String shGeogLongitude = udrResponse.get("shGeogLongitude");
        if (shGeogLongitude != null)
            data.putSingle("DeviceLongitude", shGeogLongitude);

        String shGeogUncertainty = udrResponse.get("shGeogUncertainty");
        if (shGeogUncertainty != null)
            data.putSingle("Uncertainty", shGeogUncertainty);

        // Subscriber Geodetic Location Info
        String shGeodeticTypeOfShape = udrResponse.get("shGeodeticTypeOfShape");
        if (shGeodeticTypeOfShape != null)
            data.putSingle("TypeOfShape", shGeodeticTypeOfShape);

        String shGeodeticLatitude = udrResponse.get("shGeodeticLatitude");
        if (shGeodeticLatitude != null)
            data.putSingle("DeviceLatitude", shGeodeticLatitude);

        String shGeodeticLongitude = udrResponse.get("shGeodeticLongitude");
        if (shGeodeticLongitude != null)
            data.putSingle("DeviceLongitude", shGeodeticLongitude);

        String shGeodeticUncertainty = udrResponse.get("shGeodeticUncertainty");
        if (shGeodeticUncertainty != null)
            data.putSingle("Uncertainty", shGeodeticUncertainty);

        String shGeodeticConfidence = udrResponse.get("shGeodeticConfidence");
        if (shGeodeticConfidence != null)
            data.putSingle("Confidence", shGeodeticConfidence);

        String shGeodeticScreeningAndPresentationIndicators = udrResponse.get("shGeodeticScreeningAndPresentationIndicators");
        if (shGeodeticScreeningAndPresentationIndicators != null)
            data.putSingle("ScreeningAndPresentationIndicators", shGeodeticScreeningAndPresentationIndicators);

        String vlrNumber = udrResponse.get("vlrNumber");
        if (vlrNumber != null)
            data.putSingle("NetworkEntityAddress", vlrNumber);

        String mscNumber = udrResponse.get("mscNumber");
        if (mscNumber != null)
            data.putSingle("NetworkEntityAddress", mscNumber);

        String shAgeOfLocationInformation = udrResponse.get("shAgeOfLocationInformation");
        if (shAgeOfLocationInformation != null)
            data.putSingle("LocationAge", shAgeOfLocationInformation);

        String shCurrentLocationRetrieved = udrResponse.get("shCurrentLocationRetrieved");
        if (shCurrentLocationRetrieved != null)
            data.putSingle("CurrentLocationRetrieved", shCurrentLocationRetrieved);

        // PS Subscriber Info
        // RAC
        String rac = udrResponse.get("shRac");
        if (rac != null)
            data.putSingle("RoutingAreaCode", rac);

        //SGSN Number
        String sgsnNumber = udrResponse.get("sgsnNumber");
        if (sgsnNumber != null)
            data.putSingle("NetworkEntityAddress", sgsnNumber);

        // EPS Location Info
        // TAC
        String tac = udrResponse.get("shTac");
        if (tac != null)
            data.putSingle("TrackingAreaCode", tac);

        // E-UTRAN CGI
        String eci = udrResponse.get("shEci");
        if (eci != null)
            data.putSingle("ECellId", eci);

        String eNBId = udrResponse.get("eNBId");
        if (eNBId != null)
            data.putSingle("ENodeBId", eNBId);

        // MME Name
        String mmeName = udrResponse.get("mmeName");
        if (mmeName != null)
            data.putSingle("NetworkEntityName", mmeName);

        // 5G NCI
        String nrCellId = udrResponse.get("shNci");
        if (nrCellId != null)
            data.putSingle("NRCellId", nrCellId);

        // 5G AMF Address
        String amfAddress = udrResponse.get("amfAddress");
        if (amfAddress != null)
            data.putSingle("NetworkEntityName", amfAddress);

        String ratType = udrResponse.get("shRatType");
        if (ratType != null)
            data.putSingle("RadioAccessType", ratType);

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


    /*** Helpers ***/

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

    private boolean isStringNumericAmount(String str, int amount) {
        if (str == null) {
            return false;
        }
        String regex = "\\d{"+amount+"}";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(str);
        return m.matches();
    }

    private boolean isStringNumericRange(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        String regex = "\\d{"+min+","+max+"}";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(str);
        return m.matches();
    }
}

