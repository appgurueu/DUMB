package eu.appguru;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.jodconverter.office.OfficeException;

/**
 *
 * @author lars
 */
public class FixingWorker implements DangerousRunnable {
    private final File in;
    private final File out;
    private final Map<String, Object> attrs;
    
    public FixingWorker(File in, File out, Map<String, Object> attrs) {
        this.in = in;
        this.out = out;
        this.attrs = attrs;
    }
    @Override
    public void run() throws IOException, OfficeException {
        Converter.convertAndFix(in, out, attrs);
    }
}
