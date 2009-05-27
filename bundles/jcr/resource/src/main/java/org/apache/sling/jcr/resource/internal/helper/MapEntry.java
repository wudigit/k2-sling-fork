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
package org.apache.sling.jcr.resource.internal.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver2;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>MapEntry</code> class represents a mapping entry in the mapping
 * configuration tree at <code>/etc/map</code>.
 * <p>
 *
 * @see "http://cwiki.apache.org/SLING/flexible-resource-resolution.html"
 */
public class MapEntry implements Comparable<MapEntry> {

    private static final Pattern[] PATH_TO_URL_MATCH = {
        Pattern.compile("http/([^/]+)\\.80(/.*)?$"),
        Pattern.compile("https/([^/]+)\\.443(/.*)?$"),
        Pattern.compile("([^/]+)/([^/]+)\\.(\\d+)(/.*)?$") };

    private static final String[] PATH_TO_URL_REPLACEMENT = { "http://$1$2",
        "https://$1$2", "$1://$2:$3$4" };

    private static final Logger log = LoggerFactory.getLogger(MapEntry.class);

    private final Pattern urlPattern;

    private final String[] redirect;

    private final int status;

    /**
     * If this service tracker is initialized it will track MapperResolvers for this
     * MapEntry. If not initialized it can be ignored.
     */
    private ServiceTracker tracker;

    protected List<MappingResolver> mappingResolverList = new ArrayList<MappingResolver>();

    public static String appendSlash(String path) {
        if (!path.endsWith("/")) {
            path = path.concat("/");
        }
        return path;
    }

    /**
     * Returns a string used for matching map entries against the given request
     * or URI parts.
     *
     * @param scheme The URI scheme
     * @param host The host name
     * @param port The port number. If this is negative, the default value used
     *            is 80 unless the scheme is "https" in which case the default
     *            value is 443.
     * @param path The (absolute) path
     * @return The request path string {scheme}://{host}:{port}{path}.
     */
    public static String getURI(String scheme, String host, int port,
            String path) {

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (port > 0 && !(port == 80 && "http".equals(scheme))
            && !(port == 443 && "https".equals(scheme))) {
            sb.append(':').append(port);
        }
        sb.append(path);

        return sb.toString();
    }

    public static URI toURI(String uriPath) {
        for (int i = 0; i < PATH_TO_URL_MATCH.length; i++) {
            Matcher m = PATH_TO_URL_MATCH[i].matcher(uriPath);
            if (m.find()) {
                String newUriPath = m.replaceAll(PATH_TO_URL_REPLACEMENT[i]);
                try {
                    return new URI(newUriPath);
                } catch (URISyntaxException use) {
                    // ignore, just don't return the uri as such
                }
            }
        }

        return null;
    }

    public static MapEntry createResolveEntry(String url, Resource resource,
            boolean trailingSlash, ComponentContext componentContext) {
        ValueMap props = resource.adaptTo(ValueMap.class);
        if (props != null) {
            String redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_EXTERNAL, String.class);
            if (redirect != null) {
                int status = props.get(
                    JcrResourceResolver2.PROP_REDIRECT_EXTERNAL_STATUS, 302);
                return new MapEntry(url, status, trailingSlash, redirect);
            }

            String[] internalRedirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_INTERNAL, String[].class);
            if (internalRedirect != null) {
                return new MapEntry(url, -1, trailingSlash, internalRedirect);
            }
            
            String mapper = props.get(JcrResourceResolver2.PROP_MAPPER, String.class);
            if (mapper != null) {
              if ( componentContext != null ) {
                try {
                  return new MapEntry(url, -1, trailingSlash, mapper, componentContext);
                } catch (InvalidSyntaxException e) {
                  log.warn("Mapper filter {} found at {} is invalid {} ", new Object[] {mapper,
                      resource.getPath(), e.getMessage()});
                }
              } else {
                log.warn(
                    "Mapper filter {} found at {} cannot be loaded, no component context ",
                    new Object[] {mapper, resource.getPath()});
              }
            }
            
         }

        return null;
    }

    public static List<MapEntry> createMapEntry(String url, Resource resource,
            boolean trailingSlash) {
        ValueMap props = resource.adaptTo(ValueMap.class);
        if (props != null) {
            String redirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_EXTERNAL, String.class);
            if (redirect != null) {
                // ignoring external redirects for mapping
                return null;
            }

            // check whether the url is a match hooked to then string end
            String endHook = "";
            if (url.endsWith("$")) {
                endHook = "$";
                url = url.substring(0, url.length()-1);
            }
            
            String[] internalRedirect = props.get(
                JcrResourceResolver2.PROP_REDIRECT_INTERNAL, String[].class);
            if (internalRedirect != null) {

                int status = -1;
                URI extPathPrefix = toURI(url);
                if (extPathPrefix != null) {
                    url = getURI(extPathPrefix.getScheme(),
                        extPathPrefix.getHost(), extPathPrefix.getPort(),
                        extPathPrefix.getPath());
                    status = 302;
                }

                List<MapEntry> prepEntries = new ArrayList<MapEntry>(
                    internalRedirect.length);
                for (String redir : internalRedirect) {
                    if (!redir.contains("$")) {
                        prepEntries.add(new MapEntry(redir.concat(endHook),
                            status, trailingSlash, url));
                    }
                }

                if (prepEntries.size() > 0) {
                    return prepEntries;
                }
            }
        }

        return null;
    }
    public MapEntry(String url, int status, boolean trailingSlash,
        String filter, ComponentContext context) throws InvalidSyntaxException {
      final BundleContext bundleContext = context.getBundleContext();
      Filter serviceFilter = bundleContext.createFilter("(&(objectclass="+MappingResolver.class+")"+filter+")");
      // add a service tracker that just looks for the last registered service we are interested in.
      tracker = new ServiceTracker(bundleContext,serviceFilter,new ServiceTrackerCustomizer(){        
        public Object addingService(ServiceReference reference) {
          MappingResolver service = (MappingResolver) bundleContext.getService(reference);
          synchronized (mappingResolverList) {
            mappingResolverList.add(service);            
          }
          return service;
        }
        public void modifiedService(ServiceReference reference, Object service) {    
        }

        public void removedService(ServiceReference reference, Object service) {
          synchronized (mappingResolverList) {
            mappingResolverList.remove(service);
          }
        }});
      // ensure trailing slashes on redirects if the url
      // ends with a trailing slash
      if (trailingSlash) {
          url = appendSlash(url);
      }

      // ensure pattern is hooked to the start of the string
      if (!url.startsWith("^")) {
          url = "^".concat(url);
      }

      this.urlPattern = Pattern.compile(url);
      this.redirect = null;
      this.status = status;
    }
    

    public MapEntry(String url, int status, boolean trailingSlash,
            String... redirect) {

        // ensure trailing slashes on redirects if the url
        // ends with a trailing slash
        if (trailingSlash) {
            url = appendSlash(url);
            for (int i = 0; i < redirect.length; i++) {
                redirect[i] = appendSlash(redirect[i]);
            }
        }

        // ensure pattern is hooked to the start of the string
        if (!url.startsWith("^")) {
            url = "^".concat(url);
        }

        this.urlPattern = Pattern.compile(url);
        this.redirect = redirect;
        this.status = status;
    }

    // Returns the replacement or null if the value does not match
    public String[] replace(String value) {
      if ( !isDynamic() ) {
        Matcher m = urlPattern.matcher(value);
        if (m.find()) {
            String[] redirects = getRedirect();
            
            String[] results = new String[redirects.length];
            for (int i = 0; i < redirects.length; i++) {
                results[i] = m.replaceFirst(redirects[i]);
            }
            return results;
        }
      }
      return null;
    }
    
  /**
   * Resolves a request path to an internal path, if this is a dynamic map entry.
   *
   * @param request
   *          the request.
   * @param requestPath
   *          the request path.
   * @return A resolved path if there is a match and the
   */
  public String resolve(HttpServletRequest request, String requestPath) {
    if (isDynamic()) {
      Matcher m = urlPattern.matcher(requestPath);
      if (m.find()) {
        MappingResolver mappingResolver = getMappingResolver();
        if (mappingResolver != null) {
          return mappingResolver.resolve(request, requestPath);
        }
      }
    }
    return null;
  }
    
  /**
   * Maps an internal path into an external path.
   *
   * @param request
   *          the request object.
   * @param path
   *          the internal path.
   * @return an external path.
   */
  public String map(HttpServletRequest request, String path) {
    if (isDynamic()) {
      Matcher m = urlPattern.matcher(path);
      if (m.find()) {
        MappingResolver mappingResolver = getMappingResolver();
        if (mappingResolver != null) {
          return mappingResolver.map(request, path);
        }
      }
    }
    return null;
  }
    

    /**
     * Gets the current last implementation from the ServiceTracker.
     * @return the current MapperResolver service implementation, null if there is none.
     */
    private MappingResolver getMappingResolver() {
      synchronized (mappingResolverList) {
        if ( mappingResolverList.size() > 0 ) {
          return mappingResolverList.get(mappingResolverList.size()-1);
        }
        // no service found, ignore
        return null;
      }
    }

    public String getPattern() {
        return urlPattern.toString();
    }

    public String[] getRedirect() {
        return redirect;
    }

    public boolean isInternal() {
        return getStatus() < 0;
    }
    
    public boolean isDynamic() {
      return (tracker != null);
    }

    public int getStatus() {
        return status;
    }

    // ---------- Comparable

    public int compareTo(MapEntry m) {
        if (this == m) {
            return 0;
        }

        int tlen = urlPattern.toString().length();
        int mlen = m.urlPattern.toString().length();
        if (tlen < mlen) {
            return 1;
        } else if (tlen > mlen) {
            return -1;
        }

        // lentghs are equal, but the entries are not
        // so order m after this
        return 1;
    }

    // ---------- Object overwrite

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("MapEntry: match:").append(urlPattern);

        buf.append(", replacement:");
        if (getRedirect().length == 1) {
            buf.append(getRedirect()[0]);
        } else {
            buf.append(Arrays.asList(getRedirect()));
        }

        if (isInternal()) {
            buf.append(", internal");
        } else {
            buf.append(", status:").append(getStatus());
        }
        return buf.toString();
    }
}
