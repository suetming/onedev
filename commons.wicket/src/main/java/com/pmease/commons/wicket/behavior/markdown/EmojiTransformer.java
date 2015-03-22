package com.pmease.commons.wicket.behavior.markdown;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;

import com.google.common.collect.ImmutableSet;
import com.pmease.commons.markdown.extensionpoint.HtmlTransformer;
import com.pmease.commons.util.JsoupUtils;
import com.pmease.commons.util.TextNodeVisitor;

public class EmojiTransformer implements HtmlTransformer {

	private static final Collection<String> IGNORED_TAGS = ImmutableSet.of("pre", "code", "tt", "img");
	
	private static final Pattern PATTERN = Pattern.compile("(?<=(^|\\s+))\\:([^\\s\\:]+)\\:(?=($|\\s+))");

	@Override
	public Element transform(Element body) {
		TextNodeVisitor visitor = new TextNodeVisitor() {
			
			@Override
			protected boolean isApplicable(TextNode node) {
				if (JsoupUtils.hasAncestor(node, IGNORED_TAGS))
					return false;
				
				return node.getWholeText().contains(":"); // fast scan here, do pattern match later
			}
		};
		
		NodeTraversor traversor = new NodeTraversor(visitor);
		traversor.traverse(body);
		
		for (TextNode node : visitor.getMatchedNodes()) {
			StringBuffer buffer = new StringBuffer();
			Matcher matcher = PATTERN.matcher(node.getWholeText());
			while (matcher.find()) {
				String emojiName = matcher.group(2);
				String emojiTag;
				if (RequestCycle.get() != null) {
					String emojiCode = EmojiOnes.getInstance().all().get(emojiName);
					if (emojiCode != null) {
						CharSequence emojiUrl = RequestCycle.get().urlFor(new PackageResourceReference(
								EmojiOnes.class, "emoji/" + emojiCode + ".png"), new PageParameters());
						emojiTag = String.format("<img src='%s' title='%s' alt='%s' class='emoji'></img>", 
								emojiUrl, emojiName, emojiName, emojiName);
					} else {
						emojiTag = emojiName;
					}
				} else {
					emojiTag = emojiName;
				}
				matcher.appendReplacement(buffer, matcher.group(1) + emojiTag + matcher.group(3));
			}
			matcher.appendTail(buffer);
			
			DataNode newNode = new DataNode(buffer.toString(), node.baseUri());
			node.replaceWith(newNode);
		}
		
		return body;
	}

}