package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;

/**
 * Requirement 5 - "Provide step-by-step instructions for new staff on how to
 * use the system."
 *
 * <p>The content is fetched from the clinic server, not compiled into this
 * client, so the desktop application and the web application always show the
 * same instructions and a correction reaches every front-desk machine without
 * redistributing anything.</p>
 */
public class HelpWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final List<HelpTopicDto> topics;
    private final JTextArea contentArea = new JTextArea();

    public HelpWindow(Window owner, List<HelpTopicDto> topics) {
        super("Help - Sunrise Dental Clinic");
        this.topics = topics;

        buildUi();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(880, 600));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        /* ---------------- contents list ---------------- */
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (HelpTopicDto topic : topics) {
            listModel.addElement(topic.getTitle());
        }

        JList<String> topicList = new JList<>(listModel);
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topicList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showTopic(topicList.getSelectedIndex());
            }
        });

        JScrollPane listScroll = new JScrollPane(topicList);
        listScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.BORDER), "  Contents  "));
        listScroll.setPreferredSize(new Dimension(280, 400));

        /* ---------------- topic body ---------------- */
        contentArea.setEditable(false);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setMargin(new java.awt.Insets(10, 12, 10, 12));

        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.BORDER), "  Instructions  "));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, contentScroll);
        split.setResizeWeight(0.30);
        split.setBorder(null);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        footer.add(close);

        root.add(split, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        if (!topics.isEmpty()) {
            topicList.setSelectedIndex(0);
        }
    }

    private void showTopic(int index) {
        if (index < 0 || index >= topics.size()) {
            return;
        }
        HelpTopicDto topic = topics.get(index);

        StringBuilder text = new StringBuilder();
        text.append(topic.getTitle()).append('\n');
        text.append("=".repeat(Math.min(topic.getTitle().length(), 70))).append("\n\n");
        text.append(topic.getSummary()).append("\n\n");

        text.append("STEPS\n").append("-".repeat(60)).append('\n');
        int step = 1;
        for (String instruction : topic.getSteps()) {
            text.append(String.format("%2d. %s%n%n", step++, instruction));
        }

        if (!topic.getTips().isEmpty()) {
            text.append('\n').append("TIPS\n").append("-".repeat(60)).append('\n');
            for (String tip : topic.getTips()) {
                text.append("  * ").append(tip).append("\n\n");
            }
        }

        contentArea.setText(text.toString());
        contentArea.setCaretPosition(0);
    }
}
