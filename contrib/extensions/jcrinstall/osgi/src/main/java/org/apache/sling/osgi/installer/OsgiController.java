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
package org.apache.sling.osgi.installer;

import java.io.IOException;
import java.util.Set;

/** jcrinstall component that installs/updates/removes 
 *  OSGi resources (bundles, deployment packages, configs)
 *  in the OSGi framework.
 */
public interface OsgiController {
    
    /** Schedule installation or update of supplied resource 
     *  @param uri Unique identifier for the resource
     *  @param data The data to install
     *  @return one of the {@link InstallResultCode} result codes. 
     */
    void scheduleInstallOrUpdate(String uri, InstallableData data) throws IOException, JcrInstallException;
    
    /** Schedule uninstallation of resource that was installed via given uri.
     *  Might be called several times for the same URI - needless calls should
     *  be ignored.
     *  @param uri Unique identifier for the resource
     *  @param attributes metadata stored by the OsgiController, will be
     *      removed after calling this method
     */
    void scheduleUninstall(String uri) throws IOException, JcrInstallException;
    
    /** Return the list of uri for resources that have been installed 
     *  by this controller.
     */
    Set<String> getInstalledUris();
    
    /** Get the lastModified value for given uri, assuming the resource pointed
     *  to by that uri was installed.
     *  @return -1 if we don't have info for given uri
     */
    String getDigest(String uri);
    
    /** Optionally set ResourceOverrideRules */
    void setResourceOverrideRules(ResourceOverrideRules r);
    
    /** Do the actual installs/uninistalls which were scheduled by the other methods */
    void executeScheduledOperations() throws Exception;
}
