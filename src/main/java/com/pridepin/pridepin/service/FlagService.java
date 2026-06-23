package com.pridepin.pridepin.service;

import com.pridepin.pridepin.dto.request.FlagRequest;
import com.pridepin.pridepin.dto.request.ResolveFlagRequest;
import com.pridepin.pridepin.dto.response.FlagResponse;
import com.pridepin.pridepin.entity.Flag;
import com.pridepin.pridepin.entity.Location;
import com.pridepin.pridepin.entity.User;
import com.pridepin.pridepin.enums.FlagResolution;
import com.pridepin.pridepin.enums.FlagStatus;
import com.pridepin.pridepin.exception.BadRequestException;
import com.pridepin.pridepin.exception.ResourceNotFoundException;
import com.pridepin.pridepin.repository.FlagRepository;
import com.pridepin.pridepin.repository.LocationRepository;
import com.pridepin.pridepin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flag lifecycle: community members submit flags; admins list and resolve them.
 * One open flag per (reporter, location) pair is enforced at the service layer.
 */
@Service
@RequiredArgsConstructor
public class FlagService {

    private final FlagRepository    flagRepository;
    private final LocationRepository locationRepository;
    private final UserRepository    userRepository;

    /**
     * Submits a flag against a location. Throws if the user already has an open flag
     * for the same location, or if the location does not exist.
     */
    @Transactional
    public FlagResponse submitFlag(UUID locationId, FlagRequest request, String username) {
        Location location = locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        User reporter = findActiveUser(username);

        if (flagRepository.existsByLocationIdAndReporterIdAndStatus(
                locationId, reporter.getId(), FlagStatus.OPEN)) {
            throw new BadRequestException("You have already flagged this location. Your report is awaiting admin review.");
        }

        Flag flag = Flag.builder()
                .location(location)
                .reporter(reporter)
                .reason(request.getReason())
                .note(request.getNote())
                .build();

        return toResponse(flagRepository.save(flag));
    }

    /**
     * Returns a paginated list of flags filtered by status. Admin use only.
     * Defaults to OPEN if status is null.
     */
    @Transactional(readOnly = true)
    public Page<FlagResponse> listFlags(FlagStatus status, Pageable pageable) {
        FlagStatus filter = status != null ? status : FlagStatus.OPEN;
        return flagRepository.findAllByStatusOrderByCreatedAtAsc(filter, pageable)
                .map(this::toResponse);
    }

    /**
     * Resolves a flag. DISMISS closes it without touching the location.
     * DEACTIVATE closes it and soft-deletes the location.
     * Admin use only.
     */
    @Transactional
    public FlagResponse resolveFlag(UUID flagId, ResolveFlagRequest request, String adminUsername) {
        Flag flag = flagRepository.findByIdAndActiveTrue(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Flag", "id", flagId));

        if (flag.getStatus() != FlagStatus.OPEN) {
            throw new BadRequestException("Flag has already been resolved.");
        }

        User admin = findActiveUser(adminUsername);

        flag.setStatus(request.getAction() == FlagResolution.DEACTIVATE
                ? FlagStatus.ACTIONED : FlagStatus.DISMISSED);
        flag.setResolvedBy(admin);
        flag.setResolvedAt(LocalDateTime.now());

        if (request.getAction() == FlagResolution.DEACTIVATE) {
            Location location = flag.getLocation();
            location.setActive(false);
            locationRepository.save(location);
        }

        return toResponse(flagRepository.save(flag));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private User findActiveUser(String username) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private FlagResponse toResponse(Flag flag) {
        return FlagResponse.builder()
                .id(flag.getId())
                .locationId(flag.getLocation().getId())
                .locationName(flag.getLocation().getName())
                .reporterId(flag.getReporter().getId())
                .reporterUsername(flag.getReporter().getUsername())
                .reason(flag.getReason())
                .note(flag.getNote())
                .status(flag.getStatus())
                .resolvedById(flag.getResolvedBy() != null ? flag.getResolvedBy().getId() : null)
                .resolvedByUsername(flag.getResolvedBy() != null ? flag.getResolvedBy().getUsername() : null)
                .resolvedAt(flag.getResolvedAt())
                .createdAt(flag.getCreatedAt())
                .build();
    }
}
