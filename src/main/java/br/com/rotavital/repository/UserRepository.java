package br.com.rotavital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.rotavital.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
