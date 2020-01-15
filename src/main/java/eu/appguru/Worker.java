package eu.appguru;

import static eu.appguru.Manager.QUEUED_MANAGER;
import static eu.appguru.Manager.RUNNING_THREADS;
import static eu.appguru.Manager.checkQueue;
import javax.swing.JPanel;

/**
 *
 * @author lars
 */
public class Worker extends Thread {
    private static int ID = 1;
    private final JPanel status;
    private final DangerousRunnable task;
    public Worker(String name, JPanel status, DangerousRunnable task) {
        super();
        super.setName("DUMB "+name+" Worker #"+Integer.toString(ID));
        this.status = status;
        this.task = task;
        ID++;
    }
    @Override
    public void run() {
        GUI.setLabel(status, "Waiting");
        while (RUNNING_THREADS < 0 || QUEUED_MANAGER != null) {
            Manager.checkQueue();
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                DUMB.LOGGER.debug("Sleep interruption", ex);
            }
        }
        GUI.setLabel(status, "Running");
        RUNNING_THREADS++;
        try {
            task.run();
            GUI.setButton(status, "Completed");
        } catch (Exception e) {
            DUMB.LOGGER.error(super.getName()+" failed", e);
            GUI.setButton(status, "Failed");
        } finally {
            RUNNING_THREADS--;
            checkQueue();
        }
    }
}
