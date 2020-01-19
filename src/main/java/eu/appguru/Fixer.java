package eu.appguru;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 *
 * @author lars
 */
public class Fixer {

    public static void write(Document doc, File out) {
        doc.getElementsByAttribute("meta:object-count").get(0).attr("meta:object-count", Integer.toString(doc.getElementsByTag("draw:page").get(0).children().size()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
            writer.write(doc.html());
            writer.close();
        } catch (Exception e) {
            DUMB.LOGGER.error("Writing fixed FODG failed", e);
        }
    }

    public static void fix(File in, File out, Map<String, Object> attributes) {
        Document doc;
        try (FileInputStream fis = new FileInputStream(in)) {
            doc = Jsoup.parse(fis, "UTF-8", "", Parser.xmlParser());
        } catch (Exception e) {
            DUMB.LOGGER.error("Reading FODG to fix failed", e);
            return;
        }

        final String background_op = attributes.containsKey("background") ? ((String) attributes.get("background")) : "fill";

        List<Page> pages = new ArrayList();
        
        // Remove view configuration
        for (Element e : doc.getElementsByTag("config:config-item-set")) {
            if (e.attr("config:name").equals("ooo:view-settings")) {
                e.remove();
                break;
            }
        }

        // Find first graphic style
        String first_style = null;
        for (Element e : doc.getElementsByTag("style:style")) {
            if (e.attr("style:family").equalsIgnoreCase("graphic") && e.attr("style:name").startsWith("gr")) {
                first_style = e.attr("style:name");
                break;
            }
        }

        String text_style = null;
        // Remove date and other text (which is not part of the pages)
        for (Element e : doc.getElementsByTag("draw:page").get(0).children()) {
            if (e.tagName().equalsIgnoreCase("draw:frame")) {
                if (e.attr("draw:style-name").equals(first_style) && !e.getElementsByTag("text:span").isEmpty() && (e.hasText() || !e.getElementsByTag("text:s").isEmpty())) {
                    text_style = e.attr("draw:text-style-name");
                    e.remove();
                }
            }
        }

        text_style = doc.getElementsByAttributeValue("style:name", text_style).get(0).nextElementSibling().attr("style:name");
        
        for (Element e : doc.getElementsByTag("draw:page").get(0).children()) {
            if (e.tagName().equalsIgnoreCase("draw:frame")) {
                if (!e.getElementsByTag("text:span").isEmpty() && (e.hasText() || !e.getElementsByTag("text:s").isEmpty()) && e.attr("draw:text-style-name").equals(text_style)) {
                    e.remove();
                }
            }
        }

        // Find border lines & move text at the end of the document up
        List<Element> last_page = new ArrayList();
        Elements drawing = doc.getElementsByTag("draw:page").get(0).children();
        int borders = drawing.size() - 1;
        for (; borders > -1; borders--) {
            Element e = drawing.get(borders);
            if (!e.getElementsByTag("text:span").isEmpty() && (e.hasText() || !e.getElementsByTag("text:s").isEmpty())) {
                drawing.remove(borders);
                last_page.add(0, e);
            } else {
                break;
            }
        }

        String[] style_names = new String[3];
        for (int j = 0; j < 3; j++) {
            Element e = drawing.get(borders - j);
            if (!e.tagName().equals("draw:line") && !e.tagName().equals("draw:polygon")) {
                DUMB.LOGGER.error("Reading FODG to fix failed, missing borders", e);
                return;
            }
            style_names[2 - j] = e.attr("draw:style-name");
        }

        Element layout = doc.getElementsByTag("style:page-layout-properties").get(0);
        HashMap<String, Double> old_offsets = new HashMap();
        HashMap<String, Double> offsets = new HashMap();
        for (String o : new String[] {"left", "top", "right", "bottom"}) {
            old_offsets.put(o, new Unit(layout.attr("fo:margin-" + o)).value);
            if (attributes.containsKey("margin-" + o)) {
                layout.attr("fo:margin-" + o, Unit.cm((double) attributes.get("margin-" + o)));
            }
            offsets.put(o, new Unit(layout.attr("fo:margin-" + o)).value);
        }
        if (attributes.containsKey("width")) {
            layout.attr("fo:page-width", Unit.cm((double) attributes.get("width")));
        }
        double desired_width = new Unit(layout.attr("fo:page-width")).value - new Unit(layout.attr("fo:margin-left")).value - new Unit(layout.attr("fo:margin-right")).value;

        Consumer<List<Element>> page_consumer = (List<Element> page_sublist) -> {
            ArrayList<Element> page = new ArrayList();
            page.addAll(page_sublist);

            Element background = null;

            for (int i = 0; i < page.size(); i++) {
                Element e = page.get(i);
                if (e.tagName().equalsIgnoreCase("draw:polygon")) {
                    background = e;
                    if (background_op.equals("kill")) {
                        background = null;
                        page.remove(i);
                    }
                    break;
                }
            }

            double[] min_c = {Double.MAX_VALUE, Double.MAX_VALUE};
            double[] max_c = {Double.MIN_VALUE, Double.MIN_VALUE};
            String cs = "x";
            String dim = "svg:width";
            for (byte c = 0; c < 2; c++) {
                for (String n : new String[]{"", "1", "2"}) {
                    String attr = "svg:" + cs + n;
                    for (Element e : page) {
                        if (!e.hasAttr(attr)) {
                            continue;
                        }
                        if (e == background) {
                            continue;
                        }
                        Unit u = new Unit(e.attr(attr));
                        double v = u.value;
                        for (int i = 0; i < 2; i++) {
                            if (v > max_c[c]) {
                                max_c[c] = v;
                            }
                            if (v < min_c[c]) {
                                min_c[c] = v;
                            }
                            if (!e.hasAttr(dim)) {
                                break;
                            }
                            u = new Unit(e.attr(dim));
                            v += u.value;
                        }
                    }
                }
                cs = "y";
                dim = "svg:height";
            }
            double current_width = max_c[0] - min_c[0];
            double current_height = max_c[1] - min_c[1];
            double scale = desired_width / current_width;
            double new_height = current_height * scale;
            double new_height_total = new_height + offsets.get("bottom") + offsets.get("top");
            Function<Double, Double> convertX = x -> (x - min_c[0]) * scale + offsets.get("left");
            Function<Double, Double> convertY = y -> (y - min_c[1]) * scale + offsets.get("top");
            Function<Double, Double> convertW = w -> w * scale;
            Function<Double, Double> convertH = h -> h * scale;
            Map<String, Function<Double, Double>> operations = new HashMap();
            Function<Double, Double> op = convertX;
            for (String c : new String[]{"x", "y"}) {
                for (String n : new String[]{"", "1", "2"}) {
                    operations.put("svg:" + c + n, op);
                }
                op = convertY;
            }
            for (String attr : new String[]{"width", "stroke-width", "padding-left", "padding-right"}) {
                operations.put("svg:" + attr, convertW);
            }
            for (String attr : new String[]{"height", "padding-top", "padding-bottom"}) {
                operations.put("svg:" + attr, convertH);
            }
            for (String attr : operations.keySet()) {
                for (Element e : page) {
                    if (e != background && e.hasAttr(attr)) {
                        Unit u = new Unit(e.attr(attr));
                        u.value = operations.get(attr).apply(u.value);
                        e.attr(attr, u.toString());
                    }
                }
            }

            if (background != null) {
                if (background_op.equals("margin")) {
                    background.attr("svg:x", Unit.cm(offsets.get("left")));
                    background.attr("svg:y", Unit.cm(offsets.get("top")));
                    background.attr("svg:width", Unit.cm(desired_width));
                    background.attr("svg:height", Unit.cm(new_height));
                } else {
                    background.attr("svg:x", Unit.cm(0));
                    background.attr("svg:y", Unit.cm(0));
                    background.attr("svg:width", layout.attr("fo:page-width"));
                    background.attr("svg:height", Unit.cm(new_height_total));
                }
            }
            pages.add(new Page(page, scale, new_height_total));
        };
        
        List<List<Element>> content_pages = new ArrayList();

        int last_index = 0;
        int index = 0;
        for (int i = 0; i < drawing.size(); i++) {
            String e_style_name = drawing.get(i).attr("draw:style-name");
            if (e_style_name.equals(style_names[index])) {
                index++;
            } else {
                index = 0;
                if (e_style_name.equals(style_names[index])) {
                    index++;
                }
            }

            // New page
            if (index == 3) {
                int new_index = i - index + 1;
                List<Element> sublist = drawing.subList(last_index, new_index);
                if (sublist.size() > 1) {
                    content_pages.add(sublist);
                }
                last_index = i + 1;
                index = 0;
            }
        }

        content_pages.get(content_pages.size()-1).addAll(last_page);
        
        for (List<Element> content_page:content_pages) {
            page_consumer.accept(content_page);
        }
        
        if (pages.size() == 1) {
            Page page = pages.get(0);
            layout.attr("fo:page-height", Unit.cm(page.height));
            Element draw_page = doc.getElementsByTag("draw:page").get(0);
            draw_page.children().remove();
            draw_page.insertChildren(0, page.content);
            page.scaleStyles(doc, 0);
            write(doc, out);
        } else {
            if (attributes.containsKey("pages") && ("merge".equals((String) attributes.get("pages")))) {
                double total_height = 0;
                Element draw_page = doc.getElementsByTag("draw:page").get(0);
                draw_page.children().remove();
                for (int i = 0; i < pages.size(); i++) {
                    Page page = pages.get(i);
                    for (Element e : page.content) {
                        for (String attr : new String[]{"svg:y", "svg:y1", "svg:y2"}) {
                            if (e.hasAttr(attr)) {
                                Unit u = new Unit(e.attr(attr));
                                u.value += total_height;
                                e.attr(attr, u.toString());
                            }
                        }
                        draw_page.appendChild(e);
                    }
                    total_height += page.height;
                    page.scaleStyles(doc, i);
                }
                doc.getElementsByTag("style:page-layout-properties").attr("fo:page-height", Unit.cm(total_height));
                write(doc, out);
            } else {
                if (out.exists()) {
                    out.delete();
                }
                out.mkdir();
                String ext = FilenameUtils.getExtension(out.getName());
                for (int i = 0; i < pages.size(); i++) {
                    Page page = pages.get(i);
                    Document clone = doc.clone();
                    clone.getElementsByTag("style:page-layout-properties").attr("fo:page-height", Unit.cm(page.height));
                    Element draw_page = clone.getElementsByTag("draw:page").get(0);
                    draw_page.children().remove();
                    clone.getElementsByTag("draw:page").get(0).insertChildren(0, page.content);
                    page.scaleStyles(clone, i);
                    write(clone, new File(out.getAbsolutePath() + File.separator + (i + 1) + FilenameUtils.EXTENSION_SEPARATOR_STR + ext));
                }
            }
        }
    }
}
