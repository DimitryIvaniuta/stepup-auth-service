package com.github.dimitryivaniuta.gateway.stepupauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Bearer authentication filter.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Parse {@code Authorization: Bearer ...}</li>
 *   <li>Create Authentication with authorities derived from JWT {@code roles}</li>
 * </ul>
 *
 * <p>Roles are mapped to Spring authorities using the standard {@code ROLE_} prefix.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Don’t override if someone already authenticated earlier in the chain.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (auth != null && auth.startsWith(BEARER_PREFIX)) {
                String token = auth.substring(BEARER_PREFIX.length()).trim();
                if (!token.isEmpty()) {
                    try {
                        JwtPrincipal principal = jwtService.parse(token);
                        List<SimpleGrantedAuthority> authorities = toAuthorities(principal);

                        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception ex) {
                        // Invalid token -> treat as anonymous.
                        SecurityContextHolder.clearContext();
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private static List<SimpleGrantedAuthority> toAuthorities(JwtPrincipal p) {
        List<String> roles = (p == null) ? null : p.roles();
        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return roles.stream()
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/public") || path.startsWith("/actuator");
    }
}
