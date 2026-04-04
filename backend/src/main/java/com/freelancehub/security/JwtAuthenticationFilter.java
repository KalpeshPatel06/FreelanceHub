package com.freelancehub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * This filter runs ONCE per HTTP request (OncePerRequestFilter).
 * It sits in the Spring Security filter chain and does the following:
 *
 *   1. Read the "Authorization: Bearer <token>" header
 *   2. Extract the email from the JWT
 *   3. Load the user from the database
 *   4. Validate the token
 *   5. If valid, set the authenticated user in the SecurityContext
 *      (this tells Spring "this request is from a legitimate user")
 *
 * If anything fails, the filter just passes the request along
 * and Spring Security will reject it later (401 Unauthorized).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Read Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If no token, skip (Spring Security will handle as unauthenticated)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        try {
            // Step 3: Extract email from token
            final String email = jwtUtil.extractEmail(jwt);

            // Step 4: Only proceed if email is valid and user not already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 5: Validate token and set authentication
                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials (not needed after auth)
                                    userDetails.getAuthorities()   // roles/permissions
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // Tell Spring Security this request is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
