/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *
 * To the extent possible under law, Kevin Locke has waived all copyright and
 * related or neighboring rights to this work.
 * <p/>
 * A legal description of this waiver is available in <a href="https://gist.github.com/kevinoid/3829665">LICENSE.txt</a>
 */

package com.ning.http.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.kerberos.KerberosPrincipal;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Uses the internal HostnameChecker to verify the server's hostname matches with the
 * certificate.  This is a requirement for HTTPS, but the raw SSLEngine does not have
 * this functionality.  As such, it has to be added in manually.  For a more complete
 * description of hostname verification and why it's important,
 * please read
 * <a href="http://tersesystems.com/2014/03/23/fixing-hostname-verification/">Fixing
 * Hostname Verification</a>.
 * <p/>
 * This code is based on Kevin Locke's <a href="http://kevinlocke.name/bits/2012/10/03/ssl-certificate-verification-in-dispatch-and-asynchttpclient/">guide</a> .
 * <p/>
 */
public class DefaultHostnameVerifier implements HostnameVerifier {

    private HostnameChecker checker;

    private HostnameVerifier extraHostnameVerifier;

    // Logger to log exceptions.
    private static final Logger log = LoggerFactory.getLogger(DefaultHostnameVerifier.class.getName());

    /**
     * A hostname verifier that uses the {{sun.security.util.HostnameChecker}} under the hood.
     */
    public DefaultHostnameVerifier() {
        this.checker = new ProxyHostnameChecker();
    }

    /**
     * A hostname verifier that takes an external hostname checker.  Useful for testing.
     *
     * @param checker a hostnamechecker.
     */
    public DefaultHostnameVerifier(HostnameChecker checker) {
        this.checker = checker;
    }

    /**
     * A hostname verifier that falls back to another hostname verifier if not found.
     *
     * @param extraHostnameVerifier another hostname verifier.
     */
    public DefaultHostnameVerifier(HostnameVerifier extraHostnameVerifier) {
        this.checker = new ProxyHostnameChecker();
        this.extraHostnameVerifier = extraHostnameVerifier;
    }

    /**
     * A hostname verifier with a hostname checker, that falls back to another hostname verifier if not found.
     *
     * @param checker a custom HostnameChecker.
     * @param extraHostnameVerifier another hostname verifier.
     */
    public DefaultHostnameVerifier(HostnameChecker checker, HostnameVerifier extraHostnameVerifier) {
        this.checker = checker;
        this.extraHostnameVerifier = extraHostnameVerifier;
    }

    /**
     * Matches the hostname against the peer certificate in the session.
     *
     * @param hostname the IP address or hostname of the expected server.
     * @param session  the SSL session containing the certificates with the ACTUAL hostname/ipaddress.
     * @return true if the hostname matches, false otherwise.
     */
    private boolean hostnameMatches(String hostname, SSLSession session) {
        log.debug("hostname = {}, session = {}",hostname, Base64.encode(session.getId()));

        try {
            final Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates.length == 0) {
                log.debug("No peer certificates");
                return false;
            }

            if (peerCertificates[0] instanceof X509Certificate) {
                X509Certificate peerCertificate = (X509Certificate) peerCertificates[0];
                log.debug("peerCertificate = {}", peerCertificate);
                try {
                    checker.match(hostname, peerCertificate);
                    // Certificate matches hostname if no exception is thrown.
                    return true;
                } catch (CertificateException ex) {
                    log.debug("Certificate does not match hostname", ex);
                }
            } else {
                log.debug("Peer does not have any certificates or they aren't X.509");
            }
            return false;
        } catch (SSLPeerUnverifiedException ex) {
            log.debug("Not using certificates for peers, try verifying the principal");
            try {
                Principal peerPrincipal = session.getPeerPrincipal();
                log.debug("peerPrincipal = {}", peerPrincipal);
                if (peerPrincipal instanceof KerberosPrincipal) {
                    return checker.match(hostname, (KerberosPrincipal) peerPrincipal);
                } else {
                    log.debug("Can't verify principal, not Kerberos");
                }
            } catch (SSLPeerUnverifiedException ex2) {
                // Can't verify principal, no principal
                log.debug("Can't verify principal, no principal", ex2);
            }
            return false;
        }
    }

    /**
     * Verifies the hostname against the peer certificates in a session.  Falls back to extraHostnameVerifier if
     * there is no match.
     *
     * @param hostname the IP address or hostname of the expected server.
     * @param session  the SSL session containing the certificates with the ACTUAL hostname/ipaddress.
     * @return true if the hostname matches, false otherwise.
     */
    public boolean verify(String hostname, SSLSession session) {
        if (hostnameMatches(hostname, session)) {
            return true;
        } else {
            return extraHostnameVerifier != null && extraHostnameVerifier.verify(hostname, session);
        }
    }
}
