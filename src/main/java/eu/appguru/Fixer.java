package eu.appguru;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 *
 * @author lars
 */
public class Fixer {
    public static double CM(String s) {
        return Double.parseDouble(s.substring(0, s.length()-2));
    }
    public static String CM(double v) {
        return Double.toString(v)+"cm";
    }
    
    public static void fix(File in, File out, Map<String, Object> attributes) {
        Document doc;
        try (FileInputStream fis=new FileInputStream(in)) {
            doc = Jsoup.parse(fis, "UTF-8", "", Parser.xmlParser());
        } catch (Exception e) {
            DUMB.LOGGER.error("Reading FODG to fix failed", e);
            return;
        }
        int object_diff = 0;
        boolean background_found = false;
        Element background = null;
        String background_op = (String)attributes.get("background");
        if (background_op == null) {
            background_op = "fill";
        }
        for (Element e:doc.getAllElements()) {
            if (e.tagName().equals("draw:frame") || e.tagName().equals("draw:polygon")) {
                if (!e.getElementsByTag("text:span").isEmpty() && (e.hasText() || !e.getElementsByTag("text:s").isEmpty())) {
                    e.remove();
                    object_diff--;
                } else if (!background_found && e.tagName().equals("draw:polygon")) {
                    background_found = true;
                    background = e;
                    if (background_op.equals("kill")) {
                        background = null;
                        e.remove();
                        object_diff--;
                    }
                }
            }
        }
        Element layout = doc.getElementsByTag("style:page-layout-properties").get(0);
        HashMap<String, Double> old_offsets = new HashMap();
        HashMap<String, Double> offsets = new HashMap();
        for (String o:new String[] {"left", "top", "right", "bottom"}) {
            old_offsets.put(o, CM(layout.attr("fo:margin-"+o)));
            if (attributes.containsKey("margin-"+o)) {
                layout.attr("fo:margin-"+o, CM((double)attributes.get("margin-"+o)));
            }
            offsets.put(o, CM(layout.attr("fo:margin-"+o)));
        }
        if (attributes.containsKey("width")) {
            layout.attr("fo:page-width", CM((double)attributes.get("width")));
        }
        double desired_width = CM(layout.attr("fo:page-width"))-CM(layout.attr("fo:margin-left"))-CM(layout.attr("fo:margin-right"));
        List<Element> last_styles = new ArrayList();
        last_styles.addAll(doc.getElementsByTag("style:style"));
        for (int i=last_styles.size()-1; i > -1; i--) {
            Element style = last_styles.get(i);
            if (style.attr("style:name").startsWith("gr")) {
                last_styles = last_styles.subList(i-1, i+1);
                break;
            }
        }
        for (Element style:last_styles) {
            String name = style.attr("style:name");
            for (Element e:doc.getElementsByAttributeValue("draw:style-name", name)) {
                object_diff--;
                e.remove();
            }
        }
        double[] min_c = {Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max_c = {Double.MIN_VALUE, Double.MIN_VALUE};
        String cs = "x";
        String dim = "svg:width";
        for (byte c=0; c < 2; c++) {
            for (String n:new String[] {"", "1", "2"}) {
                String attr = "svg:"+cs+n;
                for (Element e:doc.getElementsByAttribute(attr)) {
                    if (e == background) {
                        continue;
                    }
                    double d = e.hasAttr(dim) ? CM(e.attr(dim)):0;
                    double v = CM(e.attr(attr));
                    if (v > max_c[c]) {
                        max_c[c] = v;
                    }
                    if (v < min_c[c]) {
                        min_c[c] = v;
                    }
                }
            }
            cs = "y";
            dim = "svg:height";
        }
                System.out.println(min_c[0]);
        System.out.println(min_c[1]);
        System.out.println(max_c[0]);
        System.out.println(max_c[1]);
        double current_width = max_c[0] - min_c[0];
        double current_height = max_c[1] - min_c[1];
        double scale = desired_width/current_width;
        System.out.println(scale);
        double new_height = current_height*scale;
        layout.attr("fo:page-height", CM(new_height+offsets.get("bottom")+offsets.get("top")));
        Function<Double, Double> convertX = x -> (x-min_c[0])*scale+offsets.get("left");
        Function<Double, Double> convertY = y -> (y-min_c[1])*scale+offsets.get("top");
        Function<Double, Double> convertW = w -> w*scale;
        Function<Double, Double> convertH = h -> h*scale;
        Map<String, Function<Double, Double>> operations = new HashMap();
        Function<Double, Double> op = convertX;
        for (String c:new String[] {"x", "y"}) {
            for (String n:new String[] {"", "1", "2"}) {
                operations.put("svg:"+c+n, op);
            }
            op = convertY;
        }
        for (String attr:new String[] {"width", "stroke-width", "padding-left", "padding-right"}) {
            operations.put("svg:"+attr, convertW);
        }
        for (String attr:new String[] {"height", "padding-top", "padding-bottom"}) {
            operations.put("svg:"+attr, convertH);
        }
        for (String attr:operations.keySet()) {
            for (Element e:doc.getElementsByAttribute(attr)) {
                if (e != background) {
                    e.attr(attr, CM(operations.get(attr).apply(CM(e.attr(attr)))));
                }
            }
        }
        
        Function<Double, Double>[] view_box_operations = new Function[] {convertX, convertY, convertW, convertH};
        for (Element e:doc.getElementsByAttribute("svg:viewBox")) {
            if (e == background) {
                continue;
            }
            String[] view_box = e.attr("svg:viewBox").split(" ");
            for (int i=0; i < view_box.length; i++) {
                view_box[i] = Double.toString(view_box_operations[i].apply(Double.parseDouble(view_box[i])));
            }
            e.attr("svg:viewBox", String.join(" ", view_box));
        }
        
        for (Element e:doc.getElementsByAttribute("draw:points")) {
            if (e == background) {
                continue;
            }
            String[] points = e.attr("draw:points").split(" ");
            for (int i=0; i < points.length; i++) {
                String[] point = points[i].split(",");
                point[0] = Double.toString(convertX.apply(Double.parseDouble(point[0])));
                point[1] = Double.toString(convertY.apply(Double.parseDouble(point[1])));
                points[i] = String.join(",", point);
            }
            e.attr("draw:points", String.join(" ", points));
        }
        
        if (background != null) {
            if (background_op.equals("margin")) {
                background.attr("svg:x", CM(offsets.get("left")));
                background.attr("svg:y", CM(offsets.get("top")));
                background.attr("svg:width", CM(new_height));
                background.attr("svg:height", CM(desired_width));
            } else {
                background.attr("svg:x", CM(0));
                background.attr("svg:y", CM(0));
                background.attr("svg:width", layout.attr("fo:page-height"));
                background.attr("svg:height", layout.attr("fo:page-width"));
            }
        }

        Element stats = doc.getElementsByTag("meta:document-statistic").get(0);
        stats.attr("meta:object-count", Integer.toString(Integer.parseInt(stats.attr("meta:object-count"))+object_diff));
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
            writer.write(doc.html());
        } catch (Exception e) {
            DUMB.LOGGER.error("Writing fixed FODG failed", e);
        }
    }
}
