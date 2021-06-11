package net.shyshkin.study.oauth.pkce;

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

}
