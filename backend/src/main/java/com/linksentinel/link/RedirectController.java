package com.linksentinel.link;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final LinkService links;

    public RedirectController(LinkService links) {
        this.links = links;
    }

    @GetMapping("/{code:[0-9A-Za-z]+}")
    @SecurityRequirements
    @Operation(summary = "Redirect to the target URL", security = {})
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            @RequestHeader(value = "Referer", required = false) String referrer,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        URI target = links.resolve(code, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }
}
