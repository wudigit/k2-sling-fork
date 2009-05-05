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
package org.apache.sling.jcr.jackrabbit.server.event;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

/**
 * Things that implement this interface in a bundle are registered as a component are
 * wrapped in a internal jackrabbit Synchronous Event so that the event is fired within
 * the session transaction. Other types of event can simply implement the EventListener.
 */
public interface SynchronousListener extends EventListener {

  /**
   * An Object the represents the registration, see {@link javax.jcr.observation.ObservationManager#addEventListener(EventListener, int, String, boolean, String[], String[], boolean)}
   */
  public interface Registration {

    /**
     * @return the event types a bit map of event types see {@link Event} for details.
     */
    int getEventTypes();

    /**
     * @return the path against which the event is registered.
     */
    String getAbsPath();

    /**
     * @return should events in the sub path be generated
     */
    boolean isDeep();

    /**
     * @return a list of uuids to fire events, may be null.
     */
    String[] getUuids();

    /**
     * @return a list of node type names to register events against, may be null.
     */
    String[] getNodeTypeNames();

    /**
     * @return true if no events from the current session should be propagated.
     */
    boolean isNoLocal();

  }

  /**
   * @return a registration object to define how the event should be registered.
   */
  Registration getRegistration();
}
