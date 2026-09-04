package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.UiUtils;

import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * Shared behaviour for every menu command: the server connection, the parent
 * window, and one consistent way of reporting a failure.
 *
 * <p>{@link #execute()} is {@code final} and wraps {@link #run()} in a single
 * try/catch. A command therefore never has to remember its own error handling,
 * and a receptionist always sees the same style of message whichever action
 * failed - including the important distinction between "the server refused
 * this" and "the server could not be reached", which have completely different
 * fixes.</p>
 */
public abstract class AbstractMenuCommand implements MenuCommand {

    protected final JFrame owner;
    protected final ClinicApiClient api;

    protected AbstractMenuCommand(JFrame owner, ClinicApiClient api) {
        this.owner = owner;
        this.api = api;
    }

    @Override
    public final void execute() {
        try {
            run();
        } catch (ApiException ex) {
            if (ex.isConnectionFailure()) {
                UiUtils.showError(owner, "Cannot reach the server", ex.getMessage());
            } else if (ex.isAuthenticationFailure()) {
                UiUtils.showError(owner, "Your session has ended",
                        "You have been signed out, probably because the session timed out.\n"
                                + "Please close the application and sign in again.");
            } else {
                UiUtils.showError(owner, getName() + " could not be completed", ex.getMessage());
            }
        } catch (RuntimeException ex) {
            // Never let a defect in one screen take the whole client down mid-shift.
            UiUtils.showError(owner, "Unexpected problem",
                    "Something went wrong performing '" + getName() + "'.\n\n"
                            + "Technical detail: " + ex);
        }
    }

    /** The action itself. Anything that can fail may simply throw. */
    protected abstract void run() throws ApiException;

    @Override
    public KeyStroke getAccelerator() {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
