package com.opensymphony.sitemesh.decorator.jsp;

import com.opensymphony.sitemesh.Content;

import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

/**
 * JSP 2 tag that write out the a property of the {@link Content} to the page.
 *
 * <p>Optionally, this tag may contain a body that will be written out if
 * the property does not exist.</p>
 *
 * @author Joe Walnes
 */
public class ContentTag extends SimpleTagSupport {

    private String propertyName;

    public void setProperty(String property) {
        this.propertyName = property;
    }

    @Override
    public void doTag() throws JspException, IOException {
        JspContext jspContext = getJspContext();
        Content content = (Content) jspContext.getAttribute(Content.class.getName(), PageContext.REQUEST_SCOPE);

        // If the property is found...
        if (content != null) {
            Content.Property property = content.getProperty(propertyName);
            if (property.exists()) {
                // Write it...
                property.writeTo(jspContext.getOut());
                return;
            }
        }

        // Otherwise, write the tag's body as the default...
        JspFragment body = getJspBody();
        if (body != null) {
            body.invoke(jspContext.getOut());
        }
    }

    /**
     * Convenient <code>&lt;sitemesh:title&gt;</code> tag.
     * Equivalent to &lt;sitemesh:content property='title'&gt;</code>.
     */
    public static class TitleTag extends ContentTag {
        public TitleTag() {
            setProperty("title");
        }
    }

    /**
     * Convenient <code>&lt;sitemesh:head&gt;</code> tag.
     * Equivalent to &lt;sitemesh:content property='head'&gt;</code>.
     */
    public static class HeadTag extends ContentTag {
        public HeadTag() {
            setProperty("head");
        }
    }

    /**
     * Convenient <code>&lt;sitemesh:body&gt;</code> tag.
     * Equivalent to &lt;sitemesh:content property='body'&gt;</code>.
     */
    public static class BodyTag extends ContentTag {
        public BodyTag() {
            setProperty("body");
        }
    }

}
