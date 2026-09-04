package lk.icbt.cis6003.dental.server.service.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>Factory pattern</b> - turns the {@code pricing_strategy} value stored on a
 * treatment row into the {@link PricingStrategy} object that implements it.
 *
 * <p><b>Why the factory is needed at all.</b> The Strategy pattern only pays
 * off if the caller never names a concrete rule. {@code BillingService} knows a
 * treatment has a key; it must not know that {@code "SURGICAL"} means
 * {@link SurgicalPricingStrategy}. This class is the single place where that
 * mapping exists.</p>
 *
 * <p><b>Registration is automatic.</b> Spring injects every
 * {@link PricingStrategy} bean into the constructor, and the map is built from
 * each one's own {@link PricingStrategy#getKey() key}. Adding a fifth rule is
 * therefore one new {@code @Component} - no registry to remember to edit,
 * which is precisely the maintenance failure a hand-maintained factory
 * invites.</p>
 *
 * <p><b>Unknown keys do not fail a bill.</b> A treatment row carrying a typo
 * falls back to {@link StandardPricingStrategy} with a warning in the log. The
 * clinic gets a correct list-price bill and the operator gets a diagnostic;
 * refusing to bill a patient who is standing at the desk would be the wrong
 * failure mode.</p>
 */
@Component
public class PricingStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(PricingStrategyFactory.class);

    private final Map<String, PricingStrategy> registry;
    private final PricingStrategy fallback;

    public PricingStrategyFactory(List<PricingStrategy> strategies) {
        Map<String, PricingStrategy> map = new LinkedHashMap<>();
        for (PricingStrategy strategy : strategies) {
            String key = normalise(strategy.getKey());
            PricingStrategy previous = map.put(key, strategy);
            if (previous != null) {
                throw new IllegalStateException(
                        "Two pricing strategies claim the key '" + key + "': "
                                + previous.getClass().getName() + " and " + strategy.getClass().getName());
            }
        }
        this.registry = Collections.unmodifiableMap(map);
        this.fallback = map.get(StandardPricingStrategy.KEY);
        if (this.fallback == null) {
            throw new IllegalStateException(
                    "The " + StandardPricingStrategy.KEY + " pricing strategy is mandatory but was not found");
        }
        log.info("Pricing strategies registered: {}", registry.keySet());
    }

    /**
     * @param key the value of {@code treatment.pricing_strategy}
     * @return the matching rule, or the standard rule when the key is unknown
     */
    public PricingStrategy resolve(String key) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        PricingStrategy strategy = registry.get(normalise(key));
        if (strategy == null) {
            log.warn("Unknown pricing strategy '{}' - falling back to {}. "
                     + "Check the pricing_strategy column on the treatment record.",
                     key, fallback.getKey());
            return fallback;
        }
        return strategy;
    }

    /** @return {@code true} when a treatment may legally be saved with this key */
    public boolean isSupported(String key) {
        return key != null && registry.containsKey(normalise(key));
    }

    /** Every registered rule, for the treatment maintenance drop-down. */
    public Map<String, PricingStrategy> getAll() {
        return registry;
    }

    /** Keys only, in registration order. */
    public java.util.Set<String> getSupportedKeys() {
        return registry.keySet();
    }

    private static String normalise(String key) {
        return key.trim().toUpperCase(Locale.ROOT);
    }
}
