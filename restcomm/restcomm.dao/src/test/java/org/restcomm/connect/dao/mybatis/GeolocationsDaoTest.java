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

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;

import org.restcomm.connect.dao.GeolocationDao;
import org.restcomm.connect.dao.entities.Geolocation;
import org.restcomm.connect.dao.entities.Geolocation.GeolocationType;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationsDaoTest {

    private static MybatisDaoManager manager;

    public GeolocationsDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void geolocationCreateReadUpdateDelete() {

        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setLocationTimestamp(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource("mlpclient1");
        builder.setDeviceIdentifier("device1");
        builder.setMsisdn(Long.valueOf("59899077937"));
        builder.setImsi(Long.valueOf("748026871012345"));
        builder.setImei("01171400466105");
        builder.setLmsi(Long.valueOf("2915"));
        builder.setReferenceNumber((long) 5471);
        builder.setGeolocationType(GeolocationType.Notification);
        builder.setResponseStatus("successfull");
        builder.setCause("NA");
        builder.setMobileCountryCode(748);
        builder.setMobileNetworkCode(3);
        builder.setLocationAreaCode(10978);
        builder.setCellId(12345);
        builder.setServiceAreaCode(12718);
        builder.setEnodebId(334);
        builder.setTrackingAreaCode(13295);
        builder.setRoutingAreaCode(13952);
        builder.setLocationNumberAddress(819203961904L);
        builder.setNetworkEntityAddress((long) 59848779);
        builder.setNetworkEntityName("msc59801");
        builder.setAgeOfLocationInfo(0);
        builder.setNetworkEntityName("MME74801");
        builder.setSubscriberState("assumedIdle");
        builder.setNotReachableReason("imsiDetached");
        builder.setTypeOfShape("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid");
        builder.setDeviceLatitude("34.78999972343445");
        builder.setDeviceLongitude("-124.90998029708862");
        builder.setUncertainty(0.00);
        builder.setUncertaintySemiMajorAxis(2.32);
        builder.setUncertaintySemiMinorAxis(1.67);
        builder.setAngleOfMajorAxis(23.1);
        builder.setConfidence(90);
        builder.setAltitude(250);
        builder.setUncertaintyAltitude(20.0);
        builder.setInnerRadius(40);
        builder.setUncertaintyInnerRadius(1.02);
        builder.setOffsetAngle(10.0);
        builder.setIncludedAngle(1.2);
        builder.setHorizontalSpeed(30);
        builder.setVerticalSpeed(0);
        builder.setUncertaintyHorizontalSpeed(2);
        builder.setUncertaintyVerticalSpeed(0);
        builder.setBearing(1);
        builder.setDeferredLocationEventType("locationAreaId");
        builder.setGeofenceType("inside");
        builder.setGeofenceId("172");
        builder.setMotionEventRange((long) 500);
        builder.setCivicAddress("Avenida Brasil 2681, 11500, Montevideo, Uruguay");
        builder.setBarometricPressure((long) 101.325);
        builder.setInternetAddress("2001:0:9d38:6ab8:30a5:1c9d:58c6:5898");
        builder.setPhysicalAddress("D8-97-BA-19-02-D8");
        builder.setLastGeolocationResponse("true");
        builder.setCause("NA");
        builder.setApiVersion("2012-04-24");
        builder.setUri(url);
        Geolocation geolocation = builder.build();
        final GeolocationDao geolocations = manager.getGeolocationDao();

        // Create a new Geolocation in the data store.
        geolocations.addGeolocation(geolocation);

        // Read the Geolocation from the data store.
        Geolocation result = geolocations.getGeolocation(sid);

        // Validate the results.
        assertTrue(result.getSid().equals(geolocation.getSid()));
        assertTrue(result.getAccountSid().equals(geolocation.getAccountSid()));
        assertTrue(result.getDateUpdated().equals(geolocation.getDateUpdated()));
        assertTrue(result.getLocationTimestamp().equals(geolocation.getLocationTimestamp()));
        assertTrue(result.getSource().equals(geolocation.getSource()));
        assertTrue(result.getDeviceIdentifier().equals(geolocation.getDeviceIdentifier()));
        assertTrue(result.getMsisdn().equals(geolocation.getMsisdn()));
        assertTrue(result.getImsi().equals(geolocation.getImsi()));
        assertTrue(result.getImei().equals(geolocation.getImei()));
        assertTrue(result.getLmsi().equals(geolocation.getLmsi()));
        assertTrue(result.getReferenceNumber().equals(geolocation.getReferenceNumber()));
        assertTrue(result.getGeolocationType().equals(geolocation.getGeolocationType()));
        assertTrue(result.getResponseStatus().equals(geolocation.getResponseStatus()));
        assertTrue(result.getCause() == geolocation.getCause());
        assertTrue(result.getMobileCountryCode().equals(geolocation.getMobileCountryCode()));
        assertTrue(result.getMobileNetworkCode().equals(geolocation.getMobileNetworkCode()));
        assertTrue(result.getLocationAreaCode().equals(geolocation.getLocationAreaCode()));
        assertTrue(result.getCellId().equals(geolocation.getCellId()));
        assertTrue(result.getServiceAreaCode().equals(geolocation.getServiceAreaCode()));
        assertTrue(result.getEnodebId().equals(geolocation.getEnodebId()));
        assertTrue(result.getTrackingAreaCode().equals(geolocation.getTrackingAreaCode()));
        assertTrue(result.getRoutingAreaCode().equals(geolocation.getRoutingAreaCode()));
        assertTrue(result.getLocationNumberAddress().equals(geolocation.getLocationNumberAddress()));
        assertTrue(result.getNetworkEntityAddress().equals(geolocation.getNetworkEntityAddress()));
        assertTrue(result.getNetworkEntityName().equals(geolocation.getNetworkEntityName()));
        assertTrue(result.getAgeOfLocationInfo().equals(geolocation.getAgeOfLocationInfo()));
        assertTrue(result.getSubscriberState().equals(geolocation.getSubscriberState()));
        assertTrue(result.getNotReachableReason().equals(geolocation.getNotReachableReason()));
        assertTrue(result.getTypeOfShape().equals(geolocation.getTypeOfShape()));
        assertTrue(result.getDeviceLatitude().equals(geolocation.getDeviceLatitude()));
        assertTrue(result.getDeviceLongitude().equals(geolocation.getDeviceLongitude()));
        assertTrue(result.getUncertainty().equals(geolocation.getUncertainty()));
        assertTrue(result.getUncertaintySemiMajorAxis().equals(geolocation.getUncertaintySemiMajorAxis()));
        assertTrue(result.getUncertaintySemiMinorAxis().equals(geolocation.getUncertaintySemiMinorAxis()));
        assertTrue(result.getAngleOfMajorAxis().equals(geolocation.getAngleOfMajorAxis()));
        assertTrue(result.getConfidence().equals(geolocation.getConfidence()));
        assertTrue(result.getAltitude().equals(geolocation.getAltitude()));
        assertTrue(result.getUncertaintyAltitude().equals(geolocation.getUncertaintyAltitude()));
        assertTrue(result.getInnerRadius().equals(geolocation.getInnerRadius()));
        assertTrue(result.getUncertaintyInnerRadius().equals(geolocation.getUncertaintyInnerRadius()));
        assertTrue(result.getOffsetAngle().equals(geolocation.getOffsetAngle()));
        assertTrue(result.getIncludedAngle().equals(geolocation.getIncludedAngle()));
        assertTrue(result.getHorizontalSpeed().equals(geolocation.getHorizontalSpeed()));
        assertTrue(result.getVerticalSpeed().equals(geolocation.getVerticalSpeed()));
        assertTrue(result.getUncertaintyHorizontalSpeed().equals(geolocation.getUncertaintyHorizontalSpeed()));
        assertTrue(result.getUncertaintyVerticalSpeed().equals(geolocation.getUncertaintyVerticalSpeed()));
        assertTrue(result.getBearing().equals(geolocation.getBearing()));
        assertTrue(result.getDeferredLocationEventType().equals(geolocation.getDeferredLocationEventType()));
        assertTrue(result.getGeofenceType().equals(geolocation.getGeofenceType()));
        assertTrue(result.getGeofenceId().equals(geolocation.getGeofenceId()));
        assertTrue(result.getMotionEventRange().equals(geolocation.getMotionEventRange()));
        assertTrue(result.getCivicAddress().equals(geolocation.getCivicAddress()));
        assertTrue(result.getBarometricPressure().equals(geolocation.getBarometricPressure()));
        assertTrue(result.getInternetAddress().equals(geolocation.getInternetAddress()));
        assertTrue(result.getPhysicalAddress().equals(geolocation.getPhysicalAddress()));
        assertTrue(result.getLastGeolocationResponse().equals(geolocation.getLastGeolocationResponse()));
        assertTrue(result.getApiVersion().equals(geolocation.getApiVersion()));
        assertTrue(result.getUri().equals(geolocation.getUri()));

        // Update the Geolocation
        // deviceIdentifier, statusCallback and geolocationType can not be updated once created
        geolocation = geolocation.setDateUpdated(currentDateTime);
        geolocation = geolocation.setSource("ble001");
        geolocation = geolocation.setMsisdn(Long.valueOf("59899077939"));
        geolocation = geolocation.setImsi(Long.valueOf("748026871012347"));
        geolocation = geolocation.setImei("01171400466101");
        geolocation = geolocation.setLmsi(Long.valueOf("2918"));
        geolocation = geolocation.setResponseStatus("failed");
        geolocation = geolocation.setCause("API not compliant");
        geolocation = geolocation.setMobileCountryCode(1);
        geolocation = geolocation.setMobileNetworkCode(33);
        geolocation = geolocation.setLocationAreaCode(10101);
        geolocation = geolocation.setCellId(20210);
        geolocation = geolocation.setServiceAreaCode(13971);
        geolocation = geolocation.setEnodebId(30021);
        geolocation = geolocation.setTrackingAreaCode(13291);
        geolocation = geolocation.setRoutingAreaCode(32950);
        builder.setLocationNumberAddress(819203961903L);
        geolocation = geolocation.setAgeOfLocationInfo(1);
        geolocation = geolocation.setNetworkEntityAddress((long) 59848778);
        geolocation = geolocation.setNetworkEntityName("msc59801");
        geolocation = geolocation.setSubscriberState("camelBusy");
        geolocation = geolocation.setTypeOfShape("EllipsoidPointWithAltitudeAndUncertaintyEllipsoid");
        geolocation = geolocation.setDeviceLatitude("-1.638643");
        geolocation = geolocation.setDeviceLongitude("49.4394389");
        geolocation = geolocation.setUncertainty(   .00);
        geolocation = geolocation.setUncertaintySemiMajorAxis(35.949729);
        geolocation = geolocation.setUncertaintySemiMinorAxis(18.531167);
        geolocation = geolocation.setAngleOfMajorAxis(30.0);
        geolocation = geolocation.setConfidence(10);
        geolocation = geolocation.setAltitude(1500);
        geolocation = geolocation.setUncertaintyAltitude(487.851812);
        geolocation = geolocation.setInnerRadius(3);
        geolocation = geolocation.setUncertaintyInnerRadius(0.20);
        geolocation = geolocation.setOffsetAngle(1.00);
        geolocation = geolocation.setIncludedAngle(0.91);
        geolocation = geolocation.setHorizontalSpeed(51);
        geolocation = geolocation.setVerticalSpeed(1);
        geolocation = geolocation.setUncertaintyHorizontalSpeed(0);
        geolocation = geolocation.setUncertaintyVerticalSpeed(0);
        geolocation = geolocation.setBearing(2);
        geolocation = geolocation.setDeferredLocationEventType("cellGlobalId");
        geolocation = geolocation.setGeofenceType("leaving");
        geolocation = geolocation.setGeofenceId("55218");
        geolocation = geolocation.setMotionEventRange((long) 100);
        geolocation = geolocation.setCivicAddress("Avenida Brasil 2681, 11500, Montevideo, Uruguay");
        geolocation = geolocation.setBarometricPressure((long) 101.325);
        geolocation = geolocation.setHorizontalSpeed(120);
        geolocation = geolocation.setVerticalSpeed(10);
        geolocation = geolocation.setUncertaintyHorizontalSpeed(5);
        geolocation = geolocation.setUncertaintyVerticalSpeed(1);
        geolocation = geolocation.setInternetAddress("200.0.91.253");
        geolocation = geolocation.setPhysicalAddress("A1-DD-0A-27-92-00");
        geolocation = geolocation.setLastGeolocationResponse("false");

        // Update the Geolocation in the data store g
        geolocations.updateGeolocation(geolocation);

        // Read the updated Geolocation from the data store
        result = geolocations.getGeolocation(sid);

        // Validate the results
        assertTrue(result.getSid().equals(geolocation.getSid()));
        assertTrue(result.getAccountSid().equals(geolocation.getAccountSid()));
        assertTrue(result.getDateUpdated().equals(geolocation.getDateUpdated()));
        assertTrue(result.getLocationTimestamp().equals(geolocation.getLocationTimestamp()));
        assertTrue(result.getSource().equals(geolocation.getSource()));
        assertTrue(result.getDeviceIdentifier().equals(geolocation.getDeviceIdentifier()));
        assertTrue(result.getMsisdn().equals(geolocation.getMsisdn()));
        assertTrue(result.getImsi().equals(geolocation.getImsi()));
        assertTrue(result.getImei().equals(geolocation.getImei()));
        assertTrue(result.getLmsi().equals(geolocation.getLmsi()));
        assertTrue(result.getReferenceNumber().equals(geolocation.getReferenceNumber()));
        assertTrue(result.getGeolocationType().equals(geolocation.getGeolocationType()));
        assertTrue(result.getResponseStatus().equals(geolocation.getResponseStatus()));
        assertTrue(result.getCause() == geolocation.getCause());
        assertTrue(result.getMobileCountryCode().equals(geolocation.getMobileCountryCode()));
        assertTrue(result.getMobileNetworkCode().equals(geolocation.getMobileNetworkCode()));
        assertTrue(result.getLocationAreaCode().equals(geolocation.getLocationAreaCode()));
        assertTrue(result.getCellId().equals(geolocation.getCellId()));
        assertTrue(result.getServiceAreaCode().equals(geolocation.getServiceAreaCode()));
        assertTrue(result.getEnodebId().equals(geolocation.getEnodebId()));
        assertTrue(result.getTrackingAreaCode().equals(geolocation.getTrackingAreaCode()));
        assertTrue(result.getRoutingAreaCode().equals(geolocation.getRoutingAreaCode()));
        assertTrue(result.getLocationNumberAddress().equals(geolocation.getLocationNumberAddress()));
        assertTrue(result.getNetworkEntityAddress().equals(geolocation.getNetworkEntityAddress()));
        assertTrue(result.getNetworkEntityName().equals(geolocation.getNetworkEntityName()));
        assertTrue(result.getAgeOfLocationInfo().equals(geolocation.getAgeOfLocationInfo()));
        assertTrue(result.getSubscriberState().equals(geolocation.getSubscriberState()));
        assertTrue(result.getNotReachableReason().equals(geolocation.getNotReachableReason()));
        assertTrue(result.getTypeOfShape().equals(geolocation.getTypeOfShape()));
        assertTrue(result.getDeviceLatitude().equals(geolocation.getDeviceLatitude()));
        assertTrue(result.getDeviceLongitude().equals(geolocation.getDeviceLongitude()));
        assertTrue(result.getUncertainty().equals(geolocation.getUncertainty()));
        assertTrue(result.getUncertaintySemiMajorAxis().equals(geolocation.getUncertaintySemiMajorAxis()));
        assertTrue(result.getUncertaintySemiMinorAxis().equals(geolocation.getUncertaintySemiMinorAxis()));
        assertTrue(result.getAngleOfMajorAxis().equals(geolocation.getAngleOfMajorAxis()));
        assertTrue(result.getConfidence().equals(geolocation.getConfidence()));
        assertTrue(result.getAltitude().equals(geolocation.getAltitude()));
        assertTrue(result.getUncertaintyAltitude().equals(geolocation.getUncertaintyAltitude()));
        assertTrue(result.getInnerRadius().equals(geolocation.getInnerRadius()));
        assertTrue(result.getUncertaintyInnerRadius().equals(geolocation.getUncertaintyInnerRadius()));
        assertTrue(result.getOffsetAngle().equals(geolocation.getOffsetAngle()));
        assertTrue(result.getIncludedAngle().equals(geolocation.getIncludedAngle()));
        assertTrue(result.getHorizontalSpeed().equals(geolocation.getHorizontalSpeed()));
        assertTrue(result.getVerticalSpeed().equals(geolocation.getVerticalSpeed()));
        assertTrue(result.getUncertaintyHorizontalSpeed().equals(geolocation.getUncertaintyHorizontalSpeed()));
        assertTrue(result.getUncertaintyVerticalSpeed().equals(geolocation.getUncertaintyVerticalSpeed()));
        assertTrue(result.getBearing().equals(geolocation.getBearing()));
        assertTrue(result.getDeferredLocationEventType().equals(geolocation.getDeferredLocationEventType()));
        assertTrue(result.getGeofenceType().equals(geolocation.getGeofenceType()));
        assertTrue(result.getGeofenceId().equals(geolocation.getGeofenceId()));
        assertTrue(result.getMotionEventRange().equals(geolocation.getMotionEventRange()));
        assertTrue(result.getCivicAddress().equals(geolocation.getCivicAddress()));
        assertTrue(result.getBarometricPressure().equals(geolocation.getBarometricPressure()));
        assertTrue(result.getInternetAddress().equals(geolocation.getInternetAddress()));
        assertTrue(result.getPhysicalAddress().equals(geolocation.getPhysicalAddress()));
        assertTrue(result.getLastGeolocationResponse().equals(geolocation.getLastGeolocationResponse()));
        assertTrue(result.getApiVersion().equals(geolocation.getApiVersion()));
        assertTrue(result.getUri().equals(geolocation.getUri()));

        // Delete the Geolocation record
        geolocations.removeGeolocation(sid);

        // Validate the Geolocation record was removed.
        assertTrue(geolocations.getGeolocation(sid) == null);
    }

    @Test
    public void geolocationReadDeleteByAccountSid() {

        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
        URI url = URI.create("geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource("mlpclient1");
        builder.setDeviceIdentifier("device1");
        builder.setMsisdn(Long.valueOf("59899077937"));
        builder.setImsi(Long.valueOf("748026871012345"));
        builder.setImei("01171400466105");
        builder.setLmsi(Long.valueOf("2915"));
        builder.setReferenceNumber((long) 5471);
        builder.setGeolocationType(GeolocationType.Immediate);
        builder.setResponseStatus("successfull");
        builder.setCause("NA");
        builder.setMobileCountryCode(748);
        builder.setMobileNetworkCode(3);
        builder.setLocationAreaCode(30978);
        builder.setCellId(12345);
        builder.setServiceAreaCode(2718);
        builder.setEnodebId(21710);
        builder.setTrackingAreaCode(13295);
        builder.setRoutingAreaCode(13292);
        builder.setLocationNumberAddress(819203961904L);
        builder.setNetworkEntityAddress((long) 59848779);
        builder.setNetworkEntityName("msc59801");
        builder.setAgeOfLocationInfo(0);
        builder.setSubscriberState("camelBusy");
        builder.setNotReachableReason("msPurged");
        builder.setTypeOfShape("EllipsoidArc");
        builder.setDeviceLatitude("34.78999972343445");
        builder.setDeviceLongitude("-124.90998029708862");
        builder.setUncertainty(0.10);
        builder.setUncertaintySemiMajorAxis(0.00);
        builder.setUncertaintySemiMinorAxis(0.00);
        builder.setAngleOfMajorAxis(0.00);
        builder.setConfidence(40);
        builder.setAltitude(0);
        builder.setUncertaintyAltitude(0.2);
        builder.setInnerRadius(5);
        builder.setUncertaintyInnerRadius(1.641001);
        builder.setOffsetAngle(10.0);
        builder.setIncludedAngle(1.23);
        builder.setHorizontalSpeed(30);
        builder.setVerticalSpeed(0);
        builder.setUncertaintyHorizontalSpeed(2);
        builder.setUncertaintyVerticalSpeed(0);
        builder.setBearing(1);
        builder.setDeferredLocationEventType("locationAreaId");
        builder.setGeofenceType("inside");
        builder.setGeofenceId("172");
        builder.setMotionEventRange((long) 500);
        builder.setCivicAddress("Avenida Brasil 2681, 11500, Montevideo, Uruguay");
        builder.setBarometricPressure((long) 101.325);
        builder.setInternetAddress("2001:0:9d38:6ab8:30a5:1c9d:58c6:5898");
        builder.setPhysicalAddress("D8-97-BA-19-02-D8");
        builder.setLastGeolocationResponse("true");
        builder.setApiVersion("2012-04-24");
        builder.setUri(url);
        final Geolocation geolocation = builder.build();
        final GeolocationDao geolocations = manager.getGeolocationDao();

        // Create a new Geolocation in the data store.
        geolocations.addGeolocation(geolocation);

        // Get all the Geolocations for a specific account.
        assertTrue(geolocations.getGeolocations(accountSid) != null);

        // Remove the Geolocations for a specific account.
        geolocations.removeGeolocation(accountSid);

        // Validate that the Geolocation were removed.
        assertTrue(geolocations.getGeolocation(accountSid) == null);
    }

}
