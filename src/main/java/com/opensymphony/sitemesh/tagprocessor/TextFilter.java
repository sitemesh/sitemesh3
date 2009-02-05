package com.opensymphony.sitemesh.tagprocessor;

/**
 * TextFilters can be added to the {@link TagProcessor} (or specific {@link State}s) to
 * allow a simple means of filtering text content.
 * <p>More than one TextFilter may be added and they will be called in the order they were added.</p>
 *
 * @author Joe Walnes
 */
public interface TextFilter {

    String filter(String content);

}
