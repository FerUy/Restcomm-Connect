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
    private final Integer mobileNetworkCode;
    private final Integer locationAreaCode;
    private final Integer ci;
    private final Integer sac;
    private final Integer enbid;
    private final Long networkEntityAddress;
    private final String networkEntityName;
    private final Integer ageOfLocationInfo;
    private final String subscriberState;
    private final String notReachableReason;
    private final Integer tac;
    private final Integer rac;
    private final String typeOfShape;
    private final String deviceLatitude;
    private final String deviceLongitude;
    private final Double uncertainty;
    private final Double uncertaintySemiMajorAxis;
    private final Double uncertaintySemiMinorAxis;
    private final Double angleOfMajorAxis;
    private final Integer confidence;
    private final Integer altitude;
    private final Double uncertaintyAltitude;
    private final Integer innerRadius;
    private final Double uncertaintyInnerRadius;
    private final Double offsetAngle;
    private final Double includedAngle;
    private final Integer horizontalSpeed;
    private final Integer verticalSpeed;
    private final Integer uncertaintyHorizontalSpeed;
    private final Integer uncertaintyVerticalSpeed;
    private final Integer bearing;
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
                       GeolocationType geolocationType, String responseStatus, Integer mobileCountryCode, Integer mobileNetworkCode,
                       Integer locationAreaCode, Integer ci, Integer sac, Integer enbid, Long networkEntityAddress, String networkEntityName,
                       Integer ageOfLocationInfo, String subscriberState, String notReachableReason, Integer tac, Integer rac,
                       String typeOfShape, String deviceLatitude, String deviceLongitude, Double uncertainty, Double uncertaintySemiMajorAxis,
                       Double uncertaintySemiMinorAxis, Double angleOfMajorAxis, Integer confidence,
                       Integer altitude, Double uncertaintyAltitude, Integer innerRadius, Double uncertaintyInnerRadius, Double offsetAngle,
                       Double includedAngle, Integer horizontalSpeed, Integer verticalSpeed, Integer uncertaintyHorizontalSpeed, Integer uncertaintyVerticalSpeed,
                       Integer bearing, String geofenceType, String geofenceId, String geofenceEventType, Long eventRange, String civicAddress,
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
        this.ci = ci;
        this.sac = sac;
        this.enbid = enbid;
        this.networkEntityAddress = networkEntityAddress;
        this.networkEntityName = networkEntityName;
        this.ageOfLocationInfo = ageOfLocationInfo;
        this.subscriberState = subscriberState;
        this.notReachableReason = notReachableReason;
        this.tac = tac;
        this.rac = rac;
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

    public Integer getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    public Integer getLocationAreaCode() {
        return locationAreaCode;
    }

    public Integer getCi() {
        return ci;
    }

    public Integer getSac() {
        return sac;
    }

    public Integer getEnbid() {
        return enbid;
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

    public String getNotReachableReason() {
        return notReachableReason;
    }

    public Integer getTac() {
        return tac;
    }

    public Integer getRac() {
        return rac;
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

    public Double getUncertainty() {
        return uncertainty;
    }

    public Double getUncertaintySemiMajorAxis() {
        return uncertaintySemiMajorAxis;
    }

    public Double getUncertaintySemiMinorAxis() {
        return uncertaintySemiMinorAxis;
    }

    public Double getAngleOfMajorAxis() {
        return angleOfMajorAxis;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public Double getUncertaintyAltitude() {
        return uncertaintyAltitude;
    }

    public Integer getInnerRadius() {
        return innerRadius;
    }

    public Double getUncertaintyInnerRadius() {
        return uncertaintyInnerRadius;
    }

    public Double getOffsetAngle() {
        return offsetAngle;
    }

    public Double getIncludedAngle() {
        return includedAngle;
    }

    public Integer getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public Integer getVerticalSpeed() {
        return verticalSpeed;
    }

    public Integer getUncertaintyHorizontalSpeed() {
        return uncertaintyHorizontalSpeed;
    }

    public Integer getUncertaintyVerticalSpeed() {
        return uncertaintyVerticalSpeed;
    }

    public Integer getBearing() {
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
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateCreated(DateTime dateCreated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateUpdated(DateTime dateUpdated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateExecuted(DateTime dateExecuted) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationTimestamp(DateTime locationTimestamp) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccountSid(Sid accountSid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSource(String source) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceIdentifier(String deviceIdentifier) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMsisdn(Long msisdn) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setImsi(Long imsi) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setImei(String imei) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLmsi(Long lmsi) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setReferenceNumber(Long referenceNumber) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationType(GeolocationType geolocationType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setResponseStatus(String responseStatus) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileCountryCode(Integer mobileCountryCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileNetworkCode(Integer mobileNetworkCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationAreaCode(Integer locationAreaCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCi(Integer ci) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSac(Integer sac) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEnbid(Integer enbid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityAddress(Long networkEntityAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityName(String networkEntityName) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAgeOfLocationInfo(Integer ageOfLocationInfo) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSubscriberState(String subscriberState) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNotReachableReason(String notReachableReason) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setTac(Integer tac) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setRac(Integer rac) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setTypeOfShape(String typeOfShape) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLatitude(String deviceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLongitude(String deviceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertainty(Double uncertainty) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintySemiMajorAxis(Double uncertaintySemiMajorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintySemiMinorAxis(Double uncertaintySemiMinorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAngleOfMajorAxis(Double angleOfMajorAxis) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setConfidence(Integer confidence) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAltitude(Integer altitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyAltitude(Double uncertaintyAltitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInnerRadius(Integer innerRadius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyInnerRadius(Double uncertaintyInnerRadius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setOffsetAngle(Double offsetAngle) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setIncludedAngle(Double includedAngle) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setHorizontalSpeed(Integer horizontalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setVerticalSpeed(Integer verticalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyHorizontalSpeed(Integer uncertaintyHorizontalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUncertaintyVerticalSpeed(Integer uncertaintyVerticalSpeed) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setBearing(Integer bearing) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceType(String geofenceType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceId(String geofenceId) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeofenceEventType(String geofenceEventType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventRange(Long eventRange) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCivicAddress(String civicAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setBarometricPressure(Long barometricPressure) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setPhysicalAddress(String physicalAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInternetAddress(String internetAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLastGeolocationResponse(String lastGeolocationResponse) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCause(String cause) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setApiVersion(String apiVersion) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
            uncertainty, uncertaintySemiMajorAxis, uncertaintySemiMinorAxis, angleOfMajorAxis, confidence, altitude, uncertaintyAltitude,
            innerRadius, uncertaintyInnerRadius, offsetAngle, includedAngle, horizontalSpeed, verticalSpeed, uncertaintyHorizontalSpeed,
            uncertaintyVerticalSpeed, bearing, geofenceType, geofenceId, geofenceEventType, eventRange, civicAddress, barometricPressure,
            physicalAddress, internetAddress, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUri(URI uri) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, locationTimestamp, accountSid, source, deviceIdentifier, msisdn, imsi,
            imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
            networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
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
        private Integer mobileNetworkCode;
        private Integer locationAreaCode;
        private Integer ci;
        private Integer sac;
        private Integer enbid;
        private Long networkEntityAddress;
        private String networkEntityName;
        private Integer ageOfLocationInfo;
        private String subscriberState;
        private String notReachableReason;
        private Integer tac;
        private Integer rac;
        private String typeOfShape;
        private String deviceLatitude;
        private String deviceLongitude;
        private Double uncertainty;
        private Double uncertaintySemiMajorAxis;
        private Double uncertaintySemiMinorAxis;
        private Double angleOfMajorAxis;
        private Integer confidence;
        private Integer altitude;
        private Double uncertaintyAltitude;
        private Integer innerRadius;
        private Double uncertaintyInnerRadius;
        private Double offsetAngle;
        private Double includedAngle;
        private Integer horizontalSpeed;
        private Integer verticalSpeed;
        private Integer uncertaintyHorizontalSpeed;
        private Integer uncertaintyVerticalSpeed;
        private Integer bearing;
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
                imei, lmsi, referenceNumber, geolocationType, responseStatus, mobileCountryCode, mobileNetworkCode, locationAreaCode, ci, sac, enbid,
                networkEntityAddress, networkEntityName, ageOfLocationInfo, subscriberState, notReachableReason, tac, rac, typeOfShape, deviceLatitude, deviceLongitude,
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

        public void setMobileNetworkCode(Integer mobileNetworkCode) {
            this.mobileNetworkCode = mobileNetworkCode;
        }

        public void setLocationAreaCode(Integer locationAreaCode) {
            this.locationAreaCode = locationAreaCode;
        }

        public void setCi(Integer ci) {
            this.ci = ci;
        }

        public void setSac(Integer sac) {
            this.sac = sac;
        }

        public void setEnbid(Integer enbid) {
            this.enbid = enbid;
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

        public void setNotReachableReason(String notReachableReason) {
            this.notReachableReason = notReachableReason;
        }

        public void setTac(Integer tac) {
            this.tac = tac;
        }

        public void setRac(Integer rac) {
            this.rac = rac;
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

        public void setUncertainty(Double uncertainty) {
            this.uncertainty = uncertainty;
        }

        public void setUncertaintySemiMajorAxis(Double uncertaintySemiMajorAxis) {
            this.uncertaintySemiMajorAxis = uncertaintySemiMajorAxis;
        }

        public void setUncertaintySemiMinorAxis(Double uncertaintySemiMinorAxis) {
            this.uncertaintySemiMinorAxis = uncertaintySemiMinorAxis;
        }

        public void setAngleOfMajorAxis(Double angleOfMajorAxis) {
            this.angleOfMajorAxis = angleOfMajorAxis;
        }

        public void setConfidence(Integer confidence) {
            this.confidence = confidence;
        }

        public void setAltitude(Integer altitude) {
            this.altitude = altitude;
        }

        public void setUncertaintyAltitude(Double uncertaintyAltitude) {
            this.uncertaintyAltitude = uncertaintyAltitude;
        }

        public void setInnerRadius(Integer innerRadius) {
            this.innerRadius = innerRadius;
        }

        public void setUncertaintyInnerRadius(Double uncertaintyInnerRadius) {
            this.uncertaintyInnerRadius = uncertaintyInnerRadius;
        }

        public void setOffsetAngle(Double offsetAngle) {
            this.offsetAngle = offsetAngle;
        }

        public void setIncludedAngle(Double includedAngle) {
            this.includedAngle = includedAngle;
        }

        public void setHorizontalSpeed(Integer horizontalSpeed) {
            this.horizontalSpeed = horizontalSpeed;
        }

        public void setVerticalSpeed(Integer verticalSpeed) {
            this.verticalSpeed = verticalSpeed;
        }

        public void setUncertaintyHorizontalSpeed(Integer uncertaintyHorizontalSpeed) {
            this.uncertaintyHorizontalSpeed = uncertaintyHorizontalSpeed;
        }

        public void setUncertaintyVerticalSpeed(Integer uncertaintyVerticalSpeed) {
            this.uncertaintyVerticalSpeed = uncertaintyVerticalSpeed;
        }

        public void setBearing(Integer bearing) {
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
