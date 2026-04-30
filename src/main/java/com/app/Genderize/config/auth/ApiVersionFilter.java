package com.app.Genderize.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiVersionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            String v = request.getHeader("X-API-Version");

            if (!"1".equals(v)) {
                response.setStatus(400);
                response.getWriter().write(
                        "{\"status\":\"error\",\"message\":\"API version header required\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
