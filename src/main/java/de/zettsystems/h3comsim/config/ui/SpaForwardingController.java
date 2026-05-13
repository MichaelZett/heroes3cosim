package de.zettsystems.h3comsim.config.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Direct-Refresh und Deep-Links auf React-Routen wie {@code /battle} würden vom
 * Spring-MVC-Resource-Handler mit 404 quittiert (es gibt keine Datei {@code /battle}).
 * Wir leiten die bekannten SPA-Routen explizit auf {@code index.html} um — der React-
 * Router übernimmt dann das Routing client-seitig. {@code /} wird über Spring Boots
 * Welcome-Page-Mechanismus bereits automatisch auf {@code static/index.html} gemappt.
 */
@Controller
public class SpaForwardingController {

    @GetMapping("/battle")
    public String forwardBattle() {
        return "forward:/index.html";
    }
}
