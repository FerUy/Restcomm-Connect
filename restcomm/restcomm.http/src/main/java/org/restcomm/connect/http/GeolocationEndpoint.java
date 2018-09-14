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

    private static enum responseStatus {
        Queued("queued"), Sent("sent"), Processing("processing"), Successful("successful"), PartiallySuccessful(
                "partially-successful"), LastKnown(
                        "last-known"), Failed("failed"), Unauthorized("unauthorized"), Rejected("rejected");

        private final String rs;

        private responseStatus(final String rs) {
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
            cause = illegalArgumentException.getMessage();
            rStatus = responseStatus.Failed.toString();
        } catch (final UnsupportedOperationException unsupportedOperationException) {
            // API compliance check regarding parameters not allowed for Immediate type of Geolocation
            return status(BAD_REQUEST).entity(unsupportedOperationException.getMessage()).build();
        }

        /***********************************************/
        /******* Query GMLC for Location Data ********/
        /*********************************************/
        try {
            String targetMSISDN = data.getFirst("DeviceIdentifier");
            Configuration gmlcConf = configuration.subset("gmlc");
            String gmlcURI = gmlcConf.getString("gmlc-uri");
            // Authorization for further stage of the project
            String gmlcUser = gmlcConf.getString("gmlc-user");
            String gmlcPassword = gmlcConf.getString("gmlc-password");
            // Credentials credentials = new UsernamePasswordCredentials(gmlcUser, gmlcPassword);
            String psiService = data.getFirst("psiService");
            String coreNetwork = data.getFirst("coreNetwork");
            String httpRespType = data.getFirst("httpRespType");
            URIBuilder uriBuilder = new URIBuilder(gmlcURI);
            uriBuilder.addParameter("msisdn", targetMSISDN);
            if (coreNetwork != null) {
                uriBuilder.addParameter("coreNetwork", coreNetwork);
            }
            if (psiService != null) {
                uriBuilder.addParameter("psiService", psiService);
            }
            if (httpRespType != null) {
                uriBuilder.addParameter("httpRespType", httpRespType);
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
                    if (httpRespType == null && psiService == null && coreNetwork == null) {
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
                                    if (item.contains("latitude")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("DeviceLatitude", token);
                                    }
                                    if (item.contains("longitude")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("DeviceLongitude", token);
                                    }
                                    if (item.contains("innerRadius")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("Radius", token);
                                    }
                                    if (item.contains("civicAddress")) {
                                        String token = item.substring(item.lastIndexOf("=") + 1);
                                        data.putSingle("FormattedAddress", token);
                                    }
                                }
                            }
                        }
                    } else {
                        gmlcResponse = EntityUtils.toString(entity, "UTF-8");
                        if (httpRespType.equalsIgnoreCase("json")) {
                            if (gmlcResponse != null) {
                                if (psiService == null) {
                                    logger.info("Data retrieved from GMLC via MAP ATI: " + gmlcResponse);
                                    HashMap<String, String> atiResponse = parseAtiJsonString(gmlcResponse);
                                    String mcc = atiResponse.get("mcc");
                                    data.putSingle("MobileCountryCode", mcc);
                                    String mnc = atiResponse.get("mnc");
                                    data.putSingle("MobileNetworkCode", mnc);
                                    String lac = atiResponse.get("lac");
                                    data.putSingle("LocationAreaCode", lac);
                                    String ci = atiResponse.get("ci");
                                    data.putSingle("CellId", ci);
                                    String aol = atiResponse.get("aol");
                                    data.putSingle("LocationAge", aol);
                                    String vlrNumber = atiResponse.get("vlrNumber");
                                    data.putSingle("NetworkEntityAddress", vlrNumber);
                                    String subscriberState = atiResponse.get("subscriberState");
                                    data.putSingle("SubscriberState", subscriberState);
                                    String latitude = atiResponse.get("latitude");
                                    if (latitude != null)
                                        data.putSingle("DeviceLatitude", latitude);
                                    String longitude = atiResponse.get("longitude");
                                    if (longitude != null)
                                        data.putSingle("DeviceLongitude", longitude);
                                    String civicAddress = atiResponse.get("civicAddress");
                                    if (civicAddress != null)
                                        data.putSingle("FormattedAddress", civicAddress);

                                } else if (psiService.equalsIgnoreCase("true")) {
                                    logger.info("Data retrieved from GMLC via MAP PSI: " + gmlcResponse);
                                    HashMap<String, String> psiResponse = parsePsiJsonString(gmlcResponse);
                                    String imsi = psiResponse.get("imsi");
                                    data.putSingle("IMSI", imsi);
                                    String imei = psiResponse.get("imei");
                                    data.putSingle("IMEI", imei);
                                    String lmsi = psiResponse.get("lmsi");
                                    data.putSingle("LMSI", lmsi);
                                    String subscriberState = psiResponse.get("subscriberState");
                                    data.putSingle("SubscriberState", subscriberState);
                                    String mcc = psiResponse.get("gprsMcc");
                                    data.putSingle("MobileCountryCode", mcc);
                                    String mnc = psiResponse.get("gprsMnc");
                                    data.putSingle("MobileNetworkCode", mnc);
                                    String lac = psiResponse.get("gprsLac");
                                    data.putSingle("LocationAreaCode", lac);
                                    String ci = psiResponse.get("gprsCi");
                                    data.putSingle("CellId", ci);
                                    String latitude = psiResponse.get("gprsLatitude");
                                    data.putSingle("DeviceLatitude", latitude);
                                    String longitude = psiResponse.get("gprsLongitude");
                                    data.putSingle("DeviceLongitude", longitude);
                                    String typeOfShape = psiResponse.get("gprsTypeOfShape");
                                    data.putSingle("TypeOfShape", typeOfShape);
                                    String uncertainty = psiResponse.get("gprsUncertainty");
                                    data.putSingle("Uncertainty", uncertainty);
                                    latitude = psiResponse.get("gprsGeodLatitude");
                                    data.putSingle("DeviceLatitude", latitude);
                                    longitude = psiResponse.get("gprsGeodLongitude");
                                    data.putSingle("DeviceLongitude", longitude);
                                    typeOfShape = psiResponse.get("gprsGeodTypeOfShape");
                                    data.putSingle("TypeOfShape", typeOfShape);
                                    uncertainty = psiResponse.get("gprsGeodUncertainty");
                                    data.putSingle("Uncertainty", uncertainty);
                                    String confidence = psiResponse.get("gprsConfidence");
                                    data.putSingle("Confidence", confidence);
                                    String screeningAndPresInd = psiResponse.get("gprsScreeningAndPresentationIndicators");
                                    data.putSingle("ScreeningAndPresentationIndicators", screeningAndPresInd);
                                    String sgsnNumber = psiResponse.get("sgsnNumber");
                                    data.putSingle("NetworkEntityAddress", sgsnNumber);
                                    String lsaId = psiResponse.get("lsaId");
                                    data.putSingle("LSAId", lsaId);
                                    String routeingAreaId = psiResponse.get("routeingAreaId");
                                    data.putSingle("RouteingAreaId", routeingAreaId);
                                    mcc = psiResponse.get("mcc");
                                    data.putSingle("MobileCountryCode", mcc);
                                    mnc = psiResponse.get("mnc");
                                    data.putSingle("MobileNetworkCode", mnc);
                                    lac = psiResponse.get("lac");
                                    data.putSingle("LocationAreaCode", lac);
                                    ci = psiResponse.get("ci");
                                    data.putSingle("CellId", ci);
                                    String locationNumber = psiResponse.get("locationNumber");
                                    data.putSingle("LocationNumber", locationNumber);
                                    String aol = psiResponse.get("aol");
                                    data.putSingle("LocationAge", aol);
                                    String vlrNumber = psiResponse.get("vlrNumber");
                                    data.putSingle("NetworkEntityAddress", vlrNumber);
                                    String mscNumber = psiResponse.get("mscNumber");
                                    data.putSingle("NetworkEntityAddress", mscNumber);
                                    latitude = psiResponse.get("latitude");
                                    data.putSingle("DeviceLatitude", latitude);
                                    longitude = psiResponse.get("longitude");
                                    data.putSingle("DeviceLongitude", longitude);
                                    typeOfShape = psiResponse.get("typeOfShape");
                                    data.putSingle("TypeOfShape", typeOfShape);
                                    uncertainty = psiResponse.get("uncertainty");
                                    data.putSingle("Uncertainty", uncertainty);
                                    latitude = psiResponse.get("geodLatitude");
                                    data.putSingle("DeviceLatitude", latitude);
                                    longitude = psiResponse.get("geodLongitude");
                                    data.putSingle("DeviceLongitude", longitude);
                                    typeOfShape = psiResponse.get("geodTypeOfShape");
                                    data.putSingle("TypeOfShape", typeOfShape);
                                    uncertainty = psiResponse.get("geodUncertainty");
                                    data.putSingle("Uncertainty", uncertainty);
                                    confidence = psiResponse.get("confidence");
                                    data.putSingle("Confidence", confidence);
                                    screeningAndPresInd = psiResponse.get("screeningAndPresentationIndicators");
                                    data.putSingle("ScreeningAndPresentationIndicators", screeningAndPresInd);
                                    String currentLocationRetrieved = psiResponse.get("currentLocationRetrieved");
                                    data.putSingle("CurrentLocationRetrieved", currentLocationRetrieved);
                                    String mmeName = psiResponse.get("mmeName");
                                    data.putSingle("NetworkEntityName", mmeName);
                                    String eUtranCgi = psiResponse.get("eUtranCgi");
                                    data.putSingle("CellId", eUtranCgi);
                                    String trackingAreaId = psiResponse.get("trackingAreaId");
                                    data.putSingle("TrackingAreaId", trackingAreaId);
                                    String mnpStatus = psiResponse.get("mnpStatus");
                                    data.putSingle("MNPStatus", mnpStatus);
                                    String mnpMsisdn = psiResponse.get("mnpMsisdn");
                                    data.putSingle("MNPMSISDN", mnpMsisdn);
                                    String mnpImsi = psiResponse.get("mnpImsi");
                                    data.putSingle("MNPIMSI", mnpImsi);
                                    String mnpRouteingNumber = psiResponse.get("mnpRouteingNumber");
                                    data.putSingle("MNPRouteingNumber", mnpRouteingNumber);
                                }
                            }
                        }
                    }
                    if (gmlcURI != null && gmlcResponse != null) {
                        // For debugging/logging purposes only
                        if (logger.isDebugEnabled()) {
                            logger.debug("Geolocation data of " + targetMSISDN + " retrieved from GMLC at: " + gmlcURI);
                            logger.debug("MCC (Mobile Country Code) = " + getInteger("MobileCountryCode", data));
                            logger.debug("MNC (Mobile Network Code) = " + data.getFirst("MobileNetworkCode"));
                            logger.debug("LAC (Location Area Code) = " + data.getFirst("LocationAreaCode"));
                            logger.debug("CI (Cell ID) = " + data.getFirst("CellId"));
                            logger.debug("AOL (Age of Location) = " + getInteger("LocationAge", data));
                            logger.debug("NNN (Network Node Number/Address) = " + +getLong("NetworkEntityAddress", data));
                            logger.debug("Device Latitude = " + data.getFirst("DeviceLatitude"));
                            logger.debug("Device Longitude = " + data.getFirst("DeviceLongitude"));
                            logger.debug("Civic Address = " + data.getFirst("FormattedAddress"));
                        }
                    }

                } finally {
                    if (stream != null)
                        stream.close();
                }
            }

        } catch (Exception ex) {
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

        // *** DeviceIdentifier can not be null ***/
        if (!data.containsKey("DeviceIdentifier")) {
            throw new NullPointerException("DeviceIdentifier value can not be null");
        }

        // *** StatusCallback can not be null ***/
        if (!data.containsKey("StatusCallback")) {
            throw new NullPointerException("StatusCallback value can not be null");
        }

        // *** DesiredAccuracy must be API compliant: High, Average or Low***/
        if (data.containsKey("DesiredAccuracy")) {
            String desiredAccuracy = data.getFirst("DesiredAccuracy");
            if (!desiredAccuracy.equalsIgnoreCase("High") && !desiredAccuracy.equalsIgnoreCase("Average")
                    && !desiredAccuracy.equalsIgnoreCase("Low")) {
                throw new IllegalArgumentException("DesiredAccuracy value not API compliant");
            }
        }

        // *** DeviceLatitude must be API compliant***/
        if (data.containsKey("DeviceLatitude")) {
            String deviceLat = data.getFirst("DeviceLatitude");
            Boolean devLatWGS84 = validateGeoCoordinatesFormat(deviceLat);
            if (!devLatWGS84) {
                throw new IllegalArgumentException("DeviceLatitude not API compliant");
            }
        }

        // *** DeviceLongitude must be API compliant***/
        if (data.containsKey("DeviceLongitude")) {
            String deviceLong = data.getFirst("DeviceLongitude");
            Boolean devLongWGS84 = validateGeoCoordinatesFormat(deviceLong);
            if (!devLongWGS84) {
                throw new IllegalArgumentException("DeviceLongitude not API compliant");
            }
        }

        // *** GeofenceEvent must belong to Notification type of Geolocation, not null and API compliant: in, out or in-out***/
        if (!data.containsKey("GeofenceEvent") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("GeofenceEvent value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("GeofenceEvent") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceEvent only applies for Notification type of Geolocation");
        } else if (data.containsKey("GeofenceEvent") && glType.toString().equals(NotificationGT)) {
            String geofenceEvent = data.getFirst("GeofenceEvent");
            if (!geofenceEvent.equalsIgnoreCase("in") && !geofenceEvent.equalsIgnoreCase("out")
                    && !geofenceEvent.equalsIgnoreCase("in-out")) {
                throw new IllegalArgumentException("GeofenceEvent value not API compliant");
            }
        }

        // *** EventGeofenceLatitude must belong to Notification type of Geolocation, not null and API compliant ***/
        if (!data.containsKey("EventGeofenceLatitude") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("EventGeofenceLatitude value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("EventGeofenceLatitude") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventGeofenceLatitude only applies for Notification type of Geolocation");
        } else if (data.containsKey("EventGeofenceLatitude") && glType.toString().equals(NotificationGT)) {
            String eventGeofenceLat = data.getFirst("EventGeofenceLatitude");
            Boolean eventGeofenceLongWGS84 = validateGeoCoordinatesFormat(eventGeofenceLat);
            if (!eventGeofenceLongWGS84) {
                throw new IllegalArgumentException("EventGeofenceLatitude format not API compliant");
            }
        }

        // *** EventGeofenceLongitude must belong to Notification type of Geolocation and must be API compliant ***/
        if (!data.containsKey("EventGeofenceLongitude") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("EventGeofenceLongitude value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("EventGeofenceLongitude") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("EventGeofenceLongitude only applies for Notification type of Geolocation");
        } else if (data.containsKey("EventGeofenceLongitude") && glType.toString().equals(NotificationGT)) {
            String eventGeofenceLong = data.getFirst("EventGeofenceLongitude");
            Boolean eventGeofenceLongWGS84 = validateGeoCoordinatesFormat(eventGeofenceLong);
            if (!eventGeofenceLongWGS84) {
                throw new IllegalArgumentException("EventGeofenceLongitude format not API compliant");
            }
        }

        // *** GeofenceRange can not be null in Notification type of Geolocation***/
        if (!data.containsKey("GeofenceRange") && glType.toString().equals(NotificationGT)) {
            throw new NullPointerException("GeofenceRange value con not be null for Notification type of Geolocation");
        } else if (data.containsKey("GeofenceRange") && !glType.toString().equals(NotificationGT)) {
            throw new UnsupportedOperationException("GeofenceRange only applies for Notification type of Geolocation");
        }

        // *** LocationTimestamp must be API compliant: DateTime format only***/
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
        builder.setGeolocationType(glType);
        builder.setResponseStatus(data.getFirst("ResponseStatus"));
        builder.setCause(data.getFirst("Cause"));
        builder.setCellId(data.getFirst("CellId"));
        builder.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        builder.setMobileCountryCode(getInteger("MobileCountryCode", data));
        builder.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        builder.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        builder.setAgeOfLocationInfo(getInteger("LocationAge", data));
        builder.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        builder.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        builder.setAccuracy(getLong("Accuracy", data));
        builder.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        builder.setInternetAddress(data.getFirst("InternetAddress"));
        builder.setFormattedAddress(data.getFirst("FormattedAddress"));
        builder.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        builder.setEventGeofenceLatitude(data.getFirst("EventGeofenceLatitude"));
        builder.setEventGeofenceLongitude(data.getFirst("EventGeofenceLongitude"));
        builder.setRadius(getLong("Radius", data));
        builder.setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
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
        builder.setGeolocationType(glType);
        builder.setCause(cause);
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

        if (data.containsKey("CellId")) {
            updatedGeolocation = updatedGeolocation.setCellId(data.getFirst("CellId"));
        }

        if (data.containsKey("LocationAreaCode")) {
            updatedGeolocation = updatedGeolocation.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        }

        if (data.containsKey("MobileCountryCode")) {
            updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }

        if (data.containsKey("MobileNetworkCode")) {
            updatedGeolocation = updatedGeolocation.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        }

        if (data.containsKey("NetworkEntityAddress")) {
            updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        }

        if (data.containsKey("LocationAge")) {
            updatedGeolocation = updatedGeolocation.setAgeOfLocationInfo(getInteger("LocationAge", data));
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

        if (data.containsKey("Accuracy")) {
            updatedGeolocation = updatedGeolocation.setAccuracy(getLong("Accuracy", data));
        }

        if (data.containsKey("PhysicalAddress")) {
            updatedGeolocation = updatedGeolocation.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        }

        if (data.containsKey("InternetAddress")) {
            updatedGeolocation = updatedGeolocation.setInternetAddress(data.getFirst("InternetAddress"));
        }

        if (data.containsKey("FormattedAddress")) {
            updatedGeolocation = updatedGeolocation.setFormattedAddress(data.getFirst("FormattedAddress"));
        }

        if (data.containsKey("LocationTimestamp")) {
            updatedGeolocation = updatedGeolocation.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        }

        if (data.containsKey("EventGeofenceLatitude") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            String eventGeofenceLat = data.getFirst("EventGeofenceLatitude");
            Boolean eventGeofenceLatWGS84 = validateGeoCoordinatesFormat(eventGeofenceLat);
            if (!eventGeofenceLatWGS84) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(),
                        responseStatus.Failed.toString(), "EventGeofenceLatitude format not API compliant");
            } else {
                updatedGeolocation = updatedGeolocation.setEventGeofenceLatitude(eventGeofenceLat);
            }
        }

        if (data.containsKey("EventGeofenceLongitude") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            String eventGeofenceLong = data.getFirst("EventGeofenceLongitude");
            Boolean eventGeofenceLongWGS84 = validateGeoCoordinatesFormat(eventGeofenceLong);
            if (!eventGeofenceLongWGS84) {
                return buildFailedGeolocationUpdate(geolocation, data, geolocation.getGeolocationType(),
                        responseStatus.Failed.toString(), "EventGeofenceLongitude format not API compliant");
            } else {
                updatedGeolocation = updatedGeolocation.setEventGeofenceLongitude(eventGeofenceLong);
            }
        }

        if (data.containsKey("Radius") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            updatedGeolocation = updatedGeolocation.setRadius(getLong("Radius", data));
        }

        if (data.containsKey("GeolocationPositioningType")) {
            updatedGeolocation = updatedGeolocation.setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
        }

        if (data.containsKey("LastGeolocationResponse")) {
            updatedGeolocation = updatedGeolocation.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
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
        String network = jobject.get("network").getAsString();
        atiResponse.put("network", network);
        String protocol = jobject.get("protocol").getAsString();
        atiResponse.put("protocol", protocol);
        String operation = jobject.get("operation").getAsString();
        atiResponse.put("operation", operation);
        JsonObject cgiSaiLai = jelement.getAsJsonObject();
        cgiSaiLai = cgiSaiLai.getAsJsonObject("CGIorSAIorLAI");
        String mcc = cgiSaiLai.get("mcc").getAsString();
        atiResponse.put("mcc", mcc);
        String mnc = cgiSaiLai.get("mnc").getAsString();
        atiResponse.put("mnc", mnc);
        String lac = cgiSaiLai.get("lac").getAsString();
        atiResponse.put("lac", lac);
        String ci = cgiSaiLai.get("cellid").getAsString();
        atiResponse.put("ci", ci);
        String aol = jobject.get("aol").getAsString();
        atiResponse.put("aol", aol);
        String vlrNumber = jobject.get("vlrNumber").getAsString();
        atiResponse.put("vlrNumber", vlrNumber);
        String subscriberState = jobject.get("subscriberState").getAsString();
        atiResponse.put("subscriberState", subscriberState);
        return atiResponse;
    }

    private static HashMap<String, String> parsePslJsonString(String jsonLine) {
        HashMap<String, String> sriPslResponse = new HashMap<>();
        //JsonElement jelement = new JsonParser().parse(jsonLine);
        Gson gson = new Gson();
        JsonElement jelement = gson.fromJson(jsonLine, JsonElement.class);
        JsonObject jobject = jelement.getAsJsonObject();
        String network = jobject.get("network").getAsString();
        sriPslResponse.put("network", network);
        String protocol = jobject.get("protocol").getAsString();
        sriPslResponse.put("protocol", protocol);
        String operation = jobject.get("operation").getAsString();
        sriPslResponse.put("operation", operation);
        String lcsReferenceNumber = jobject.get("lcsReferenceNumber").getAsString();
        sriPslResponse.put("lcsReferenceNumber", lcsReferenceNumber);
        JsonObject sri = jobject.getAsJsonObject("SRIforLCS");
        String msisdn = sri.get("msisdn").getAsString();
        sriPslResponse.put("msisdn", msisdn);
        String imsi = sri.get("imsi").getAsString();
        sriPslResponse.put("imsi", imsi);
        String lmsi = sri.get("lmsi").getAsString();
        sriPslResponse.put("lmsi", lmsi);
        String networkNodeNumber = sri.get("networkNodeNumber").getAsString();
        sriPslResponse.put("networkNodeNumber", networkNodeNumber);
        String gprsNodeIndicator = sri.get("gprsNodeIndicator").getAsString();
        sriPslResponse.put("gprsNodeIndicator", gprsNodeIndicator);
        String mmeName = sri.get("mmeName").getAsString();
        sriPslResponse.put("mmeName", mmeName);
        String sgsnName = sri.get("sgsnName").getAsString();
        sriPslResponse.put("sgsnName", sgsnName);
        String tgppAAAServerName = sri.get("3GPPAAAServerName").getAsString();
        sriPslResponse.put("tgppAAAServerName", tgppAAAServerName);
        String hGmlcAddress = sri.get("hGmlcAddress").getAsString();
        sriPslResponse.put("hGmlcAddress", hGmlcAddress);
        String vGmlcAddress = sri.get("vGmlcAddress").getAsString();
        sriPslResponse.put("vGmlcAddress", vGmlcAddress);
        String pprAddress = sri.get("pprAddress").getAsString();
        sriPslResponse.put("pprAddress", pprAddress);
        JsonObject psl = jelement.getAsJsonObject();
        psl = psl.getAsJsonObject("PSL");
        JsonObject locationEstimate = psl.getAsJsonObject("LocationEstimate");
        String typeOfShape = locationEstimate.get("typeOfShape").getAsString();
        sriPslResponse.put("typeOfShape", typeOfShape);
        String latitude = locationEstimate.get("latitude").getAsString();
        sriPslResponse.put("latitude", latitude);
        String longitude = locationEstimate.get("longitude").getAsString();
        sriPslResponse.put("longitude", longitude);
        String uncertainty = locationEstimate.get("uncertainty").getAsString();
        sriPslResponse.put("uncertainty", uncertainty);
        String uncertaintySemiMajorAxis = locationEstimate.get("uncertaintySemiMajorAxis").getAsString();
        sriPslResponse.put("uncertaintySemiMajorAxis", uncertaintySemiMajorAxis);
        String uncertaintySemiMinorAxis = locationEstimate.get("uncertaintySemiMinorAxis").getAsString();
        sriPslResponse.put("uncertaintySemiMinorAxis", uncertaintySemiMinorAxis);
        String angleOfMajorAxis = locationEstimate.get("angleOfMajorAxis").getAsString();
        sriPslResponse.put("angleOfMajorAxis", angleOfMajorAxis);
        String confidence = locationEstimate.get("confidence").getAsString();
        sriPslResponse.put("confidence", confidence);
        String altitude = locationEstimate.get("altitude").getAsString();
        sriPslResponse.put("altitude", altitude);
        String uncertaintyAltitude = locationEstimate.get("uncertaintyAltitude").getAsString();
        sriPslResponse.put("uncertaintyAltitude", uncertaintyAltitude);
        String innerRadius = locationEstimate.get("innerRadius").getAsString();
        sriPslResponse.put("innerRadius", innerRadius);
        String uncertaintyRadius = locationEstimate.get("uncertaintyRadius").getAsString();
        sriPslResponse.put("uncertaintyRadius", uncertaintyRadius);
        String offsetAngle = locationEstimate.get("offsetAngle").getAsString();
        sriPslResponse.put("offsetAngle", offsetAngle);
        String includedAngle = locationEstimate.get("includedAngle").getAsString();
        sriPslResponse.put("includedAngle", includedAngle);
        String ageOfLocationEstimate = psl.get("ageOfLocationEstimate").getAsString();
        sriPslResponse.put("ageOfLocationEstimate", ageOfLocationEstimate);
        String deferredMTLRresponseIndicator = psl.get("deferredMTLRresponseIndicator").getAsString();
        sriPslResponse.put("deferredMTLRresponseIndicator", deferredMTLRresponseIndicator);
        String moLrShortCircuitIndicator = psl.get("moLrShortCircuitIndicator").getAsString();
        sriPslResponse.put("moLrShortCircuitIndicator", deferredMTLRresponseIndicator);
        JsonObject cgiSaiLai = psl.getAsJsonObject("CGIorSAIorLAI");
        String mcc = cgiSaiLai.get("mcc").getAsString();
        sriPslResponse.put("mcc", mcc);
        String mnc = cgiSaiLai.get("mnc").getAsString();
        sriPslResponse.put("mnc", mnc);
        String lac = cgiSaiLai.get("lac").getAsString();
        sriPslResponse.put("lac", lac);
        String ci = cgiSaiLai.get("cellid").getAsString();
        sriPslResponse.put("ci", ci);
        JsonObject geranPosInfo = psl.getAsJsonObject("GERANPositioningInfo");
        String geranPositioningInfo = geranPosInfo.get("geranPositioningInfo").getAsString();
        sriPslResponse.put("geranPositioningInfo", geranPositioningInfo);
        String geranGanssPositioningData = geranPosInfo.get("geranGanssPositioningData").getAsString();
        sriPslResponse.put("geranGanssPositioningData", geranGanssPositioningData);
        JsonObject utranPosInfo = psl.getAsJsonObject("UTRANPositioningInfo");
        String utranPositioningInfo = utranPosInfo.get("utranPositioningInfo").getAsString();
        sriPslResponse.put("utranPositioningInfo", utranPositioningInfo);
        String utranGanssPositioningData = utranPosInfo.get("utranGanssPositioningData").getAsString();
        sriPslResponse.put("utranGanssPositioningData", utranGanssPositioningData);
        JsonObject velocityEstimate = psl.getAsJsonObject("VelocityEstimate");
        String horizontalSpeed = velocityEstimate.get("horizontalSpeed").getAsString();
        sriPslResponse.put("horizontalSpeed", horizontalSpeed);
        String bearing = velocityEstimate.get("bearing").getAsString();
        sriPslResponse.put("bearing", bearing);
        String verticalSpeed = velocityEstimate.get("verticalSpeed").getAsString();
        sriPslResponse.put("verticalSpeed", verticalSpeed);
        String uncertaintyHorizontalSpeed = velocityEstimate.get("uncertaintyHorizontalSpeed").getAsString();
        sriPslResponse.put("uncertaintyHorizontalSpeed", uncertaintyHorizontalSpeed);
        String uncertaintyVerticalSpeed = velocityEstimate.get("uncertaintyVerticalSpeed").getAsString();
        sriPslResponse.put("uncertaintyVerticalSpeed", uncertaintyVerticalSpeed);
        String velocityType = velocityEstimate.get("velocityType").getAsString();
        sriPslResponse.put("velocityType", velocityType);
        return sriPslResponse;
    }

    private static HashMap<String, String> parsePsiJsonString(String jsonLine) {
        HashMap<String, String> psiResponse = new HashMap<>();
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        String network = jobject.get("network").getAsString();
        psiResponse.put("network", network);
        String protocol = jobject.get("protocol").getAsString();
        psiResponse.put("protocol", protocol);
        String operation = jobject.get("operation").getAsString();
        psiResponse.put("operation", operation);
        String imsi = jobject.get("imsi").getAsString();
        psiResponse.put("imsi", imsi);
        String imei = jobject.get("imei").getAsString();
        psiResponse.put("imei", imei);
        String lmsi = jobject.get("lmsi").getAsString();
        psiResponse.put("lmsi", lmsi); //
        String subscriberState = jobject.get("subscriberState").getAsString();
        psiResponse.put("subscriberState", subscriberState);
        JsonObject locationInfo = jelement.getAsJsonObject();
        locationInfo = locationInfo.getAsJsonObject("LocationInformation");
        JsonObject cgiSaiLai = locationInfo.getAsJsonObject("CGIorSAIorLAI");
        String mcc = cgiSaiLai.get("mcc").getAsString();
        psiResponse.put("mcc", mcc);
        String mnc = cgiSaiLai.get("mnc").getAsString();
        psiResponse.put("mnc", mnc);
        String lac = cgiSaiLai.get("lac").getAsString();
        psiResponse.put("lac", lac);
        String ci = cgiSaiLai.get("cellid").getAsString();
        psiResponse.put("ci", ci);
        String saiPresent = locationInfo.get("saiPresent").getAsString();
        psiResponse.put("saiPresent", saiPresent);
        String locationNumber = locationInfo.get("locationNumber").getAsString();
        psiResponse.put("locationNumber", locationNumber);
        String aol = locationInfo.get("aol").getAsString();
        psiResponse.put("aol", aol);
        String vlrNumber = locationInfo.get("vlrNumber").getAsString();
        psiResponse.put("vlrNumber", vlrNumber);
        String mscNumber = locationInfo.get("mscNumber").getAsString();
        psiResponse.put("mscNumber", mscNumber);
        JsonObject geographicalInformation = locationInfo.getAsJsonObject("GeographicalInformation");
        String latitude = geographicalInformation.get("latitude").getAsString();
        psiResponse.put("latitude", latitude);
        String longitude = geographicalInformation.get("longitude").getAsString();
        psiResponse.put("longitude", longitude);
        String typeOfShape = geographicalInformation.get("typeOfShape").getAsString();
        psiResponse.put("typeOfShape", typeOfShape);
        String uncertainty = geographicalInformation.get("uncertainty").getAsString();
        psiResponse.put("uncertainty", uncertainty);
        JsonObject geodeticInformation = locationInfo.getAsJsonObject("GeodeticInformation");
        String geodLatitude = geodeticInformation.get("latitude").getAsString();
        psiResponse.put("geodLatitude", geodLatitude);
        String geodLongitude = geodeticInformation.get("longitude").getAsString();
        psiResponse.put("geodLongitude", geodLongitude);
        String geodTypeOfShape = geodeticInformation.get("typeOfShape").getAsString();
        psiResponse.put("geodTypeOfShape", geodTypeOfShape);
        String geodUncertainty = geodeticInformation.get("uncertainty").getAsString();
        psiResponse.put("geodUncertainty", geodUncertainty);
        String confidence = geodeticInformation.get("confidence").getAsString();
        psiResponse.put("confidence", confidence);
        String screeningAndPresentationIndicators = geodeticInformation.get("screeningAndPresentationIndicators").getAsString();
        psiResponse.put("screeningAndPresentationIndicators", screeningAndPresentationIndicators);
        String currentLocationRetrieved = locationInfo.get("currentLocationRetrieved").getAsString();
        psiResponse.put("currentLocationRetrieved", currentLocationRetrieved);
        JsonObject locationInfoEPS = locationInfo.getAsJsonObject("LocationInformationEPS");
        String mmeName = locationInfoEPS.get("mmeName").getAsString();
        psiResponse.put("mmeName", mmeName);
        String eUtranCgi = locationInfoEPS.get("eUtranCgi").getAsString();
        psiResponse.put("eUtranCgi", eUtranCgi);
        String trackingAreaId = locationInfoEPS.get("trackingAreaId").getAsString();
        psiResponse.put("trackingAreaId", trackingAreaId);
        JsonObject locationInfoGPRS = jelement.getAsJsonObject();
        locationInfoGPRS = locationInfoGPRS.getAsJsonObject("LocationInformationGPRS");
        cgiSaiLai = locationInfoGPRS.getAsJsonObject("CGIorSAIorLAI");
        mcc = cgiSaiLai.get("mcc").getAsString();
        psiResponse.put("gprsMcc", mcc);
        mnc = cgiSaiLai.get("mnc").getAsString();
        psiResponse.put("gprsMnc", mnc);
        lac = cgiSaiLai.get("lac").getAsString();
        psiResponse.put("gprsLac", lac);
        ci = cgiSaiLai.get("cellid").getAsString();
        psiResponse.put("gprsCi", ci);
        JsonObject geoInfoGPRS = locationInfoGPRS.getAsJsonObject("GeographicalInformationGPRS");
        latitude = geoInfoGPRS.get("latitude").getAsString();
        psiResponse.put("gprsLatitude", latitude);
        longitude = geoInfoGPRS.get("longitude").getAsString();
        psiResponse.put("gprsLongitude", longitude);
        typeOfShape = geoInfoGPRS.get("typeOfShape").getAsString();
        psiResponse.put("gprsTypeOfShape", typeOfShape);
        uncertainty = geoInfoGPRS.get("uncertainty").getAsString();
        psiResponse.put("gprsUncertainty", uncertainty);
        JsonObject geodInfoGPRS = locationInfoGPRS.getAsJsonObject("GeodeticInformationGPRS");
        latitude = geodInfoGPRS.get("latitude").getAsString();
        psiResponse.put("gprsGeodLatitude", latitude);
        longitude = geodInfoGPRS.get("longitude").getAsString();
        psiResponse.put("gprsGeodLongitude", longitude);
        typeOfShape = geodInfoGPRS.get("typeOfShape").getAsString();
        psiResponse.put("gprsGeodTypeOfShape", typeOfShape);
        uncertainty = geodInfoGPRS.get("uncertainty").getAsString();
        psiResponse.put("gprsGeodUncertainty", uncertainty);
        confidence = geodInfoGPRS.get("confidence").getAsString();
        psiResponse.put("gprsConfidence", confidence);
        screeningAndPresentationIndicators = geodInfoGPRS.get("screeningAndPresentationIndicators").getAsString();
        psiResponse.put("gprsScreeningAndPresentationIndicators", screeningAndPresentationIndicators);
        String sgsnNumber = locationInfoGPRS.get("sgsnNumber").getAsString();
        psiResponse.put("sgsnNumber", sgsnNumber);
        String lsaId = locationInfoGPRS.get("lsaId").getAsString();
        psiResponse.put("lsaId", lsaId);
        String routeingAreaId = locationInfoGPRS.get("routeingAreaId").getAsString();
        psiResponse.put("routeingAreaId", routeingAreaId);
        JsonObject mnpInfoResult = jelement.getAsJsonObject();
        mnpInfoResult = mnpInfoResult.getAsJsonObject("MNPInfoResult");
        String mnpStatus = mnpInfoResult.get("mnpStatus").getAsString();
        psiResponse.put("mnpStatus", mnpStatus);
        String mnpMsisdn = mnpInfoResult.get("mnpMsisdn").getAsString();
        psiResponse.put("mnpMsisdn", mnpMsisdn);
        String mnpImsi = mnpInfoResult.get("mnpImsi").getAsString();
        psiResponse.put("mnpImsi", mnpImsi);
        String mnpRouteingNumber = mnpInfoResult.get("mnpRouteingNumber").getAsString();
        psiResponse.put("mnpRouteingNumber", mnpRouteingNumber);
        return psiResponse;
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
