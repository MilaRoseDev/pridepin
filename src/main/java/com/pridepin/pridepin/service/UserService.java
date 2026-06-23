package com.pridepin.pridepin.service;

import com.pridepin.pridepin.dto.request.UpdateUserRequest;
import com.pridepin.pridepin.dto.response.UserResponse;
import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.exception.ResourceNotFoundException;
import com.pridepin.pridepin.exception.UserAlreadyExistsException;
import com.pridepin.pridepin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User profile and admin operations: get/update/delete current user, and admin list/get/delete by ID.
 * All mutations use soft-delete (active = false).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Returns the profile of the given user (must be active). */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        return toResponse(findActiveUser(username));
    }

    /** Updates the user's username, email, and/or password; only non-null fields are changed. Rejects if username/email already taken. */
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request, String username) {
        User user = findActiveUser(username);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepository.save(user));
    }

    /** Soft-deletes the user (sets active = false). */
    @Transactional
    public void deleteCurrentUser(String username) {
        User user = findActiveUser(username);
        user.setActive(false);
        userRepository.save(user);
    }

    /** Returns all users (active and inactive). Used by admin. */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Returns a user by ID (any active/inactive). Used by admin. */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id)));
    }

    /** Soft-deletes the user with the given ID. */
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        userRepository.save(user);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Finds an active user by username or throws ResourceNotFoundException. */
    private User findActiveUser(String username) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /** Maps User entity to UserResponse DTO. */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
