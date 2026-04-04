package com.freelancehub.repository;

import com.freelancehub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - database access for the User entity.
 *
 * Extending JpaRepository gives us these methods for FREE:
 *   save(user)        - INSERT or UPDATE
 *   findById(id)      - SELECT by primary key
 *   findAll()         - SELECT all users
 *   delete(user)      - DELETE
 *   count()           - COUNT(*)
 *
 * We add custom query methods by following Spring's naming convention.
 * Spring automatically generates the SQL from the method name.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Spring translates this method name to:
     *   SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Spring generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}
