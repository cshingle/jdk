/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @summary Verify that all methods in the HttpURLConnection.methods whitelist
 *          are accepted by setRequestMethod, and that an invalid method is rejected.
 * @library /test/lib
 * @run junit/othervm ${test.main.class}
 */

import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* @test
 * @summary This test checks the HttpURLConnection::setRequestMethod validation.
 * @bug 8207840
 * @library /test/lib
 * @modules java.base/sun.net.www.http
 *          java.base/sun.net.www.protocol.http
 * @build java.base/sun.net.www.http.HttpClientAccess
 * @run junit/othervm ${test.main.class}
 */

public class RequestMethodWhitelist {
    private static final String TEST_CONTEXT = "/methodwhitelist";
    private static HttpServer server;

    @BeforeAll
    public static void setup() throws Exception {
        server = createServer();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testAllWhitelistedMethods() throws Exception {
        String[] methods = {"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH"};
        for (String method : methods) {
            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .host(server.getAddress().getAddress())
                    .port(server.getAddress().getPort())
                    .path(TEST_CONTEXT)
                    .toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod(method);
                assertEquals(method, conn.getRequestMethod(),
                        "setRequestMethod(" + method + ") should set the method to " + method);
            } finally {
                conn.disconnect();
            }
        }
    }

    @Test
    public void testInvalidMethodRejected() throws Exception {
        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .host(server.getAddress().getAddress())
                .port(server.getAddress().getPort())
                .path(TEST_CONTEXT)
                .toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            ProtocolException ex = assertThrows(ProtocolException.class,
                    () -> conn.setRequestMethod("INVALID"),
                    "setRequestMethod(INVALID) should throw ProtocolException");
            assertEquals("Invalid HTTP method: INVALID", ex.getMessage());
        } finally {
            conn.disconnect();
        }
    }

    private static HttpServer createServer() throws IOException {
        final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        final HttpServer server = HttpServer.create(serverAddress, -1);
        server.createContext(TEST_CONTEXT, exchange -> {
            exchange.sendResponseHeaders(200, 1);
            exchange.close();
        });
        server.start();
        System.out.println("Server started on " + server.getAddress());
        return server;
    }
}
