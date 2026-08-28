package com.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Silently 404 missing static resources (favicon.ico, etc.) — no error page, no log noise. */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResource(NoResourceFoundException ex,
                                  HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception on {} {} from ip={} ua={}: {}",
                request.getMethod(), request.getRequestURI(), clientIp(request),
                request.getHeader("User-Agent"), ex.getMessage(), ex);
        // Never expose raw exception messages to the user
        model.addAttribute("errorMessage", "We couldn't process your request. Please try again or go back.");
        model.addAttribute("referer", request.getHeader("Referer"));
        return "error";
    }

    /** Render sits in front as a reverse proxy, so the real client IP is in X-Forwarded-For, not getRemoteAddr(). */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
