package lucasfend.backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lucasfend.backend.dto.LoginResponse;
import lucasfend.backend.model.UserTwitter;
import lucasfend.backend.repository.UserTwitterRepository;
import lucasfend.backend.security.JwtUtil;
import lucasfend.backend.service.TwitterService;
import lucasfend.backend.service.UserTwitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class TwitterOAuthController {

    @Value("${twitter.client-id}")
    private String clientId;

    @Value("${twitter.redirect-uri}")
    private String redirectUri;

    @Value("${twitter.client-secret}")
    private String clientSecret;

    @Autowired
    private UserTwitterService userTwitterService;

    @Autowired
    private TwitterService twitterService;

    @Autowired
    private UserTwitterRepository userTwitterRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final RestTemplate restTemplate;

    public TwitterOAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String generateCodeVerifier() {
        int length = 43;
        StringBuilder codeVerifier = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            codeVerifier.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt(random.nextInt(64)));
        }
        return codeVerifier.toString();
    }

    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @GetMapping("/twitter/auth-url")
    public ResponseEntity<String> getTwitterAuthUrl(HttpSession session) {
        try {
            // Gerando o code verifier e o code challenge
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);

            // Armazenar o codeVerifier na sessão
            session.setAttribute("codeVerifier", codeVerifier);

            // Gerar um valor aleatório para o state
            String state = generateRandomState();
            session.setAttribute("state", state);

            // Garantir que a URL de redirecionamento seja exatamente a mesma configurada no Twitter
            String scopes = "tweet.read%20users.read";

            String twitterAuthUrl = UriComponentsBuilder
                    .fromUriString("https://twitter.com/i/oauth2/authorize")
                    .queryParam("response_type", "code")
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("scope", scopes)
                    .queryParam("state", state)
                    .queryParam("code_challenge", codeChallenge)
                    .queryParam("code_challenge_method", "S256")
                    .build()
                    .toUriString();

            System.out.println("URL de autorização gerada: " + twitterAuthUrl);

            // Retornar a URL de autorização para o frontend
            return ResponseEntity.ok(twitterAuthUrl);
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar o code challenge: " + e.getMessage());
        }
    }

    @GetMapping("/login/oauth2/code/twitter")
    public void twitterCallback(@RequestParam String code,
                                @RequestParam(required = false) String state,
                                HttpSession session,
                                HttpServletResponse response) throws IOException {

        System.out.println("Callback recebido! Code: " + code + ", State: " + state);

        try {
            // Verificar o state para evitar CSRF
            String savedState = (String) session.getAttribute("state");
            if (state != null && !state.equals(savedState)) {
                response.sendRedirect("http://localhost:4200/login?error=invalid_state");
                return;
            }

            // Recuperar o code_verifier da sessão
            String codeVerifier = (String) session.getAttribute("codeVerifier");
            if (codeVerifier == null) {
                response.sendRedirect("http://localhost:4200/login?error=missing_code_verifier");
                return;
            }

            // Preparar o token request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            headers.add("Authorization", "Basic " + new String(encodedAuth));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", code);
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("redirect_uri", redirectUri);
            body.add("code_verifier", codeVerifier);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            System.out.println("Enviando requisição para obter token de acesso");

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://api.twitter.com/2/oauth2/token", request, Map.class
            );

            if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
                String accessToken = (String) tokenResponse.getBody().get("access_token");

                System.out.println("Access token obtido com sucesso!");
                session.setAttribute("twitter_access_token", accessToken);

                // Recuperar as informações do usuário
                UserTwitter userTwitter = getUserInfoFromTwitter(accessToken);
                if (userTwitter != null) {
                    UserTwitter existingUser = userTwitterService.findByTwitterId(userTwitter.getTwitterId());
                    if (existingUser == null) {
                        userTwitterService.saveUser(userTwitter);
                    }
                }

                // Redirecionar para Angular com token na URL
                String redirectUrl = "http://localhost:4200/timeline?token=" + accessToken;
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect("http://localhost:4200/login?error=token_failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("http://localhost:4200/login?error=exception");
        }
    }


    @GetMapping("/twitter/following")
    public ResponseEntity<?> getFollowing(@RequestParam(required = false) String token, HttpSession session) {
        try {
            // Usar o token fornecido no parâmetro ou recuperar da sessão
            String accessToken = token;
            if (accessToken == null) {
                accessToken = (String) session.getAttribute("twitter_access_token");
                if (accessToken == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Nenhum token de acesso encontrado. Faça login primeiro.");
                }
            }

            // Primeiro, buscar o ID do usuário logado
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Recuperar as informações do usuário primeiro
            String meUrl = "https://api.twitter.com/2/users/me";
            ResponseEntity<Map> meResponse = restTemplate.exchange(meUrl, HttpMethod.GET, entity, Map.class);

            if (meResponse.getStatusCode() != HttpStatus.OK || meResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro ao buscar informações do usuário");
            }

            // Extrair o ID do usuário da resposta
            Map<String, Object> userData = (Map<String, Object>) meResponse.getBody().get("data");
            String userId = (String) userData.get("id");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Não foi possível recuperar o ID do usuário");
            }

            // Agora buscar os seguidos pelo usuário
            String followingUrl = UriComponentsBuilder
                    .fromUriString("https://api.twitter.com/2/users/" + userId + "/following")
                    .queryParam("max_results", 10)  // Limitar a 10 para teste
                    .build()
                    .toUriString();

            ResponseEntity<Map> followingResponse = restTemplate.exchange(
                    followingUrl, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(followingResponse.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro ao buscar páginas seguidas: " + e.getMessage());
        }
    }

    @GetMapping("/twitter/me")
    public ResponseEntity<?> getMyInfo(@RequestParam(required = false) String token, HttpSession session) {
        try {
            // Usar o token fornecido no parâmetro ou recuperar da sessão
            String accessToken = token;
            if (accessToken == null) {
                accessToken = (String) session.getAttribute("twitter_access_token");
                if (accessToken == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Nenhum token de acesso encontrado. Faça login primeiro.");
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.twitter.com/2/users/me";

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro ao buscar informações do usuário: " + e.getMessage());
        }
    }

    private UserTwitter getUserInfoFromTwitter(String accessToken) {
        String url = "https://api.twitter.com/2/users/me";
        // Endpoint para obter as informações

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> userData = (Map<String, Object>) response.getBody().get("data");

            UserTwitter user = new UserTwitter();
            user.setTwitterId((String) userData.get("id"));
            user.setUsername((String) userData.get("username"));
            user.setProfileImageUrl((String) userData.get("profile_image_url"));

            return user;
        }

        return null;
    }

    //TESTE TWITTER AUTH
    @PostMapping("/twitter/exchange")
    public ResponseEntity<LoginResponse> exchangeTwitterToken(@RequestParam String twitterToken) {
        UserTwitter twitterUser = getUserInfoFromTwitter(twitterToken);
        if (twitterUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid Twitter token"));
        }

        UserTwitter userTwitter = userTwitterRepository.findByTwitterId(twitterUser.getTwitterId());
        if (userTwitter == null) {
            UserTwitter newUser = new UserTwitter();
            newUser.setTwitterId(twitterUser.getTwitterId());
            userTwitterRepository.save(newUser);
            userTwitter = newUser;
        }

        // Gerar token específico para UserTwitter
        String jwt = jwtUtil.generateTokenForUserTwitter(userTwitter);
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

    @GetMapping("/debug")
    public ResponseEntity<String> debug() {
        return ResponseEntity.ok("Debugando -> Client ID: " + clientId.substring(0, 5) + "...");
    }

    private String generateRandomState() {
        return Integer.toHexString(new Random().nextInt());
    }
}