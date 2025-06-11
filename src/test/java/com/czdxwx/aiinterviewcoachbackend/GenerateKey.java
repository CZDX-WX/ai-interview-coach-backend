package com.czdxwx.aiinterviewcoachbackend;
import javax.crypto.SecretKey;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class GenerateKey {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512); // 生成适用于HS512的密钥
        String secretString = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated HS512 Key (Base64 Encoded): " + secretString);
        System.out.println("Key length in bytes: " + key.getEncoded().length); // 应为64
    }
}