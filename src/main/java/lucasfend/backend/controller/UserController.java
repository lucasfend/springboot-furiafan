package lucasfend.backend.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lucasfend.backend.model.User;
import lucasfend.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cadastro")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {
    private UserRepository userRepository;

    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public User registerUsr(@RequestBody User data) {
        System.out.println("Recebido: " + data); // LOG PARA DEBUG
        String encryptedPassword = bCryptPasswordEncoder.encode(data.getPassword());
        data.setPassword(encryptedPassword);

        return userRepository.save(data);
    }

}
