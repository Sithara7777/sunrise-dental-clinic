package lk.icbt.cis6003.dental.server.service.validation;

import jakarta.annotation.PostConstruct;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Assembles and runs the booking validation chain.
 *
 * <p>Spring injects every {@link BookingValidationHandler} bean; this class
 * sorts them by {@link BookingValidationHandler#getOrder()} and links them.
 * Adding a seventh rule is therefore one new {@code @Component} - the chain
 * rebuilds itself, and no existing class changes.</p>
 *
 * <p>The assembled order is logged once at start-up. That is the answer to the
 * pattern's main drawback: there is no single method listing every rule, so the
 * application states the rule order it is actually running with.</p>
 */
@Component
public class BookingValidationChain {

    private static final Logger log = LoggerFactory.getLogger(BookingValidationChain.class);

    private final List<BookingValidationHandler> handlers;
    private BookingValidationHandler head;

    public BookingValidationChain(List<BookingValidationHandler> handlers) {
        this.handlers = new ArrayList<>(handlers);
    }

    @PostConstruct
    void assembleChain() {
        handlers.sort(Comparator.comparingInt(BookingValidationHandler::getOrder));

        if (handlers.isEmpty()) {
            throw new IllegalStateException("No booking validation handlers were found - "
                    + "appointments would be created with no checks at all.");
        }

        head = handlers.get(0);
        BookingValidationHandler current = head;
        for (int i = 1; i < handlers.size(); i++) {
            current = current.setNext(handlers.get(i));
        }

        log.info("Booking validation chain assembled ({} rules): {}",
                 handlers.size(),
                 handlers.stream()
                         .map(h -> h.getOrder() + ":" + h.getRuleName())
                         .collect(Collectors.joining(" -> ")));
    }

    /**
     * Runs every rule and returns the first failure, or a valid outcome.
     *
     * @param request the booking to inspect
     */
    public ValidationOutcome validate(BookingValidationRequest request) {
        ValidationOutcome outcome = head.handle(request);
        if (!outcome.isValid()) {
            log.debug("Booking rejected by rule {}: {}", outcome.getFailedRule(), outcome.getMessage());
        }
        return outcome;
    }

    /**
     * Runs every rule and throws on the first failure.
     *
     * <p>The convenience form used by the service tier, where a failed
     * validation always means "abandon the transaction and tell the user".</p>
     *
     * @throws BusinessException carrying the failing rule's error code and message
     */
    public void validateOrThrow(BookingValidationRequest request) {
        ValidationOutcome outcome = validate(request);
        if (!outcome.isValid()) {
            throw new BusinessException(outcome.getErrorCode(), outcome.getMessage());
        }
    }

    /** The rules in the order they run - used by the Help screen. */
    public List<String> describeRules() {
        return handlers.stream()
                .map(BookingValidationHandler::getRuleName)
                .collect(Collectors.toList());
    }
}
