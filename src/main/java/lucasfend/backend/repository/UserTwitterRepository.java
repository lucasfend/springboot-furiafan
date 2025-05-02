package lucasfend.backend.repository;

import lucasfend.backend.model.UserTwitter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTwitterRepository extends JpaRepository<UserTwitter, String> {
    UserTwitter findByTwitterId(String twitterId);
}
