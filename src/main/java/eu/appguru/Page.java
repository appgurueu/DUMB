package eu.appguru;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author lars
 */
public class Page {
    public List<Element> content;
    public double scale;
    public double height;

    public Page(List<Element> content, double scale, double height) {
        this.content = content;
        this.scale = scale;
        this.height = height;
    }
    
    public void scaleStyles(Document doc, int page_num) {
        Map<String, String> scaled = new HashMap();
        Function<Element, Element> scale_style = new Function<Element, Element>() {
            @Override
            public Element apply(Element style) {
                String style_name = style.attr("style:name");
                if (scaled.containsKey(style_name)) {
                    return doc.getElementsByAttributeValue("style:name", scaled.get(style_name)).get(0);
                }
                Element new_style = style.clone();
                String new_style_name_proposal = style_name + "sc" + page_num;
                String new_style_name = new_style_name_proposal;
                int unique_part = 0;
                while (!doc.getElementsByAttributeValue("style:name", new_style_name).isEmpty()) {
                    new_style_name = new_style_name_proposal + "u" + unique_part;
                    unique_part++;
                }
                new_style.attr("style:name", new_style_name);
                // Scaling stuff
                /* Ignoring for now: "fo:padding-top", "fo:padding-bottom", "fo:padding-left", "fo:padding-right" */
                for (String attr:new String[] {"svg:stroke-width", "fo:font-size", "style:font-size-asian", "style:font-size-complex"}) {
                    for (Element el:new_style.getElementsByAttribute(attr)) {
                        Unit u = new Unit(el.attr(attr));
                        u.value *= scale;
                        el.attr(attr, u.toString());
                    }
                }
                if (new_style.hasAttr("style:parent-style-name")) {
                    new_style.attr("style:parent-style-name", this.apply(doc.getElementsByAttributeValue("style:name",
                        new_style.attr("style:parent-style-name")).get(0)).attr("style:name"));
                }
                style.after(new_style);
                scaled.put(style_name, new_style_name);
                return new_style;
            }
        };
        for (Element p:content) {
            for (String style_attr_name: new String[] {"draw:style-name", "draw:text-style-name", "text:style-name"}) {
                for (Element e:p.getElementsByAttribute(style_attr_name)) {
                    String style_name = e.attr(style_attr_name);
                    if (!scaled.containsKey(style_name)) {
                        Element style = doc.getElementsByAttributeValue("style:name", style_name).get(0);
                        Element new_style = scale_style.apply(style);
                        e.attr(style_attr_name, new_style.attr("style:name"));
                    } else {
                        e.attr(style_attr_name, scaled.get(style_name));
                    }
                }
            }
        }
    }
}
