package com.molina.gainstrack.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Servicio encargado de la generación y validación de tokens JWT.
 * Encapsula toda la lógica criptográfica relacionada con jjwt,
 * siendo el único punto de contacto con la librería en todo el sistema.
 */
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expirationTime;

    /**
     * Genera un token JWT firmado para el usuario indicado.
     * El email se almacena como subject en el payload del token.
     *
     * @param email correo electrónico del usuario autenticado
     * @return token JWT como String listo para ser enviado al cliente
     */
    public String generateToken(String email) {
        return Jwts.builder()
                   .subject(email)
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + expirationTime))
                   .signWith(getSignigKey())
                   .compact();
    }

    /**
     * Extrae el email del subject del payload de un token JWT.
     *
     * @param token token JWT del cual extraer el email
     * @return email del usuario contenido en el token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Valida si un token JWT es legítimo y no ha expirado.
     * Intenta parsear el token — si la firma fue manipulada
     * o el token expiró, jjwt lanza JwtException y devuelve false.
     *
     * @param token token JWT a validar
     * @return true si el token es válido, false si fue manipulado o expiró
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Método genérico para extraer cualquier campo del payload del token.
     * Recibe una función que define qué campo extraer, permitiendo
     * reutilizar la lógica de parseo para distintos claims en el futuro.
     *
     * @param <T>            tipo del valor a extraer
     * @param token          token JWT del cual extraer el claim
     * @param claimsResolver función que define qué campo extraer del payload
     * @return valor del claim solicitado
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return  claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Parsea y verifica el token JWT completo, devolviendo todos sus claims.
     * Es el núcleo de la validación — aquí jjwt verifica la firma contra
     * la SECRET_KEY. Si el payload fue modificado, la firma no coincide
     * y se lanza una JwtException.
     *
     * @param token token JWT a parsear
     * @return Claims objeto con todos los campos del payload
     * @throws JwtException si el token es inválido, expirado o fue manipulado
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                   .verifyWith(getSignigKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    /**
     * Convierte el secreto almacenado en Base64 a un objeto SecretKey
     * utilizable por jjwt para firmar y verificar tokens HMAC-SHA256.
     *
     * @return SecretKey lista para ser usada en operaciones criptográficas
     */
    private SecretKey getSignigKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
