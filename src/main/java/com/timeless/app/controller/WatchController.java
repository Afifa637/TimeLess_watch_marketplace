package com.timeless.app.controller;

import com.timeless.app.dto.request.WatchCreateRequest;
import com.timeless.app.dto.request.WatchUpdateRequest;
import com.timeless.app.dto.response.WatchResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.WatchService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watches")
@RequiredArgsConstructor
@Tag(name = "Watches", description = "Browse and manage watch listings")
public class WatchController {

    private final WatchService watchService;

    @GetMapping
    @Operation(summary = "Get active watches with filtering and pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watches fetched successfully")
    })
    public ResponseEntity<Page<WatchResponse>> getActiveWatches(
        @RequestParam(required = false) List<String> brands,
        @RequestParam(required = false) List<String> categories,
        @RequestParam(required = false) List<String> conditions,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String search,
        @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(watchService.getActiveWatches(brands, categories, conditions, minPrice, maxPrice, search, pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get current seller's watch listings")
    public ResponseEntity<List<WatchResponse>> getMyWatches() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(watchService.getWatchesBySeller(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get watch detail by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watch fetched successfully", content = @Content(schema = @Schema(implementation = WatchResponse.class))),
        @ApiResponse(responseCode = "404", description = "Watch not found")
    })
    public ResponseEntity<WatchResponse> getWatchById(@PathVariable Long id) {
        return ResponseEntity.ok(watchService.getWatchById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create a new watch listing as seller")
    public ResponseEntity<WatchResponse> createWatch(@Valid @RequestBody WatchCreateRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(watchService.createWatch(request, user.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Update an existing watch listing")
    public ResponseEntity<WatchResponse> updateWatch(@PathVariable Long id, @Valid @RequestBody WatchUpdateRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(watchService.updateWatch(id, request, user.getId(), user.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(summary = "Delete a watch listing")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Watch deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteWatch(@PathVariable Long id) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        watchService.deleteWatch(id, user.getId(), user.getRole());
        return ResponseEntity.noContent().build();
    }
}
