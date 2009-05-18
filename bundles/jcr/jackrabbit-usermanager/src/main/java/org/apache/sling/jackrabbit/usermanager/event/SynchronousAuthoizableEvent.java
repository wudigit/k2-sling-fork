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

import java.util.Dictionary;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * SynchronousAuthoizableEvent, that is fired Synchronously, bound to the thread and
 * request containing sufficient object for listeners to make additional changes to the
 * JCR. The OSGi container should be configured to restrict the propagation of this event
 * to those listeners that are trusted to receive this event.
 */
public class SynchronousAuthoizableEvent extends AuthoizableEvent {

  /**
   * The topic that the event is sent as.
   */
  public static final String TOPIC = SynchronousAuthoizableEvent.class.getName().replace('.', '/');;
  /**
   * The session performing modifications.
   */
  protected static final String SESSION = "session";
  /**
   * The request performing the operation.
   */
  protected static final String REQUEST = "request";
  /**
   * The Auithorizable being accessed.
   */
  protected static final String AUTHORIZABLE = "authorizable";

  /**
   * @param arg0
   * @param arg1
   */
  @SuppressWarnings("unchecked")
  public SynchronousAuthoizableEvent(String arg0, Dictionary arg1) {
    super(arg0, arg1);
  }

  /**
   * @param operation2
   * @param session2
   * @param request2
   * @param item
   * @param changes2
   * @throws RepositoryException
   */
  public SynchronousAuthoizableEvent(Operation operation, Session session,
      SlingHttpServletRequest request, Authorizable item, List<Modification> changes)
      throws RepositoryException {
    super(SynchronousAuthoizableEvent.TOPIC, newDictionary(operation, session, request,
        item, changes));
  }

  /**
   * @param operation2
   * @param session2
   * @param request2
   * @param item
   * @param changes2
   * @return
   * @throws RepositoryException
   */
  private static Dictionary<String, Object> newDictionary(Operation operation,
      Session session, SlingHttpServletRequest request, Authorizable authorizable,
      List<Modification> changes) throws RepositoryException {
    Dictionary<String, Object> eventDictionary = AuthoizableEvent.newDictionary(
        operation, authorizable, changes);
    eventDictionary.put(SESSION, session);
    eventDictionary.put(AUTHORIZABLE, authorizable);
    eventDictionary.put(REQUEST, request);
    return eventDictionary;
  }

  public Session getSession() {
    return (Session) getProperty(SESSION);
  }

  public Authorizable getAuthorizable() {
    return (Authorizable) getProperty(AUTHORIZABLE);
  }

  public SlingHttpServletRequest getRequest() {
    return (SlingHttpServletRequest) getProperty(REQUEST);
  }

}
