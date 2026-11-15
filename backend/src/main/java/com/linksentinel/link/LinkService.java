package com.linksentinel.link;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.linksentinel.api.CreateLinkRequest;
import com.linksentinel.api.LinkResponse;
import com.linksentinel.api.ScreeningResponse;
import com.linksentinel.api.StatsResponse;
import com.linksentinel.click.ClickRepository;
import com.linksentinel.click.ClickTracker;
import com.linksentinel.config.AppProperties;
import com.linksentinel.screen.ScreeningResult;
import com.linksentinel.screen.ScreeningService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LinkService {

    private static final Set<String> RESERVED = Set.of("api", "v3", "error", "actuator", "swagger");

    private final LinkRepository links;
    private final ClickRepository clicks;
    private final LinkCache cache;
    private final ScreeningService screening;
    private final ClickTracker tracker;
    private final AppProperties props;

    public LinkService(
            LinkRepository links,
            ClickRepository clicks,
            LinkCache cache,
            ScreeningService screening,
            ClickTracker tracker,
            AppProperties props) {
        this.links = links;
        this.clicks = clicks;
        this.cache = cache;
        this.screening = screening;
        this.tracker = tracker;
        this.props = props;
    }

    public LinkResponse create(long ownerId, CreateLinkRequest request) {
        String url = normalizeUrl(request.url());
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry must be in the future");
        }
        ScreeningResult screen = screening.screen(url);
        if (request.alias() != null) {
            String alias = checkAlias(request.alias());
            if (links.existsByCode(alias)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That alias is already in use");
            }
            try {
                return insert(ownerId, alias, url, request.expiresAt(), screen);
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That alias is already in use");
            }
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = Base62.encode(links.nextCodeValue());
            if (RESERVED.contains(code.toLowerCase(Locale.ROOT)) || links.existsByCode(code)) {
                continue;
            }
            try {
                return insert(ownerId, code, url, request.expiresAt(), screen);
            } catch (DataIntegrityViolationException ex) {
                // Another alias took this code between the check and the insert.
            }
        }
        throw new IllegalStateException("Could not allocate a short code");
    }

    public List<LinkResponse> list(long ownerId) {
        return links.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toResponse).toList();
    }

    public void delete(long ownerId, String code) {
        Link link = owned(ownerId, code);
        links.delete(link);
        cache.evict(code);
    }

    public StatsResponse stats(long ownerId, String code) {
        Link link = owned(ownerId, code);
        List<StatsResponse.DayCount> days = clicks.countByDay(link.getId()).stream()
                .map(row -> new StatsResponse.DayCount(
                        java.time.LocalDate.parse(row[0].toString()), ((Number) row[1]).longValue()))
                .toList();
        List<StatsResponse.LabelCount> referrers = clicks.countByReferrer(link.getId()).stream()
                .map(row -> new StatsResponse.LabelCount(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();
        List<StatsResponse.LabelCount> devices = clicks.countByDevice(link.getId()).stream()
                .map(row -> new StatsResponse.LabelCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
        return new StatsResponse(
                link.getCode(),
                link.getUrl(),
                clicks.countByLinkId(link.getId()),
                link.getCreatedAt(),
                link.getExpiresAt(),
                screeningOf(link),
                days,
                referrers,
                devices);
    }

    public URI resolve(String code, String referrer, String userAgent) {
        var cached = cache.get(code);
        if (cached.isPresent()) {
            tracker.track(cached.get().id(), referrer, userAgent);
            return URI.create(cached.get().url());
        }
        Link link = links.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        if (link.getExpiresAt() != null && !link.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
        }
        cache.put(link);
        tracker.track(link.getId(), referrer, userAgent);
        return URI.create(link.getUrl());
    }

    private LinkResponse insert(long ownerId, String code, String url, Instant expiresAt, ScreeningResult screen) {
        Link link = new Link();
        link.setOwnerId(ownerId);
        link.setCode(code);
        link.setUrl(url);
        link.setExpiresAt(expiresAt);
        link.setScreenSafe(screen.safe());
        link.setScreenReason(screen.reason());
        link.setScreenTitle(screen.title());
        link.setScreenSummary(screen.summary());
        link.setScreenSource(screen.source());
        links.saveAndFlush(link);
        return toResponse(link);
    }

    private Link owned(long ownerId, String code) {
        Link link = links.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        if (link.getOwnerId() != ownerId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found");
        }
        return link;
    }

    private LinkResponse toResponse(Link link) {
        String base = props.baseUrl().endsWith("/")
                ? props.baseUrl().substring(0, props.baseUrl().length() - 1)
                : props.baseUrl();
        return new LinkResponse(
                link.getCode(),
                base + "/" + link.getCode(),
                link.getUrl(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                screeningOf(link));
    }

    private static ScreeningResponse screeningOf(Link link) {
        return new ScreeningResponse(
                link.isScreenSafe(),
                link.getScreenReason(),
                link.getScreenTitle(),
                link.getScreenSummary(),
                link.getScreenSource());
    }

    private static String checkAlias(String alias) {
        if (RESERVED.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That alias is reserved");
        }
        return alias;
    }

    private static String normalizeUrl(String raw) {
        String url = raw.trim();
        if (url.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is too long");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (java.net.URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is not valid");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must start with http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must include a host");
        }
        return url;
    }
}
