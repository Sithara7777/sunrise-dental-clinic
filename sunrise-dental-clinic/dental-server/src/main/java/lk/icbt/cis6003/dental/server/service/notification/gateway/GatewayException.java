package lk.icbt.cis6003.dental.server.service.notification.gateway;

/**
 * A delivery failure inside one transport.
 *
 * <p>Checked on purpose - the opposite of the choice made for
 * {@code BusinessException}. A caller <em>can</em> and must recover from a
 * failed notification: it records the failure and continues. Making it checked
 * forces every call site to decide that explicitly rather than let a runtime
 * exception unwind a booking transaction.</p>
 */
public class GatewayException extends Exception {

    private static final long serialVersionUID = 1L;

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
