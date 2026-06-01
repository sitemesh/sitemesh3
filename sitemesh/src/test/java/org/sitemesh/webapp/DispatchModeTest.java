/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp;

import jakarta.servlet.ServletContext;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;

/**
 * Unit tests for {@link DispatchMode}, in particular that {@link
 * DispatchMode#DETECT} keys on the Tomcat <em>major</em> version (so 12+ is
 * handled, not just an exact "11" match) and falls back to forward elsewhere.
 */
public class DispatchModeTest extends TestCase {

    /** A {@link ServletContext} that only answers {@code getServerInfo()}. */
    private static ServletContext withServerInfo(final String serverInfo) {
        return (ServletContext) Proxy.newProxyInstance(
                DispatchModeTest.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> "getServerInfo".equals(method.getName()) ? serverInfo : null);
    }

    public void testIncludeAlwaysUsesInclude() {
        assertTrue(DispatchMode.INCLUDE.useInclude(withServerInfo("Apache Tomcat/11.0.15")));
        assertTrue(DispatchMode.INCLUDE.useInclude(withServerInfo("jetty/12.1.8")));
        assertTrue(DispatchMode.INCLUDE.useInclude(null));
    }

    public void testForwardAlwaysUsesForward() {
        assertFalse(DispatchMode.FORWARD.useInclude(withServerInfo("Apache Tomcat/11.0.15")));
        assertFalse(DispatchMode.FORWARD.useInclude(withServerInfo("jetty/12.1.8")));
        assertFalse(DispatchMode.FORWARD.useInclude(null));
    }

    public void testDetectUsesIncludeOnTomcat11AndLater() {
        assertTrue(DispatchMode.DETECT.useInclude(withServerInfo("Apache Tomcat/11.0.15")));
        assertTrue("Tomcat 12 must also use include",
                DispatchMode.DETECT.useInclude(withServerInfo("Apache Tomcat/12.0.0")));
        assertTrue("future Tomcat majors must use include",
                DispatchMode.DETECT.useInclude(withServerInfo("Apache Tomcat/27.4.1")));
    }

    public void testDetectUsesForwardOnOlderTomcatAndOtherContainers() {
        assertFalse("Tomcat 10 forwards fine", DispatchMode.DETECT.useInclude(withServerInfo("Apache Tomcat/10.1.30")));
        assertFalse(DispatchMode.DETECT.useInclude(withServerInfo("jetty/12.1.8")));
        assertFalse(DispatchMode.DETECT.useInclude(withServerInfo("Eclipse GlassFish 7.0.0")));
        assertFalse(DispatchMode.DETECT.useInclude(withServerInfo(null)));
        assertFalse(DispatchMode.DETECT.useInclude(null));
    }
}
