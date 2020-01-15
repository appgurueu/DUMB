package eu.appguru;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.jodconverter.JodConverter;
import org.jodconverter.office.OfficeException;

/**
 *
 * @author lars
 */
public class Converter {
    public static void convert(File in, File out) throws OfficeException {
        JodConverter.convert(in).to(out).execute();
    }
    public static void convertAndFix(File in, File out, Map<String, Object> attrs) throws IOException, OfficeException {
        String in_type = FilenameUtils.getExtension(in.getName()).toLowerCase();
        String out_type = FilenameUtils.getExtension(out.getName()).toLowerCase();
        File tmp_in = in_type.equals("fodg") ? in:File.createTempFile("DUMB-tmp-in-", FilenameUtils.EXTENSION_SEPARATOR_STR+"fodg");
        File tmp_out = out_type.equals("fodg") ? out:File.createTempFile("DUMB-tmp-out-", FilenameUtils.EXTENSION_SEPARATOR_STR+"fodg");
        if (in != tmp_in) {
            convert(in, tmp_in);
        }
        Fixer.fix(tmp_in, tmp_out, attrs);
        if (in != tmp_in) {
            tmp_in.delete();
        }
        if (out != tmp_out) {
            convert(tmp_out, out);
            tmp_out.delete();
        }
    }
}
