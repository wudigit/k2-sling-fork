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
package org.apache.sling.adapter.internal;

import org.apache.sling.api.adapter.AdapterFactory;

/**
 * The <code>AdapterFactoryDescriptor</code> is an entry in the
 * {@link AdapterFactoryDescriptorMap} conveying the list of adapter (target)
 * types and the respective {@link AdapterFactory}.
 */
public class AdapterFactoryDescriptor {
    
    private AdapterFactory factory;

    private String[] adapters;

    public AdapterFactoryDescriptor(AdapterFactory factory, String[] adapters) {
        this.factory = factory;
        this.adapters = adapters;
    }

    public AdapterFactory getFactory() {
        return factory;
    }

    public String[] getAdapters() {
        return adapters;
    }

}