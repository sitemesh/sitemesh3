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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controls how {@link WebAppContext#dispatch} hands the request off to the
 * decorator resource.
 *
 * <p>The choice is a genuine trade-off, so it is exposed as a setting rather
 * than hard-coded:</p>
 * <ul>
 *   <li>{@link #FORWARD} &mdash; {@code RequestDispatcher.forward()}. The
 *       decorator's {@code setDateHeader}/{@code Last-Modified} reaches
 *       SiteMesh's response buffer, so conditional-GET works. <em>But</em>
 *       Tomcat 11+ defaults {@code Context.suspendWrappedResponseAfterForward}
 *       to {@code true}, which unwraps SiteMesh's response wrapper during the
 *       forward and commits a blank response.</li>
 *   <li>{@link #INCLUDE} &mdash; {@code RequestDispatcher.include()}. Safe on
 *       every container (it never commits the wrapper), but Servlet include
 *       semantics silently drop the decorator's {@code Last-Modified}.</li>
 *   <li>{@link #DETECT} &mdash; choose per container: {@code include()} on
 *       Tomcat 11 and later (where {@code forward()} is unsafe), {@code
 *       forward()} everywhere else. This is the default.</li>
 * </ul>
 */
public enum DispatchMode {

    /** Always dispatch with {@code RequestDispatcher.forward()}. */
    FORWARD,

    /** Always dispatch with {@code RequestDispatcher.include()}. */
    INCLUDE,

    /**
     * Pick {@code include()} on Tomcat 11+ and {@code forward()} elsewhere,
     * based on {@link ServletContext#getServerInfo()}. The default.
     */
    DETECT;

    /**
     * Matches the major version in Tomcat's server-info string, e.g.
     * {@code "Apache Tomcat/11.0.15"} &rarr; {@code 11}. Deliberately keyed on
     * the major number rather than an exact {@code "11"} prefix so Tomcat 12
     * and later (which keep the {@code suspendWrappedResponseAfterForward}
     * default) are handled too.
     */
    private static final Pattern TOMCAT_MAJOR_VERSION = Pattern.compile("Apache Tomcat/(\\d+)");

    /**
     * Parse {@code include}/{@code forward}/{@code detect} (case-insensitive,
     * surrounding whitespace ignored) into a {@link DispatchMode}, returning
     * {@code fallback} for {@code null}, blank, or unrecognised values. Shared
     * by every configuration entry point (XML, properties, Spring) so the
     * lenient-parsing behaviour stays consistent.
     *
     * @param value The configured value to parse (may be null).
     * @param fallback The mode to return when the value cannot be parsed.
     * @return The parsed mode, or {@code fallback}.
     */
    public static DispatchMode fromString(String value, DispatchMode fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Resolve this mode to a concrete include/forward decision for the given
     * container. {@link #DETECT} consults
     * {@link ServletContext#getServerInfo()}.
     *
     * @param servletContext The servlet context of the running container (may be null).
     * @return {@code true} to dispatch with {@code include()}, {@code false}
     *         to dispatch with {@code forward()}.
     */
    public boolean useInclude(ServletContext servletContext) {
        return switch (this) {
            case INCLUDE -> true;
            case FORWARD -> false;
            case DETECT -> isTomcat11OrLater(servletContext);
        };
    }

    /**
     * @return {@code true} if {@code servletContext} reports a Tomcat 11+
     *         server, where {@code forward()} unwraps SiteMesh's response
     *         wrapper and must be avoided.
     */
    private static boolean isTomcat11OrLater(ServletContext servletContext) {
        if (servletContext == null) {
            return false;
        }
        String serverInfo = servletContext.getServerInfo();
        if (serverInfo == null) {
            return false;
        }
        Matcher matcher = TOMCAT_MAJOR_VERSION.matcher(serverInfo);
        if (!matcher.find()) {
            return false;
        }
        try {
            return Long.parseLong(matcher.group(1)) >= 11;
        } catch (NumberFormatException e) {
            // An absurdly long digit run (not a real Tomcat version); treat as
            // "not Tomcat 11+" rather than aborting decorator dispatch.
            return false;
        }
    }
}
