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
package org.apache.sling.jcr.contentloader.internal.readers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.contentloader.internal.ContentCreator;
import org.apache.sling.jcr.contentloader.internal.ContentReader;
import org.apache.sling.jcr.contentloader.internal.ImportProvider;

/**
 * The <code>JsonReader</code> Parses a Json document on content load and creates the
 * corresponding node structure with properties. Will not update protected nodes and
 * properties like rep:Policy and children.
 * 
 * <pre>
 * Nodes, Properties and in fact complete subtrees may be described in JSON files
 * using the following skeleton structure (see http://www.json.org for information
 * on the syntax of JSON) :
 * 
 * # the name of the node is taken from the name of the file without the .json ext.
 *   {
 *   
 *     # optional primary node type, default &quot;nt:unstructured&quot;
 *     &quot;jcr:primaryType&quot;:&quot;sling:ScriptedComponent&quot;,
 *     # optional mixin node types as array
 *     &quot;jcr:mixinTypes&quot;: [ ],
 *     
 *       
 *       # &quot;properties&quot; are added as key value pairs, the name of the key being the name
 *       # of the property. The value is either the string property value, array for 
 *       # multi-values or an object whose value[s] property denotes the property 
 *       # value(s) and whose type property denotes the property type
 *       &quot;sling:contentClass&quot;: &quot;com.day.sling.jcr.test.Test&quot;,
 *       &quot;sampleMulti&quot;: [ &quot;v1&quot;, &quot;v2&quot; ],
 *       &quot;sampleStruct&quot;: 1,
 *       &quot;sampleStructMulti&quot;: [ 1, 2, 3 ],
 *       
 *       # reference properties start with jcr:reference
 *       &quot;jcr:reference:sampleReference&quot;: &quot;/test/content&quot;,
 *       
 *       # path propertie start with jcr:path
 *       &quot;jcr:path:sampleReference&quot;: &quot;/test/path&quot;,
 *         
 *       # nested nodes are added as nested maps. 
 *     &quot;sling:scripts&quot;:  {
 *               
 *         &quot;jcr:primaryType&quot;: &quot;sling:ScriptList&quot;,
 *         &quot;script1&quot; :{
 *             &quot;primaryNodeType&quot;: &quot;sling:Script&quot;,
 *               &quot;sling:name&quot;: &quot;/test/content/jsp/start.jsp&quot;,
 *             &quot;sling:type&quot;: &quot;jsp&quot;,
 *             &quot;sling:glob&quot;: &quot;*&quot;
 *         }
 *     }
 *   }
 * 
 * </pre>
 */
public class JsonReader implements ContentReader {

    private static final String REFERENCE = "jcr:reference:";
    private static final String PATH = "jcr:path:";

    private static final Set<String> ignoredNames = new HashSet<String>();
    static {
        ignoredNames.add("jcr:primaryType");
        ignoredNames.add("jcr:mixinTypes");
        ignoredNames.add("jcr:uuid");
        ignoredNames.add("jcr:baseVersion");
        ignoredNames.add("jcr:predecessors");
        ignoredNames.add("jcr:successors");
        ignoredNames.add("jcr:checkedOut");
        ignoredNames.add("jcr:created");
    }

    public static final ImportProvider PROVIDER = new ImportProvider() {
        private JsonReader jsonReader;

        public ContentReader getReader() {
            if (jsonReader == null) {
                jsonReader = new JsonReader();
            }
            return jsonReader;
        }
    };

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentReader#parse(java.net.URL, org.apache.sling.jcr.contentloader.internal.ContentCreator)
     */
    public void parse(java.net.URL url, ContentCreator contentCreator)
    throws IOException, RepositoryException {
        InputStream ins = null;
        try {
            ins = url.openStream();
            parse(ins, contentCreator);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public void parse(InputStream ins, ContentCreator contentCreator) throws IOException, RepositoryException {
        try {
            String jsonString = toString(ins).trim();
            if (!jsonString.startsWith("{")) {
                jsonString = "{" + jsonString + "}";
            }

            JSONObject json = new JSONObject(jsonString);
            this.createNode(null, json, contentCreator);
        } catch (JSONException je) {
            throw (IOException) new IOException(je.getMessage()).initCause(je);
        }
    }

    protected void createNode(String name, JSONObject obj, ContentCreator contentCreator)
    throws JSONException, RepositoryException {
        Object primaryTypeObj = obj.opt("jcr:primaryType");
        String primaryType = null;
        if (primaryTypeObj != null) {
            primaryType = String.valueOf(primaryTypeObj);
        }

        String[] mixinTypes = null;
        Object mixinsObject = obj.opt("jcr:mixinTypes");
        if (mixinsObject instanceof JSONArray) {
            JSONArray mixins = (JSONArray) mixinsObject;
            mixinTypes = new String[mixins.length()];
            for (int i = 0; i < mixins.length(); i++) {
                mixinTypes[i] = mixins.getString(i);
            }
        }

        contentCreator.createNode(name, primaryType, mixinTypes);

        // add properties and nodes
        JSONArray names = obj.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            final String n = names.getString(i);
            // skip well known objects
            if (!ignoredNames.contains(n)) {
                Object o = obj.get(n);
                if (o instanceof JSONObject) {
                    this.createNode(n, (JSONObject) o, contentCreator);
                } else {
                    this.createProperty(n, o, contentCreator);
                }
            }
        }
        contentCreator.finishNode();
    }

    protected void createProperty(String name, Object value, ContentCreator contentCreator)
    throws JSONException, RepositoryException {
        // assume simple value
        if (value instanceof JSONArray) {
            // multivalue
            final JSONArray array = (JSONArray) value;
            if (array.length() > 0) {
                final String values[] = new String[array.length()];
                for (int i = 0; i < array.length(); i++) {
                    values[i] = array.get(i).toString();
                }
                final int propertyType = getType(name, values[0]);
                contentCreator.createProperty(getName(name), propertyType, values);
            } else {
                contentCreator.createProperty(getName(name), PropertyType.STRING, new String[0]);
            }

        } else {
            // single value
            final int propertyType = getType(name, value);
            contentCreator.createProperty(getName(name), propertyType, value.toString());
        }
    }

    protected int getType(String name, Object object) {
        if (object instanceof Double || object instanceof Float) {
            return PropertyType.DOUBLE;
        } else if (object instanceof Number) {
            return PropertyType.LONG;
        } else if (object instanceof Boolean) {
            return PropertyType.BOOLEAN;
        } else if (object instanceof String) {
            if (name.startsWith(REFERENCE)) return PropertyType.REFERENCE;
            if (name.startsWith(PATH)) return PropertyType.PATH;
        }

        // fall back to default
        return PropertyType.STRING;
    }

    protected String getName(String name) {
        if (name.startsWith(REFERENCE)) return name.substring(REFERENCE.length());
        if (name.startsWith(PATH)) return name.substring(PATH.length());
        return name;
    }

    private String toString(InputStream ins) throws IOException {
        if (!ins.markSupported()) {
            ins = new BufferedInputStream(ins);
        }

        String encoding;
        ins.mark(5);
        int c = ins.read();
        if (c == '#') {
            // character encoding following
            StringBuffer buf = new StringBuffer();
            for (c = ins.read(); !Character.isWhitespace((char) c); c = ins.read()) {
                buf.append((char) c);
            }
            encoding = buf.toString();
        } else {
            ins.reset();
            encoding = "UTF-8";
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int rd;
        while ( (rd = ins.read(buf)) >= 0) {
            bos.write(buf, 0, rd);
        }
        bos.close(); // just to comply with the contract

        return new String(bos.toByteArray(), encoding);
    }
}
