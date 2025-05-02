package lucasfend.backend.service;

import lucasfend.backend.model.UserTwitter;
import lucasfend.backend.repository.UserTwitterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserTwitterService {

    @Autowired
    private UserTwitterRepository userTwitterRepository;

    public UserTwitter saveUser(UserTwitter userTwitter) {
        return userTwitterRepository.save(userTwitter);
    }

    public UserTwitter findByTwitterId(String twitterId) {
        return userTwitterRepository.findByTwitterId(twitterId);
    }
}
