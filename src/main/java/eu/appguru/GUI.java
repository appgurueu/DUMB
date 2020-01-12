package eu.appguru;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.HyperlinkEvent;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author lars
 */
public class GUI extends JFrame {
    private JFileChooser source, destination, office_home;
    private String[] laf_lookup;
    public JTextPane log = new JTextPane();
    
    public static void setLabel(JPanel text_and_button, String text) {
        for (Component c:text_and_button.getComponents()) {
            if (c.getClass() == JLabel.class) {
                ((JLabel)c).setText(text);
                c.setVisible(true);
            } else {
                c.setVisible(false);
            }
        }
    }
    
    public static void setButton(JPanel text_and_button, String text) {
        for (Component c:text_and_button.getComponents()) {
            if (c.getClass() == JButton.class) {
                ((JButton)c).setText(text);
                c.setVisible(true);
            } else {
                c.setVisible(false);
            }
        }
    }
    
    public ActionListener ReplaceContentListener(Component new_content, Container container) {
        return (ActionEvent ae) -> {
            for (Component c:container.getComponents()) {
                c.setVisible(c == new_content);
            }
            container.repaint();
        };
    }
    
    public ActionListener ToggleListener(Component to_toggle) {
        return (ActionEvent ae) -> {
            boolean selected = ((JCheckBox)ae.getSource()).isSelected();
            to_toggle.setEnabled(selected);
        };
    }
    
    public JCheckBox Toggle(String title, Component to_toggle) {
        JCheckBox box = new JCheckBox(title);
        box.setSelected(false);
        box.addActionListener(ToggleListener(to_toggle));
        return box;
    }
    
    public void updateComponentTreeUI() {
        SwingUtilities.updateComponentTreeUI(source);
        SwingUtilities.updateComponentTreeUI(destination);
        SwingUtilities.updateComponentTreeUI(office_home);
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    public void changeLook(String lnfName) {
        try {
            UIManager.setLookAndFeel(lnfName);
            updateComponentTreeUI();
        } catch (Exception ex) {
            DUMB.LOGGER.error("Setting theme failed", ex);
        }
    }

    public GUI() throws IOException {
        super("DUMB");
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            DUMB.LOGGER.error("Setting initial theme failed", ex);
        }
        
        try {
            ImageIcon icon = new ImageIcon("res/DUMB_256x256.png");
            super.setIconImage(icon.getImage());
        } catch (Exception e) {
            DUMB.LOGGER.error("Loading icon failed", e);
        }
        
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
                Manager.free();
                System.exit(0);
            }
        });
        
        source = new JFileChooser();
        source.setFileSelectionMode(JFileChooser.FILES_ONLY);
        destination = new JFileChooser();
        destination.setFileSelectionMode(JFileChooser.FILES_ONLY);
        office_home = new JFileChooser();
        office_home.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        JPanel frame = new JPanel();
        GroupLayout frame_layout = new GroupLayout(frame);
        frame.setLayout(frame_layout);
        JPanel tasks = new JPanel(new GridBagLayout());
        GridBagConstraints task_constraints = new GridBagConstraints();
        task_constraints.fill = GridBagConstraints.HORIZONTAL;
        task_constraints.gridy = 0;
        task_constraints.gridx = 0;
        task_constraints.weightx = 0.2;
        task_constraints.anchor = GridBagConstraints.NORTH;
        tasks.add(new JLabel("Operation"), task_constraints);
        task_constraints.gridx = 1;
        task_constraints.weightx = 0.35;
        tasks.add(new JLabel("Source"), task_constraints);
        task_constraints.gridx = 2;
        task_constraints.weightx = 0.35;
        tasks.add(new JLabel("Destination"), task_constraints);
        task_constraints.gridx = 3;
        task_constraints.weightx = 0.1;
        tasks.add(new JLabel("Status"), task_constraints);
        task_constraints.gridy = 1;
        task_constraints.gridx = 0;
        task_constraints.weightx = 0;
        GridBagLayout op_layout = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();
        
        JPanel op = new JPanel(op_layout);
        ButtonGroup ops = new ButtonGroup();
        JToggleButton fix = new JToggleButton("Fix");
        fix.setSelected(true);
        con.fill = GridBagConstraints.HORIZONTAL;
        con.gridx = 0;
        con.gridy = 0;
        con.weightx = 0.5;
        op.add(fix, con);
        ops.add(fix);
        JToggleButton convert = new JToggleButton("Convert");
        convert.setSelected(false);
        con.gridy = 0;
        con.gridx = 1;
        con.weightx = 0.5;
        op.add(convert, con);
        ops.add(convert);
        // Options
        JPanel options = new JPanel(new GridBagLayout());
        JComboBox background_op = new JComboBox(new String[] {"Fill", "Margin", "Kill"});
        ((JLabel)background_op.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
        //background_op.setEnabled(false);
        con.gridy=0;
        con.gridx = 0;
        options.add(new JLabel("Background:"), con);
        con.gridx++;
        options.add(background_op, con);
        JSpinner width = new JSpinner();
        width.setModel(new SpinnerNumberModel(30d, 0d, Short.MAX_VALUE, 1d));
        JSpinner margin = new JSpinner();
        margin.setModel(new SpinnerNumberModel(1d, 0d, Short.MAX_VALUE, 1d));
        con.gridy++;
        con.gridx = 0;
        JCheckBox width_toggle = Toggle("Width (cm):", width);
        options.add(width_toggle, con);
        width.setEnabled(false);
        con.gridx++;
        options.add(width, con);
        con.gridx = 0;
        con.gridy++;
        JCheckBox margin_toggle = Toggle("Margin (cm):", margin);
        options.add(margin_toggle, con);
        con.gridx++;
        margin.setEnabled(false);
        options.add(margin, con);
        Component[] margins = new Component[8];
        int ci = 0;
        for (String dir:new String[] {"Left", "Right", "Top", "Bottom"}) {
            con.gridy++;
            con.gridx = 0;
            JCheckBox some_margin_box = new JCheckBox(dir+" Margin (cm): ");
            margins[ci++] = some_margin_box;
            some_margin_box.setSelected(false);
            some_margin_box.setEnabled(false);
            options.add(some_margin_box, con);
            con.gridx++;
            JSpinner some_margin = new JSpinner();
            some_margin_box.addActionListener(ToggleListener(some_margin));
            margins[ci++] = some_margin;
            some_margin.setModel(new SpinnerNumberModel(1d, 0d, Short.MAX_VALUE, 1d));
            some_margin.setEnabled(false);
            options.add(some_margin, con);
        }
        margin_toggle.addActionListener((ActionEvent ae) -> {
            boolean selected = ((JCheckBox)ae.getSource()).isSelected();
            for (int c=0; c < 8; c+=2) {
                margins[c].setEnabled(selected);
            }
        });
        tasks.add(op, task_constraints);
        task_constraints.gridy = 1;
        task_constraints.gridx = 1;
        JButton open = new JButton("Open");
        open.addActionListener((ActionEvent ae) -> {
            JButton open1 = (JButton)ae.getSource();
            int file_op = source.showOpenDialog(source);
            if (file_op == JFileChooser.APPROVE_OPTION) {
                open1.setText("<html><code>"+source.getSelectedFile().getName()+"</code> - <b>change</b></html>");
            }
        });
        tasks.add(open, task_constraints);
        task_constraints.gridy = 1;
        task_constraints.gridx = 2;
        JButton save = new JButton("Save");
        save.addActionListener((ActionEvent ae) -> {
            JButton save1 = (JButton)ae.getSource();
            int file_op = destination.showSaveDialog(destination);
            if (file_op == JFileChooser.APPROVE_OPTION) {
                save1.setText("<html><code>"+destination.getSelectedFile().getName()+"</code> - <b>change</b></html>");
            }
        });
        tasks.add(save, task_constraints);
        task_constraints.gridy = 1;
        task_constraints.gridx = 3;
        JButton start = new JButton("Start");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 2;
        start.addActionListener((ActionEvent ae) -> {
            boolean fix_it = fix.isSelected();
            File source_file = source.getSelectedFile();
            File destination_file = destination.getSelectedFile();
            if (source_file == null || destination_file == null) {
                return;
            }
            JLabel mode = new JLabel(fix_it ? "Fix":"Convert");
            JLabel source_label = new JLabel(source_file.getName());
            JLabel destination_label = new JLabel(destination_file.getName());
            JPanel status = new JPanel();
            JLabel starting = new JLabel("Starting");
            starting.setVisible(false);
            status.add(starting);
            JButton action = new JButton("Action");
            action.addActionListener((ActionEvent ae1) -> {
                tasks.remove(mode);
                tasks.remove(source_label);
                tasks.remove(destination_label);
                tasks.remove(status);
                tasks.revalidate();
                tasks.repaint();
            });
            action.setVisible(true);
            status.add(action);
            gbc.gridx = 0;
            tasks.add(mode, gbc);
            gbc.gridx++;
            tasks.add(source_label, gbc);
            gbc.gridx++;
            tasks.add(destination_label, gbc);
            gbc.gridx++;
            tasks.add(status, gbc);
            if (fix_it) {
                Map<String, Object> conf = new HashMap();
                conf.put("background", ((String)background_op.getSelectedItem()).toLowerCase());
                if (width_toggle.isSelected()) {
                    conf.put("width", width.getValue());
                }
                if (margin_toggle.isSelected()) {
                    String[] margin_names = new String[] {"margin-left", "margin-right", "margin-top", "margin-bottom"};
                    for (int c=0; c < 8; c+=2) {
                        JCheckBox toggle = (JCheckBox)margins[c];
                        conf.put(margin_names[c/2], toggle.isSelected() ? (double)((JSpinner)margins[c+1]).getValue():(double)margin.getValue());
                    }
                }
                System.out.println(conf);
                //conf.put("")
                new Worker("Fixing", status, new FixingWorker(source_file, destination_file, new HashMap())).start();
            } else {
                new Worker("Conversion", status, new ConversionWorker(source_file, destination_file)).start();
            }
            gbc.gridy++;            
            tasks.repaint();
        });
        tasks.add(start, task_constraints);
        // Tabs: [About] [Options] [Logs]
        JPanel tabs = new JPanel(new GridBagLayout());
        GridBagConstraints tab_constraints = new GridBagConstraints();
        tab_constraints.gridx = tab_constraints.gridy = 0;
        tab_constraints.anchor = GridBagConstraints.NORTH;
        tab_constraints.weightx = 0.1;
        ButtonGroup tabs_group = new ButtonGroup();
        JToggleButton about = new JToggleButton("About");
        about.setSelected(true);
        tabs.add(about, tab_constraints);
        tab_constraints.gridx++;
        tab_constraints.weightx = 0;
        JToggleButton opts = new JToggleButton("Options");
        tabs.add(opts, tab_constraints);
        tab_constraints.gridx++;
        tab_constraints.weightx = 0.1;
        JToggleButton logs = new JToggleButton("Logs");
        tabs.add(logs, tab_constraints);
        tabs_group.add(about);
        tabs_group.add(opts);
        tabs_group.add(logs);
        tab_constraints.gridx = 0;
        tab_constraints.gridy = 1;
        tab_constraints.fill = GridBagConstraints.HORIZONTAL;
        tab_constraints.weightx = 0;
        tab_constraints.weighty = 0;
        tab_constraints.gridwidth = 3;
        JPanel confs = new JPanel(new GridBagLayout());
        GridBagConstraints conf_constraints = new GridBagConstraints();
        conf_constraints.gridx = conf_constraints.gridy = 0;
        confs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    "Preferences"),
                BorderFactory.createEmptyBorder(10,10,10,10)));
        conf_constraints.fill = GridBagConstraints.HORIZONTAL;
        conf_constraints.weightx = 0.5;
        conf_constraints.weighty = 0.1;
        confs.add(new JLabel("Theme:"), conf_constraints);
        LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        String[] choices = new String[lafs.length];
        laf_lookup = new String[lafs.length];
        int selected_index = 0;
        for (int i=0; i < lafs.length; i++) {
            choices[i] = lafs[i].getName();
            laf_lookup[i] = lafs[i].getClassName();
            if (laf_lookup[i].equals(UIManager.getSystemLookAndFeelClassName())) {
                selected_index = i;
            }
        }
        JComboBox theme_op = new JComboBox(choices);
        theme_op.setSelectedIndex(selected_index);
        theme_op.addActionListener((ActionEvent ae) -> {
            int selected = ((JComboBox)ae.getSource()).getSelectedIndex();
            changeLook(laf_lookup[selected]);
        });
        ((JLabel)theme_op.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
        conf_constraints.gridx++;
        confs.add(theme_op, conf_constraints);
        conf_constraints.gridx=0;
        conf_constraints.gridy=1;
        JButton open_office_home = new JButton("Open");
        open_office_home.addActionListener((ActionEvent ae) -> {
            JButton open1 = (JButton)ae.getSource();
            int file_op = office_home.showOpenDialog(office_home);
            if (file_op == JFileChooser.APPROVE_OPTION) {
                open1.setText("<html><code>"+office_home.getSelectedFile().getName()+"</code> - <b>change</b></html>");
                Manager.setOfficeHome(office_home.getSelectedFile());
            }
        });
        JCheckBox toggle_office_home = Toggle("Office Home: ", open_office_home);
        toggle_office_home.addActionListener((ActionEvent ae) -> {
            boolean selected = ((JCheckBox)ae.getSource()).isSelected();
            if (!selected) {
                Manager.setOfficeHome(null);
            }
        });
        confs.add(toggle_office_home, conf_constraints);
        conf_constraints.gridx++;
        confs.add(open_office_home, conf_constraints);
        options.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    "Fixed Document Properties"),
                BorderFactory.createEmptyBorder(10,10,10,10)));
        JTextPane infos = new JTextPane();
        infos.setContentType("text/html");
        infos.setEditable(false);
        infos.setBackground(null);
        infos.setBorder(null);
        infos.setEnabled(true);
        infos.setFocusable(true);
        infos.addHyperlinkListener((HyperlinkEvent he) -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(he.getEventType())) {
                try {
                    Desktop.getDesktop().browse(he.getURL().toURI());
                } catch (Exception e) {
                    DUMB.LOGGER.error("Failed to open link", e);
                }
            }
        });
        JPanel prefs = new JPanel();
        BoxLayout prefs_layout = new BoxLayout(prefs, BoxLayout.PAGE_AXIS);
        prefs.setLayout(prefs_layout);
        prefs.add(options);
        prefs.add(confs);
        JPanel tab_container = new JPanel();
        tab_container.setBackground(null);
        infos.setText(FileUtils.readFileToString(new File("res/About.html"), StandardCharsets.UTF_8));
        about.addActionListener(ReplaceContentListener(infos, tab_container));
        opts.addActionListener(ReplaceContentListener(prefs, tab_container));
        log.setEditable(false);
        log.setBackground(null);
        log.setBorder(null);
        log.setEnabled(true);
        log.setFocusable(true);
        JPanel log_container = new JPanel();
        log_container.add(log);
        JScrollPane log_scroller = new JScrollPane(log_container);
        log_scroller.setVisible(false);
        logs.addActionListener(ReplaceContentListener(log_scroller, tab_container));
        tab_container.add(infos);
        tab_container.add(prefs);
        prefs.setVisible(false);
        tab_container.add(log_scroller);
        tabs.add(tab_container, tab_constraints);
        
        JScrollPane tab_scrollpane = new JScrollPane(tabs);
        JScrollPane task_scrollpane = new JScrollPane(tasks);
        frame_layout.setHorizontalGroup(
            frame_layout.createParallelGroup()
               .addComponent(task_scrollpane)
               .addComponent(tab_scrollpane)
         );
         frame_layout.setVerticalGroup(
            frame_layout.createSequentialGroup()
               .addComponent(task_scrollpane)
               .addComponent(tab_scrollpane)
         );

        super.add(frame);
        super.pack();
        super.setVisible(true);
    }
}
