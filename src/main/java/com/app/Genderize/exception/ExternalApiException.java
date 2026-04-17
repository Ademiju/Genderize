package com.app.Genderize.exception;

public class ExternalApiException extends RuntimeException {
    private final String api;

    public ExternalApiException(String api) {
        super(api + " returned an invalid response");
        this.api = api;
    }

    public String getApi() {
        return api;
    }
}