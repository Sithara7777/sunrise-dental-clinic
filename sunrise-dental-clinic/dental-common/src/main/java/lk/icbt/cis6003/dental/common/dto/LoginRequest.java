package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credentials posted to {@code /api/v1/auth/login} by the desktop client.
 *
 * <p>{@link #toString()} deliberately masks the password so credentials can
 * never leak into a log file - one of the secure-coding practices called for
 * by the Ethical strand of the Cardiff Met EDGE criteria.</p>
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    public LoginRequest() {
        // required by Jackson
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "', password='********'}";
    }
}
