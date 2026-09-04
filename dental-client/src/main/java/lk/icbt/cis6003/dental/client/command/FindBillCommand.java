package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClientSession;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.ReceiptWindow;
import lk.icbt.cis6003.dental.client.ui.UiUtils;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.KeyEvent;

/**
 * Reprints an existing bill.
 *
 * <p>A bill can be issued only once per appointment, so "print it again" is a
 * separate action from "create it" &mdash; and the receipt it produces is
 * fetched from the server rather than rebuilt, so a reprint is identical to the
 * original.</p>
 */
public class FindBillCommand extends AbstractMenuCommand {

    public FindBillCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Reprint a Bill";
    }

    @Override
    public String getDescription() {
        return "Look up an existing bill by its number and print the receipt again.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_P;
    }

    @Override
    public boolean isPermitted() {
        return ClientSession.getInstance().canHandleBilling();
    }

    @Override
    protected void run() throws ApiException {
        String invoiceNumber = JOptionPane.showInputDialog(owner,
                "Enter the bill number (for example INV-2026-000137):",
                "Reprint a Bill", JOptionPane.QUESTION_MESSAGE);

        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return;
        }

        InvoiceDto invoice = api.findInvoice(invoiceNumber.trim());
        String receipt = api.receiptText(invoice.getInvoiceNumber());

        UiUtils.showInfo(owner, "Bill found",
                invoice.getInvoiceNumber() + " for " + invoice.getPatientName()
                        + "\nTotal " + UiUtils.moneyWithCurrency(invoice.getTotalAmount())
                        + ", balance " + UiUtils.moneyWithCurrency(invoice.getBalanceDue())
                        + "\nStatus: " + invoice.getPaymentStatus().getDisplayName());

        new ReceiptWindow(owner, invoice, receipt).setVisible(true);
    }
}
