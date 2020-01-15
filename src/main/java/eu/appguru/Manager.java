package eu.appguru;

import java.io.File;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.OfficeException;
import org.jodconverter.office.OfficeManager;
import org.jodconverter.office.OfficeUtils;

/**
 *
 * @author lars
 */
public class Manager {

    public static volatile int RUNNING_THREADS = 0;
    private static volatile OfficeManager OFFICE_MANAGER;
    public static volatile OfficeManager QUEUED_MANAGER;

    public static void checkQueue() {
        if (RUNNING_THREADS == 0 && QUEUED_MANAGER != null) {
            RUNNING_THREADS = -1;
            synchronized (OFFICE_MANAGER) {
                free();
                OFFICE_MANAGER = QUEUED_MANAGER;
                try {
                    OFFICE_MANAGER.start();
                } catch (OfficeException e) {

                } finally {
                    RUNNING_THREADS = 0;
                }
            }
        }
    }

    public static void init() {
        RUNNING_THREADS = -1;
        OFFICE_MANAGER = LocalOfficeManager.builder().install().build();
        try {
            OFFICE_MANAGER.start();
            RUNNING_THREADS = 0;
        } catch (OfficeException ex) {
            DUMB.LOGGER.error("Initialising office manager failed", ex);
        }
    }

    public static void setOfficeHome(File f) {
        LocalOfficeManager.Builder builder = LocalOfficeManager.builder().install();
        if (f != null) {
            builder = builder.officeHome(f);
        }
        QUEUED_MANAGER = builder.build();
        checkQueue();
    }

    public static void free() {
        try {
            OfficeUtils.stopQuietly(OFFICE_MANAGER);
        } catch (Exception e) {
            DUMB.LOGGER.error("Stopping office manager failed", e);
        }
    }
}
