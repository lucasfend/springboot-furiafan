package lucasfend.backend.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lucasfend.backend.model.User;
import lucasfend.backend.repository.UserRepository;
import lucasfend.backend.service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/documento")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentoController {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private UserRepository userRepository;

    @Value("${google.credentials.path}")
    private String credentialsPath;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocumento(@RequestParam("file") MultipartFile file, @RequestParam("cpf") String cpf) throws Exception {
        // Verificação do CPF no banco de dados
        System.out.println("CPF recebido: " + cpf);
        String cpfDocumentoLimpo = cpf.replace("[^\\d]", "");

        User user = userRepository.findByCpf(cpf);
        if (user == null) {
            // Retorna erro caso o CPF não seja encontrado no banco de dados
            return ResponseEntity.badRequest().body(Map.of("message", "CPF não encontrado no banco de dados."));
        }

        // Cria um arquivo temporário para armazenar o documento
        Path tempFile = Files.createTempFile("documento", file.getOriginalFilename());
        Files.write(tempFile, file.getBytes());

        // Processa o documento usando a Vision API do Google Cloud
        String textoExtraidoApi = processarOCR(tempFile.toFile());

        // Cria um mapa para armazenar os dados extraídos
        Map<String, String> data = new HashMap<>();
        String nome = extrairNome(textoExtraidoApi);
        String cpfDocumento = extrairCPF(textoExtraidoApi);

        // Salva os dados no Firebase (nome e CPF extraídos do documento)
        firebaseService.salvarDadosDocumento(nome, cpfDocumento);

        data.put("nome", nome);
        data.put("cpf", cpfDocumento);

        // Compara os CPFs para verificar se são iguais
        if (cpfDocumentoLimpo.equals(cpf)) {
            // CPF confere, documento cadastrado com sucesso
            return ResponseEntity.ok(Map.of("message", "Documento APROVADO!"));
        } else {
            // CPF não confere, solicita documento correto
            return ResponseEntity.badRequest().body(Map.of("message", "O CPF do documento não corresponde ao do cadastro. Envie um documento válido."));
        }
    }

    private String processarOCR(File file) throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(settings)) {
            ByteString imgBytes = ByteString.readFrom(new FileInputStream(file));

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            AnnotateImageResponse imageResponse = response.getResponsesList().get(0);

            if (imageResponse.hasError()) {
                throw new Exception("Erro no OCR: " + imageResponse.getError().getMessage());
            }

            return imageResponse.getFullTextAnnotation().getText();
        }
    }

    // extrair nome regex
    private String extrairNome(String texto) {
        String[] linhas = texto.split("\\r?\\n");

        for (int i = 0; i < linhas.length - 1; i++) {
            if (linhas[i].toUpperCase().contains("NOME E SOBRENOME")) {
                // Retorna a próxima linha como nome
                return linhas[i + 1].trim();
            }
        }

        return "Nome não encontrado";
    }

    // extrair cpf regex
    private String extrairCPF(String texto) {
        Pattern pattern = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group();
        }
        return "CPF não encontrado";
    }
}
