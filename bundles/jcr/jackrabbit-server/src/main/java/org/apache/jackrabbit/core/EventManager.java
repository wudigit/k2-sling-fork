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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.sling.jcr.jackrabbit.server.event.SynchronousListener;
import org.apache.sling.jcr.jackrabbit.server.event.SynchronousListener.Registration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Manages event registration of Components implementing {@link SynchronousListener},
 * registration is performed via OSGi.
 * 
 * @scr.component immediate="true" label="EventManagerServiceImpl"
 *                description="Implementation of the Event Manager Service"
 * 
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Event Manager Service Implementation"
 * @scr.property name="workspaceName" value="default"
 * @scr.reference name="repository" interface="javax.jcr.Repository" bind="bindRepository"
 *                unbind="unbindRepository"
 * 
 */
public class EventManager implements ManagedService {

  /**
   * A Wrapper class that wraps the external {@link SynchronousListener} into an internal
   * {@link SynchronousEventListener}
   */
  public class WrappedSynchronouseEventListener implements SynchronousEventListener {

    private SynchronousListener listener;

    /**
     * @param listener
     */
    public WrappedSynchronouseEventListener(SynchronousListener listener) {
      this.listener = listener;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator eventIterator) {
      listener.onEvent(eventIterator);
    }

    /**
     * @return
     */
    public Registration getRegistration() {
      return listener.getRegistration();
    }

  }

  /**
   * class logger.
   */
  protected static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

  /**
   * The service tracker.
   */
  private ServiceTracker serviceTracker;
  /**
   * Map of listeners against service reference.
   */
  private Map<ServiceReference, WrappedSynchronouseEventListener> listeners = new ConcurrentHashMap<ServiceReference, WrappedSynchronouseEventListener>();

  /**
   * The workspace the manager is connected to.
   */
  private String workspaceName;
  /**
   * The system session for this event manager.
   */
  private SystemSession session;

  /**
   * The current observation manager.
   */
  private ObservationManager observationManager;

  /**
   * The current repository implementation.
   */
  private RepositoryImpl repository;

  public void activate(ComponentContext ctx) throws NoSuchWorkspaceException,
      RepositoryException {
    final BundleContext bundleContext = ctx.getBundleContext();
    workspaceName = (String) ctx.getProperties().get("workspaceName");
    rebind();

    serviceTracker = new ServiceTracker(bundleContext, SynchronousListener.class
        .getName(), null) {
      /**
       * {@inheritDoc}
       * 
       * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
       */
      @Override
      public Object addingService(ServiceReference reference) {
        SynchronousListener listener = (SynchronousListener) bundleContext
            .getService(reference);
        WrappedSynchronouseEventListener syncEventListener = new WrappedSynchronouseEventListener(
            listener);
        listeners.put(reference, syncEventListener);
        EventManager.this.addListener(syncEventListener);
        return super.addingService(reference);
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
       *      java.lang.Object)
       */
      @Override
      public void removedService(ServiceReference reference, Object service) {
        SynchronousEventListener syncEventLister = listeners.get(reference);
        try {
          observationManager.removeEventListener(syncEventLister);
        } catch (RepositoryException e) {
          LOGGER.error(e.getMessage(), e);
        }
        listeners.remove(reference);
        super.removedService(reference, service);
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference,
       *      java.lang.Object)
       */
      @Override
      public void modifiedService(ServiceReference reference, Object service) {
        SynchronousListener listener = (SynchronousListener) bundleContext
            .getService(reference);
        SynchronousEventListener syncEventLister = listeners.get(reference);
        try {
          observationManager.removeEventListener(syncEventLister);
        } catch (RepositoryException e) {
          LOGGER.error(e.getMessage(), e);
        }
        listeners.remove(reference);
        WrappedSynchronouseEventListener syncEventListener = new WrappedSynchronouseEventListener(
            listener);
        listeners.put(reference, syncEventListener);
        EventManager.this.addListener(syncEventListener);
        super.modifiedService(reference, service);
      }
    };
    serviceTracker.open();

  }

  /**
   * Deactivate this EventManager.
   * @param ctx
   */
  public void deactivate(ComponentContext ctx) {
    session.logout();
    session = null;
    repository = null;
    serviceTracker.close();
  }

  /**
   * Bind the repository.
   * 
   * @param repository
   * @throws NoSuchWorkspaceException
   * @throws RepositoryException
   */
  protected synchronized void bindRepository(Repository repository) throws NoSuchWorkspaceException,
      RepositoryException {
    this.repository = (RepositoryImpl) repository;
    rebind();
  }

  /**
   * rebind the the list of events to the observation manager.
   * @throws RepositoryException
   * @throws NoSuchWorkspaceException
   * 
   */
  private synchronized void rebind() throws NoSuchWorkspaceException, RepositoryException {
    if (workspaceName != null && repository != null) {
      if (session != null) {
        session.logout();
      }
      RepositoryImpl repositoryImpl = repository;
      session = repositoryImpl.getSystemSession(workspaceName);
      observationManager = session.getWorkspace().getObservationManager();
      for (WrappedSynchronouseEventListener listener : listeners.values()) {
        addListener(listener);
      }
    }
  }
  
  

  /**
   * Add a listener to the current observation manager.
   * @param listener
   */
  private synchronized void addListener(WrappedSynchronouseEventListener listener) {
    Registration registration = listener.getRegistration();
    try {
      observationManager.addEventListener(listener, registration.getEventTypes(),
          registration.getAbsPath(), registration.isDeep(), registration.getUuids(),
          registration.getNodeTypeNames(), registration.isNoLocal());
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Unbind the repository.
   * @param repository
   */
  protected synchronized void unbindRepository(Repository repository) {
    if (session != null) {
      session.logout();
    }
    repository = null;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    String newWorkspaceName = (String) properties.get("workspaceName");
    if ( !newWorkspaceName.equals(workspaceName) ) {
      workspaceName = newWorkspaceName;
      try {
        rebind();
      } catch (NoSuchWorkspaceException e) {
        throw new ConfigurationException("workspaceName",e.getMessage());
      } catch (RepositoryException e) {
        throw new ConfigurationException("workspaceName",e.getMessage());
      }
    }
  }

}
