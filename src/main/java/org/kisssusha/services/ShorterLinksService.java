package org.kisssusha.services;


import lombok.Setter;
import org.kisssusha.entities.Link;
import org.kisssusha.entities.User;
import org.kisssusha.repositories.LinksRepository;
import org.kisssusha.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShorterLinksService {

    public static final String SHORT_LINK_PREFIX = "http://localhost:8080/api/";

    @Autowired
    private LinksRepository linksRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Setter
    @Value("${sf-sl.default-click-limit}")
    private int defaultClickLimit;
    @Setter
    @Value("${sf-sl.default-live-seconds}")
    private long defaultTtlSeconds;

    public User getOrCreateUser(UUID userUuid, String email) {
        if (userUuid == null) {
            userUuid = UUID.randomUUID();
        }
        UUID finalUserUuid = userUuid;
        return usersRepository.findById(finalUserUuid).orElseGet(() -> {
            User newUser = new User();
            newUser.setUuid(finalUserUuid);
            usersRepository.save(newUser);
            return newUser;
        });
    }

    public Link createShortLink(UUID userUuid, String email, String originalUrl,
                                Integer clickLimit, Long liveSeconds) {
        User user = getOrCreateUser(userUuid, email);

        int finalClickLimit = (clickLimit == null) ? defaultClickLimit : Math.max(Math.abs(clickLimit), defaultClickLimit);
        long finalLiveSeconds = (liveSeconds == null) ? defaultTtlSeconds : Math.min(liveSeconds, defaultTtlSeconds);

        Link link = new Link();
        link.setLongUrl(originalUrl);
        link.setShortUrl(generateShortUrl());
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(LocalDateTime.now().plusSeconds(finalLiveSeconds));
        link.setClickLimit(finalClickLimit);
        link.setCurrentClicks(0);
        link.setActive(true);
        link.setUser(user);

        linksRepository.save(link);
        return link;
    }

    private String generateShortUrl() {
        return SHORT_LINK_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    public Link findByShortUrl(String shortUrl) {
        return linksRepository.findByShortUrl(shortUrl);
    }

    public void incClicks(Link link) {
        link.setCurrentClicks(link.getCurrentClicks() + 1);
        linksRepository.save(link);
    }

    public String deactivateLink(Link link, int type) {
        link.setActive(false);
        linksRepository.save(link);

        String message;
        String postfix = "по причине: " + (type == 0 ? "лимит переходов исчерпан" : "время жизни ссылки истекло");
        if (link.getUser() != null) {
            message = String.format("Ссылка %s деактивирована для пользователя с UUID %s.",
                    link.getShortUrl(),
                    link.getUser().getUuid()) + postfix;
        } else {
            message = String.format("Ссылка %s деактивирована.",
                    link.getShortUrl()) + postfix;
        }

        return message;
    }

    public void deleteLink(UUID userUuid, String shortUrl) {
        String tmpUrl = SHORT_LINK_PREFIX + shortUrl;
        Optional<Link> optLink = Optional.ofNullable(linksRepository.findByShortUrl(tmpUrl));
        if (optLink.isPresent()) {
            Link link = optLink.get();
            if (!link.getUser().getUuid().equals(userUuid)) {
                throw new RuntimeException("Чужая ссылка, не может быть удалена!");
            }
            linksRepository.delete(link);
        }
    }

    public void editLimitClicksForLink(Link link, int newLimit) {
        link.setClickLimit(newLimit);
        linksRepository.save(link);
    }

    public void ediLiveSecondsForLink(Link link, LocalDateTime newLimit) {
        link.setExpiresAt(newLimit);
        linksRepository.save(link);
    }
}

