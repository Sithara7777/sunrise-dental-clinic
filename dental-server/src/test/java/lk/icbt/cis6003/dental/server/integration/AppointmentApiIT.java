package lk.icbt.cis6003.dental.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the appointment web services - requirements 2 and 3.
 *
 * <p>These run the whole stack: HTTP request, security filter, controller,
 * validation, service, validation chain, repository, and a real database with
 * the real constraints. Only the network is simulated, by MockMvc.</p>
 *
 * <p>They are the tests that prove the <em>published contract</em> behaves as
 * documented - correct status codes, correct error codes, correct envelope -
 * which is what the desktop client depends on.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Appointment web services")
class AppointmentApiIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /* ================================================================== */
    /* Requirement 2 - register                                            */
    /* ================================================================== */

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("registers an appointment and returns 201 with the issued number")
    void registersAnAppointment() throws Exception {
        MvcResult result = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Anura Bandara", "0771230001",
                                LocalTime.of(9, 0), LocalDate.now().plusDays(20)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientName").value("Anura Bandara"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn();

        String number = read(result).path("data").path("appointmentNumber").asText();

        assertThat(number)
                .as("the server must issue the number, in the documented format")
                .matches("APT-\\d{4}-\\d{6}");
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("registering a new patient inline also creates their patient record")
    void registersTheNewPatientToo() throws Exception {
        MvcResult result = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Piyumi Ekanayake", "0771230002",
                                LocalTime.of(9, 30), LocalDate.now().plusDays(20)))))
                .andExpect(status().isCreated())
                .andReturn();

        String patientCode = read(result).path("data").path("patientCode").asText();
        assertThat(patientCode).matches("PAT-\\d{6}");

        mockMvc.perform(get(ApiPaths.PATIENTS + "/" + patientCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Piyumi Ekanayake"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a second booking of the same dentist slot is refused with 409 SLOT_UNAVAILABLE")
    void refusesDoubleBooking() throws Exception {
        LocalDate date = LocalDate.now().plusDays(21);

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("First Patient", "0771230003", LocalTime.of(10, 0), date))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Second Patient", "0771230004", LocalTime.of(10, 0), date))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SLOT_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("an invalid contact number is refused with 400 and names the field")
    void refusesInvalidContactNumber() throws Exception {
        AppointmentRequest request = request("Bad Phone", "12345",
                LocalTime.of(11, 0), LocalDate.now().plusDays(22));

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("contactNumber"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a booking in the past is refused")
    void refusesPastBooking() throws Exception {
        AppointmentRequest request = request("Time Traveller", "0771230005",
                LocalTime.of(10, 0), LocalDate.now().minusDays(3));

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a time off the 30-minute grid is refused")
    void refusesUnalignedTime() throws Exception {
        AppointmentRequest request = request("Off Grid", "0771230006",
                LocalTime.of(10, 17), LocalDate.now().plusDays(23));

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("hour or the half hour")));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a booking outside the dentist's shift is refused")
    void refusesOutsideDentistShift() throws Exception {
        // DEN-004 (Dr Jayawardena) finishes at 14:00.
        AppointmentRequest request = request("Late Arrival", "0771230007",
                LocalTime.of(15, 0), LocalDate.now().plusDays(24));
        request.setDentistCode("DEN-004");

        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("14:00")));
    }

    /* ================================================================== */
    /* Requirement 3 - display                                             */
    /* ================================================================== */

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("searching by appointment number returns the complete record")
    void findsByAppointmentNumber() throws Exception {
        MvcResult created = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Searchable Patient", "0771230008",
                                LocalTime.of(12, 0), LocalDate.now().plusDays(25)))))
                .andExpect(status().isCreated())
                .andReturn();

        String number = read(created).path("data").path("appointmentNumber").asText();

        mockMvc.perform(get(ApiPaths.APPOINTMENTS + "/" + number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentNumber").value(number))
                .andExpect(jsonPath("$.data.patientName").value("Searchable Patient"))
                .andExpect(jsonPath("$.data.address").exists())
                .andExpect(jsonPath("$.data.contactNumber").value("0771230008"))
                .andExpect(jsonPath("$.data.dentistName").exists())
                .andExpect(jsonPath("$.data.treatmentName").exists())
                .andExpect(jsonPath("$.data.appointmentDate").exists())
                .andExpect(jsonPath("$.data.appointmentTime").exists());
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("an unknown appointment number returns 404 with a NOT_FOUND code")
    void unknownNumberReturns404() throws Exception {
        mockMvc.perform(get(ApiPaths.APPOINTMENTS + "/APT-2026-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("the search endpoint returns a paged envelope")
    void searchReturnsAPage() throws Exception {
        mockMvc.perform(get(ApiPaths.APPOINTMENTS).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    /* ================================================================== */
    /* Availability and lifecycle                                          */
    /* ================================================================== */

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("the availability endpoint reports free and taken slots")
    void reportsAvailability() throws Exception {
        mockMvc.perform(get(ApiPaths.APPOINTMENT_AVAILABLE_SLOTS)
                        .param("dentistCode", "DEN-001")
                        .param("date", LocalDate.now().plusDays(26).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].startTime").exists())
                .andExpect(jsonPath("$.data[0].available").exists());
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a booked slot is reported as taken, naming the appointment holding it")
    void bookedSlotIsReportedAsTaken() throws Exception {
        LocalDate date = LocalDate.now().plusDays(27);

        MvcResult created = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Slot Holder", "0771230009", LocalTime.of(13, 0), date))))
                .andExpect(status().isCreated())
                .andReturn();

        String number = read(created).path("data").path("appointmentNumber").asText();

        MvcResult slots = mockMvc.perform(get(ApiPaths.APPOINTMENT_AVAILABLE_SLOTS)
                        .param("dentistCode", "DEN-001")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode taken = read(slots).path("data");
        boolean found = false;
        for (JsonNode slot : taken) {
            if ("13:00:00".equals(slot.path("startTime").asText())) {
                assertThat(slot.path("available").asBoolean()).isFalse();
                assertThat(slot.path("occupiedBy").asText()).isEqualTo(number);
                found = true;
            }
        }
        assertThat(found).as("the 13:00 slot must appear in the diary").isTrue();
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a legal status change is accepted; an illegal one is refused with 422")
    void statusTransitionsAreEnforced() throws Exception {
        MvcResult created = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Status Patient", "0771230010",
                                LocalTime.of(14, 0), LocalDate.now().plusDays(28)))))
                .andExpect(status().isCreated())
                .andReturn();

        String number = read(created).path("data").path("appointmentNumber").asText();

        mockMvc.perform(patch(ApiPaths.APPOINTMENTS + "/" + number + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest(AppointmentStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Cancelled is terminal - completing it would bill a visit that never happened.
        mockMvc.perform(patch(ApiPaths.APPOINTMENTS + "/" + number + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest(AppointmentStatus.COMPLETED))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("cancelling frees the slot for another patient")
    void cancellingFreesTheSlot() throws Exception {
        LocalDate date = LocalDate.now().plusDays(29);
        LocalTime time = LocalTime.of(15, 0);

        MvcResult created = mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Cancelling Patient", "0771230011", time, date))))
                .andExpect(status().isCreated())
                .andReturn();

        String number = read(created).path("data").path("appointmentNumber").asText();

        mockMvc.perform(patch(ApiPaths.APPOINTMENTS + "/" + number + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest(AppointmentStatus.CANCELLED))))
                .andExpect(status().isOk());

        // The same slot must now be bookable again.
        mockMvc.perform(post(ApiPaths.APPOINTMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Replacement Patient", "0771230012", time, date))))
                .andExpect(status().isCreated());
    }

    /* ------------------------------------------------------------------ */

    private AppointmentRequest request(String name, String contact, LocalTime time, LocalDate date) {
        return AppointmentRequest.builder()
                .patientName(name)
                .address("No. 1, Test Road, Colombo 05")
                .contactNumber(contact)
                .dentistCode("DEN-001")
                .treatmentCode("SCALING")
                .appointmentDate(date)
                .appointmentTime(time)
                .build();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
