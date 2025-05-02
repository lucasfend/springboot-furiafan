package lucasfend.backend.repository;

import lucasfend.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface AuthRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
