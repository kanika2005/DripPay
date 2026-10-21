package com.drippay.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds JDBC URL from MYSQL_HOST when DB_URL is unset; rejects doc placeholders left in DB_URL.
 */
public class RenderDatasourceEnvPostProcessor implements EnvironmentPostProcessor {

    private static final Set<String> PLACEHOLDER_HOSTS = Set.of(
            "YOUR_MYSQL_HOST",
            "YOUR-HOST",
            "YOUR_MYSQL"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DB_URL");
        if (dbUrl != null && containsPlaceholder(dbUrl)) {
            throw new IllegalStateException(
                    "DB_URL still contains a placeholder host (e.g. YOUR_MYSQL_HOST). "
                            + "On Render: open your MySQL private service → Connect → copy the internal hostname "
                            + "(like mysql-abc123), then set either:\n"
                            + "  DB_URL=jdbc:mysql://mysql-abc123:3306/drippay?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC\n"
                            + "or delete DB_URL and set MYSQL_HOST, MYSQL_DATABASE, DB_USERNAME, DB_PASSWORD.\n"
                            + "See docs/DEPLOY_RENDER.md in the repo.");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            String host = environment.getProperty("MYSQL_HOST");
            if (host != null && !host.isBlank()) {
                String port = environment.getProperty("MYSQL_PORT", "3306");
                String database = environment.getProperty("MYSQL_DATABASE", "drippay");
                String built = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("DB_URL", built);
                environment.getPropertySources().addFirst(new MapPropertySource("renderMysqlJdbc", map));
            }
        }
    }

    private static boolean containsPlaceholder(String dbUrl) {
        String upper = dbUrl.toUpperCase();
        for (String token : PLACEHOLDER_HOSTS) {
            if (upper.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
