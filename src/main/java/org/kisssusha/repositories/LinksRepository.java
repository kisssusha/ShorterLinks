package org.kisssusha.repositories;

import org.kisssusha.entities.Link;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinksRepository extends JpaRepository<Link, Long> {
    Link findByShortUrl(String shortUrl);
}

