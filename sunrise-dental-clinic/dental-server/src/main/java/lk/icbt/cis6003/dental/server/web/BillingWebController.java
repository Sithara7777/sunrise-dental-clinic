package lk.icbt.cis6003.dental.server.web;

import jakarta.servlet.http.HttpServletResponse;
import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.PaymentRequest;
import lk.icbt.cis6003.dental.common.enums.PaymentMethod;
import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.service.BillingService;
import lk.icbt.cis6003.dental.server.service.ClinicFacade;
import lk.icbt.cis6003.dental.server.util.ReceiptPrinter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Browser screens for requirement 4 - "Calculate and Print Bill".
 *
 * <p>The flow is deliberately two-step: <em>calculate</em> shows the full
 * itemised breakdown with nothing saved, and only then does <em>issue</em>
 * commit it and consume a bill number. A receptionist can therefore try a
 * discount, see its effect and change their mind, which is what stops
 * mis-priced bills being issued and then having to be voided.</p>
 */
@Controller
public class BillingWebController {

    private final BillingService billingService;
    private final ClinicFacade clinicFacade;
    private final ReceiptPrinter receiptPrinter;

    public BillingWebController(BillingService billingService,
                                ClinicFacade clinicFacade,
                                ReceiptPrinter receiptPrinter) {
        this.billingService = billingService;
        this.clinicFacade = clinicFacade;
        this.receiptPrinter = receiptPrinter;
    }

    /* ------------------------------------------------------------------ */
    /* List                                                                */
    /* ------------------------------------------------------------------ */

    @GetMapping("/invoices")
    public String list(@RequestParam(required = false) String term,
                       @RequestParam(required = false) PaymentStatus status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        model.addAttribute("results", billingService.search(term, status, fromDate, toDate, page, 15));
        model.addAttribute("term", term);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("totalOutstanding", billingService.totalOutstanding());
        model.addAttribute("pageTitle", "Bills");
        return "billing/list";
    }

    /* ------------------------------------------------------------------ */
    /* Calculate then issue                                                */
    /* ------------------------------------------------------------------ */

    /** Step one: calculate. Nothing is saved and no bill number is consumed. */
    @GetMapping("/billing/calculate/{appointmentNumber}")
    public String calculate(@PathVariable String appointmentNumber,
                            @RequestParam(required = false, defaultValue = "0") BigDecimal discountPercentage,
                            @RequestParam(required = false) String discountReason,
                            Model model) {
        try {
            InvoiceDto preview = billingService.previewBill(appointmentNumber, discountPercentage);
            model.addAttribute("preview", preview);
            model.addAttribute("appointmentNumber", appointmentNumber);
            model.addAttribute("discountPercentage", discountPercentage);
            model.addAttribute("discountReason", discountReason);
            model.addAttribute("pageTitle", "Calculate Bill - " + appointmentNumber);
            return "billing/calculate";

        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("appointmentNumber", appointmentNumber);
            model.addAttribute("pageTitle", "Calculate Bill");
            return "billing/calculate";
        }
    }

    /** Step two: issue. From here the bill exists and can be reprinted, not re-created. */
    @PostMapping("/billing/issue")
    public String issue(@RequestParam String appointmentNumber,
                        @RequestParam(required = false, defaultValue = "0") BigDecimal discountPercentage,
                        @RequestParam(required = false) String discountReason,
                        @RequestParam(required = false) String remarks,
                        RedirectAttributes redirect) {
        try {
            BillingRequest request = new BillingRequest(appointmentNumber);
            request.setDiscountPercentage(discountPercentage);
            request.setDiscountReason(discountReason);
            request.setRemarks(remarks);

            InvoiceDto invoice = billingService.generateBill(request);
            redirect.addFlashAttribute("successMessage",
                    "Bill " + invoice.getInvoiceNumber() + " issued for "
                            + invoice.getPatientName() + ".");
            return "redirect:/invoices/" + invoice.getInvoiceNumber();

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/billing/calculate/" + appointmentNumber;
        }
    }

    /**
     * The Facade shortcut: mark the visit completed and bill it in one
     * transaction. This is the single action the front desk performs when a
     * patient walks out of the surgery.
     */
    @PostMapping("/billing/complete-and-bill")
    public String completeAndBill(@RequestParam String appointmentNumber,
                                  @RequestParam(required = false, defaultValue = "0")
                                  BigDecimal discountPercentage,
                                  @RequestParam(required = false) String discountReason,
                                  RedirectAttributes redirect) {
        try {
            InvoiceDto invoice = clinicFacade.completeAndBill(
                    appointmentNumber, discountPercentage, discountReason);
            redirect.addFlashAttribute("successMessage",
                    "Visit completed and bill " + invoice.getInvoiceNumber() + " issued.");
            return "redirect:/invoices/" + invoice.getInvoiceNumber();

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/appointments/" + appointmentNumber;
        }
    }

    /* ------------------------------------------------------------------ */
    /* View, print, settle                                                 */
    /* ------------------------------------------------------------------ */

    @GetMapping("/invoices/{invoiceNumber}")
    public String view(@PathVariable String invoiceNumber, Model model) {
        model.addAttribute("invoice", billingService.findByInvoiceNumber(invoiceNumber));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("pageTitle", "Bill " + invoiceNumber);
        return "billing/view";
    }

    /** The printable receipt page - opens the browser's print dialog. */
    @GetMapping("/invoices/{invoiceNumber}/receipt")
    public String receipt(@PathVariable String invoiceNumber, Model model) {
        InvoiceDto invoice = billingService.findByInvoiceNumber(invoiceNumber);
        model.addAttribute("invoice", invoice);
        model.addAttribute("receiptText", receiptPrinter.render(invoice));
        model.addAttribute("pageTitle", "Receipt " + invoiceNumber);
        return "billing/receipt";
    }

    /**
     * The same receipt as a downloadable text file, in the 48-column layout a
     * counter-top thermal printer accepts.
     */
    @GetMapping("/invoices/{invoiceNumber}/receipt.txt")
    @ResponseBody
    public void receiptDownload(@PathVariable String invoiceNumber,
                                HttpServletResponse response) throws IOException {
        String text = clinicFacade.receiptText(invoiceNumber);

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"receipt-" + invoiceNumber + ".txt\"");
        response.getWriter().write(text);
    }

    @PostMapping("/invoices/{invoiceNumber}/payment")
    public String recordPayment(@PathVariable String invoiceNumber,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod paymentMethod,
                                @RequestParam(required = false) String reference,
                                RedirectAttributes redirect) {
        try {
            PaymentRequest request = new PaymentRequest(amount, paymentMethod);
            request.setReference(reference);
            InvoiceDto invoice = billingService.recordPayment(invoiceNumber, request);

            redirect.addFlashAttribute("successMessage",
                    invoice.getPaymentStatus() == PaymentStatus.PAID
                            ? "Bill settled in full. Thank you."
                            : "Payment recorded. Balance outstanding: " + invoice.getBalanceDue());
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/invoices/" + invoiceNumber;
    }

    @PostMapping("/invoices/{invoiceNumber}/cancel")
    public String cancel(@PathVariable String invoiceNumber,
                         @RequestParam String reason,
                         RedirectAttributes redirect) {
        try {
            billingService.cancelInvoice(invoiceNumber, reason);
            redirect.addFlashAttribute("successMessage", "Bill " + invoiceNumber + " cancelled.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/invoices/" + invoiceNumber;
    }
}
