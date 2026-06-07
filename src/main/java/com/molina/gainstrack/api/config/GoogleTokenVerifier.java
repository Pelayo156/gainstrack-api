package com.molina.gainstrack.api.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.molina.gainstrack.api.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servicio que verifica tokens de identidad de Google OAuth.
 * El verificador se construye una sola vez al arrancar Spring
 * para evitar el costo de crear conexiones HTTP en cada request.
 * Valida que el token sea legítimo, no haya expirado y
 * pertenezca a esta aplicación mediante el CLIENT_ID.
 */
@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    /**
     * Construye el verificador de tokens de Google.
     * Se ejecuta una sola vez al inicializar el contenedor de Spring.
     *
     * @param clientId identificador de la aplicación en Google Cloud Console
     */
    public GoogleTokenVerifier(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                                                 .setAudience(Collections.singletonList(clientId))
                                                 .build();
    }

    /**
     * Verifica un token de identidad de Google y retorna su payload.
     * El payload contiene el email, nombre e identificador único del usuario.
     *
     * @param token token de identidad recibido desde el cliente iOS
     * @return GoogleIdToken.Payload con los datos del usuario verificado
     * @throws ForbiddenException si el token es inválido, expirado o no pertenece a esta app
     */
    public GoogleIdToken.Payload verify(String token) {
        try {
            GoogleIdToken idToken = verifier.verify(token);

            if (idToken == null) {
                throw new ForbiddenException("Token de Google inválido");
            }
            return idToken.getPayload();
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new ForbiddenException("Token de Google inválido");
        }
    }
}
