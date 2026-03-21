package com.whatsappomie.bot.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Render pode enviar {@code SPRING_DATASOURCE_URL} vazio ({@code ""}); nesse caso o Spring ignora o
 * default do YAML e o Hibernate falha. Aqui definimos {@code spring.datasource.url} em
 * {@link System#setProperty} antes do boot, com precedência correta.
 *
 * <p>Ordem: {@code SPRING_DATASOURCE_URL} preenchida → {@code DATABASE_URL} (postgres://) → JDBC
 * Supabase por defeito.
 *
 * <p>Render + Supabase: erro {@code Network is unreachable} na conexão direta costuma ser IPv6 vs IPv4.
 * Use no Supabase a string do <strong>pooler (Session)</strong> ou add-on IPv4; passe-a por env, não pelo
 * default em código.
 */
public final class DatabaseUrlEnvironmentSetup {

    private static final String DEFAULT_JDBC =
            "jdbc:postgresql://db.rzvdnjmwffvlqxkbhres.supabase.co:5432/postgres?sslmode=require";

    private DatabaseUrlEnvironmentSetup() {}

    public static void apply() {
        resolveDataSourceUrl();
        requirePostgresPasswordIfNeeded();
    }

    /**
     * Evita NPE obscuro no Hibernate ao falhar JDBC: sem senha a conexão quebra e a stack some confusa.
     */
    private static void requirePostgresPasswordIfNeeded() {
        String url = System.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:postgresql:")) {
            return;
        }
        String fromProp = firstNonBlank(System.getProperty("spring.datasource.password"));
        String fromEnv = firstNonBlank(System.getenv("SPRING_DATASOURCE_PASSWORD"));
        if (fromProp != null || fromEnv != null) {
            return;
        }
        throw new IllegalStateException(
                "PostgreSQL sem senha: defina SPRING_DATASOURCE_PASSWORD no ambiente ou use DATABASE_URL "
                        + "com user:password@ (Render/Supabase).");
    }

    private static void resolveDataSourceUrl() {
        String explicit = firstNonBlank(System.getenv("SPRING_DATASOURCE_URL"));
        if (explicit != null) {
            System.setProperty("spring.datasource.url", explicit);
            return;
        }
        String databaseUrl = firstNonBlank(System.getenv("DATABASE_URL"));
        if (databaseUrl != null) {
            Parsed p = parsePostgresDatabaseUrl(databaseUrl);
            System.setProperty("spring.datasource.url", p.jdbcUrl());
            setIfEnvBlank("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", p.username());
            setIfEnvBlank("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", p.password());
            return;
        }
        System.setProperty("spring.datasource.url", DEFAULT_JDBC);
    }

    private static void setIfEnvBlank(String envKey, String springKey, String value) {
        if (firstNonBlank(System.getenv(envKey)) == null) {
            System.setProperty(springKey, value);
        }
    }

    private static String firstNonBlank(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static Parsed parsePostgresDatabaseUrl(String databaseUrl) {
        String normalized =
                databaseUrl.replaceFirst("^postgres(ql)?:", "http:").replaceFirst("^jdbc:postgresql:", "http:");
        URI uri = URI.create(normalized);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL sem credenciais (user:pass@)");
        }
        int colon = userInfo.indexOf(':');
        String user = urlDecode(colon > 0 ? userInfo.substring(0, colon) : userInfo);
        String pass = colon > 0 ? urlDecode(userInfo.substring(colon + 1)) : "";
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            throw new IllegalArgumentException("DATABASE_URL sem nome da base (path)");
        }
        String db = path.startsWith("/") ? path.substring(1) : path;
        String query = uri.getRawQuery();
        String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        if (query != null && !query.isEmpty()) {
            jdbc += "?" + query;
        }
        return new Parsed(jdbc, user, pass);
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private record Parsed(String jdbcUrl, String username, String password) {}
}
