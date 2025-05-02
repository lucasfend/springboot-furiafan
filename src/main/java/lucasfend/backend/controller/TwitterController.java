package lucasfend.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/timeline")
@CrossOrigin(origins = "http://localhost:4200")
public class TwitterController {

    @Value("${BEARER_TOKEN}")
    private String BEARER_TOKEN;

    @GetMapping("/tweet/{id}")
    public ResponseEntity<String> getTweetById(@PathVariable String id) throws IOException {
        String url = "https://api.twitter.com/2/tweets?ids=" + id +
                "&expansions=attachments.media_keys" +
                "&media.fields=url,preview_image_url,type";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + BEARER_TOKEN);

        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    response.append(line);
                }
            return ResponseEntity.ok(response.toString());
        }
    }
}