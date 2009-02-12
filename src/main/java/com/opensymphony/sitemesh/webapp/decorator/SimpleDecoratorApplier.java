package com.opensymphony.sitemesh.webapp.decorator;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.DecoratorApplier;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very simple {@link DecoratorApplier} implementation that is passed a template and will
 * substitute tokens that look like <code>{{xxx}}</code> for the equivalent {@link Content}
 * properties. The functionality of this is very limited and is mostly used for quick
 * examples and tests.
 *
 * @author Joe Walnes
 */
public class SimpleDecoratorApplier implements DecoratorApplier {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([\\w\\.\\-]+)\\}\\}");

    private final Node parsedTemplate;

    public SimpleDecoratorApplier(CharSequence template) {
        parsedTemplate = parse(template, TOKEN_PATTERN);
    }

    @Override
    public boolean decorate(Content content, Context context) throws IOException {
        parsedTemplate.render(content, context.getWriter());
        return true;
    }

    /**
     * Parse the template into an AST. This is done once at construction time, and the AST
     * is reused each time the decorator is applied. Makes it pretty quick.
     *
     * Actually the template capabilities are so basic, that this could technically be represented
     * as a list instead of a tree. However the tree strucuture will make it trivial to plug in
     * additional constructs such as conditionals, loops, etc, if ever needed.
     */
    private Node parse(CharSequence template, Pattern tokenPattern) {
        Matcher matcher = tokenPattern.matcher(template);
        ContainerNode containerNode = new ContainerNode();
        int last = 0;
        while (matcher.find()) {
            containerNode.add(new TextNode(template.subSequence(last, matcher.start())));
            containerNode.add(new PropertyNode(matcher.group(1)));
            last = matcher.end();
        }
        if (last < template.length()) {
            containerNode.add(new TextNode(template.subSequence(last, template.length())));
        }
        return containerNode;
    }

    // -- Abstract Syntax Tree --

    private static interface Node {
        void render(Content content, PrintWriter out) throws IOException;
    }

    private static class ContainerNode implements Node {
        private final List<Node> children = new ArrayList<Node>();

        @Override
        public void render(Content content, PrintWriter out) throws IOException {
            for (Node child : children) {
                child.render(content, out);
            }
        }

        public void add(Node child) {
            children.add(child);
        }
    }

    private static class TextNode implements Node {
        private final CharSequence text;

        public TextNode(CharSequence text) {
            this.text = text;
        }

        @Override
        public void render(Content content, PrintWriter out) throws IOException {
            out.append(text);
        }
    }

    private static class PropertyNode implements Node {
        private final String name;

        public PropertyNode(String name) {
            this.name = name;
        }

        @Override
        public void render(Content content, PrintWriter out) throws IOException {
            Content.Property property = content.getProperty(name);
            property.writeTo(out);
        }
    }
}
