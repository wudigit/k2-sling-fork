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
package org.apache.sling.jackrabbit.usermanager.api.event;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jackrabbit.usermanager.api.event.AuthorizableEvent.Operation;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.Event;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A utility class for creating events.
 */
public class AuthorizableEventUtil {

  /**
   * Create a new authorizable event, not bound to the thread or request.
   * 
   * @param operation
   *          the operation that was performed.
   * @param authorizable
   *          the authorizable being created, updated or deleted.
   * @param request
   *          the request performing the change, not propagated.
   * @param changes
   *          the list of changes.
   * @throws RepositoryException
   *           in a Principal cannot be resolved.
   */
  public static Event newAuthorizableEvent(Operation operation,
      Authorizable authorizable, 
      List<Modification> changes) throws RepositoryException {
    Dictionary<String, Object> eventDictionary = new Hashtable<String, Object>();
    eventDictionary.put(AuthorizableEvent.OPERATION, operation);
    eventDictionary.put(AuthorizableEvent.PRINCIPAL, authorizable.getPrincipal());
    eventDictionary.put(AuthorizableEvent.CHANGES, changes);
    return new Event(operation.getTopic(), eventDictionary);
  }

  /**
   * SynchronousAuthoizableEvent, that is fired Synchronously, bound to the thread and
   * request containing sufficient object for listeners to make additional changes to the
   * JCR. The OSGi container should be configured to restrict the propagation of this
   * event to those listeners that are trusted to receive this event.
   * 
   * @param operation the operation that was performed.
   * @param session the session used to perform the operation.
   * @param request the request that caused the operation.
   * @param authorizable the authorizable object on which the operation was performed.
   * @param changes a list of modification changes.
   * @throws RepositoryException
   */
  public static Event newAuthorizableEvent(Operation operation, Session session,
      SlingHttpServletRequest request, Authorizable authorizable,
      List<Modification> changes) throws RepositoryException {
    Dictionary<String, Object> eventDictionary = new Hashtable<String, Object>();
    eventDictionary.put(AuthorizableEvent.OPERATION, operation);
    eventDictionary.put(AuthorizableEvent.PRINCIPAL, authorizable.getPrincipal());
    eventDictionary.put(AuthorizableEvent.CHANGES, changes);
    eventDictionary.put(AuthorizableEvent.SESSION, session);
    eventDictionary.put(AuthorizableEvent.AUTHORIZABLE, authorizable);
    eventDictionary.put(AuthorizableEvent.REQUEST, request);
    return new Event(operation.getSyncTopic(), eventDictionary);
  }
  
}
