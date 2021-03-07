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

package org.restcomm.connect.http.converter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.dao.entities.Geolocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;


/**
 * @author <a href="mailto:fernando.mendioroz@gmail.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationConverter extends AbstractConverter implements JsonSerializer<Geolocation> {

    public GeolocationConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Geolocation.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {

        final Geolocation geolocation = (Geolocation) object;
        writer.startNode("Geolocation");
        writeSid(geolocation.getSid(), writer);
        writeDateCreated(geolocation.getDateCreated(), writer);
        writeDateUpdated(geolocation.getDateUpdated(), writer);
        writeDateExecuted(geolocation.getDateExecuted(), writer);
        writeAccountSid(geolocation.getAccountSid(), writer);
        writeSource(geolocation.getSource(), writer);
        writeDeviceIdentifier(geolocation.getDeviceIdentifier(), writer);
        writeMsisdn(geolocation.getMsisdn(), writer);
        writeImsi(geolocation.getImsi(), writer);
        writeImei(geolocation.getImei(), writer);
        writeReferenceNumber(geolocation.getReferenceNumber(), writer);
        writeGeolocationType(geolocation.getGeolocationType(), writer);
        writeResponseStatus(geolocation.getResponseStatus(), writer);
        writeGeolocationData(geolocation, writer); /*** GeolocationData XML ***/
        writeLastGeolocationResponse(geolocation.getLastGeolocationResponse(), writer);
        writeCause(geolocation.getCause(), writer);
        writeApiVersion(geolocation.getApiVersion(), writer);
        writeUri(geolocation.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Geolocation geolocation, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(geolocation.getSid(), object);
        writeDateCreated(geolocation.getDateCreated(), object);
        writeDateUpdated(geolocation.getDateUpdated(), object);
        writeDateExecuted(geolocation.getDateExecuted(), object);
        writeAccountSid(geolocation.getAccountSid(), object);
        writeSource(geolocation.getSource(), object);
        writeDeviceIdentifier(geolocation.getDeviceIdentifier(), object);
        writeMsisdn(geolocation.getMsisdn(), object);
        writeImsi(geolocation.getImsi(), object);
        writeImei(geolocation.getImei(), object);
        writeReferenceNumber(geolocation.getReferenceNumber(), object);
        writeGeolocationType(geolocation.getGeolocationType(), object);
        writeResponseStatus(geolocation.getResponseStatus(), object);
        writeGeolocationData(geolocation, object); /*** GeolocationData JSON ***/
        writeLastGeolocationResponse(geolocation.getLastGeolocationResponse(), object);
        writeCause(geolocation.getCause(), object);
        writeApiVersion(geolocation.getApiVersion(), object);
        writeUri(geolocation.getUri(), object);
        return object;
    }

    protected void writeDateExecuted(final DateTime dateExecuted, final HierarchicalStreamWriter writer) {
        writer.startNode("DateExecuted");
        writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateExecuted.toDate()));
        writer.endNode();
    }

    protected void writeDateExecuted(final DateTime dateExecuted, final JsonObject object) {
        object.addProperty("date_executed",
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateExecuted.toDate()));
    }

    protected void writeSource(final String source, final HierarchicalStreamWriter writer) {
        if (source != null) {
            writer.startNode("Source");
            writer.setValue(source);
            writer.endNode();
        }
    }

    protected void writeSource(final String source, final JsonObject object) {
        if (source != null) {
            object.addProperty("source", source);
        } else {
            object.add("source", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceIdentifier(final String deviceIdentifier, final HierarchicalStreamWriter writer) {
        if (deviceIdentifier != null) {
            writer.startNode("DeviceIdentifier");
            writer.setValue(deviceIdentifier);
            writer.endNode();
        }
    }

    protected void writeDeviceIdentifier(final String deviceIdentifier, final JsonObject object) {
        if (deviceIdentifier != null) {
            object.addProperty("device_identifier", deviceIdentifier);
        } else {
            object.add("device_identifier", JsonNull.INSTANCE);
        }
    }

    protected void writeMsisdn(final Long msisdn, final HierarchicalStreamWriter writer) {
        if (msisdn != null) {
            writer.startNode("MSISDN");
            writer.setValue(msisdn.toString());
            writer.endNode();
        }
    }

    protected void writeMsisdn(final Long msisdn, final JsonObject object) {
        if (msisdn != null) {
            object.addProperty("msisdn", msisdn);
        } else {
            object.add("msisdn", JsonNull.INSTANCE);
        }
    }

    protected void writeImsi(final Long imsi, final HierarchicalStreamWriter writer) {
        if (imsi != null) {
            writer.startNode("IMSI");
            writer.setValue(imsi.toString());
            writer.endNode();
        }
    }

    protected void writeImsi(final Long imsi, final JsonObject object) {
        if (imsi != null) {
            object.addProperty("imsi", imsi);
        } else {
            object.add("imsi", JsonNull.INSTANCE);
        }
    }

    protected void writeImei(final String imei, final HierarchicalStreamWriter writer) {
        if (imei != null) {
            writer.startNode("IMEI");
            writer.setValue(imei);
            writer.endNode();
        }
    }

    protected void writeImei(final String imei, final JsonObject object) {
        if (imei != null) {
            object.addProperty("imei", imei);
        } else {
            object.add("imei", JsonNull.INSTANCE);
        }
    }

    protected void writeLmsi(final Long lmsi, final HierarchicalStreamWriter writer) {
        if (lmsi != null) {
            writer.startNode("LMSI");
            writer.setValue(lmsi.toString());
            writer.endNode();
        }
    }

    protected void writeLmsi(final Long lmsi, final JsonObject object) {
        if (lmsi != null) {
            object.addProperty("lmsi", lmsi);
        } else {
            object.add("lmsi", JsonNull.INSTANCE);
        }
    }

    protected void writeReferenceNumber(final Long referenceNumber, final HierarchicalStreamWriter writer) {
        if (referenceNumber != null) {
            writer.startNode("ReferenceNumber");
            writer.setValue(referenceNumber.toString());
            writer.endNode();
        }
    }

    protected void writeReferenceNumber(final Long referenceNumber, final JsonObject object) {
        if (referenceNumber != null) {
            object.addProperty("reference_number", referenceNumber);
        } else {
            object.add("reference_number", JsonNull.INSTANCE);
        }
    }

    protected void writeGeolocationType(final Geolocation.GeolocationType geolocationType,
                                        final HierarchicalStreamWriter writer) {
        if (geolocationType != null) {
            writer.startNode("GeolocationType");
            writer.setValue(geolocationType.toString());
            writer.endNode();
        }
    }

    protected void writeGeolocationType(final Geolocation.GeolocationType geolocationType, final JsonObject object) {
        if (geolocationType != null) {
            object.addProperty("geolocation_type", geolocationType.toString());
        } else {
            object.add("geolocation_type", JsonNull.INSTANCE);
        }
    }

    protected void writeResponseStatus(final String responseStatus, final HierarchicalStreamWriter writer) {
        if (responseStatus != null) {
            writer.startNode("ResponseStatus");
            writer.setValue(responseStatus);
            writer.endNode();
        }
    }

    protected void writeResponseStatus(final String responseStatus, final JsonObject object) {
        if (responseStatus != null) {
            object.addProperty("response_status", responseStatus);
        } else {
            object.add("response_status", JsonNull.INSTANCE);
        }
    }

    protected void writeGeolocationData(Geolocation geolocation, final HierarchicalStreamWriter writer) {
        writer.startNode("GeolocationData");
        if (geolocation != null) {
            writeLocationTimestamp(geolocation.getLocationTimestamp(), writer);
            writeMobileCountryCode(geolocation.getMobileCountryCode(), writer);
            writeMobileNetworkCode(geolocation.getMobileNetworkCode(), writer);
            writeLocationAreaCode(geolocation.getLocationAreaCode(), writer);
            writeCellId(geolocation.getCellId(), writer);
            writeECellId(geolocation.getECellId(), writer);
            writeNrCellId(geolocation.getNrCellId(), writer);
            writeSac(geolocation.getServiceAreaCode(), writer);
            writeEnbid(geolocation.getEnodebId(), writer);
            writeTrackingAreaCode(geolocation.getTrackingAreaCode(), writer);
            writeRoutingAreaCode(geolocation.getRoutingAreaCode(), writer);
            writeLocationNumberAddress(geolocation.getLocationNumberAddress(), writer);
            writeNetworkEntityAddress(geolocation.getNetworkEntityAddress(), writer);
            writeNetworkEntityName(geolocation.getNetworkEntityName(), writer);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), writer);
            writeSubscriberState(geolocation.getSubscriberState(), writer);
            writeNotReachableReason(geolocation.getNotReachableReason(), writer);
            writeTypeOfShape(geolocation.getTypeOfShape(), writer);
            writeDeviceLatitude(geolocation.getDeviceLatitude(), writer);
            writeDeviceLongitude(geolocation.getDeviceLongitude(), writer);
            writeUncertainty(geolocation.getUncertainty(), writer);
            writeUncertaintySemiMajorAxis(geolocation.getUncertaintySemiMajorAxis(), writer);
            writeUncertaintySemiMinorAxis(geolocation.getUncertaintySemiMinorAxis(), writer);
            writeAngleOfMajorAxis(geolocation.getAngleOfMajorAxis(), writer);
            writeConfidence(geolocation.getConfidence(), writer);
            writeDeviceAltitude(geolocation.getAltitude(), writer);
            writeDeviceAltitudeUncertainty(geolocation.getUncertaintyAltitude(), writer);
            writeInnerRadius(geolocation.getInnerRadius(), writer);
            writeUncertaintyInnerRadius(geolocation.getUncertaintyInnerRadius(), writer);
            writeIncludedAngle(geolocation.getIncludedAngle(), writer);
            writeHorizontalSpeed(geolocation.getHorizontalSpeed(), writer);
            writeVerticalSpeed(geolocation.getVerticalSpeed(), writer);
            writeUncertaintyHorizontalSpeed(geolocation.getUncertaintyHorizontalSpeed(), writer);
            writeUncertaintyVerticalSpeed(geolocation.getUncertaintyVerticalSpeed(), writer);
            writeBearing(geolocation.getBearing(), writer);
            writeDeferredLocationEventType(geolocation.getDeferredLocationEventType(), writer);
            writeGeofenceType(geolocation.getGeofenceType(), writer);
            writeGeofenceId(geolocation.getGeofenceId(), writer);
            writeMotionEventRange(geolocation.getMotionEventRange(), writer);
            writeCivicAddress(geolocation.getCivicAddress(), writer);
            writeBarometricPressure(geolocation.getBarometricPressure(), writer);
            writeRadioAccessType(geolocation.getRadioAccessType(), writer);
            writeInternetAddress(geolocation.getInternetAddress(), writer);
            writePhysicalAddress(geolocation.getPhysicalAddress(), writer);
        }
        writer.endNode();
    }

    protected void writeGeolocationData(Geolocation geolocation, final JsonObject object) {
        if (geolocation != null) {
            final JsonObject locationDataJsonObject = new JsonObject();
            writeLocationTimestamp(geolocation.getLocationTimestamp(), locationDataJsonObject);
            writeMobileCountryCode(geolocation.getMobileCountryCode(), locationDataJsonObject);
            writeMobileNetworkCode(geolocation.getMobileNetworkCode(), locationDataJsonObject);
            writeLocationAreaCode(geolocation.getLocationAreaCode(), locationDataJsonObject);
            writeCellId(geolocation.getCellId(), locationDataJsonObject);
            writeECellId(geolocation.getECellId(), locationDataJsonObject);
            writeNrCellId(geolocation.getNrCellId(), locationDataJsonObject);
            writeSac(geolocation.getServiceAreaCode(), locationDataJsonObject);
            writeEnbid(geolocation.getEnodebId(), locationDataJsonObject);
            writeTrackingAreaCode(geolocation.getTrackingAreaCode(), locationDataJsonObject);
            writeRoutingAreaCode(geolocation.getRoutingAreaCode(), locationDataJsonObject);
            writeLocationNumberAddress(geolocation.getLocationNumberAddress(), locationDataJsonObject);
            writeNetworkEntityAddress(geolocation.getNetworkEntityAddress(), locationDataJsonObject);
            writeNetworkEntityName(geolocation.getNetworkEntityName(), locationDataJsonObject);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), locationDataJsonObject);
            writeSubscriberState(geolocation.getSubscriberState(), locationDataJsonObject);
            writeNotReachableReason(geolocation.getNotReachableReason(), locationDataJsonObject);
            writeTypeOfShape(geolocation.getTypeOfShape(), locationDataJsonObject);
            writeDeviceLatitude(geolocation.getDeviceLatitude(), locationDataJsonObject);
            writeDeviceLongitude(geolocation.getDeviceLongitude(), locationDataJsonObject);
            writeUncertainty(geolocation.getUncertainty(), locationDataJsonObject);
            writeUncertaintySemiMajorAxis(geolocation.getUncertaintySemiMajorAxis(), locationDataJsonObject);
            writeUncertaintySemiMinorAxis(geolocation.getUncertaintySemiMinorAxis(), locationDataJsonObject);
            writeAngleOfMajorAxis(geolocation.getAngleOfMajorAxis(), locationDataJsonObject);
            writeConfidence(geolocation.getConfidence(), locationDataJsonObject);
            writeDeviceAltitude(geolocation.getAltitude(), locationDataJsonObject);
            writeDeviceAltitudeUncertainty(geolocation.getUncertaintyAltitude(), locationDataJsonObject);
            writeInnerRadius(geolocation.getInnerRadius(), locationDataJsonObject);
            writeUncertaintyInnerRadius(geolocation.getUncertaintyInnerRadius(), locationDataJsonObject);
            writeOffsetAngle(geolocation.getOffsetAngle(), locationDataJsonObject);
            writeIncludedAngle(geolocation.getIncludedAngle(), locationDataJsonObject);
            writeHorizontalSpeed(geolocation.getHorizontalSpeed(), locationDataJsonObject);
            writeVerticalSpeed(geolocation.getVerticalSpeed(), locationDataJsonObject);
            writeUncertaintyHorizontalSpeed(geolocation.getUncertaintyHorizontalSpeed(), locationDataJsonObject);
            writeUncertaintyVerticalSpeed(geolocation.getUncertaintyVerticalSpeed(), locationDataJsonObject);
            writeBearing(geolocation.getBearing(), locationDataJsonObject);
            writeDeferredLocationEventType(geolocation.getDeferredLocationEventType(), locationDataJsonObject);
            writeGeofenceType(geolocation.getGeofenceType(), locationDataJsonObject);
            writeGeofenceId(geolocation.getGeofenceId(), locationDataJsonObject);
            writeMotionEventRange(geolocation.getMotionEventRange(), locationDataJsonObject);
            writeCivicAddress(geolocation.getCivicAddress(), locationDataJsonObject);
            writeBarometricPressure(geolocation.getBarometricPressure(), locationDataJsonObject);
            writeRadioAccessType(geolocation.getRadioAccessType(), locationDataJsonObject);
            writeInternetAddress(geolocation.getInternetAddress(), locationDataJsonObject);
            writePhysicalAddress(geolocation.getPhysicalAddress(), locationDataJsonObject);
            object.add("geolocation_data", locationDataJsonObject);
        } else {
            object.add("geolocation_data", JsonNull.INSTANCE);
        }
    }

    protected void writeMobileCountryCode(final Integer mobileCountryCode, final HierarchicalStreamWriter writer) {
        if (mobileCountryCode != null) {
            writer.startNode("MobileCountryCode");
            writer.setValue(mobileCountryCode.toString());
            writer.endNode();
        }
    }

    protected void writeMobileCountryCode(final Integer mobileCountryCode, final JsonObject object) {
        if (mobileCountryCode != null) {
            object.addProperty("mobile_country_code", mobileCountryCode);
        } else {
            object.add("mobile_country_code", JsonNull.INSTANCE);
        }
    }

    protected void writeMobileNetworkCode(final Integer mobileNetworkCode, final HierarchicalStreamWriter writer) {
        if (mobileNetworkCode != null) {
            writer.startNode("MobileNetworkCode");
            writer.setValue(String.valueOf(mobileNetworkCode));
            writer.endNode();
        }
    }

    protected void writeMobileNetworkCode(final Integer mobileNetworkCode, final JsonObject object) {
        if (mobileNetworkCode != null) {
            object.addProperty("mobile_network_code", mobileNetworkCode);
        } else {
            object.add("mobile_network_code", JsonNull.INSTANCE);
        }
    }

    protected void writeLocationAreaCode(final Integer locationAreaCode, final HierarchicalStreamWriter writer) {
        if (locationAreaCode != null) {
            writer.startNode("LocationAreaCode");
            writer.setValue(String.valueOf(locationAreaCode));
            writer.endNode();
        }
    }

    protected void writeLocationAreaCode(final Integer locationAreaCode, final JsonObject object) {
        if (locationAreaCode != null) {
            object.addProperty("location_area_code", locationAreaCode);
        } else {
            object.add("location_area_code", JsonNull.INSTANCE);
        }
    }

    protected void writeCellId(final Integer cellId, final HierarchicalStreamWriter writer) {
        if (cellId != null) {
            writer.startNode("CellId");
            writer.setValue(String.valueOf(cellId));
            writer.endNode();
        }
    }

    protected void writeCellId(final Integer cellId, final JsonObject object) {
        if (cellId != null) {
            object.addProperty("cell_id", cellId);
        } else {
            object.add("cell_id", JsonNull.INSTANCE);
        }
    }

    protected void writeECellId(final Long eCellId, final HierarchicalStreamWriter writer) {
        if (eCellId != null) {
            writer.startNode("ECellId");
            writer.setValue(String.valueOf(eCellId));
            writer.endNode();
        }
    }

    protected void writeECellId(final Long eCellId, final JsonObject object) {
        if (eCellId != null) {
            object.addProperty("e_cell_id", eCellId);
        } else {
            object.add("e_cell_id", JsonNull.INSTANCE);
        }
    }

    protected void writeNrCellId(final Long nrCellId, final HierarchicalStreamWriter writer) {
        if (nrCellId != null) {
            writer.startNode("NRCellId");
            writer.setValue(String.valueOf(nrCellId));
            writer.endNode();
        }
    }

    protected void writeNrCellId(final Long nrCellId, final JsonObject object) {
        if (nrCellId != null) {
            object.addProperty("nr_cell_id", nrCellId);
        } else {
            object.add("nr_cell_id", JsonNull.INSTANCE);
        }
    }

    protected void writeSac(final Integer sac, final HierarchicalStreamWriter writer) {
        if (sac != null) {
            writer.startNode("ServiceAreaCode");
            writer.setValue(String.valueOf(sac));
            writer.endNode();
        }
    }

    protected void writeSac(final Integer sac, final JsonObject object) {
        if (sac != null) {
            object.addProperty("service_area_code", sac);
        } else {
            object.add("service_area_code", JsonNull.INSTANCE);
        }
    }

    protected void writeEnbid(final Integer enbid, final HierarchicalStreamWriter writer) {
        if (enbid != null) {
            writer.startNode("ENodeBId");
            writer.setValue(String.valueOf(enbid));
            writer.endNode();
        }
    }

    protected void writeEnbid(final Integer enbid, final JsonObject object) {
        if (enbid != null) {
            object.addProperty("enodeb_id", enbid);
        } else {
            object.add("enodeb_id", JsonNull.INSTANCE);
        }
    }

    protected void writeTrackingAreaCode(final Integer tac, final HierarchicalStreamWriter writer) {
        if (tac != null) {
            writer.startNode("TrackingAreaCode");
            writer.setValue(String.valueOf(tac));
            writer.endNode();
        }
    }

    protected void writeTrackingAreaCode(final Integer tac, final JsonObject object) {
        if (tac != null) {
            object.addProperty("tracking_area_code", tac);
        } else {
            object.add("tracking_area_code", JsonNull.INSTANCE);
        }
    }

    protected void writeRoutingAreaCode(final Integer rac, final HierarchicalStreamWriter writer) {
        if (rac != null) {
            writer.startNode("RoutingAreaCode");
            writer.setValue(String.valueOf(rac));
            writer.endNode();
        }
    }

    protected void writeRoutingAreaCode(final Integer rac, final JsonObject object) {
        if (rac != null) {
            object.addProperty("routing_area_code", rac);
        } else {
            object.add("routing_area_code", JsonNull.INSTANCE);
        }
    }

    protected void writeLocationNumberAddress(final Long address, final HierarchicalStreamWriter writer) {
        if (address != null) {
            writer.startNode("LocationNumberAddress");
            writer.setValue(address.toString());
            writer.endNode();
        }
    }

    protected void writeLocationNumberAddress(final Long address, final JsonObject object) {
        if (address != null) {
            object.addProperty("location_number_address", address);
        } else {
            object.add("location_number_address", JsonNull.INSTANCE);
        }
    }

    protected void writeNetworkEntityAddress(final Long networkEntityAddress, final HierarchicalStreamWriter writer) {
        if (networkEntityAddress != null) {
            writer.startNode("NetworkEntityAddress");
            writer.setValue(networkEntityAddress.toString());
            writer.endNode();
        }
    }

    protected void writeNetworkEntityAddress(final Long networkEntityAddress, final JsonObject object) {
        if (networkEntityAddress != null) {
            object.addProperty("network_entity_address", networkEntityAddress);
        } else {
            object.add("network_entity_address", JsonNull.INSTANCE);
        }
    }

    protected void writeNetworkEntityName(final String networkEntityName, final HierarchicalStreamWriter writer) {
        if (networkEntityName != null) {
            writer.startNode("NetworkEntityName");
            writer.setValue(networkEntityName);
            writer.endNode();
        }
    }

    protected void writeNetworkEntityName(final String networkEntityName, final JsonObject object) {
        if (networkEntityName != null) {
            object.addProperty("network_entity_name", networkEntityName);
        } else {
            object.add("network_entity_name", JsonNull.INSTANCE);
        }
    }

    protected void writeAgeOfLocationInfo(final Integer ageOfLocationInfo, final HierarchicalStreamWriter writer) {
        if (ageOfLocationInfo != null) {
            writer.startNode("LocationAge");
            writer.setValue(String.valueOf(ageOfLocationInfo));
            writer.endNode();
        }
    }

    protected void writeAgeOfLocationInfo(final Integer ageOfLocationInfo, final JsonObject object) {
        if (ageOfLocationInfo != null) {
            object.addProperty("location_age", ageOfLocationInfo);
        } else {
            object.add("location_age", JsonNull.INSTANCE);
        }
    }

    protected void writeSubscriberState(final String subscriberState, final HierarchicalStreamWriter writer) {
        if (subscriberState != null) {
            writer.startNode("SubscriberState");
            writer.setValue(subscriberState);
            writer.endNode();
        }
    }

    protected void writeSubscriberState(final String subscriberState, final JsonObject object) {
        if (subscriberState != null) {
            object.addProperty("subscriber_state", subscriberState);
        } else {
            object.add("subscriber_state", JsonNull.INSTANCE);
        }
    }

    protected void writeNotReachableReason(final String notReachableReason, final HierarchicalStreamWriter writer) {
        if (notReachableReason != null) {
            writer.startNode("NotReachableReason");
            writer.setValue(notReachableReason);
            writer.endNode();
        }
    }

    protected void writeNotReachableReason(final String notReachableReason, final JsonObject object) {
        if (notReachableReason != null) {
            object.addProperty("not_reachable_reason", notReachableReason);
        } else {
            object.add("not_reachable_reason", JsonNull.INSTANCE);
        }
    }

    protected void writeTypeOfShape(final String typeOfShape, final HierarchicalStreamWriter writer) {
        if (typeOfShape != null) {
            writer.startNode("TypeOfShape");
            writer.setValue(typeOfShape);
            writer.endNode();
        }
    }

    protected void writeTypeOfShape(final String typeOfShape, final JsonObject object) {
        if (typeOfShape != null) {
            object.addProperty("type_of_shape", typeOfShape);
        } else {
            object.add("type_of_shape", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceLatitude(final String deviceLatitude, final HierarchicalStreamWriter writer) {
        if (deviceLatitude != null) {
            writer.startNode("DeviceLatitude");
            writer.setValue(deviceLatitude);
            writer.endNode();
        }
    }

    protected void writeDeviceLatitude(final String deviceLatitude, final JsonObject object) {
        if (deviceLatitude != null) {
            object.addProperty("device_latitude", deviceLatitude);
        } else {
            object.add("device_latitude", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceLongitude(final String deviceLongitude, final HierarchicalStreamWriter writer) {
        if (deviceLongitude != null) {
            writer.startNode("DeviceLongitude");
            writer.setValue(deviceLongitude);
            writer.endNode();
        }
    }

    protected void writeDeviceLongitude(final String deviceLongitude, final JsonObject object) {
        if (deviceLongitude != null) {
            object.addProperty("device_longitude", deviceLongitude);
        } else {
            object.add("device_longitude", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertainty(final Double uncertainty, final HierarchicalStreamWriter writer) {
        if (uncertainty != null) {
            writer.startNode("Uncertainty");
            writer.setValue(String.valueOf(uncertainty));
            writer.endNode();
        }
    }

    protected void writeUncertainty(final Double uncertainty, final JsonObject object) {
        if (uncertainty != null) {
            object.addProperty("uncertainty", uncertainty);
        } else {
            object.add("uncertainty", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertaintySemiMajorAxis(final Double uncertaintySemiMajorAxis, final HierarchicalStreamWriter writer) {
        if (uncertaintySemiMajorAxis != null) {
            writer.startNode("UncertaintySemiMajorAxis");
            writer.setValue(String.valueOf(uncertaintySemiMajorAxis));
            writer.endNode();
        }
    }

    protected void writeUncertaintySemiMajorAxis(final Double uncertaintySemiMajorAxis, final JsonObject object) {
        if (uncertaintySemiMajorAxis != null) {
            object.addProperty("uncertainty_semi_major_axis", uncertaintySemiMajorAxis);
        } else {
            object.add("uncertainty_semi_major_axis", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertaintySemiMinorAxis(final Double uncertaintySemiMinorAxis, final HierarchicalStreamWriter writer) {
        if (uncertaintySemiMinorAxis != null) {
            writer.startNode("UncertaintySemiMinorAxis");
            writer.setValue(String.valueOf(uncertaintySemiMinorAxis));
            writer.endNode();
        }
    }

    protected void writeUncertaintySemiMinorAxis(final Double uncertaintySemiMinorAxis, final JsonObject object) {
        if (uncertaintySemiMinorAxis != null) {
            object.addProperty("uncertainty_semi_minor_axis", uncertaintySemiMinorAxis);
        } else {
            object.add("uncertainty_semi_minor_axis", JsonNull.INSTANCE);
        }
    }

    protected void writeAngleOfMajorAxis(final Double angleOfMajorAxis, final HierarchicalStreamWriter writer) {
        if (angleOfMajorAxis != null) {
            writer.startNode("AngleOfMajorAxis");
            writer.setValue(String.valueOf(angleOfMajorAxis));
            writer.endNode();
        }
    }

    protected void writeAngleOfMajorAxis(final Double angleOfMajorAxis, final JsonObject object) {
        if (angleOfMajorAxis != null) {
            object.addProperty("angle_of_major_axis", angleOfMajorAxis);
        } else {
            object.add("angle_of_major_axis", JsonNull.INSTANCE);
        }
    }

    protected void writeConfidence(final Integer confidence, final HierarchicalStreamWriter writer) {
        if (confidence != null) {
            writer.startNode("Confidence");
            writer.setValue(String.valueOf(confidence));
            writer.endNode();
        }
    }

    protected void writeConfidence(final Integer confidence, final JsonObject object) {
        if (confidence != null) {
            object.addProperty("confidence", confidence);
        } else {
            object.add("confidence", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceAltitude(final Integer altitude, final HierarchicalStreamWriter writer) {
        if (altitude != null) {
            writer.startNode("DeviceAltitude");
            writer.setValue(String.valueOf(altitude));
            writer.endNode();
        }
    }

    protected void writeDeviceAltitude(final Integer altitude, final JsonObject object) {
        if (altitude != null) {
            object.addProperty("device_altitude", altitude);
        } else {
            object.add("device_altitude", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceAltitudeUncertainty(final Double altitudeUncertainty, final HierarchicalStreamWriter writer) {
        if (altitudeUncertainty != null) {
            writer.startNode("DeviceAltitudeUncertainty");
            writer.setValue(String.valueOf(altitudeUncertainty));
            writer.endNode();
        }
    }

    protected void writeDeviceAltitudeUncertainty(final Double altitudeUncertainty, final JsonObject object) {
        if (altitudeUncertainty != null) {
            object.addProperty("device_altitude_uncertainty", altitudeUncertainty);
        } else {
            object.add("device_altitude_uncertainty", JsonNull.INSTANCE);
        }
    }

    protected void writeInnerRadius(final Integer innerRadius, final HierarchicalStreamWriter writer) {
        if (innerRadius != null) {
            writer.startNode("InnerRadius");
            writer.setValue(String.valueOf(innerRadius));
            writer.endNode();
        }
    }

    protected void writeInnerRadius(final Integer innerRadius, final JsonObject object) {
        if (innerRadius != null) {
            object.addProperty("inner_radius", innerRadius);
        } else {
            object.add("inner_radius", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertaintyInnerRadius(final Double uncertaintyInnerRadius, final HierarchicalStreamWriter writer) {
        if (uncertaintyInnerRadius != null) {
            writer.startNode("UncertaintyInnerRadius");
            writer.setValue(String.valueOf(uncertaintyInnerRadius));
            writer.endNode();
        }
    }

    protected void writeUncertaintyInnerRadius(final Double uncertaintyInnerRadius, final JsonObject object) {
        if (uncertaintyInnerRadius != null) {
            object.addProperty("uncertainty_inner_radius", uncertaintyInnerRadius);
        } else {
            object.add("uncertainty_inner_radius", JsonNull.INSTANCE);
        }
    }

    protected void writeOffsetAngle(final Double offsetAngle, final HierarchicalStreamWriter writer) {
        if (offsetAngle != null) {
            writer.startNode("OffsetAngle");
            writer.setValue(String.valueOf(offsetAngle));
            writer.endNode();
        }
    }

    protected void writeOffsetAngle(final Double offsetAngle, final JsonObject object) {
        if (offsetAngle != null) {
            object.addProperty("offset_angle", offsetAngle);
        } else {
            object.add("offset_angle", JsonNull.INSTANCE);
        }
    }

    protected void writeIncludedAngle(final Double includedAngle, final HierarchicalStreamWriter writer) {
        if (includedAngle != null) {
            writer.startNode("IncludedAngle");
            writer.setValue(String.valueOf(includedAngle));
            writer.endNode();
        }
    }

    protected void writeIncludedAngle(final Double includedAngle, final JsonObject object) {
        if (includedAngle != null) {
            object.addProperty("included_angle", includedAngle);
        } else {
            object.add("included_angle", JsonNull.INSTANCE);
        }
    }

    protected void writeHorizontalSpeed(final Integer horizontalSpeed, final HierarchicalStreamWriter writer) {
        if (horizontalSpeed != null) {
            writer.startNode("DeviceHorizontalSpeed");
            writer.setValue(String.valueOf(horizontalSpeed));
            writer.endNode();
        }
    }

    protected void writeHorizontalSpeed(final Integer horizontalSpeed, final JsonObject object) {
        if (horizontalSpeed != null) {
            object.addProperty("device_horizontal_speed", horizontalSpeed);
        } else {
            object.add("device_horizontal_speed", JsonNull.INSTANCE);
        }
    }

    protected void writeVerticalSpeed(final Integer verticalSpeed, final HierarchicalStreamWriter writer) {
        if (verticalSpeed != null) {
            writer.startNode("DeviceVerticalSpeed");
            writer.setValue(String.valueOf(verticalSpeed));
            writer.endNode();
        }
    }

    protected void writeVerticalSpeed(final Integer verticalSpeed, final JsonObject object) {
        if (verticalSpeed != null) {
            object.addProperty("device_vertical_speed", verticalSpeed);
        } else {
            object.add("device_vertical_speed", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertaintyHorizontalSpeed(final Integer uncertaintyHorizontalSpeed, final HierarchicalStreamWriter writer) {
        if (uncertaintyHorizontalSpeed != null) {
            writer.startNode("UncertaintyHorizontalSpeed");
            writer.setValue(String.valueOf(uncertaintyHorizontalSpeed));
            writer.endNode();
        }
    }

    protected void writeUncertaintyHorizontalSpeed(final Integer uncertaintyHorizontalSpeed, final JsonObject object) {
        if (uncertaintyHorizontalSpeed != null) {
            object.addProperty("uncertainty_horizontal_speed", uncertaintyHorizontalSpeed);
        } else {
            object.add("uncertainty_horizontal_speed", JsonNull.INSTANCE);
        }
    }

    protected void writeUncertaintyVerticalSpeed(final Integer uncertaintyVerticalSpeed, final HierarchicalStreamWriter writer) {
        if (uncertaintyVerticalSpeed != null) {
            writer.startNode("UncertaintyVerticalSpeed");
            writer.setValue(String.valueOf(uncertaintyVerticalSpeed));
            writer.endNode();
        }
    }

    protected void writeUncertaintyVerticalSpeed(final Integer uncertaintyVerticalSpeed, final JsonObject object) {
        if (uncertaintyVerticalSpeed != null) {
            object.addProperty("uncertainty_vertical_speed", uncertaintyVerticalSpeed);
        } else {
            object.add("uncertainty_vertical_speed", JsonNull.INSTANCE);
        }
    }

    protected void writeBearing(final Integer bearing, final HierarchicalStreamWriter writer) {
        if (bearing != null) {
            writer.startNode("Bearing");
            writer.setValue(String.valueOf(bearing));
            writer.endNode();
        }
    }

    protected void writeBearing(final Integer bearing, final JsonObject object) {
        if (bearing != null) {
            object.addProperty("bearing", bearing);
        } else {
            object.add("bearing", JsonNull.INSTANCE);
        }
    }

    protected void writePhysicalAddress(final String physicalAddress, final HierarchicalStreamWriter writer) {
        if (physicalAddress != null) {
            writer.startNode("PhysicalAddress");
            writer.setValue(physicalAddress);
            writer.endNode();
        }
    }

    protected void writePhysicalAddress(final String physicalAddress, final JsonObject object) {
        if (physicalAddress != null) {
            object.addProperty("physical_address", physicalAddress);
        } else {
            object.add("physical_address", JsonNull.INSTANCE);
        }
    }

    protected void writeInternetAddress(final String internetAddress, final HierarchicalStreamWriter writer) {
        if (internetAddress != null) {
            writer.startNode("InternetAddress");
            writer.setValue(internetAddress);
            writer.endNode();
        }
    }

    protected void writeInternetAddress(final String internetAddress, final JsonObject object) {
        if (internetAddress != null) {
            object.addProperty("internet_address", internetAddress);
        } else {
            object.add("internet_address", JsonNull.INSTANCE);
        }
    }

    protected void writeLocationTimestamp(final DateTime locationTimestamp, final HierarchicalStreamWriter writer) {
        if (locationTimestamp != null) {
            writer.startNode("LocationTimestamp");
            writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(locationTimestamp.toDate()));
            writer.endNode();
        }
    }

    protected void writeLocationTimestamp(final DateTime locationTimestamp, final JsonObject object) {
        if (locationTimestamp != null) {
            object.addProperty("location_timestamp",
                new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(locationTimestamp.toDate()));
        } else {
            object.add("location_timestamp", JsonNull.INSTANCE);
        }
    }

    protected void writeGeofenceType(final String geofenceType, final HierarchicalStreamWriter writer) {
        if (geofenceType != null) {
            writer.startNode("GeofenceType");
            writer.setValue(geofenceType);
            writer.endNode();
        }
    }

    protected void writeGeofenceType(final String geofenceType, final JsonObject object) {
        if (geofenceType != null) {
            object.addProperty("geofence_type", geofenceType);
        } else {
            object.add("geofence_type", JsonNull.INSTANCE);
        }
    }

    protected void writeGeofenceId(final String geofenceId, final HierarchicalStreamWriter writer) {
        if (geofenceId != null) {
            writer.startNode("GeofenceId");
            writer.setValue(geofenceId);
            writer.endNode();
        }
    }

    protected void writeGeofenceId(final String geofenceId, final JsonObject object) {
        if (geofenceId != null) {
            object.addProperty("geofence_id", geofenceId);
        } else {
            object.add("geofence_id", JsonNull.INSTANCE);
        }
    }

    protected void writeDeferredLocationEventType(final String geofenceEventType,
                                                  final HierarchicalStreamWriter writer) {
        if (geofenceEventType != null) {
            writer.startNode("DeferredLocationEventType");
            writer.setValue(geofenceEventType);
            writer.endNode();
        }
    }

    protected void writeDeferredLocationEventType(final String geofenceEventType, final JsonObject object) {
        if (geofenceEventType != null) {
            object.addProperty("deferred_location_event_type", geofenceEventType);
        } else {
            object.add("deferred_location_event_type", JsonNull.INSTANCE);
        }
    }

    protected void writeMotionEventRange(final Long motionEventRange, final HierarchicalStreamWriter writer) {
        if (motionEventRange != null) {
            writer.startNode("MotionEventRange");
            writer.setValue(motionEventRange.toString());
            writer.endNode();
        }
    }

    protected void writeMotionEventRange(final Long motionEventRange, final JsonObject object) {
        if (motionEventRange != null) {
            object.addProperty("motion_event_range", motionEventRange);
        } else {
            object.add("motion_event_range", JsonNull.INSTANCE);
        }
    }

    protected void writeCivicAddress(final String formattedAddress, final HierarchicalStreamWriter writer) {
        if (formattedAddress != null) {
            writer.startNode("CivicAddress");
            writer.setValue(formattedAddress);
            writer.endNode();
        }
    }

    protected void writeCivicAddress(final String formattedAddress, final JsonObject object) {
        if (formattedAddress != null) {
            object.addProperty("formatted_address", formattedAddress);
        } else {
            object.add("formatted_address", JsonNull.INSTANCE);
        }
    }

    protected void writeBarometricPressure(final Long barometricPressure, final HierarchicalStreamWriter writer) {
        if (barometricPressure != null) {
            writer.startNode("BarometricPressure");
            writer.setValue(barometricPressure.toString());
            writer.endNode();
        }
    }

    protected void writeBarometricPressure(final Long barometricPressure, final JsonObject object) {
        if (barometricPressure != null) {
            object.addProperty("barometric_pressure", barometricPressure);
        } else {
            object.add("barometric_pressure", JsonNull.INSTANCE);
        }
    }

    protected void writeRadioAccessType(final String radioAccessType, final HierarchicalStreamWriter writer) {
        if (radioAccessType != null) {
            writer.startNode("RadioAccessType");
            writer.setValue(radioAccessType);
            writer.endNode();
        }
    }

    protected void writeRadioAccessType(final String radioAccessType, final JsonObject object) {
        if (radioAccessType != null) {
            object.addProperty("radio_access_type", radioAccessType);
        } else {
            object.add("radio_access_type", JsonNull.INSTANCE);
        }
    }

    protected void writeLastGeolocationResponse(final String lastGeolocationResponse, final HierarchicalStreamWriter writer) {
        if (lastGeolocationResponse != null) {
            writer.startNode("LastGeolocationResponse");
            writer.setValue(lastGeolocationResponse);
            writer.endNode();
        }
    }

    protected void writeLastGeolocationResponse(final String lastGeolocationResponse, final JsonObject object) {
        if (lastGeolocationResponse != null) {
            object.addProperty("last_geolocation_response", lastGeolocationResponse);
        } else {
            object.add("last_geolocation_response", JsonNull.INSTANCE);
        }
    }

    protected void writeCause(final String cause, final HierarchicalStreamWriter writer) {
        if (cause != null) {
            writer.startNode("Cause");
            writer.setValue(cause);
            writer.endNode();
        }
    }

    protected void writeCause(final String cause, final JsonObject object) {
        if (cause != null) {
            object.addProperty("cause", cause);
        } else {
            object.add("cause", JsonNull.INSTANCE);
        }
    }

}


