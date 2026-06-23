package com.pridepin.pridepin.security;

import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads user data for Spring Security from the database by username.
 * Only active users are returned; unverified users are marked disabled so login is blocked until email verification.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by username (active only). Builds a Spring Security User with encoded password
     * and ROLE_* authority. Sets enabled = user.isVerified() so unverified users cannot log in.
     *
     * @param username the username (used as the principal identifier in this app)
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException if no active user exists with this username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isVerified())   // Spring Security throws DisabledException on login if false
                .build();
    }
}
