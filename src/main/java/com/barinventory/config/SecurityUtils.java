package com.barinventory.config;

import org.springframework.security.core.context.SecurityContextHolder;

import com.barinventory.services.CustomUserDetails;

public class SecurityUtils {

    public static CustomUserDetails getCurrentUser() {

        return (CustomUserDetails)
                SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public static Long getBarId() {
        return getCurrentUser().getBarId();
    }

    public static String getUsername() {
        return getCurrentUser().getUsername();
    }
}