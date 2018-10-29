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

package org.restcomm.connect.dao.entities;

import java.net.URI;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 */
public final class Geolocation {

    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final DateTime dateExecuted;
    private final DateTime locationTimestamp;
    private final Sid accountSid;
    private final String source;
    private final String deviceIdentifier;
    private final Long msisdn;
    private final Long imsi;
    private final String imei;
    private final Long lmsi;
    private final Long referenceNumber;
    private final GeolocationType geolocationType;
    private final String responseStatus;
    private final Integer mobileCountryCode;
    private final String mobileNetworkCode;
    private final String locationAreaCode;
    private final String cellId;
    private final String sai;
    private final Long ecid;
    private final Long networkEntityAddress;
    private final String networkEntityName;
    private final Integer ageOfLocationInfo;
    private final String subscriberState;
    private final String tac;
    private final String rai;
    private final String typeOfShape;
    private final String deviceLatitude;
    private final String deviceLongitude;
    private final String uncertainty;
    private final String uncertaintySemiMajorAxis;
    private final String uncertaintySemiMinorAxis;
    private final String angleOfMajorAxis;
    private final String confidence;
    private final String altitude;
    private final String uncertaintyAltitude;
    private final String innerRadius;
    private final String uncertaintyInnerRadius;
    private final String offsetAngle;
    private final String includedAngle;
    private final String horizontalSpeed;
    private final String verticalSpeed;
    private final String uncertaintyHorizontalSpeed;
    private final String uncertaintyVerticalSpeed;
    private final String bearing;
    private final String geofenceType;
    private final String geofenceId;
    private final String geofenceEventType;
    private final Long eventRange;
    private final String civicAddress;
    private Long barometricPressure;
    private final String physicalAddress;
    private final String internetAddress;
    private final String lastGeolocationResponse;
    private final String cause;
    private final String apiVersion;
    private final URI uri;

    public Geolocation(Sid sid, DateTime dateCreated, DateTime dateUpdated, DateTime dateExecuted, DateTime locationTimestamp, Sid accountSid,
                       String source, String deviceIdentifier, Long msisdn, Long imsi, String imei, Long lmsi, Long referenceNumber,
                       GeolocationType geolocationType, String responseStatus, Integer mobileCountryCode, String mobileNetworkCode,
                       String locationAreaCode, String cellId, String sai, Long ecid, Long networkEntityAddress, String networkEntityName,
                       Integer ageOfLocationInfo, String subscriberState, String tac, String rai,
                       String typeOfShape, String deviceLatitude, String deviceLongitude, String uncertainty, String uncertaintySemiMajorAxis,
                       String uncertaintySemiMinorAxis, String angleOfMajorAxis, String confidence,
                       String altitude, String uncertaintyAltitude, String innerRadius, String uncertaintyInnerRadius, String offsetAngle,
                       String includedAngle, String horizontalSpeed, String verticalSpeed, String uncertaintyHorizontalSpeed, String uncertaintyVerticalSpeed,
                       String bearing, String geofenceType, String geofenceId, String geofenceEventType, Long eventRange, String civicAddress,
                       Long barometricPressure, String physicalAddress, String internetAddress, String lastGeolocationResponse, String cause,
                       String apiVersion, URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateExecuted = dateExecuted;
        this.locationTimestamp = locationTimestamp;
        this.accountSid = accountSid;
        this.source = source;
        this.deviceIdentifier = deviceIdentifier;
        this.msisdn = msisdn;
        this.imsi = imsi;
        this.imei = imei;
        this.lmsi = lmsi;
        this.referenceNumber = referenceNumber;
        this.geolocationType = geolocationType;
        this.responseStatus = responseStatus;
        this.mobileCountryCode = mobileCountryCode;
        this.mobileNetworkCode = mobileNetworkCode;
        this.locationAreaCode = locationAreaCode;
        this.cellId = cellId;
        this.sai = sai;
        this.ecid = ecid;
        this.networkEntityAddress = networkEntityAddress;
        this.networkEntityName = networkEntityName;
        this.ageOfLocationInfo = ageOfLocationInfo;
        this.subscriberState = subscriberState;
        this.tac = tac;
        this.rai = rai;
        this.typeOfShape = typeOfShape;
        this.deviceLatitude = deviceLatitude;
        this.deviceLongitude = deviceLongitude;
        this.uncertainty = uncertainty;
        this.uncertaintySemiMajorAxis = uncertaintySemiMajorAxis;
        this.uncertaintySemiMinorAxis = uncertaintySemiMinorAxis;
        this.angleOfMajorAxis = angleOfMajorAxis;
        this.confidence = confidence;
        this.altitude = altitude;
        this.uncertaintyAltitude = uncertaintyAltitude;
        this.innerRadius = innerRadius;
        this.uncertaintyInnerRadius = uncertaintyInnerRadius;
        this.offsetAngle = offsetAngle;
        this.includedAngle = includedAngle;
        this.horizontalSpeed = horizontalSpeed;
        this.verticalSpeed = verticalSpeed;
        this.uncertaintyHorizontalSpeed = uncertaintyHorizontalSpeed;
        this.uncertaintyVerticalSpeed = uncertaintyVerticalSpeed;
        this.bearing = bearing;
        this.geofenceType = geofenceType;
        this.geofenceId = geofenceId;
        this.geofenceEventType = geofenceEventType;
        this.eventRange = eventRange;
        this.civicAddress = civicAddress;
        this.barometricPressure = barometricPressure;
        this.physicalAddress = physicalAddress;
        this.internetAddress = internetAddress;
        this.lastGeolocationResponse = lastGeolocationResponse;
        this.cause = cause;
        this.apiVersion = apiVersion;
        this.uri = uri;
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public DateTime getDateExecuted() {
        return dateExecuted;
    }

    public DateTime getLocationTimestamp() {
        return locationTimestamp;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getSource() {
        return source;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public Long getMsisdn() {
        return msisdn;
    }

    public Long getImsi() {
        return imsi;
    }

    public String getImei() {
        return imei;
    }

    public Long getLmsi() {
        return lmsi;
    }

    public Long getReferenceNumber() {
        return referenceNumber;
    }

    public GeolocationType getGeolocationType() {
        return geolocationType;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public Integer getMobileCountryCode() {
        return mobileCountryCode;
    }

    public String getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    public String getLocationAreaCode() {
        return locationAreaCode;
    }

    public String getCellId() {
        return cellId;
    }

    public String getSai() {
        return sai;
    }

    public Long getEcid() {
        return ecid;
    }

    public Long getNetworkEntityAddress() {
        return networkEntityAddress;
    }

    public String getNetworkEntityName() {
        return networkEntityName;
    }

    public Integer getAgeOfLocationInfo() {
        return ageOfLocationInfo;
    }

    public String getSubscriberState() {
        return subscriberState;
    }

    public String getTac() {
        return tac;
    }

    public String getRai() {
        return rai;
    }

    public String getTypeOfShape() {
        return typeOfShape;
    }

    public String getDeviceLatitude() {
        return deviceLatitude;
    }

    public String getDeviceLongitude() {
        return deviceLongitude;
    }

    public String getUncertainty() {
        return uncertainty;
    }

    public String getUncertaintySemiMajorAxis() {
        return uncertaintySemiMajorAxis;
    }

    public String getUncertaintySemiMinorAxis() {
        return uncertaintySemiMinorAxis;
    }

    public String getAngleOfMajorAxis() {
        return angleOfMajorAxis;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getAltitude() {
        return altitude;
    }

    public String getUncertaintyAltitude() {
        return uncertaintyAltitude;
    }

    public String getInnerRadius() {
        return innerRadius;
    }

    public String getUncertaintyInnerRadius() {
        return uncertaintyInnerRadius;
    }

    public String getOffsetAngle() {
        return offsetAngle;
    }

    public String getIncludedAngle() {
        return includedAngle;
    }

    public String getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public String getVerticalSpeed() {
        return verticalSpeed;
    }

    public String getUncertaintyHorizontalSpeed() {
        return uncertaintyHorizontalSpeed;
    }

    public String getUncertaintyVerticalSpeed() {
        return uncertaintyVerticalSpeed;
    }

    public String getBearing() {
        return bearing;
    }

    public String getGeofenceType() {
        return geofenceType;
    }

    public String getGeofenceId() {
        return geofenceId;
    }

    public String getGeofenceEventType() {
        return geofenceEventType;
    }

    public Long getEventRange() {
        return eventRange;
    }

    public String getCivicAddress() {
        return civicAddress;
    }

    public Long getBarometricPressure() {
        return barometricPressure;
    }

    public String getPhysicalAddress() {
        return physicalAddress;
    }

    public String getInternetAddress() {
        return internetAddress;
    }

    public String getLastGeolocationResponse() {
        return lastGeolocationResponse;
    }

    public String getCause() {
        return cause;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public URI getUri() {
        return uri;
    }

    public enum GeolocationType {
        Immediate("Immediate"), Notification("Notification");

        private final String glt;

        GeolocationType(final String glt) {
            this.glt = glt;
        }

        public static GeolocationType getValueOf(final String glt) {
            GeolocationType[] values = values();
            for (final GeolocationType value : values) {
                if (value.toString().equals(glt)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(glt + " is not a valid GeolocationType.");
        }

        @Override
        public String toString() {
            return glt;
        }
    }

    ;

    public Geolocation setSid(Sid sid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateCreated(DateTime dateCreated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateUpdated(DateTime dateUpdated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateExecuted(DateTime dateExecuted) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationTimestamp(DateTime locationTimestamp) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccountSid(Sid accountSid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSource(String source) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceIdentifier(String deviceIdentifier) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMsisdn(Long msisdn) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setImsi(Long imsi) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setImei(String imei) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLmsi(Long lmsi) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setReferenceNumber(Long referenceNumber) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationType(GeolocationType geolocationType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setResponseStatus(String responseStatus) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileCountryCode(Integer mobileCountryCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileNetworkCode(String mobileNetworkCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationAreaCode(String locationAreaCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCellId(String cellId) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSai(String sai) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEcid(Long ecid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityAddress(Long networkEntityAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityName(String networkEntityName) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAgeOfLocationInfo(Integer ageOfLocationInfo) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSubscriberState(String subscriberState) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setTac(String tac) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setRai(String rai) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setTypeOfShape(String typeOfShape) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLatitude(String deviceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLongitude(String deviceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertainty(String uncertainty) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintySemiMajorAxis(String uncertaintySemiMajorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintySemiMinorAxis(String uncertaintySemiMinorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAngleOfMajorAxis(String angleOfMajorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setConfidence(String confidence) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAltitude(String altitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyAltitude(String uncertaintyAltitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInnerRadius(String innerRadius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyInnerRadius(String uncertaintyInnerRadius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setOffsetAngle(String offsetAngle) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setIncludedAngle(String includedAngle) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setHorizontalSpeed(String horizontalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setVerticalSpeed(String verticalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyHorizontalSpeed(String uncertaintyHorizontalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyVerticalSpeed(String uncertaintyVerticalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setBearing(String bearing) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceType(String geofenceType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceId(String geofenceId) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceEventType(String geofenceEventType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventRange(Long eventRange) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCivicAddress(String civicAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setBarometricPressure(Long barometricPressure) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setPhysicalAddress(String physicalAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInternetAddress(String internetAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLastGeolocationResponse(String lastGeolocationResponse) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCause(String cause) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setApiVersion(String apiVersion) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUri(URI uri) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static final class Builder {

        private Sid sid;
        private DateTime dateCreated;
        private DateTime dateUpdated;
        private DateTime dateExecuted;
        private DateTime locationTimestamp;
        private Sid accountSid;
        private String source;
        private String deviceIdentifier;
        private Long msisdn;
        private Long imsi;
        private String imei;
        private Long lmsi;
        private Long referenceNumber;
        private GeolocationType geolocationType;
        private String responseStatus;
        private Integer mobileCountryCode;
        private String mobileNetworkCode;
        private String locationAreaCode;
        private String cellId;
        private String sai;
        private Long ecid;
        private Long networkEntityAddress;
        private String networkEntityName;
        private Integer ageOfLocationInfo;
        private String subscriberState;
        private String tac;
        private String rai;
        private String typeOfShape;
        private String deviceLatitude;
        private String deviceLongitude;
        private String uncertainty;
        private String uncertaintySemiMajorAxis;
        private String uncertaintySemiMinorAxis;
        private String angleOfMajorAxis;
        private String confidence;
        private String altitude;
        private String uncertaintyAltitude;
        private String innerRadius;
        private String uncertaintyInnerRadius;
        private String offsetAngle;
        private String includedAngle;
        private String horizontalSpeed;
        private String verticalSpeed;
        private String uncertaintyHorizontalSpeed;
        private String uncertaintyVerticalSpeed;
        private String bearing;
        private String geofenceType;
        private String geofenceId;
        private String geofenceEventType;
        private Long eventRange;
        private String civicAddress;
        private Long barometricPressure;
        private String physicalAddress;
        private String internetAddress;
        private String lastGeolocationResponse;
        private String cause;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
        }

        public Geolocation build() {
            final DateTime now = DateTime.now();
            return new Geolocation(sid, now, dateUpdated, now, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
                imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, sai, ecid,
                networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, tac, rai, typeOfShape, deviceLatitude, deviceLongitude,
                uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
                innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
                uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
                physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
        }

        public void setSid(Sid sid) {
            this.sid = sid;
        }

        public void setDateUpdated(DateTime dateUpdated) {
            this.dateUpdated = dateUpdated;
        }

        public void setLocationTimestamp(DateTime locationTimestamp) {
            try {
                this.locationTimestamp = locationTimestamp;
            } catch (Exception exception) {
                DateTime locTimestamp = DateTime.parse("1900-01-01");
                this.locationTimestamp = locTimestamp;
            }
        }

        public void setAccountSid(Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setDeviceIdentifier(String deviceIdentifier) {
            this.deviceIdentifier = deviceIdentifier;
        }

        public void setMsisdn(Long msisdn) {
            this.msisdn = msisdn;
        }

        public void setImsi(Long imsi) {
            this.imsi = imsi;
        }

        public void setImei(String imei) {
            this.imei = imei;
        }

        public void setLmsi(Long lmsi) {
            this.lmsi = lmsi;
        }

        public void setReferenceNumber(Long referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public void setGeolocationType(GeolocationType geolocationType) {
            this.geolocationType = geolocationType;
        }

        public void setResponseStatus(String responseStatus) {
            this.responseStatus = responseStatus;
        }

        public void setMobileCountryCode(Integer mobileCountryCode) {
            this.mobileCountryCode = mobileCountryCode;
        }

        public void setMobileNetworkCode(String mobileNetworkCode) {
            this.mobileNetworkCode = mobileNetworkCode;
        }

        public void setLocationAreaCode(String locationAreaCode) {
            this.locationAreaCode = locationAreaCode;
        }

        public void setCellId(String cellId) {
            this.cellId = cellId;
        }

        public void setSai(String sai) {
            this.sai = sai;
        }

        public void setEcid(Long ecid) {
            this.ecid = ecid;
        }

        public void setNetworkEntityAddress(Long networkEntityAddress) {
            this.networkEntityAddress = networkEntityAddress;
        }

        public void setNetworkEntityName(String networkEntityName) {
            this.networkEntityName = networkEntityName;
        }

        public void setAgeOfLocationInfo(Integer ageOfLocationInfo) {
            this.ageOfLocationInfo = ageOfLocationInfo;
        }

        public void setSubscriberState(String subscriberState) {
            this.subscriberState = subscriberState;
        }

        public void setTac(String tac) {
            this.tac = tac;
        }

        public void setRai(String rai) {
            this.rai = rai;
        }

        public void setTypeOfShape(String typeOfShape) {
            this.typeOfShape = typeOfShape;
        }

        public void setDeviceLatitude(String devLatitude) {
            this.deviceLatitude = devLatitude;
        }

        public void setDeviceLongitude(String devLongitude) {
            this.deviceLongitude = devLongitude;
        }

        public void setUncertainty(String uncertainty) {
            this.uncertainty = uncertainty;
        }

        public void setUncertaintySemiMajorAxis(String uncertaintySemiMajorAxis) {
            this.uncertaintySemiMajorAxis = uncertaintySemiMajorAxis;
        }

        public void setUncertaintySemiMinorAxis(String uncertaintySemiMinorAxis) {
            this.uncertaintySemiMinorAxis = uncertaintySemiMinorAxis;
        }

        public void setAngleOfMajorAxis(String angleOfMajorAxis) {
            this.angleOfMajorAxis = angleOfMajorAxis;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public void setAltitude(String altitude) {
            this.altitude = altitude;
        }

        public void setUncertaintyAltitude(String uncertaintyAltitude) {
            this.uncertaintyAltitude = uncertaintyAltitude;
        }

        public void setInnerRadius(String innerRadius) {
            this.innerRadius = innerRadius;
        }

        public void setUncertaintyInnerRadius(String uncertaintyInnerRadius) {
            this.uncertaintyInnerRadius = uncertaintyInnerRadius;
        }

        public void setOffsetAngle(String offsetAngle) {
            this.offsetAngle = offsetAngle;
        }

        public void setIncludedAngle(String includedAngle) {
            this.includedAngle = includedAngle;
        }

        public void setHorizontalSpeed(String horizontalSpeed) {
            this.horizontalSpeed = horizontalSpeed;
        }

        public void setVerticalSpeed(String verticalSpeed) {
            this.verticalSpeed = verticalSpeed;
        }

        public void setUncertaintyHorizontalSpeed(String uncertaintyHorizontalSpeed) {
            this.uncertaintyHorizontalSpeed = uncertaintyHorizontalSpeed;
        }

        public void setUncertaintyVerticalSpeed(String uncertaintyVerticalSpeed) {
            this.uncertaintyVerticalSpeed = uncertaintyVerticalSpeed;
        }

        public void setBearing(String bearing) {
            this.bearing = bearing;
        }

        public void setPhysicalAddress(String physicalAddress) {
            this.physicalAddress = physicalAddress;
        }

        public void setInternetAddress(String internetAddress) {
            this.internetAddress = internetAddress;
        }

        public void setGeofenceType(String geofenceType) {
            this.geofenceType = geofenceType;
        }

        public void setGeofenceId(String geofenceId) {
            this.geofenceId = geofenceId;
        }

        public void setGeofenceEventType(String geofenceEventType) {
            this.geofenceEventType = geofenceEventType;
        }

        public void setEventRange(Long eventRange) {
            this.eventRange = eventRange;
        }

        public void setCivicAddress(String civicAddress) {
            this.civicAddress = civicAddress;
        }

        public void setBarometricPressure(Long barometricPressure) {
            this.barometricPressure = barometricPressure;
        }

        public void setLastGeolocationResponse(String lastGeolocationResponse) {
            this.lastGeolocationResponse = lastGeolocationResponse;
        }

        public void setCause(String cause) {
            if (responseStatus != null && (responseStatus.equalsIgnoreCase("rejected")
                || responseStatus.equalsIgnoreCase("unauthorized") || responseStatus.equalsIgnoreCase("failed"))) {
                this.cause = cause;
                // "cause" is only updated if "responseStatus" is not null and is either "rejected", "unauthorized" or "failed"
                // Otherwise, it's value in HTTP POST/PUT is ignored
            }
            if (responseStatus != null && (!responseStatus.equalsIgnoreCase("rejected")
                && !responseStatus.equalsIgnoreCase("unauthorized") && !responseStatus.equalsIgnoreCase("failed"))) {
                this.cause = null;
                // "cause" is set to null if "responseStatus" is not null and is neither "rejected", "unauthorized" nor "failed"
            }
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

    }

}
