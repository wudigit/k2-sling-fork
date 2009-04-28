/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.sling.jcr.jackrabbit.server.impl.security.standard.EntryCollectorImpl;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;

import java.util.List;
import java.util.Map;

/**
 * This EntryCollector implementation uses a principal manager to check each potential
 * principal against a dynamic principal manager to see if the ACE should be included in
 * the resolved entry.
 */
public class DynamicEntryCollector extends EntryCollectorImpl {

  private DynamicPrincipalManager dynamicPrincipalManager;

  /**
   * Construct this type of EntryCollector with a principal manager. 
   */
  public DynamicEntryCollector(DynamicPrincipalManager dynamicPrincipalManager) {
    this.dynamicPrincipalManager = dynamicPrincipalManager;
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.standard.EntryCollectorImpl#hasPrincipal(java.lang.String, org.apache.jackrabbit.core.NodeImpl, java.util.Map)
   */
  @Override
  protected boolean hasPrincipal(String principalName, NodeImpl aclNode,
      Map<String, List<AccessControlEntry>> princToEntries) {
    // TODO Auto-generated method stub
    if( super.hasPrincipal(principalName, aclNode, princToEntries) ) {
      return true;
    } 
    return dynamicPrincipalManager.hasPrincipalInContext(principalName, aclNode);
  }


}
