package org.kisssusha.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String longUrl;
    private String shortUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private int currentClicks;
    private int clickLimit;

    @ManyToOne
    @JoinColumn(name = "users_uuid", nullable = false)
    private User user;

    private boolean active;
}

