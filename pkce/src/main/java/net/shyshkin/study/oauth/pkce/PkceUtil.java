package net.shyshkin.study.oauth.pkce;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {

    public String generateCodeVerifier() {
        var secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(codeVerifier);
    }

    public String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        var bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        var messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes);
        byte[] digest = messageDigest.digest();
        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(digest);
    }

}
