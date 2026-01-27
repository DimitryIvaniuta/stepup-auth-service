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
 *   <li>Create an {@link org.springframework.security.core.Authentication} with authorities derived from JWT claim {@code roles}</li>
 *   <li>Keep the application stateless (no sessions)</li>
 * </ul>
 *
 * <p>Roles are mapped to Spring authorities using the standard {@code ROLE_} prefix.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                JwtPrincipal p = jwtService.parse(auth.substring("Bearer ".length()));
                List<SimpleGrantedAuthority> authorities = toAuthorities(p);

                var a = new UsernamePasswordAuthenticationToken(p, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(a);
            } catch (Exception ignored) {
                // Invalid token -> act as anonymous. Controllers will return 401/403 via security config.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private static List<SimpleGrantedAuthority> toAuthorities(JwtPrincipal p) {
        if (p == null || p.roles() == null || p.roles().isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return p.roles().stream()
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
