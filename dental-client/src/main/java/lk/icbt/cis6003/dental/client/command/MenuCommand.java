package lk.icbt.cis6003.dental.client.command;

import javax.swing.KeyStroke;

/**
 * <b>Command pattern</b> - one menu action, as an object.
 *
 * <p><b>The problem it solves.</b> The scenario asks for a "menu driven
 * application". Every menu item in this client is reachable from at least two
 * places: the menu bar, and a large button on the main window - some also from
 * a keyboard accelerator. Writing the behaviour inside an
 * {@code actionPerformed} on the menu item forces the button to duplicate it or
 * to reach into the menu and fire it, and neither ages well.</p>
 *
 * <p><b>How this is better.</b> Each action is a class that knows its own
 * label, mnemonic, accelerator, whether the signed-in user's role permits it,
 * and what it does. The menu bar and the button panel are both built by
 * iterating the same command list, so a new action appears in both by writing
 * one class and registering it - the two can never drift out of step.</p>
 *
 * <p>It also puts role checks in one place: {@link #isPermitted()} is asked
 * once, and any control that offers the command is disabled together.</p>
 *
 * <p><b>Cost, honestly stated.</b> Ten small classes where ten lambdas would
 * have done. Worth it here because each command carries four pieces of
 * metadata besides its behaviour, and because the role rule would otherwise be
 * repeated at every control that offers the action.</p>
 */
public interface MenuCommand {

    /** Label shown on the menu item and the button. */
    String getName();

    /** Tooltip and status-bar text - what the action will actually do. */
    String getDescription();

    /** Alt-key mnemonic, e.g. {@code 'R'} for Register. */
    int getMnemonic();

    /** Keyboard accelerator, or {@code null} for none. */
    KeyStroke getAccelerator();

    /**
     * @return whether the signed-in user's role allows this action. The server
     *         enforces the same rule; this only avoids offering a control that
     *         would be refused.
     */
    default boolean isPermitted() {
        return true;
    }

    /** Runs the action. Implementations report their own failures to the user. */
    void execute();
}
