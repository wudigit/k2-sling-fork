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
package org.apache.sling.jackrabbit.usermanager.event;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.Event;

import java.security.Principal;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * A notification style event fired asynchronously with serializable properties detached
 * form the session and request.
 */
public class AuthoizableEvent extends Event {
  /**
   * The topic that the event is fired on.
   */
  public static final String TOPIC = AuthoizableEvent.class.getName().replace('.', '/');
  /**
   * Key for the operation.
   */
  protected static final String OPERATION = "operation";
  /**
   * A List of Modifications.
   */
  protected static final String CHANGES = "changes";
  /**
   * Principal of the authorizable.
   */
  private static final String PRINCIPAL = "principal";

  /**
   * Operations
   */
  public static enum Operation {
    delete(), update(), create();
  }

  /**
   * Default constructor for events.
   * @param topic the topic of the event.
   * @param dictionary the dictionary of the event.
   */
  @SuppressWarnings("unchecked")
  public AuthoizableEvent(String arg0, Dictionary arg1) {
    super(arg0, arg1);
  }


  /**
   * Constructor 
   * @param operation the operation that was performed.
   * @param authorizable the authorizable being created, updated or deleted.
   * @param request the request performing the change, not propagated.
   * @param changes the list of changes.
   * @throws RepositoryException in a Principal cannot be resolved.
   */
  public AuthoizableEvent(Operation operation, Authorizable authorizable,
      SlingHttpServletRequest request, List<Modification> changes)
      throws RepositoryException {
    super(TOPIC, newDictionary(operation, authorizable, changes));
  }

  /**
   * create an populate a dictionary
   * @param operation the operation.
   * @param authorizable the authorizable being modified.
   * @param changes a list of changes.
   * @return A dictionary, newly created containing the properties of the event.
   * @throws RepositoryException if the Principal of the Authorizable cant be determined.
   */
  protected static Dictionary<String, Object> newDictionary(Operation operation,
      Authorizable authorizable, List<Modification> changes) throws RepositoryException {
    Dictionary<String, Object> eventDictionary = new Hashtable<String, Object>();
    eventDictionary.put(OPERATION, operation);
    eventDictionary.put(PRINCIPAL, authorizable.getPrincipal());
    eventDictionary.put(CHANGES, changes);
    return eventDictionary;
  }

  /**
   * @return the operation the event was sent as a result off.
   */
  public Operation getOperation() {
    return (Operation) getProperty(OPERATION);
  }

  /**
   * @return a list of modifications.
   */
  @SuppressWarnings("unchecked")
  public List<Modification> getChanges() {
    return (List<Modification>) getProperty(CHANGES);
  }

  /**
   * @return the Principal of the Authorizable.
   */
  public Principal getPrincipal() {
    return (Principal) getProperty(PRINCIPAL);
  }

}
