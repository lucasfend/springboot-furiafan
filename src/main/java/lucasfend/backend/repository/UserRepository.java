package lucasfend.backend.repository;

import lucasfend.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    User findByCpf(String cpf);
}
