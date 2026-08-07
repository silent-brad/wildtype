package org.wildtype.service;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class FragmentRenderer {

    private final SpringTemplateEngine templateEngine;

    public FragmentRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderSightingCard(Object sighting) {
        var ctx = new Context();
        ctx.setVariable("s", sighting);
        return templateEngine.process("fragments/sighting-card", ctx);
    }
}
