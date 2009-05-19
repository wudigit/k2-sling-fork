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



/**
 * A notification style event fired asynchronously with serializable properties detached
 * form the session and request.
 */
public interface AuthorizableEvent {
  /**
   * The topic that the synchronous event is fired on.
   */
  public static final String SYNC_TOPIC = "org/apache/sling/jackrabbit/usermanager/snyc/event/";
  /**
   * The topic that the event is sent as.
   */
  public static final String TOPIC = "org/apache/sling/jackrabbit/usermanager/event/";
  
  /**
   * Key for the operation.
   */
  public static final String OPERATION = "operation";
  /**
   * A List of Modifications.
   */
  public static final String CHANGES = "changes";
  /**
   * Principal of the authorizable, only available on asynchronous events.
   */
  public static final String PRINCIPAL = "principal";
  
  /**
   * The session performing modifications, only available to synchronous events.
   */
  public static final String SESSION = "session";
  /**
   * The request performing the operation, only available to synchronous events.
   */
  public static final String REQUEST = "request";
  /**
   * The Authorizable being accessed, only available to synchronous events.
   */
  public static final String AUTHORIZABLE = "authorizable";


  /**
   * Operations
   */
  public static enum Operation {
    delete(), update(), create();
    
    public String getSyncTopic() {
      return SYNC_TOPIC+toString();
    }
    public String getTopic() {
      return TOPIC+toString();
    }
  }


}
