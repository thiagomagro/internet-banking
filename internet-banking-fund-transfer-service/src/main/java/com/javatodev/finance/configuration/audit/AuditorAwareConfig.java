package com.javatodev.finance.configuration.audit;

import com.javatodev.finance.configuration.filter.ApiRequestContextHolder;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class AuditorAwareConfig implements AuditorAware<String> {
    @Override public Optional<String> getCurrentAuditor() {
        String authId = ApiRequestContextHolder.getContext().getAuthId();
        if (authId == null || authId.isEmpty()) {
            return Optional.of("SYSTEM_USER");
        }
        return Optional.of(authId);
    }
}
