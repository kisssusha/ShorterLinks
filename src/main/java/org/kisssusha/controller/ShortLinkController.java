package org.kisssusha.controller;

import org.kisssusha.entities.Link;
import org.kisssusha.services.ShorterLinksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.kisssusha.services.ShorterLinksService.SHORT_LINK_PREFIX;

@RestController
@RequestMapping("/api")
public class ShortLinkController {

    @Autowired
    private ShorterLinksService shortLinkService;

    @PostMapping("/create")
    public ResponseEntity<String> createShortLink(
            @RequestParam(required = false) String userUuid,
            @RequestParam(required = false) String email,
            @RequestParam String originalUrl,
            @RequestParam(required = false) Integer clickLimit,
            @RequestParam(required = false) Long liveSeconds
    ) {
        UUID uuid = (userUuid == null || userUuid.isEmpty()) ? null : UUID.fromString(userUuid);
        Link link = shortLinkService.createShortLink(uuid, email, originalUrl, clickLimit, liveSeconds);

        return ResponseEntity.ok("Ссылка: " + link.getShortUrl() + "\nВаш UUID: " + link.getUser().getUuid());
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<String> redirectToOriginal(
            @PathVariable String shortUrl
    ) {
        Link link = shortLinkService.findByShortUrl(SHORT_LINK_PREFIX + shortUrl);

        if (link == null || !link.isActive()) {
            return ResponseEntity.badRequest().body("Ссылка недоступна!");
        }

        if (link.getCurrentClicks() >= link.getClickLimit()) {
            String message = shortLinkService.deactivateLink(link, 0);
            return ResponseEntity.badRequest().body(message);
        }

        if (link.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            String message = shortLinkService.deactivateLink(link, 1);
            return ResponseEntity.badRequest().body(message);
        }

        shortLinkService.incClicks(link);

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(link.getLongUrl()))
                .build();
    }

    @PutMapping("/editLimit/{shortUrl}")
    public ResponseEntity<String> editLimitClicksForLink(
            @RequestParam String userUuid,
            @PathVariable String shortUrl,
            @RequestParam int newLimit
    ) {
        UUID uuid = UUID.fromString(userUuid);

        Optional<Link> optLink = Optional.ofNullable(shortLinkService.findByShortUrl(SHORT_LINK_PREFIX + shortUrl));
        if (optLink.isEmpty()) {
            return ResponseEntity.badRequest().body("Ссылка не найдена!");
        }
        Link link = optLink.get();

        if (!link.getUser().getUuid().equals(uuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Нет доступа для изменения лимита!");
        }

        shortLinkService.editLimitClicksForLink(link, newLimit);

        return ResponseEntity.ok("Лимит обновлён на " + newLimit + " для ссылки = " + link.getShortUrl());
    }

    @PutMapping("/ediLiveSeconds/{shortUrl}")
    public ResponseEntity<String> ediLiveSecondsForLink(
            @RequestParam String userUuid,
            @PathVariable String shortUrl,
            @RequestParam long LiveSecond
    ) {
        UUID uuid = UUID.fromString(userUuid);
        LocalDateTime localDateTime = LocalDateTime.now().plusSeconds(LiveSecond);

        Optional<Link> optLink = Optional.ofNullable(shortLinkService.findByShortUrl(SHORT_LINK_PREFIX + shortUrl));
        if (optLink.isEmpty()) {
            return ResponseEntity.badRequest().body("Ссылка не найдена!");
        }
        Link link = optLink.get();

        if (!link.getUser().getUuid().equals(uuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Нет доступа для изменения срока жизни ссылки!");
        }

        shortLinkService.ediLiveSecondsForLink(link, localDateTime);

        return ResponseEntity.ok("Срока жизни обновлён на " + localDateTime + " для ссылки = " + link.getShortUrl());
    }

    @DeleteMapping("/delete/{shortUrl}")
    public ResponseEntity<String> deleteLink(
            @RequestParam String userUuid,
            @PathVariable String shortUrl
    ) {
        UUID uuid = UUID.fromString(userUuid);
        shortLinkService.deleteLink(uuid, shortUrl);
        return ResponseEntity.ok("Ссылка удалена!");
    }
}
