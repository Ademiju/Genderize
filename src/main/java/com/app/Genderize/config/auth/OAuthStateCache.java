package com.app.Genderize.config.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateCache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public void save(String state, String verifier) {
        store.put(state, verifier);
    }

    public String get(String state) {
        return store.remove(state); // one-time use
    }
}
