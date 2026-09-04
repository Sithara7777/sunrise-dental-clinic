package lk.icbt.cis6003.dental.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.PaymentRequest;
import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.service.BillingService;
import lk.icbt.cis6003.dental.server.service.ClinicFacade;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Web service for requirement 4 - "Calculate and Print Bill".
 *
 * <p>Note what the API does <em>not</em> accept: at no point can a caller send
 * an amount. Prices come from the dentist and treatment records on the server.
 * The only figure a client may influence is the discount percentage, which is
 * capped at 50% by the DTO, again by the pricing tier and again by a database
 * CHECK constraint.</p>
 */
@RestController
@Tag(name = "3. Billing", description = "Calculate, issue, print and settle patient bills")
public class InvoiceApiController {

    private final BillingService billingService;
    private final ClinicFacade clinicFacade;

    public InvoiceApiController(BillingService billingService, ClinicFacade clinicFacade) {
        this.billingService = billingService;
        this.clinicFacade = clinicFacade;
    }

    @GetMapping(ApiPaths.INVOICE_PREVIEW)
    @Operation(summary = "Calculate a bill without issuing it",
               description = "Shows what the bill would come to, itemised. Nothing is saved and no "
                       + "bill number is consumed, so a receptionist can quote a figure and try a "
                       + "discount before committing.")
    public ResponseEntity<ApiResponse<InvoiceDto>> preview(
            @PathVariable String appointmentNumber,
            @RequestParam(required = false, defaultValue = "0") BigDecimal discountPercentage) {

        return ResponseEntity.ok(ApiResponse.ok(
                billingService.previewBill(appointmentNumber, discountPercentage)));
    }

    @PostMapping(ApiPaths.INVOICES)
    @Operation(summary = "Issue the bill for a completed visit",
               description = "Only a COMPLETED appointment can be billed, and only once - both "
                       + "rules are enforced by the database as well as by the service.")
    public ResponseEntity<ApiResponse<InvoiceDto>> generate(@Valid @RequestBody BillingRequest request) {
        InvoiceDto invoice = billingService.generateBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(invoice, "Bill " + invoice.getInvoiceNumber() + " issued."));
    }

    @PostMapping(ApiPaths.INVOICES + "/complete-and-bill/{appointmentNumber}")
    @Operation(summary = "Complete the visit and issue its bill in one step",
               description = "The Facade operation the desktop client uses. One network round "
                       + "trip and one transaction: either the visit is completed AND billed, or "
                       + "neither happens.")
    public ResponseEntity<ApiResponse<InvoiceDto>> completeAndBill(
            @PathVariable String appointmentNumber,
            @RequestParam(required = false, defaultValue = "0") BigDecimal discountPercentage,
            @RequestParam(required = false) String discountReason) {

        InvoiceDto invoice = clinicFacade.completeAndBill(
                appointmentNumber, discountPercentage, discountReason);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(invoice,
                        "Visit completed and bill " + invoice.getInvoiceNumber() + " issued."));
    }

    @GetMapping(ApiPaths.INVOICE_BY_NUMBER)
    @Operation(summary = "Retrieve a bill")
    public ResponseEntity<ApiResponse<InvoiceDto>> findByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.findByInvoiceNumber(invoiceNumber)));
    }

    @GetMapping(ApiPaths.INVOICE_FOR_APPOINTMENT)
    @Operation(summary = "Retrieve the bill raised against an appointment")
    public ResponseEntity<ApiResponse<InvoiceDto>> findByAppointment(
            @PathVariable String appointmentNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                billingService.findByAppointmentNumber(appointmentNumber)));
    }

    @GetMapping(value = ApiPaths.INVOICE_RECEIPT_TEXT, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Print the receipt",
               description = "Returns the 48-column plain-text receipt, ready for a thermal "
                       + "printer. Rendered on the server so the desktop client and the web "
                       + "application can never print different layouts.")
    public ResponseEntity<String> receipt(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(clinicFacade.receiptText(invoiceNumber));
    }

    @PatchMapping(ApiPaths.INVOICE_PAY)
    @Operation(summary = "Record a payment",
               description = "Full or partial. An over-payment is refused by the entity, and "
                       + "again by the chk_invoice_amount_paid database constraint.")
    public ResponseEntity<ApiResponse<InvoiceDto>> pay(@PathVariable String invoiceNumber,
                                                       @Valid @RequestBody PaymentRequest request) {
        InvoiceDto invoice = billingService.recordPayment(invoiceNumber, request);
        return ResponseEntity.ok(ApiResponse.ok(invoice,
                invoice.getPaymentStatus() == PaymentStatus.PAID
                        ? "Bill settled in full."
                        : "Payment recorded. Balance outstanding: " + invoice.getBalanceDue()));
    }

    @PatchMapping(ApiPaths.INVOICE_BY_NUMBER + "/cancel")
    @Operation(summary = "Void a bill raised in error",
               description = "Refused once the bill has been paid.")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancel(@PathVariable String invoiceNumber,
                                                          @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok(
                billingService.cancelInvoice(invoiceNumber, reason), "Bill cancelled."));
    }

    @GetMapping(ApiPaths.INVOICES)
    @Operation(summary = "Search bills")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceDto>>> search(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                billingService.search(term, status, fromDate, toDate, page, size)));
    }

    @GetMapping(ApiPaths.INVOICES + "/outstanding")
    @Operation(summary = "Unpaid and part-paid bills")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> outstanding() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listOutstanding()));
    }
}
