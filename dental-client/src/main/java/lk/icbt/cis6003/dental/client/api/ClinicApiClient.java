package lk.icbt.cis6003.dental.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.LoginRequest;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.PatientDto;
import lk.icbt.cis6003.dental.common.dto.PaymentRequest;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.common.dto.UserDto;
import lk.icbt.cis6003.dental.common.dto.report.DashboardStatsDto;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The desktop client's <b>only</b> link to the clinic system.
 *
 * <p>This class is what makes the solution genuinely distributed rather than
 * two halves of one program. It speaks HTTP and JSON to the published web
 * services and nothing else: there is no JDBC driver, no Spring, no shared
 * service class. The client and the server could run on different machines,
 * different operating systems and different JVM versions, and the only thing
 * they agree on is the {@code dental-common} DTO contract.</p>
 *
 * <p><b>Sessions across the network.</b> A {@link CookieManager} is installed
 * on the {@link HttpClient}, so the {@code JSESSIONID} the server issues at
 * sign-in is stored and replayed automatically on every later call. The
 * receptionist types their password once per shift; the distributed client uses
 * the same session mechanism as the browser rather than a second, parallel
 * scheme.</p>
 *
 * <p>All I/O uses {@code java.net.http.HttpClient} from the JDK itself, which
 * keeps the client jar small enough to hand to a front-desk machine.</p>
 */
public class ClinicApiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClinicApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .cookieHandler(cookieManager)          // holds the session cookie
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /* ================================================================== */
    /* Authentication - requirement 1                                      */
    /* ================================================================== */

    /**
     * Signs in and establishes the session used by every later call.
     *
     * @throws ApiException on bad credentials, a locked account, or an
     *         unreachable server - the three cases the login dialog must tell
     *         the user apart
     */
    public UserDto login(String username, String password) throws ApiException {
        UserDto user = post(ApiPaths.AUTH_LOGIN, new LoginRequest(username, password),
                            new TypeReference<ApiResponse<UserDto>>() { });
        ClientSession.getInstance().setCurrentUser(user);
        ClientSession.getInstance().setServerBaseUrl(baseUrl);
        return user;
    }

    /** Confirms the session is still valid - used before showing the main window. */
    public UserDto currentUser() throws ApiException {
        return get(ApiPaths.AUTH_ME, new TypeReference<ApiResponse<UserDto>>() { });
    }

    /**
     * Ends the session on the server - requirement 6, "allow users to safely
     * close the application".
     *
     * <p>Called on exit so the session is released immediately rather than
     * left to time out, which matters on a shared front-desk machine.</p>
     */
    public void logout() {
        try {
            post(ApiPaths.AUTH_LOGOUT, null, new TypeReference<ApiResponse<String>>() { });
        } catch (ApiException ignored) {
            // Closing down must never be blocked by a failing sign-out call.
        } finally {
            ClientSession.getInstance().clear();
        }
    }

    /* ================================================================== */
    /* Appointments - requirements 2 and 3                                 */
    /* ================================================================== */

    public AppointmentDto registerAppointment(AppointmentRequest request) throws ApiException {
        return post(ApiPaths.APPOINTMENTS, request,
                    new TypeReference<ApiResponse<AppointmentDto>>() { });
    }

    public AppointmentDto findAppointment(String appointmentNumber) throws ApiException {
        return get(ApiPaths.APPOINTMENTS + "/" + encode(appointmentNumber),
                   new TypeReference<ApiResponse<AppointmentDto>>() { });
    }

    public PageResponse<AppointmentDto> searchAppointments(String term, String status,
                                                           LocalDate from, LocalDate to,
                                                           int page, int size) throws ApiException {
        StringBuilder query = new StringBuilder(ApiPaths.APPOINTMENTS)
                .append("?page=").append(page).append("&size=").append(size);
        appendIfPresent(query, "term", term);
        appendIfPresent(query, "status", status);
        appendIfPresent(query, "fromDate", from == null ? null : from.toString());
        appendIfPresent(query, "toDate", to == null ? null : to.toString());

        return get(query.toString(),
                   new TypeReference<ApiResponse<PageResponse<AppointmentDto>>>() { });
    }

    public List<AppointmentDto> appointmentsForDay(LocalDate date) throws ApiException {
        return get(ApiPaths.APPOINTMENTS + "/day/" + date,
                   new TypeReference<ApiResponse<List<AppointmentDto>>>() { });
    }

    public List<SlotDto> availableSlots(String dentistCode, LocalDate date) throws ApiException {
        return get(ApiPaths.APPOINTMENT_AVAILABLE_SLOTS
                        + "?dentistCode=" + encode(dentistCode) + "&date=" + date,
                   new TypeReference<ApiResponse<List<SlotDto>>>() { });
    }

    public AppointmentDto updateStatus(String appointmentNumber, StatusUpdateRequest request)
            throws ApiException {
        return patch(ApiPaths.APPOINTMENTS + "/" + encode(appointmentNumber) + "/status", request,
                     new TypeReference<ApiResponse<AppointmentDto>>() { });
    }

    /* ================================================================== */
    /* Billing - requirement 4                                             */
    /* ================================================================== */

    public InvoiceDto previewBill(String appointmentNumber, BigDecimal discount) throws ApiException {
        return get(ApiPaths.INVOICES + "/preview/" + encode(appointmentNumber)
                        + "?discountPercentage=" + (discount == null ? "0" : discount),
                   new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    public InvoiceDto generateBill(BillingRequest request) throws ApiException {
        return post(ApiPaths.INVOICES, request, new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    /** The Facade call: complete the visit and bill it in one round trip. */
    public InvoiceDto completeAndBill(String appointmentNumber, BigDecimal discount, String reason)
            throws ApiException {
        StringBuilder path = new StringBuilder(ApiPaths.INVOICES)
                .append("/complete-and-bill/").append(encode(appointmentNumber))
                .append("?discountPercentage=").append(discount == null ? "0" : discount);
        appendIfPresent(path, "discountReason", reason);
        return post(path.toString(), null, new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    public InvoiceDto findInvoice(String invoiceNumber) throws ApiException {
        return get(ApiPaths.INVOICES + "/" + encode(invoiceNumber),
                   new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    public InvoiceDto findInvoiceForAppointment(String appointmentNumber) throws ApiException {
        return get(ApiPaths.INVOICES + "/appointment/" + encode(appointmentNumber),
                   new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    public InvoiceDto recordPayment(String invoiceNumber, PaymentRequest request) throws ApiException {
        return patch(ApiPaths.INVOICES + "/" + encode(invoiceNumber) + "/payment", request,
                     new TypeReference<ApiResponse<InvoiceDto>>() { });
    }

    /**
     * The printable receipt, rendered by the server.
     *
     * <p>Fetched as plain text rather than rebuilt locally, so the desktop
     * client and the web application cannot print different receipts for the
     * same bill.</p>
     */
    public String receiptText(String invoiceNumber) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ApiPaths.INVOICES + "/" + encode(invoiceNumber) + "/receipt"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/plain")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), null,
                        "The server could not produce the receipt (HTTP " + response.statusCode() + ").");
            }
            return response.body();
        } catch (IOException | InterruptedException ex) {
            restoreInterrupt(ex);
            throw connectionFailure(ex);
        }
    }

    /* ================================================================== */
    /* Reference data                                                      */
    /* ================================================================== */

    public List<DentistDto> listDentists() throws ApiException {
        return get(ApiPaths.DENTISTS, new TypeReference<ApiResponse<List<DentistDto>>>() { });
    }

    public List<TreatmentDto> listTreatments() throws ApiException {
        return get(ApiPaths.TREATMENTS, new TypeReference<ApiResponse<List<TreatmentDto>>>() { });
    }

    public PageResponse<PatientDto> searchPatients(String term, int page, int size) throws ApiException {
        StringBuilder query = new StringBuilder(ApiPaths.PATIENT_SEARCH)
                .append("?page=").append(page).append("&size=").append(size);
        appendIfPresent(query, "term", term);
        return get(query.toString(), new TypeReference<ApiResponse<PageResponse<PatientDto>>>() { });
    }

    /* ================================================================== */
    /* Reports, dashboard and help - requirement 5                         */
    /* ================================================================== */

    public List<Map<String, String>> listReports() throws ApiException {
        return get(ApiPaths.REPORTS, new TypeReference<ApiResponse<List<Map<String, String>>>>() { });
    }

    /**
     * Runs any report.
     *
     * <p>Deserialised with {@code Object} as the row type on purpose: the
     * client renders {@code report.getCells()}, the pre-formatted string grid
     * the server supplies, so one report window displays every report without
     * knowing any report's row class.</p>
     */
    public ReportDto<Object> runReport(String reportCode, LocalDate from, LocalDate to)
            throws ApiException {
        StringBuilder query = new StringBuilder(ApiPaths.REPORTS).append('/').append(encode(reportCode));
        boolean first = true;
        if (from != null) {
            query.append(first ? '?' : '&').append("fromDate=").append(from);
            first = false;
        }
        if (to != null) {
            query.append(first ? '?' : '&').append("toDate=").append(to);
        }
        return get(query.toString(), new TypeReference<ApiResponse<ReportDto<Object>>>() { });
    }

    public DashboardStatsDto dashboard() throws ApiException {
        return get(ApiPaths.REPORT_DASHBOARD, new TypeReference<ApiResponse<DashboardStatsDto>>() { });
    }

    public List<HelpTopicDto> help() throws ApiException {
        return get(ApiPaths.HELP, new TypeReference<ApiResponse<List<HelpTopicDto>>>() { });
    }

    /* ================================================================== */
    /* Transport                                                           */
    /* ================================================================== */

    private <T> T get(String path, TypeReference<ApiResponse<T>> type) throws ApiException {
        return exchange(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build(), type);
    }

    private <T> T post(String path, Object body, TypeReference<ApiResponse<T>> type) throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");

        if (body == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                   .POST(HttpRequest.BodyPublishers.ofString(writeJson(body), StandardCharsets.UTF_8));
        }
        return exchange(builder.build(), type);
    }

    private <T> T patch(String path, Object body, TypeReference<ApiResponse<T>> type) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("PATCH", body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(writeJson(body), StandardCharsets.UTF_8))
                .build();
        return exchange(request, type);
    }

    /**
     * Sends one request and unwraps the standard envelope.
     *
     * <p>Because every endpoint returns the same shape, this single method is
     * the client's entire response-handling code - the practical payoff of the
     * server publishing a uniform contract.</p>
     */
    private <T> T exchange(HttpRequest request, TypeReference<ApiResponse<T>> type) throws ApiException {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            restoreInterrupt(ex);
            throw connectionFailure(ex);
        }

        String body = response.body();
        if (body == null || body.isBlank()) {
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), null,
                        "The server returned HTTP " + response.statusCode() + " with no detail.");
            }
            return null;
        }

        ApiResponse<T> envelope;
        try {
            envelope = objectMapper.readValue(body, type);
        } catch (IOException ex) {
            throw new ApiException(response.statusCode(), null,
                    "The server's reply could not be understood. It may not be a Sunrise Dental "
                            + "Clinic server, or it may be running a different version.", ex);
        }

        if (!envelope.isSuccess()) {
            throw ApiException.from(response.statusCode(), envelope);
        }
        return envelope.getData();
    }

    private String writeJson(Object body) throws ApiException {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException ex) {
            throw new ApiException("The request could not be prepared: " + ex.getMessage(), ex);
        }
    }

    /** Connection problems get their own wording - the fix is different. */
    private ApiException connectionFailure(Exception cause) {
        return new ApiException(
                "Could not reach the clinic server at " + baseUrl + ".\n\n"
                        + "Check that the server is running and that the address is correct.\n"
                        + "Technical detail: " + cause.getMessage(), cause);
    }

    private void restoreInterrupt(Exception ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void appendIfPresent(StringBuilder query, String name, String value) {
        if (value != null && !value.isBlank()) {
            query.append(query.indexOf("?") >= 0 ? '&' : '?')
                 .append(name).append('=').append(encode(value));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
