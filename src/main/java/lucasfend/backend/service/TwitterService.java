package lucasfend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;

@Service
public class TwitterService {

    private final RestTemplate restTemplate = new RestTemplate();

}
