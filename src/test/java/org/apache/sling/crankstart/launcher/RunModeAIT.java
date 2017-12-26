/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/** Test our run modes support */ 
public class RunModeAIT {
    
    @ClassRule
    public static CrankstartSetup C = new CrankstartSetup().withModelResources(U.DEFAULT_MODELS);
    
    private WebconsoleClient osgiConsole;
    private DefaultHttpClient client;
    private static final String RUN_MODES = "foo,bar,A";
    
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    @BeforeClass
    public static void setupClass() throws Exception {
        System.setProperty(RunModeFilter.SLING_RUN_MODES, RUN_MODES);
    }
    
    @Before
    public void setup() throws IOException {
        osgiConsole = new WebconsoleClient(C.getBaseUrl(), U.ADMIN, U.ADMIN);
        client = new DefaultHttpClient();
    }
    
    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(RunModeFilter.SLING_RUN_MODES);
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testSlingApiVersionA() throws Exception {
        assertEquals("2.9.0", osgiConsole.getBundleVersion(U.SLING_API_BUNDLE));
    }
    
    @Test
    @Retry(timeoutMsec=U.LONG_TIMEOUT_MSEC, intervalMsec=U.STD_INTERVAL)
    public void testConfigA() throws Exception {
        U.setAdminCredentials(client);
        U.assertHttpGet(C, client,
                "/test/config/runmode.test", 
                "runmode.test#mode=(String)This is A#service.pid=(String)runmode.test##EOC#");
    }
    
}