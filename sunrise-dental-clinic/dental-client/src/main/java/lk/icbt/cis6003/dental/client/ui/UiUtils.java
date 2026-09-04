package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.common.ClinicConstants;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Look, feel and message conventions shared by every window in the client.
 *
 * <p>Kept in one class so the desktop application looks like one product
 * rather than eight screens written on different afternoons, and so that a
 * change of palette is a change in one file.</p>
 *
 * <p>The palette matches the web application's stylesheet on purpose: a
 * receptionist who uses both should not have to learn two colour vocabularies
 * for the same statuses.</p>
 */
public final class UiUtils {

    private UiUtils() {
        throw new AssertionError("UiUtils is a utility class and must not be instantiated");
    }

    /* ---------------------------- palette ----------------------------- */

    public static final Color TEAL_DARK = new Color(0x0B, 0x3D, 0x43);
    public static final Color TEAL = new Color(0x12, 0x62, 0x6C);
    public static final Color TEAL_LIGHT = new Color(0xD7, 0xEE, 0xF0);
    public static final Color SLATE_TEXT = new Color(0x17, 0x20, 0x29);
    public static final Color SLATE_MUTED = new Color(0x6B, 0x7A, 0x8A);
    public static final Color SLATE_BG = new Color(0xF7, 0xF9, 0xFB);
    public static final Color BORDER = new Color(0xC3, 0xCD, 0xD7);
    public static final Color GREEN = new Color(0x1F, 0x7A, 0x45);
    public static final Color AMBER = new Color(0x96, 0x66, 0x0A);
    public static final Color RED = new Color(0xB3, 0x26, 0x1E);

    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_H1 = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_H2 = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    /**
     * Installs the platform look and feel and the clinic's base font.
     *
     * <p>The system look and feel rather than Swing's default Metal: staff use
     * this alongside other Windows applications all day, and a window that
     * looks foreign is a window they distrust.</p>
     */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Metal is perfectly usable - never fail to start over cosmetics.
            System.err.println("Falling back to the default look and feel: " + ex.getMessage());
        }
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BODY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TableHeader.font", FONT_BOLD);
        UIManager.put("Menu.font", FONT_BODY);
        UIManager.put("MenuItem.font", FONT_BODY);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BODY);
        UIManager.put("ToolTip.font", FONT_BODY);
    }

    /* ---------------------------- messages ---------------------------- */

    /**
     * Reports a failure.
     *
     * <p>The message goes into a scrolling text area rather than a plain
     * label: server validation failures list every rejected field, and a
     * dialog that clips them at the screen edge would hide the one the user
     * needs.</p>
     */
    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, wrapLongMessage(message), title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, wrapLongMessage(message), title,
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showSuccess(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, wrapLongMessage(message), title,
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String question) {
        return JOptionPane.showConfirmDialog(parent, wrapLongMessage(question), title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static Component wrapLongMessage(String message) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);

        int lines = message.split("\n", -1).length;
        area.setRows(Math.min(Math.max(lines, 2), 14));
        area.setColumns(52);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(480, Math.min(Math.max(lines * 20, 45), 300)));
        return scroll;
    }

    /* ---------------------------- components -------------------------- */

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_H1);
        label.setForeground(TEAL_DARK);
        return label;
    }

    public static JLabel subHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_H2);
        label.setForeground(TEAL_DARK);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(SLATE_MUTED);
        return label;
    }

    /** A titled panel border, matching the web application's card headings. */
    public static void titleBorder(JComponent component, String title) {
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER), title, 0, 0, FONT_BOLD, TEAL_DARK),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
    }

    /**
     * Applies the clinic's table conventions: readable row height, no cell
     * grid clutter, zebra striping and a right-aligned money column where the
     * heading says so.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(24);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEE, 0xF2, 0xF6));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionBackground(TEAL_LIGHT);
        table.setSelectionForeground(SLATE_TEXT);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(0xEE, 0xF2, 0xF6));
        header.setForeground(new Color(0x3D, 0x4A, 0x58));
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                if (!selected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : SLATE_BG);
                }
                String heading = t.getColumnName(column);
                boolean numeric = heading != null && (heading.contains("Amount") || heading.contains("Total")
                        || heading.contains("Balance") || heading.contains("Revenue")
                        || heading.contains("Paid") || heading.contains("%"));
                setHorizontalAlignment(numeric ? JLabel.RIGHT : JLabel.LEFT);
                return c;
            }
        });
    }

    /** Sizes each column to its widest cell, capped so one long note cannot dominate. */
    public static void autoSizeColumns(JTable table) {
        TableModel model = table.getModel();
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int width = 60;

            Component headerComp = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, col);
            width = Math.max(width, headerComp.getPreferredSize().width + 24);

            int sampleRows = Math.min(model.getRowCount(), 60);
            for (int row = 0; row < sampleRows; row++) {
                Component cell = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(width, cell.getPreferredSize().width + 20);
            }
            column.setPreferredWidth(Math.min(width, 320));
        }
    }

    /* ---------------------------- formatting -------------------------- */

    public static String money(BigDecimal value) {
        return value == null ? "0.00" : MONEY.format(value);
    }

    public static String moneyWithCurrency(BigDecimal value) {
        return ClinicConstants.CURRENCY_SYMBOL + " " + money(value);
    }

    public static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
