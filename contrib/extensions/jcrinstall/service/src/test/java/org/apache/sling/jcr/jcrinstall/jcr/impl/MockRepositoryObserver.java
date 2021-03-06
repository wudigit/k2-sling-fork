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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.osgi.installer.OsgiController;
import org.osgi.service.component.ComponentContext;

/** Slightly customized RepositoryObserver
 *  used for testing.
 */
public class MockRepositoryObserver extends RepositoryObserver {
    private Properties props;
    private final File serviceDataFile;
    
    MockRepositoryObserver(SlingRepository repo, final OsgiController c) {
        this(repo, c, null);
    }
    
    MockRepositoryObserver(SlingRepository repo, final OsgiController c, File serviceDataFile) {
        repository = repo;
        osgiController = c;
        scanDelayMsec = 0;
        this.serviceDataFile = serviceDataFile;
    }
    
    public void run() {
        // Do not run the observation cycle - we do that ourselves in testing
    }
    
    boolean folderIsWatched(String path) throws Exception {
        boolean result = false;
        for(WatchedFolder wf : getWatchedFolders()) {
            if(wf.getPath().equals("/" + path)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    public void setProperties(Properties p) {
        props = p;
    }
    
    protected String getPropertyValue(ComponentContext ctx, String name) {
        return (props == null ? null : props.getProperty(name));
    }
    
    protected File getServiceDataFile(ComponentContext context) {
        if(serviceDataFile != null) {
            return serviceDataFile;
        }
        
        try {
            final File f = File.createTempFile(getClass().getSimpleName(), ".properties");
            f.deleteOnExit();
            return f;
        } catch(IOException ioe) {
            throw new Error("IOException", ioe);
        }
    }
}
