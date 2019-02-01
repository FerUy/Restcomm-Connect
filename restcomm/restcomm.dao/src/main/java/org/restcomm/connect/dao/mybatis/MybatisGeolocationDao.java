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

package org.restcomm.connect.dao.mybatis;

import static org.restcomm.connect.dao.DaoUtils.readDateTime;
import static org.restcomm.connect.dao.DaoUtils.readDouble;
import static org.restcomm.connect.dao.DaoUtils.readInteger;
import static org.restcomm.connect.dao.DaoUtils.readSid;
import static org.restcomm.connect.dao.DaoUtils.readString;
import static org.restcomm.connect.dao.DaoUtils.readUri;
import static org.restcomm.connect.dao.DaoUtils.writeDateTime;
import static org.restcomm.connect.dao.DaoUtils.writeSid;
import static org.restcomm.connect.dao.DaoUtils.writeUri;
import static org.restcomm.connect.dao.DaoUtils.readGeolocationType;
import static org.restcomm.connect.dao.DaoUtils.readLong;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.restcomm.connect.dao.GeolocationDao;
import org.restcomm.connect.dao.entities.Geolocation;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class MybatisGeolocationDao implements GeolocationDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.GeolocationDao.";
    private final SqlSessionFactory sessions;

    public MybatisGeolocationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addGeolocation(Geolocation gl) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addGeolocation", toMap(gl));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Geolocation getGeolocation(Sid sid) {
        return getGeolocation(namespace + "getGeolocation", sid.toString());
    }

    private Geolocation getGeolocation(final String selector, final String parameter) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameter);
            if (result != null) {
                return toGeolocation(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Geolocation> getGeolocations(Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getGeolocations", accountSid.toString());
            final List<Geolocation> geolocations = new ArrayList<Geolocation>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    geolocations.add(toGeolocation(result));
                }
            }
            return geolocations;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeGeolocation(Sid sid) {
        removeGeolocations(namespace + "removeGeolocation", sid);
    }

    @Override
    public void removeGeolocations(final Sid accountSid) {
        removeGeolocations(namespace + "removeGeolocations", accountSid);
    }

    private void removeGeolocations(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateGeolocation(Geolocation gl) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateGeolocation", toMap(gl));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(Geolocation gl) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(gl.getSid()));
        map.put("date_created", writeDateTime(gl.getDateCreated()));
        map.put("date_updated", writeDateTime(gl.getDateUpdated()));
        map.put("date_executed", writeDateTime(gl.getDateExecuted()));
        map.put("location_timestamp", writeDateTime(gl.getLocationTimestamp()));
        map.put("account_sid", writeSid(gl.getAccountSid()));
        map.put("source", gl.getSource());
        map.put("device_identifier", gl.getDeviceIdentifier());
        map.put("msisdn", gl.getMsisdn());
        map.put("imsi", gl.getImsi());
        map.put("imei", gl.getImei());
        map.put("lmsi", gl.getLmsi());
        map.put("reference_number", gl.getReferenceNumber());
        map.put("geolocation_type", gl.getGeolocationType());
        map.put("response_status", gl.getResponseStatus());
        map.put("mobile_country_code", gl.getMobileCountryCode());
        map.put("mobile_network_code", gl.getMobileNetworkCode());
        map.put("location_area_code", gl.getLocationAreaCode());
        map.put("ci", gl.getCi());
        map.put("sac", gl.getSac());
        map.put("enbid", gl.getEnbid());
        map.put("network_entity_address", gl.getNetworkEntityAddress());
        map.put("network_entity_name", gl.getNetworkEntityName());
        map.put("age_of_location_info", gl.getAgeOfLocationInfo());
        map.put("subscriber_state", gl.getSubscriberState());
        map.put("not_reachable_reason", gl.getNotReachableReason());
        map.put("tac", gl.getTac());
        map.put("rac", gl.getRac());
        map.put("type_of_shape", gl.getTypeOfShape());
        map.put("device_latitude", gl.getDeviceLatitude());
        map.put("device_longitude", gl.getDeviceLongitude());
        map.put("uncertainty", gl.getUncertainty());
        map.put("uncertainty_semi_major_axis", gl.getUncertaintySemiMajorAxis());
        map.put("uncertainty_semi_minor_axis", gl.getUncertaintySemiMinorAxis());
        map.put("angle_of_major_axis", gl.getAngleOfMajorAxis());
        map.put("confidence", gl.getConfidence());
        map.put("altitude", gl.getAltitude());
        map.put("uncertainty_altitude", gl.getUncertaintyAltitude());
        map.put("inner_radius", gl.getInnerRadius());
        map.put("uncertainty_inner_radius", gl.getUncertaintyInnerRadius());
        map.put("offset_angle", gl.getOffsetAngle());
        map.put("included_angle", gl.getIncludedAngle());
        map.put("horizontal_speed", gl.getHorizontalSpeed());
        map.put("vertical_speed", gl.getVerticalSpeed());
        map.put("uncertainty_horizontal_speed", gl.getUncertaintyHorizontalSpeed());
        map.put("uncertainty_vertical_speed", gl.getUncertaintyVerticalSpeed());
        map.put("bearing", gl.getBearing());
        map.put("geofence_type", gl.getGeofenceType());
        map.put("geofence_id", gl.getGeofenceId());
        map.put("geofence_event_type", gl.getGeofenceEventType());
        map.put("event_range", gl.getEventRange());
        map.put("civic_address", gl.getCivicAddress());
        map.put("barometric_pressure", gl.getBarometricPressure());
        map.put("physical_address", gl.getPhysicalAddress());
        map.put("internet_address", gl.getInternetAddress());
        map.put("last_geolocation_response", gl.getLastGeolocationResponse());
        map.put("cause", gl.getCause());
        map.put("api_version", gl.getApiVersion());
        map.put("uri", writeUri(gl.getUri()));
        return map;
    }

    private Geolocation toGeolocation(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime date_created = readDateTime(map.get("date_created"));
        final DateTime date_updated = readDateTime(map.get("date_updated"));
        final DateTime date_executed = readDateTime(map.get("date_executed"));
        final DateTime location_timestamp = readDateTime(map.get("location_timestamp"));
        final Sid account_sid = readSid(map.get("account_sid"));
        final String source = readString(map.get("source"));
        final String device_identifier = readString(map.get("device_identifier"));
        final Long msisdn = readLong(map.get("msisdn"));
        final Long imsi = readLong(map.get("imsi"));
        final Long imei = readLong(map.get("imei"));
        final Long lmsi = readLong(map.get("lmsi"));
        final Long reference_number = readLong(map.get("reference_number"));
        final Geolocation.GeolocationType geolocation_type = readGeolocationType(map.get("geolocation_type"));
        final String response_status = readString(map.get("response_status"));
        final Integer mobile_country_code = readInteger(map.get("mobile_country_code"));
        final Integer mobile_network_code = readInteger(map.get("mobile_network_code"));
        final Integer location_area_code = readInteger(map.get("location_area_code"));
        final Integer ci = readInteger(map.get("ci"));
        final Integer sac = readInteger(map.get("sac"));
        final Integer enbid = readInteger(map.get("enbid"));
        final Long network_entity_address = readLong(map.get("network_entity_address"));
        final String network_entity_name = readString(map.get("network_entity_name"));
        final Integer age_of_location_info = readInteger(map.get("age_of_location_info"));
        final String subscriber_state = readString(map.get("subscriber_state"));
        final String not_reachable_reason = readString(map.get("not_reachable_reason"));
        final Integer tac = readInteger(map.get("tracking_area_code"));
        final Integer rac = readInteger(map.get("routing_area_code"));
        final String type_of_shape = readString(map.get("type_of_shape"));
        final String device_latitude = readString(map.get("device_latitude"));
        final String device_longitude = readString(map.get("device_longitude"));
        final Double uncertainty = readDouble(map.get("uncertainty"));
        final Double uncertainty_semi_major_axis = readDouble(map.get("uncertainty_semi_major_axis"));
        final Double uncertainty_semi_minor_axis = readDouble(map.get("uncertainty_semi_minor_axis"));
        final Double angle_of_major_axis = readDouble(map.get("angle_of_major_axis"));
        final Integer confidence = readInteger(map.get("confidence"));
        final Integer altitude = readInteger(map.get("altitude"));
        final Double uncertainty_altitude = readDouble(map.get("uncertainty_altitude"));
        final Integer inner_radius = readInteger(map.get("inner_radius"));
        final Double uncertainty_inner_radius = readDouble(map.get("uncertainty_inner_radius"));
        final Double offset_angle = readDouble(map.get("offset_angle"));
        final Double included_angle = readDouble(map.get("included_angle"));
        final Integer horizontal_speed = readInteger(map.get("horizontal_speed"));
        final Integer vertical_speed = readInteger(map.get("vertical_speed"));
        final Integer uncertainty_horizontal_speed = readInteger(map.get("uncertainty_horizontal_speed"));
        final Integer uncertainty_vertical_speed = readInteger(map.get("uncertainty_vertical_speed"));
        final Integer bearing = readInteger(map.get("bearing"));
        final String geofence_type = readString(map.get("geofence_type"));
        final String geofence_id = readString(map.get("geofence_id"));
        final String geofence_event_type = readString(map.get("geofence_event_type"));
        final Long event_range = readLong(map.get("event_range"));
        final String civic_address = readString(map.get("civic_address"));
        final Long barometric_pressure = readLong(map.get("barometric_pressure"));
        final String physical_address = readString(map.get("physical_address"));
        final String internet_address = readString(map.get("internet_address"));
        final String last_geolocation_response = readString(map.get("last_geolocation_response"));
        final String cause = readString(map.get("cause"));
        final String api_version = readString(map.get("api_version"));
        final URI uri = readUri(map.get("uri"));
        return new Geolocation(sid, date_created, date_updated, date_executed, location_timestamp, account_sid, source, device_identifier, msisdn, imsi,
            imei, lmsi, reference_number, geolocation_type, response_status, mobile_country_code, mobile_network_code, location_area_code, ci, sac, enbid,
            network_entity_address, network_entity_name, age_of_location_info, subscriber_state, not_reachable_reason, tac, rac, type_of_shape, device_latitude, device_longitude,
            uncertainty, uncertainty_semi_major_axis, uncertainty_semi_minor_axis, angle_of_major_axis, confidence, altitude, uncertainty_altitude,
            inner_radius, uncertainty_inner_radius, offset_angle, included_angle, horizontal_speed, vertical_speed, uncertainty_horizontal_speed,
            uncertainty_vertical_speed, bearing, geofence_type, geofence_id, geofence_event_type, event_range, civic_address, barometric_pressure,
            physical_address, internet_address, last_geolocation_response, cause, api_version, uri);
    }

}
