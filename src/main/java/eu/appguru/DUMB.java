package eu.appguru;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 *
 * @author lars
 */
public class DUMB {
    
    static {
        System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
        System.setProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true");
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    }
    
    public static Logger LOGGER = LoggerFactory.getLogger(DUMB.class);;

    public static Map<String, Object> readArguments(String[] args) {
        if (args.length % 2 != 0) {
            return null;
        }
        HashMap<String, Object> arguments = new HashMap();
        for (int i=0; i < args.length; i+=2) {
            arguments.put(args[i], args[i+1]);
        }
        for (String key: new String[] {"operation", "source", "destination"}) {
            if (!arguments.containsKey(key)) {
                return null;
            }
        }
        String operation = ((String)arguments.get("operation")).toLowerCase();
        arguments.put("operation", operation);
        if (!operation.equals("fix") && !operation.equals("convert")) {
            return null;
        }
        if (arguments.containsKey("background")) {
            String background = ((String)arguments.get("background")).toLowerCase();
            if (!background.equals("fill") && !background.equals("kill") && !background.equals("margin")) {
                return null;
            }
            arguments.put("background", background);
        }
        for (String num:new String[] {"width", "margin", "margin-left", "margin-right", "margin-top", "margin-bottom"}) {
            if (arguments.containsKey(num)) {
                String val = (String)arguments.get(num);
                try {
                    arguments.put(num, Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return arguments;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Thread.currentThread().setName("DUMB");
        Map<String, Object> arguments = readArguments(args);
        if (arguments == null) {
            try {
                GUI gui = new GUI();
                PrintStream true_out = System.out;
                PrintStream new_out = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int i) throws IOException {
                        true_out.write(i);
                        if (i >= 0) {
                            gui.log.setText(gui.log.getText()+(char)i);
                        }
                    }
                });
                System.setOut(new_out);
            } catch (Exception e) {
                LOGGER.error("Opening GUI failed", e);
            }
            try {
                Manager.init();
            } catch (Exception e) {
                LOGGER.error("Starting office manager failed", e);
            }
        }
        System.setErr(System.out);
        if (arguments != null) {
            try {
                String op = (String)arguments.get("operation");
                String oh = (String)arguments.get("officehome");
                Manager.setOfficeHome(oh == null ? new File(oh):null);
                if (op.equals("fix")) {
                    Converter.convertAndFix(new File((String)arguments.get("source")), new File((String)arguments.get("destination")), arguments);
                } else {
                    Converter.convert(new File((String)arguments.get("source")), new File((String)arguments.get("destination")));
                }
            } catch (Exception e) {
                LOGGER.error("Executing command "+arguments.toString()+" failed", e);
            } finally {
                Manager.free();
            }
        }
    }
}
