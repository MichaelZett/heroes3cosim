package de.zettsystems.h3comsim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nur aktiv, wenn springdoc-openapi auf dem Classpath liegt (= im dev-bootRun). Im
 * bootJar fehlt {@code springdoc-openapi-starter-webmvc-ui} per {@code developmentOnly},
 * der ConditionalOnClass schaltet diese Konfiguration dort lautlos ab.
 */
@Configuration
@ConditionalOnClass(GroupedOpenApi.class)
public class OpenApiConfig {

    @Bean
    public OpenAPI heroes3OpenApi(@Nullable BuildProperties buildProperties) {
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        return new OpenAPI().info(new Info()
                .title("Heroes 3 Combat Simulator API")
                .description("""
                        REST-API des H3-Combat-Simulators. Bietet drei thematische Bereiche:

                        * **Single Battle** — eine deterministische Einzelschlacht inkl. Event-Stream zum Replay.
                        * **Matrix** — asynchrone All-vs-All-Experimente mit aggregierten Stats und Anomalien.
                        * **Catalog** — read-only Listen von Units und Faktionen für den Frontend-Konfigurator.

                        Alle Antworten sind streng JSON. `BattleEvent` ist polymorph (`type`-Diskriminator).
                        """)
                .version(version)
                .contact(new Contact()
                        .name("Michael Zoeller")
                        .email("michael2.zoeller@gmail.com")));
    }

    @Bean
    public GroupedOpenApi h3ApiGroup() {
        return GroupedOpenApi.builder()
                .group("h3-api")
                .pathsToMatch("/api/**")
                .build();
    }
}
