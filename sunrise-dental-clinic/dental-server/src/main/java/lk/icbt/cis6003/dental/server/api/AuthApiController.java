package lk.icbt.cis6003.dental.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.dto.LoginRequest;
import lk.icbt.cis6003.dental.common.dto.UserDto;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.security.ClinicUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Requirement 1 - "User Authentication (Login)" - as a web service.
 *
 * <p>This is the door the menu-driven desktop client comes through. Posting
 * credentials here authenticates the caller and establishes a server-side
 * <b>session</b>; the {@code JSESSIONID} cookie in the response authenticates
 * every subsequent call the client makes, so credentials cross the network
 * exactly once per run rather than on every request.</p>
 *
 * <p>That is the same session mechanism the browser uses. One authentication
 * model serves both front ends, which is why the audit trail can attribute a
 * booking to a member of staff whether it was made in the browser or on the
 * desktop.</p>
 */
@RestController
@Tag(name = "1. Authentication", description = "Sign in, identify the current user, sign out")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthApiController(AuthenticationManager authenticationManager,
                             SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping(ApiPaths.AUTH_LOGIN)
    @Operation(summary = "Sign in",
               description = "Authenticates a member of staff and starts a session. The response "
                       + "carries a JSESSIONID cookie which authenticates every later request.")
    public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // Establish the session explicitly. Without this the authentication
            // would be discarded at the end of the request and the client would
            // have to re-send credentials on every single call.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            HttpSession session = httpRequest.getSession(true);
            ClinicUserDetails principal = (ClinicUserDetails) authentication.getPrincipal();

            UserDto dto = toDto(principal, session.getId());
            log.info("API sign-in succeeded for '{}' (session {})", dto.getUsername(), session.getId());

            return ResponseEntity.ok(ApiResponse.ok(dto,
                    "Welcome, " + principal.getFullName() + "."));

        } catch (LockedException ex) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "This account is temporarily locked after repeated failed sign-in attempts. "
                            + "Please try again in a few minutes.");
        } catch (DisabledException ex) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED,
                    "This account has been deactivated. Please contact your administrator.");
        } catch (BadCredentialsException ex) {
            // Identical message whether the username or the password was wrong.
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED,
                    "Invalid username or password.");
        }
    }

    @GetMapping(ApiPaths.AUTH_ME)
    @Operation(summary = "Current user",
               description = "Returns the signed-in member of staff. Used by the desktop client "
                       + "to confirm its session is still valid before showing the main window.")
    public ResponseEntity<ApiResponse<UserDto>> currentUser(Authentication authentication,
                                                            HttpSession session) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ClinicUserDetails principal)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "No user is signed in.");
        }
        return ResponseEntity.ok(ApiResponse.ok(toDto(principal, session.getId())));
    }

    @PostMapping(ApiPaths.AUTH_LOGOUT)
    @Operation(summary = "Sign out",
               description = "Ends the session. Requirement 6 of the scenario - the desktop client "
                       + "calls this before closing so the session is released rather than left to "
                       + "time out.")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? "unknown" : SecurityContextHolder.getContext().getAuthentication().getName();

        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        log.info("API sign-out for '{}'", username);
        return ResponseEntity.ok(ApiResponse.ok("SIGNED_OUT", "You have been signed out."));
    }

    private UserDto toDto(ClinicUserDetails principal, String sessionId) {
        UserDto dto = new UserDto();
        dto.setId(principal.getId());
        dto.setUsername(principal.getUsername());
        dto.setFullName(principal.getFullName());
        dto.setEmail(principal.getEmail());
        dto.setRole(principal.getRole());
        dto.setActive(principal.isEnabled());
        dto.setSessionId(sessionId);
        return dto;
    }
}
