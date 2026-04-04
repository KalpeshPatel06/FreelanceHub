package com.freelancehub.security;

import com.freelancehub.entity.User;
import com.freelancehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomUserDetailsService
 *
 * Spring Security calls this when it needs to look up a user by their
 * identifier (in our case, the email address).
 *
 * We return a Spring Security UserDetails object that contains:
 *   - The username (email in our case)
 *   - The hashed password (Spring will compare this with what the user typed)
 *   - The user's roles/authorities
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        // Convert our User entity into Spring Security's UserDetails format.
        // The role becomes a "ROLE_CLIENT" or "ROLE_FREELANCER" authority.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())    // already bcrypt-hashed
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }
}
