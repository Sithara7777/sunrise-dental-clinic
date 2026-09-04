package lk.icbt.cis6003.dental.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.PaymentRequest;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.common.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for requirement 4 - "Calculate and Print Bill".
 *
 * <p>These verify the money end to end, against the real database: the total is
 * computed from the dentist's fee and the treatment price, it is stored, it can
 * be printed, and the three rules that protect the clinic's cash - bill only a
 * completed visit, bill only once, never over-pay - hold across the HTTP
 * boundary as well as inside the entity.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Billing web services")
class BillingApiIT {

    private static final AtomicInteger CONTACT_SEQUENCE = new AtomicInteger(100);
    private static final AtomicInteger DAY_SEQUENCE = new AtomicInteger(40);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("previewing a bill saves nothing and consumes no bill number")
    void previewSavesNothing() throws Exception {
        String appointmentNumber = completedAppointment();

        mockMvc.perform(get(ApiPaths.INVOICES + "/preview/" + appointmentNumber)
                        .param("discountPercentage", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value("(not yet issued)"))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.lines").isArray());

        // Nothing was persisted, so fetching the bill must still fail.
        mockMvc.perform(get(ApiPaths.INVOICES + "/appointment/" + appointmentNumber))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("issues a bill totalling consultation fee + treatment price + VAT")
    void issuesACorrectlyCalculatedBill() throws Exception {
        String appointmentNumber = completedAppointment();

        // Dr Perera's consultation is 1,500; scaling is 6,500; VAT is 18%.
        // 8,000 + 1,440 = 9,440.00
        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BillingRequest(appointmentNumber))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consultationFee").value(1500.00))
                .andExpect(jsonPath("$.data.treatmentCost").value(6500.00))
                .andExpect(jsonPath("$.data.subTotal").value(8000.00))
                .andExpect(jsonPath("$.data.taxAmount").value(1440.00))
                .andExpect(jsonPath("$.data.totalAmount").value(9440.00))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("applies an approved discount before VAT")
    void appliesDiscountBeforeVat() throws Exception {
        String appointmentNumber = completedAppointment();

        BillingRequest request = new BillingRequest(appointmentNumber);
        request.setDiscountPercentage(new BigDecimal("10"));
        request.setDiscountReason("Approved by the practice manager");

        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountAmount").value(800.00))
                .andExpect(jsonPath("$.data.taxableAmount").value(7200.00))
                .andExpect(jsonPath("$.data.taxAmount").value(1296.00))
                .andExpect(jsonPath("$.data.totalAmount").value(8496.00));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a discount above 50% is refused by validation")
    void refusesExcessiveDiscount() throws Exception {
        BillingRequest request = new BillingRequest(completedAppointment());
        request.setDiscountPercentage(new BigDecimal("80"));

        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("an appointment that has not happened cannot be billed")
    void refusesToBillAnIncompleteVisit() throws Exception {
        String appointmentNumber = bookAppointment();   // still SCHEDULED

        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BillingRequest(appointmentNumber))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("NOT_BILLABLE"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("an appointment cannot be billed twice")
    void refusesToBillTwice() throws Exception {
        String appointmentNumber = completedAppointment();

        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BillingRequest(appointmentNumber))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BillingRequest(appointmentNumber))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_INVOICED"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("prints a receipt containing the clinic header and the total")
    void printsAReceipt() throws Exception {
        String invoiceNumber = issuedBill();

        mockMvc.perform(get(ApiPaths.INVOICES + "/" + invoiceNumber + "/receipt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUNRISE DENTAL CLINIC")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PATIENT BILL / RECEIPT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TOTAL PAYABLE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(invoiceNumber)));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a full payment settles the bill")
    void fullPaymentSettlesTheBill() throws Exception {
        String invoiceNumber = issuedBill();

        mockMvc.perform(patch(ApiPaths.INVOICES + "/" + invoiceNumber + "/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PaymentRequest(new BigDecimal("9440.00"), PaymentMethod.CASH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.balanceDue").value(0.00));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a part payment leaves the correct balance outstanding")
    void partPaymentLeavesABalance() throws Exception {
        String invoiceNumber = issuedBill();

        mockMvc.perform(patch(ApiPaths.INVOICES + "/" + invoiceNumber + "/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PaymentRequest(new BigDecimal("4000.00"), PaymentMethod.CARD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.balanceDue").value(5440.00));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("an over-payment is refused with 422")
    void overPaymentIsRefused() throws Exception {
        String invoiceNumber = issuedBill();

        mockMvc.perform(patch(ApiPaths.INVOICES + "/" + invoiceNumber + "/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PaymentRequest(new BigDecimal("99999.00"), PaymentMethod.CASH))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_EXCEEDS_BALANCE"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("complete-and-bill does both in one call - the Facade operation")
    void completeAndBillDoesBoth() throws Exception {
        String appointmentNumber = bookAppointment();   // still SCHEDULED

        mockMvc.perform(post(ApiPaths.INVOICES + "/complete-and-bill/" + appointmentNumber)
                        .param("discountPercentage", "0"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").exists());

        mockMvc.perform(get(ApiPaths.APPOINTMENTS + "/" + appointmentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.invoiced").value(true));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("the bill number follows the published format")
    void billNumberFollowsTheFormat() throws Exception {
        assertThat(issuedBill()).matches("INV-\\d{4}-\\d{6}");
    }

    /* ------------------------------------------------------------------ */
    /* Fixtures                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Books an appointment on its own day and slot, so tests never collide.
     *
     * <p>The contact number is built as {@code 07} plus eight digits, which is
     * exactly the ten-character Sri Lankan format the DTO validates. Test data
     * has to satisfy the real rules; a fixture that quietly breaks them tests
     * only the validator.</p>
     */
    private String bookAppointment() throws Exception {
        int offset = DAY_SEQUENCE.incrementAndGet();

        AppointmentRequest request = AppointmentRequest.builder()
                .patientName("Billing Test Patient")
                .address("No. 7, Billing Road, Colombo 04")
                .contactNumber(String.format("07%08d", CONTACT_SEQUENCE.incrementAndGet()))
                .dentistCode("DEN-001")
                .treatmentCode("SCALING")
                .appointmentDate(LocalDate.now().plusDays(offset))
                .appointmentTime(LocalTime.of(9, 0))
                .build();

        MvcResult result = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result).path("data").path("appointmentNumber").asText();
    }

    private String completedAppointment() throws Exception {
        String number = bookAppointment();

        mockMvc.perform(patch(ApiPaths.APPOINTMENTS + "/" + number + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest(AppointmentStatus.COMPLETED))))
                .andExpect(status().isOk());

        return number;
    }

    private String issuedBill() throws Exception {
        MvcResult result = mockMvc.perform(post(ApiPaths.INVOICES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BillingRequest(completedAppointment()))))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result).path("data").path("invoiceNumber").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
