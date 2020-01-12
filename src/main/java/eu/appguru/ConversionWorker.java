package eu.appguru;

import java.io.File;
import org.jodconverter.office.OfficeException;

/**
 *
 * @author lars
 */
public class ConversionWorker implements DangerousRunnable {
    private final File in;
    private final File out;
    
    public ConversionWorker(File in, File out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() throws OfficeException {
        Converter.convert(in, out);
    }
}
