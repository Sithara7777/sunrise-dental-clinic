package lk.icbt.cis6003.dental.server.web;

import lk.icbt.cis6003.dental.server.service.HelpService;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategyFactory;
import lk.icbt.cis6003.dental.server.service.validation.BookingValidationChain;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Requirement 5 - the Help section.
 *
 * <p>Reachable without signing in, on purpose: a new member of staff who
 * cannot get past the login screen is exactly the person who needs the "Signing
 * in" topic, and locking the instructions behind the thing they cannot do would
 * be perverse. The content is instructional only and exposes no patient
 * data.</p>
 *
 * <p>The page also lists the booking rules and pricing rules the running system
 * actually has, read from the validation chain and the pricing factory rather
 * than retyped. Help that is generated from the live configuration cannot drift
 * out of date the way a hand-written page does.</p>
 */
@Controller
public class HelpWebController {

    private final HelpService helpService;
    private final BookingValidationChain validationChain;
    private final PricingStrategyFactory pricingStrategyFactory;

    public HelpWebController(HelpService helpService,
                             BookingValidationChain validationChain,
                             PricingStrategyFactory pricingStrategyFactory) {
        this.helpService = helpService;
        this.validationChain = validationChain;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    @GetMapping("/help")
    public String help(Model model) {
        model.addAttribute("topics", helpService.listTopics());
        model.addAttribute("bookingRules", validationChain.describeRules());
        model.addAttribute("pricingRules", pricingStrategyFactory.getAll().values());
        model.addAttribute("pageTitle", "Help");
        return "help";
    }
}
