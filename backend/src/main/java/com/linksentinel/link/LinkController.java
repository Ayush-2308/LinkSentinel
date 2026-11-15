package com.linksentinel.link;

import java.util.List;

import com.linksentinel.api.CreateLinkRequest;
import com.linksentinel.api.LinkResponse;
import com.linksentinel.api.StatsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService links;

    public LinkController(LinkService links) {
        this.links = links;
    }

    @PostMapping
    @Operation(summary = "Create a short link")
    public ResponseEntity<LinkResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(links.create(userId, request));
    }

    @GetMapping
    @Operation(summary = "List your links")
    public List<LinkResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return links.list(userId);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one of your links")
    public void delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String code) {
        links.delete(userId, code);
    }

    @GetMapping("/{code}/stats")
    @Operation(summary = "Click totals, referrers, devices, and the screening verdict")
    public StatsResponse stats(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String code) {
        return links.stats(userId, code);
    }
}
