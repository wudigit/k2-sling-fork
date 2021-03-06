/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.jcrinstall;

import java.util.LinkedList;
import java.util.List;

/** Verify that stopping and restarting jcrinstall (simulating a disappearing
 * 	repository) works as expected.
 */
public class StopAndRestartTest extends JcrinstallTestBase {
	
	public void testStopAndRestart() throws Exception {
		final int activeBeforeTest = getActiveBundlesCount();
		final List<String> installed = new LinkedList<String>();
		
		final int nBundles = 10 * scaleFactor;
		for(int i=0 ; i < nBundles; i++) {
			installed.add(installClonedBundle(null, null));
		}
		
    enableJcrinstallService(true);
		assertActiveBundleCount("after adding bundles", 
				activeBeforeTest + nBundles, defaultBundlesTimeout);

		// Bundles stay active if disabling jcrinstall,
		// startlevel manipulations have been removed
		enableJcrinstallService(false);
		sleep(1000L);
		
		assertActiveBundleCount("after disabling jcrinstall", 
				activeBeforeTest + nBundles, defaultBundlesTimeout);
		
		enableJcrinstallService(true);
    sleep(1000L);
		
		assertActiveBundleCount("after re-enabling jcrinstall", 
				activeBeforeTest + nBundles, defaultBundlesTimeout);
		
		for(String path : installed) {
			removeClonedBundle(path);
		}
		
		assertActiveBundleCount("after removing added bundles", 
				activeBeforeTest, defaultBundlesTimeout);
	}
}
