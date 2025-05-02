package lucasfend.backend.controller;

import lucasfend.backend.dto.LoginRequest;
import lucasfend.backend.dto.LoginResponse;
import lucasfend.backend.model.User;
import lucasfend.backend.repository.AuthRepository;
import lucasfend.backend.repository.UserRepository;
import lucasfend.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    private AuthRepository authRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private JwtUtil jwtUtil;

    public AuthController(AuthRepository authRepository, BCryptPasswordEncoder bCryptPasswordEncoder, JwtUtil jwtUtil) {
        this.authRepository = authRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtUtil = jwtUtil;
    }


    @PostMapping
    public ResponseEntity<LoginResponse> auth(@RequestBody LoginRequest r) {
        User user = authRepository.findByEmail(r.getEmail());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("User not found."));
        }

        boolean correctPassword = bCryptPasswordEncoder.matches(r.getPassword(), user.getPassword());

        if (!correctPassword) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid password"));
        }

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(token));
    }
}
