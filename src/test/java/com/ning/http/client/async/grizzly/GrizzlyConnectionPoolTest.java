/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.ning.http.client.async.grizzly;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.util.concurrent.TimeUnit;

import com.ning.http.client.ListenableFuture;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.async.ConnectionPoolTest;
import com.ning.http.client.async.ProviderUtil;
import java.util.concurrent.ExecutionException;

public class GrizzlyConnectionPoolTest extends ConnectionPoolTest {

    @Override
    public AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config) {
        return ProviderUtil.grizzlyProvider(config);
    }

    @Override
    @Test
    public void testMaxTotalConnectionsException() {
        try(AsyncHttpClient client = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setMaxConnections(1).build())) {
            String url = getTargetUrl();
            ListenableFuture<?> lockRequest = null;
            try {
                lockRequest = client.prepareGet(url).addHeader("LockThread", "true").execute();
            } catch (Exception e) {
                fail("Unexpected exception thrown.", e);
            }
            try {
                client.prepareConnect(url).execute().get();
            } catch (ExecutionException ee) {
                final Throwable cause = ee.getCause();
                assertNotNull(cause);
                assertEquals("Max connections exceeded", cause.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception thrown.", e);
            }
            lockRequest.cancel(true);
        }
    }

    @Override
    @Test
    public void multipleMaxConnectionOpenTest() throws Throwable {
        AsyncHttpClientConfig cg = new AsyncHttpClientConfig.Builder().setAllowPoolingConnections(true).setConnectTimeout(5000).setMaxConnections(1).build();
        try (AsyncHttpClient c = getAsyncHttpClient(cg)) {
            String body = "hello there";

            // once
            Response response = c.preparePost(getTargetUrl()).setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);

            assertEquals(response.getResponseBody(), body);

            // twice
            Exception exception = null;
            try {
                c.preparePost(String.format("http://127.0.0.1:%d/foo/test", port2)).setBody(body).execute().get(TIMEOUT, TimeUnit.SECONDS);
                fail("Should throw exception. Too many connections issued.");
            } catch (Exception ex) {
                ex.printStackTrace();
                exception = ex;
            }
            assertNotNull(exception);
        }
    }
}
