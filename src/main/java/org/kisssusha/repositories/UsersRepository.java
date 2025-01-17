package org.kisssusha.repositories;

import org.kisssusha.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsersRepository extends JpaRepository<User, UUID> {
}
