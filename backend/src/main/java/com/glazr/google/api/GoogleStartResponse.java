package com.glazr.google.api;

public record GoogleStartResponse(String authorizationUrl) {
    public static GoogleStartResponse google() { return new GoogleStartResponse("/oauth2/authorization/google"); }
}
