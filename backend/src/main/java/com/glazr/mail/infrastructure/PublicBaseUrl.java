package com.glazr.mail.infrastructure;

import java.net.URI;

public final class PublicBaseUrl {
    private final String origin;

    public PublicBaseUrl(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("GLAZR_PUBLIC_BASE_URL inválida", exception);
        }
        boolean validScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        boolean rootPath = uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());
        if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !rootPath) {
            throw new IllegalArgumentException("GLAZR_PUBLIC_BASE_URL deve conter somente uma origem HTTP(S)");
        }
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        this.origin = uri.getScheme().toLowerCase() + "://" + uri.getHost() + port;
    }

    public String invitationLink(String rawToken) { return link("/activate", rawToken); }
    public String passwordResetLink(String rawToken) { return link("/reset-password", rawToken); }

    private String link(String path, String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Token inválido");
        return origin + path + "#token=" + token;
    }
}
