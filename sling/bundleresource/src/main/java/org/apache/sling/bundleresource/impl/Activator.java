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
package org.apache.sling.bundleresource.impl;

import static org.apache.sling.jcr.resource.JcrResourceConstants.BUNDLE_RESOURCE_ROOTS;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, BundleListener {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Map<Long, BundleResourceProvider> bundleResourceProviderMap = new HashMap<Long, BundleResourceProvider>();
    
    private BundleContext bundleContext;

    public void start(BundleContext context) throws Exception {

        this.bundleContext = context;
        
        context.addBundleListener(this);

        try {
            Bundle[] bundles = context.getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    // add bundle resource provider for active bundles
                    addBundleResourceProvider(bundle);
                }
            }
        } catch (Throwable t) {
            log.error("activate: Problem while loading initial content and"
                + " registering mappings for existing bundles", t);
        }
    }

    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
        this.bundleContext = null;
    }

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                // register resource provider for the started bundle
                addBundleResourceProvider(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove resource provider after the bundle has stopped
                removeBundleResourceProvider(event.getBundle());
                break;
        }
    }
    
    // ---------- Bundle provided resources -----------------------------------

    private void addBundleResourceProvider(Bundle bundle) {
        String prefixes = (String) bundle.getHeaders().get(
            BUNDLE_RESOURCE_ROOTS);
        if (prefixes != null) {
            BundleResourceProvider brp = new BundleResourceProvider(bundle,
                prefixes);
            brp.registerService(bundleContext);
            bundleResourceProviderMap.put(bundle.getBundleId(), brp);
        }
    }

    private void removeBundleResourceProvider(Bundle bundle) {
        BundleResourceProvider brp = bundleResourceProviderMap.remove(bundle.getBundleId());
        if (brp != null) {
            brp.unregisterService();
        }
    }


}