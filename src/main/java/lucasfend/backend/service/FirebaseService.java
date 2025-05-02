package lucasfend.backend.service;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseService {

    @Value("${firebase.credentials.path}")
    private Resource fbCredentialPath;

    public void init() throws IOException {
        try (InputStream svcAccount = fbCredentialPath.getInputStream()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(svcAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized.");
            }
        }
    }

    public void salvarDadosDocumento(String nome, String cpf) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        System.out.println("Projeto firabse que ta conectado: " + db.getOptions().getProjectId()); //apenas para debug

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("cpf", cpf);

        // Exemplo: cria com ID automático
        ApiFuture<DocumentReference> addedDocRef = db.collection("documentos").add(dados);
        System.out.println("Documento salvo no Firestore com ID: " + addedDocRef.get().getId()); //apenas para debug
    }
}
