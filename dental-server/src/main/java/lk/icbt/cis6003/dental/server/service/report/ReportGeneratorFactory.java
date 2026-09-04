package lk.icbt.cis6003.dental.server.service.report;

import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>Factory pattern</b> - resolves a report code to its generator.
 *
 * <p>Because every report shares the {@code ReportDto} envelope and this
 * factory, the REST layer needs one {@code /reports/{code}} endpoint, the web
 * UI needs one report page and the Swing client needs one report window - no
 * matter how many reports exist. Adding a sixth report is a single new
 * {@code @Component}; nothing else in the system is touched.</p>
 *
 * <p>Unlike the pricing factory, an unknown report code is a hard error rather
 * than a fallback. Silently producing the wrong report would be worse than
 * telling the caller their code does not exist.</p>
 */
@Component
public class ReportGeneratorFactory {

    private static final Logger log = LoggerFactory.getLogger(ReportGeneratorFactory.class);

    private final Map<String, AbstractReportGenerator<?>> registry;

    public ReportGeneratorFactory(List<AbstractReportGenerator<?>> generators) {
        Map<String, AbstractReportGenerator<?>> map = new LinkedHashMap<>();
        for (AbstractReportGenerator<?> generator : generators) {
            String code = normalise(generator.getCode());
            AbstractReportGenerator<?> previous = map.put(code, generator);
            if (previous != null) {
                throw new IllegalStateException("Two report generators claim the code '" + code + "'");
            }
        }
        this.registry = Collections.unmodifiableMap(map);
        log.info("Report generators registered: {}", registry.keySet());
    }

    /**
     * @param code the report code, e.g. {@code REVENUE}
     * @throws ResourceNotFoundException when no such report exists
     */
    public AbstractReportGenerator<?> resolve(String code) {
        if (code == null || code.isBlank()) {
            throw new ResourceNotFoundException("A report code must be supplied");
        }
        AbstractReportGenerator<?> generator = registry.get(normalise(code));
        if (generator == null) {
            throw new ResourceNotFoundException(
                    "Report '" + code + "' does not exist. Available reports: " + registry.keySet());
        }
        return generator;
    }

    /** Every report, for the reports menu in both user interfaces. */
    public List<ReportDescriptor> listAvailable() {
        return registry.values().stream()
                .map(g -> new ReportDescriptor(g.getCode(), g.getTitle(), g.getDescription()))
                .toList();
    }

    public boolean exists(String code) {
        return code != null && registry.containsKey(normalise(code));
    }

    private static String normalise(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /** A report as advertised in the reports menu. */
    public record ReportDescriptor(String code, String title, String description) {
    }
}
