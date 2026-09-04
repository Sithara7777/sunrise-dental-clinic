package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.common.dto.InvoiceDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Requirement 4, second half - "Print the patient bill/receipt."
 *
 * <p>The receipt text is <b>fetched from the server</b>, already rendered in
 * the clinic's 48-column layout. This window does not build it. That is the
 * point: the desktop client and the web application request the same text from
 * the same renderer, so they cannot print different receipts for one bill, and
 * a change to the layout reaches both without redistributing this client.</p>
 *
 * <p>{@code JTextArea.print()} sends it to a real printer; the save button
 * writes the same characters to a file, which is what a counter-top thermal
 * printer accepts directly.</p>
 */
public class ReceiptWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final InvoiceDto invoice;
    private final JTextArea receiptArea = new JTextArea();

    public ReceiptWindow(Window owner, InvoiceDto invoice, String receiptText) {
        super("Receipt " + invoice.getInvoiceNumber());
        this.invoice = invoice;

        receiptArea.setText(receiptText);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        receiptArea.setCaretPosition(0);

        buildUi();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(520, 640));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel header = new JLabel("<html><b>" + invoice.getInvoiceNumber() + "</b> &mdash; "
                + invoice.getPatientName() + "<br>"
                + "<span style='color:#6B7A8A'>Total "
                + UiUtils.moneyWithCurrency(invoice.getTotalAmount())
                + " &nbsp;|&nbsp; balance "
                + UiUtils.moneyWithCurrency(invoice.getBalanceDue())
                + " &nbsp;|&nbsp; "
                + (invoice.getPaymentStatus() == null ? "" : invoice.getPaymentStatus().getDisplayName())
                + "</span></html>");

        JScrollPane scroll = new JScrollPane(receiptArea);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtils.BORDER));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton printButton = new JButton("Print");
        JButton saveButton = new JButton("Save as text file");
        JButton closeButton = new JButton("Close");

        printButton.addActionListener(e -> print());
        saveButton.addActionListener(e -> save());
        closeButton.addActionListener(e -> dispose());

        buttons.add(closeButton);
        buttons.add(saveButton);
        buttons.add(printButton);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void print() {
        try {
            boolean printed = receiptArea.print();
            if (printed) {
                UiUtils.showSuccess(this, "Sent to the printer",
                        "Receipt " + invoice.getInvoiceNumber() + " has been sent to the printer.");
            }
        } catch (PrinterException ex) {
            UiUtils.showError(this, "Could not print",
                    "The receipt could not be sent to the printer.\n\n"
                            + "You can still save it as a text file and print that.\n\n"
                            + "Technical detail: " + ex.getMessage());
        }
    }

    private void save() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save the receipt");
        chooser.setSelectedFile(new File("receipt-" + invoice.getInvoiceNumber() + ".txt"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        try {
            Files.writeString(target.toPath(), receiptArea.getText(), StandardCharsets.UTF_8);
            UiUtils.showSuccess(this, "Receipt saved",
                    "Saved to:\n" + target.getAbsolutePath()
                            + "\n\nThe file is in the 48-column layout an 80 mm thermal printer "
                            + "accepts directly.");
        } catch (IOException ex) {
            UiUtils.showError(this, "Could not save the receipt",
                    "The file could not be written.\n\n"
                            + "Check that you have permission to write to that folder.\n\n"
                            + "Technical detail: " + ex.getMessage());
        }
    }
}
