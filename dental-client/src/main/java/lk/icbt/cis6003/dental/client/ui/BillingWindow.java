package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.InvoiceLineDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

/**
 * Requirement 4 - "Calculate the total treatment cost based on treatment type
 * and consultation fee. Print the patient bill/receipt."
 *
 * <p>The two-step flow is deliberate: <b>Calculate</b> shows the full itemised
 * breakdown with nothing saved and no bill number consumed, and only
 * <b>Issue bill &amp; print</b> commits it. A receptionist can therefore try a
 * discount, see its effect and change their mind, which is what stops
 * mis-priced bills being issued and then having to be voided.</p>
 *
 * <p>Note there is no field anywhere on this window for an <em>amount</em>.
 * Prices come from the dentist and treatment records on the server; the only
 * figure a user may influence is the discount, and that is capped at 50% by the
 * server, by the pricing tier and by a database constraint.</p>
 */
public class BillingWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = { "Description", "Qty", "Unit price", "Amount" };

    private final ClinicApiClient api;

    private final JTextField appointmentField = new JTextField(20);
    private final JTextField discountField = new JTextField("0", 6);
    private final JTextField discountReasonField = new JTextField(22);

    private final DefaultTableModel lineModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable lineTable = new JTable(lineModel);

    private final JLabel patientLabel = new JLabel(" ");
    private final JLabel ruleLabel = new JLabel(" ");
    private final JLabel totalLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Enter an appointment number and click Calculate.");

    private final JButton calculateButton = new JButton("Calculate");
    private final JButton issueButton = new JButton("Issue bill & print");

    public BillingWindow(Window owner, ClinicApiClient api, String appointmentNumber) {
        super("Calculate & Print Bill - Sunrise Dental Clinic");
        this.api = api;

        buildUi();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(760, 560));
        setLocationRelativeTo(owner);

        if (appointmentNumber != null && !appointmentNumber.isBlank()) {
            appointmentField.setText(appointmentNumber);
            calculate();
        }
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));

        /* ---------------- inputs ---------------- */
        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        UiUtils.titleBorder(inputs, "  Which visit?  ");
        inputs.add(new JLabel("Appointment number:"));
        inputs.add(appointmentField);
        inputs.add(new JLabel("Discount %:"));
        inputs.add(discountField);
        inputs.add(new JLabel("Reason:"));
        inputs.add(discountReasonField);
        inputs.add(calculateButton);

        appointmentField.setToolTipText("For example APT-2026-000137");
        discountField.setToolTipText("0 to 50. Senior and child concessions are applied "
                + "automatically and do not need entering here.");

        /* ---------------- breakdown ---------------- */
        JPanel breakdown = new JPanel(new BorderLayout(0, 6));
        UiUtils.titleBorder(breakdown, "  How the total was calculated  ");

        JPanel summary = new JPanel(new BorderLayout());
        patientLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ruleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ruleLabel.setForeground(UiUtils.SLATE_MUTED);
        summary.add(patientLabel, BorderLayout.NORTH);
        summary.add(ruleLabel, BorderLayout.SOUTH);

        UiUtils.styleTable(lineTable);
        JScrollPane scroll = new JScrollPane(lineTable);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtils.BORDER));

        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        totalLabel.setForeground(UiUtils.TEAL_DARK);
        totalLabel.setHorizontalAlignment(JLabel.RIGHT);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 8));

        breakdown.add(summary, BorderLayout.NORTH);
        breakdown.add(scroll, BorderLayout.CENTER);
        breakdown.add(totalLabel, BorderLayout.SOUTH);

        /* ---------------- footer ---------------- */
        JPanel footer = new JPanel(new BorderLayout());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.SLATE_MUTED);
        footer.add(statusLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        buttons.add(close);
        buttons.add(issueButton);
        footer.add(buttons, BorderLayout.EAST);

        issueButton.setEnabled(false);

        calculateButton.addActionListener(e -> calculate());
        appointmentField.addActionListener(e -> calculate());
        issueButton.addActionListener(e -> issue());

        root.add(inputs, BorderLayout.NORTH);
        root.add(breakdown, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /* ------------------------------------------------------------------ */
    /* Step one - calculate. Nothing is saved.                             */
    /* ------------------------------------------------------------------ */

    private void calculate() {
        String appointmentNumber = appointmentField.getText().trim();
        if (appointmentNumber.isEmpty()) {
            statusLabel.setText("Enter the appointment number first.");
            return;
        }

        BigDecimal discount = parseDiscount();
        if (discount == null) {
            statusLabel.setText("The discount must be a number between 0 and 50.");
            return;
        }

        calculateButton.setEnabled(false);
        issueButton.setEnabled(false);
        statusLabel.setText("Calculating...");

        new SwingWorker<InvoiceDto, Void>() {
            @Override
            protected InvoiceDto doInBackground() throws ApiException {
                return api.previewBill(appointmentNumber, discount);
            }

            @Override
            protected void done() {
                calculateButton.setEnabled(true);
                try {
                    showPreview(get());
                    issueButton.setEnabled(true);
                    statusLabel.setText("This is a preview - nothing has been saved. "
                            + "Click 'Issue bill & print' to confirm.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    lineModel.setRowCount(0);
                    patientLabel.setText(" ");
                    ruleLabel.setText(" ");
                    totalLabel.setText(" ");
                    statusLabel.setText("Could not calculate the bill.");
                    UiUtils.showError(BillingWindow.this, "Bill could not be calculated",
                                      ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void showPreview(InvoiceDto preview) {
        patientLabel.setText(preview.getPatientName()
                + "  -  " + preview.getTreatmentName()
                + "  with Dr " + preview.getDentistName());
        ruleLabel.setText("Pricing rule applied: " + preview.getPricingStrategyApplied()
                + (preview.getDiscountReason() == null ? ""
                   : "   |   " + preview.getDiscountReason()));

        lineModel.setRowCount(0);
        for (InvoiceLineDto line : preview.getLines()) {
            lineModel.addRow(new Object[] {
                line.getDescription(),
                line.getQuantity(),
                UiUtils.money(line.getUnitPrice()),
                UiUtils.money(line.getLineTotal())
            });
        }
        lineModel.addRow(new Object[] { "Sub-total", "", "", UiUtils.money(preview.getSubTotal()) });
        if (preview.getDiscountAmount() != null
                && preview.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            lineModel.addRow(new Object[] {
                "Discount (" + preview.getDiscountPercentage() + "%)", "", "",
                "-" + UiUtils.money(preview.getDiscountAmount()) });
        }
        lineModel.addRow(new Object[] { "Taxable amount", "", "",
                UiUtils.money(preview.getTaxableAmount()) });
        lineModel.addRow(new Object[] { "VAT", "", "", UiUtils.money(preview.getTaxAmount()) });

        UiUtils.autoSizeColumns(lineTable);
        totalLabel.setText("TOTAL PAYABLE:  " + UiUtils.moneyWithCurrency(preview.getTotalAmount()));
    }

    /* ------------------------------------------------------------------ */
    /* Step two - issue. From here the bill exists.                        */
    /* ------------------------------------------------------------------ */

    private void issue() {
        String appointmentNumber = appointmentField.getText().trim();
        BigDecimal discount = parseDiscount();
        String reason = discountReasonField.getText().trim();

        boolean confirmed = UiUtils.confirm(this, "Issue the bill",
                "Issue the bill for " + appointmentNumber + "?\n\n"
                        + "A bill can only be issued once per appointment. After this it can be "
                        + "reprinted, but not recreated.");
        if (!confirmed) {
            return;
        }

        issueButton.setEnabled(false);
        statusLabel.setText("Issuing the bill...");

        new SwingWorker<InvoiceDto, Void>() {
            @Override
            protected InvoiceDto doInBackground() throws ApiException {
                // The Facade call: complete the visit and bill it in one
                // transaction, so a network drop cannot leave a completed visit
                // with no bill.
                return api.completeAndBill(appointmentNumber, discount,
                                           reason.isEmpty() ? null : reason);
            }

            @Override
            protected void done() {
                try {
                    InvoiceDto invoice = get();
                    statusLabel.setText("Bill " + invoice.getInvoiceNumber() + " issued.");

                    String receipt = api.receiptText(invoice.getInvoiceNumber());
                    new ReceiptWindow(BillingWindow.this, invoice, receipt).setVisible(true);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    issueButton.setEnabled(true);
                    statusLabel.setText("The bill was not issued.");
                    UiUtils.showError(BillingWindow.this, "Bill not issued",
                                      ex.getCause().getMessage());
                } catch (ApiException ex) {
                    UiUtils.showError(BillingWindow.this, "Receipt could not be printed",
                                      ex.getMessage());
                }
            }
        }.execute();
    }

    private BigDecimal parseDiscount() {
        String value = discountField.getText().trim();
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal discount = new BigDecimal(value);
            if (discount.compareTo(BigDecimal.ZERO) < 0
                    || discount.compareTo(new BigDecimal("50")) > 0) {
                return null;
            }
            return discount;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
