package lk.icbt.cis6003.dental.server.service.validation;

/**
 * <b>Chain of Responsibility pattern</b> - one link in the booking validation
 * chain.
 *
 * <p><b>The problem it solves.</b> Registering an appointment must satisfy six
 * independent rules: the date must be sensible, the clinic must be open, the
 * time must sit on the diary grid, the dentist must be on shift, the dentist
 * must be free, and the patient must not already be somewhere else. Written as
 * one method that is six nested {@code if} statements, adding a seventh rule
 * means editing a method every booking in the clinic depends on, and unit
 * testing rule five means constructing the four preceding rules' inputs
 * too.</p>
 *
 * <p><b>How this is better.</b> Each rule is a class with one job and its own
 * test. The chain short-circuits on the first failure, so the receptionist sees
 * the most fundamental problem ("the clinic is closed at 21:00") rather than a
 * confusing downstream one ("that slot is taken"). The order is data, not
 * control flow - {@link #getOrder()} - so rearranging the rules cannot break
 * them.</p>
 *
 * <p><b>Cost, honestly stated.</b> There is no single method a newcomer can
 * read to see every rule, and a chain wired in the wrong order would produce
 * confusing messages. {@link BookingValidationChain} answers the first by
 * logging the assembled order at start-up, and the explicit
 * {@link #getOrder()} contract answers the second.</p>
 */
public abstract class BookingValidationHandler {

    private BookingValidationHandler next;

    /**
     * Links this handler to the next one.
     *
     * @return the handler just linked, so the chain can be built fluently
     */
    public BookingValidationHandler setNext(BookingValidationHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Runs this rule and, if it passes, delegates to the rest of the chain.
     *
     * <p>{@code final} on purpose: a subclass that forgot to call
     * {@code next.handle(...)} would silently skip every remaining rule, and
     * the missing check would only surface as a double booking weeks later.</p>
     */
    public final ValidationOutcome handle(BookingValidationRequest request) {
        ValidationOutcome outcome = check(request);
        if (!outcome.isValid()) {
            return outcome;
        }
        return next == null ? ValidationOutcome.valid() : next.handle(request);
    }

    /** Applies this rule only. Implementations must not call the next handler. */
    protected abstract ValidationOutcome check(BookingValidationRequest request);

    /** Lower runs earlier. Determines the order the chain is assembled in. */
    public abstract int getOrder();

    /** Rule name, used in log lines and in the failed-rule field of the outcome. */
    public abstract String getRuleName();
}
