package lucasfend.backend.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lucasfend.backend.model.User;
import lucasfend.backend.model.UserTwitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {

    private final long EXPIRATION_TIME = 86400000;
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("cpf", user.getCpf())
                .claim("name", user.getName())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @Value("${jwt.secret}")
    private String secretKey;

    //metodo token twitter
    public String generateTokenForUserTwitter(UserTwitter userTwitter) {

        // Define a data de expiração do token
        long expirationTime = 1000 * 60 * 60;
        Date expirationDate = new Date(System.currentTimeMillis() + expirationTime);

        // Gera o token
        return Jwts.builder()
                .setSubject(userTwitter.getTwitterId())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secretKey) // Algoritmo de assinatura e chave secreta
                .compact();
    }
}
