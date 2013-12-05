/*
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
 */package org.mobicents.servlet.restcomm.telephony.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;

/**
 * Helper methods for proxying SIP messages between Restcomm clients that are connecting in peer to peer mode
 * @author ivelin.ivanov@telestax.com
 * @author jean.deruelle@telestax.com
 * @author gvagenas@telestax.com
 */
public class B2BUAHelper {

    public static final String B2BUA_LAST_REQUEST = "lastRequest";
    private static final String B2BUA_LAST_RESPONSE = "lastResponse";
    private static final String B2BUA_LINKED_SESSION = "linkedSession";
    private static final String CDR_SID = "callDetailRecord_sid";

    private static final Logger logger = Logger.getLogger(B2BUAHelper.class);

    // private static CallDetailRecord callRecord = null;
    private static DaoManager daoManager;

    /**
     * @param request
     * @param client
     * @param toClient
     * @throws IOException
     */
    public static boolean redirectToB2BUA(final SipServletRequest request, final Client client, Client toClient,
            DaoManager storage, SipFactory sipFactory) throws IOException {
        request.getSession().setAttribute("lastRequest", request);
        if (logger.isInfoEnabled()) {
            logger.info("B2BUA (p2p proxy): Got request:\n" + request.getMethod());
            logger.info(String.format("B2BUA: Proxying a session between %s and %s", client.getUri(), toClient.getUri()));
        }

        if (daoManager == null) {
            daoManager = storage;
        }

        String user = ((SipURI) request.getTo().getURI()).getUser();

        final RegistrationsDao registrations = daoManager.getRegistrationsDao();
        final Registration registration = registrations.getRegistration(user);
        if (registration != null) {
            final String location = registration.getLocation();
            SipURI to;
            try {
                to = (SipURI) sipFactory.createURI(location);

                final SipSession incomingSession = request.getSession();
                // create and send the outgoing invite and do the session linking
                incomingSession.setAttribute(B2BUA_LAST_REQUEST, request);
                SipServletRequest outRequest = sipFactory.createRequest(request.getApplicationSession(), request.getMethod(),
                        request.getFrom().getURI(), request.getTo().getURI());
                outRequest.setRequestURI(to);
                if (request.getContent() != null) {
                    outRequest.setContent(request.getContent(), request.getContentType());
                }
                final SipSession outgoingSession = outRequest.getSession();
                if (request.isInitial()) {
                    incomingSession.setAttribute(B2BUA_LINKED_SESSION, outgoingSession);
                    outgoingSession.setAttribute(B2BUA_LINKED_SESSION, incomingSession);
                }
                outgoingSession.setAttribute(B2BUA_LAST_REQUEST, outRequest);
                request.createResponse(100).send();
                outRequest.send();

                final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                builder.setSid(Sid.generate(Sid.Type.CALL));
                builder.setDateCreated(DateTime.now());
                builder.setAccountSid(client.getAccountSid());
                builder.setTo(toClient.getFriendlyName());
                builder.setCallerName(client.getFriendlyName());
                builder.setFrom(client.getFriendlyName());
                // builder.setForwardedFrom(callInfo.forwardedFrom());
                // builder.setPhoneNumberSid(phoneId);
                builder.setStatus(CallStateChanged.State.QUEUED.name());
                builder.setDirection("Client-To-Client");
                builder.setApiVersion(client.getApiVersion());
                builder.setPrice(new BigDecimal("0.00"));
                // TODO implement currency property to be read from Configuration
                builder.setPriceUnit(Currency.getInstance("USD"));
                final StringBuilder buffer = new StringBuilder();
                buffer.append("/").append(client.getApiVersion()).append("/Accounts/");
                buffer.append(client.getAccountSid().toString()).append("/Calls/");
                buffer.append(client.getSid().toString());
                final URI uri = URI.create(buffer.toString());
                builder.setUri(uri);

                CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();
                CallDetailRecord callRecord = builder.build();
                records.addCallDetailRecord(callRecord);

                incomingSession.setAttribute(CDR_SID, callRecord.getSid());
                outgoingSession.setAttribute(CDR_SID, callRecord.getSid());

                return true; // successfully proxied the SIP request between two registered clients
            } catch (ServletParseException badUriEx) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("B2BUA: Error parsing Client Contact URI: %s", location), badUriEx);
                }
            }
        }
        return false;
    }

    public static SipServletResponse getLinkedResponse(SipServletMessage message) {
        SipSession linkedB2BUASession = getLinkedSession(message);
        // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
        if (linkedB2BUASession != null) {
            SipServletResponse response = (SipServletResponse) linkedB2BUASession.getAttribute(B2BUA_LAST_RESPONSE);
            return response;
        }
        return null;
    }

    public static SipServletRequest getLinkedRequest(SipServletMessage message) {
        SipSession linkedB2BUASession = getLinkedSession(message);
        if (linkedB2BUASession != null) {
            SipServletRequest linkedRequest = (SipServletRequest) linkedB2BUASession.getAttribute(B2BUA_LAST_REQUEST);
            return linkedRequest;
        }
        return null;
    }

    public static SipSession getLinkedSession(SipServletMessage message) {
        SipSession sipSession = (SipSession) message.getSession().getAttribute(B2BUA_LINKED_SESSION);
        if (sipSession == null) {
            logger.info("SIP SESSION is NULL");
        }
        return sipSession;
    }

    /**
     * @param response
     * @throws IOException
     */
    public static void forwardResponse(final SipServletResponse response) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("B2BUA: Got response: \n %s", response));
        }
        CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();

        // container handles CANCEL related responses no need to forward them
        if (response.getStatus() == 487 || (response.getStatus() == 200 && response.getMethod().equalsIgnoreCase("CANCEL"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("response to CANCEL not forwarding");
            }
            // Update CallDetailRecord
            SipServletRequest request = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);
            CallDetailRecord callRecord = records.getCallDetailRecord((Sid) request.getSession().getAttribute(CDR_SID));

            if (callRecord != null) {
                logger.info("CDR found! Updating");
                callRecord = callRecord.setStatus(CallStateChanged.State.CANCELED.name());
                final DateTime now = DateTime.now();
                callRecord = callRecord.setEndTime(now);
                final int seconds = (int) (DateTime.now().getMillis() - callRecord.getStartTime().getMillis()) / 1000;
                callRecord = callRecord.setDuration(seconds);
                records.updateCallDetailRecord(callRecord);
            }
            return;
        }
        // forward the response
        response.getSession().setAttribute(B2BUA_LAST_RESPONSE, response);
        SipServletRequest request = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);
        SipServletResponse resp = request.createResponse(response.getStatus());
        if (response.getContent() != null) {
            resp.setContent(response.getContent(), response.getContentType());
        }
        resp.send();

        CallDetailRecord callRecord = records.getCallDetailRecord((Sid) request.getSession().getAttribute(CDR_SID));
        if (callRecord != null) {
            logger.info("CDR found! Updating");
            if (!request.getMethod().equalsIgnoreCase("BYE")) {
                if (response.getStatus() == 100 || response.getStatus() == 180) {
                    callRecord = callRecord.setStatus(CallStateChanged.State.RINGING.name());
                } else if (response.getStatus() == 200 || response.getStatus() == 202) {
                    callRecord = callRecord.setStatus(CallStateChanged.State.IN_PROGRESS.name());
                    callRecord = callRecord.setAnsweredBy(((SipURI) response.getTo().getURI()).getUser());
                    final DateTime now = DateTime.now();
                    callRecord = callRecord.setStartTime(now);

                } else if (response.getStatus() == 486 || response.getStatus() == 600) {
                    callRecord = callRecord.setStatus(CallStateChanged.State.BUSY.name());
                } else if (response.getStatus() > 400) {
                    callRecord = callRecord.setStatus(CallStateChanged.State.FAILED.name());
                }
            } else {
                callRecord = callRecord.setStatus(CallStateChanged.State.COMPLETED.name());
                final DateTime now = DateTime.now();
                callRecord = callRecord.setEndTime(now);
                final int seconds = (int) ((DateTime.now().getMillis() - callRecord.getStartTime().getMillis()) / 1000);
                callRecord = callRecord.setDuration(seconds);
            }

            records.updateCallDetailRecord(callRecord);
        }

    }

    /**
     * Check whether a SIP request or response belongs to a peer to peer (B2BUA) session
     * @param sipMessage
     * @return
     */
    public static boolean isB2BUASession(SipServletMessage sipMessage) {
        SipSession linkedB2BUASession = getLinkedSession(sipMessage);
        return (linkedB2BUASession != null);
    }

}
