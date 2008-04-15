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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;

/** Test node move via the SlingPostServlet */
public class PostServletCopyTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-copy-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCopyNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", testPath + "/dest");
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", "src");
        props.put("sling:post:copyDest", "dest");
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeNew() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", "new");
        String newNode = testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
        String content = getContent(newNode + "/new.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeNew2() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", "*");
        String newNode = testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
        String content = getContent(newNode + ".json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeExistingFail() throws IOException {
        final String testPath = TEST_BASE_PATH + "/exist/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", testPath + "/dest");
        try {
            testClient.createNode(HTTP_BASE_URL + testPath, props);
        } catch (IOException ioe) {
            // if we do not get the status code 200 message, fail
            if (!ioe.getMessage().startsWith("Expected status code 302 for POST, got 200, URL=")) {
                throw ioe;
            }
        }

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.text)");
    }

    public void testCopyNodeExistingReplace() throws IOException {
        final String testPath = TEST_BASE_PATH + "/replace/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", testPath + "/dest");
        props.put("sling:post:copyFlags", "replace");  // replace dest
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeDeep() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", "deep/new");
        String newNode = testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
        String content = getContent(newNode + "/deep/new.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeDeepFail() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new_fail/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("sling:post:copySrc", testPath + "/src");
        props.put("sling:post:copyDest", "/some/not/existing/structure");
        try {
            testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
            // not quite correct. should check status response
            fail("Moving node to a 'forgein' locaition should fail.");
        } catch (IOException e) {
            // ignore
        }
    }

 }