package net.shyshkin.study.oauth.pkce;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class PkceUtilTest {

    private static PkceUtil pkce;

    @BeforeAll
    static void beforeAll() {
        pkce = new PkceUtil();
    }

    @RepeatedTest(10)
    void generateCodeChallenge() throws NoSuchAlgorithmException {
        //given
        String codeVerifier = pkce.generateCodeVerifier();

        //when
        String codeChallenge = pkce.generateCodeChallenge(codeVerifier);

        //then
        log.debug("Code Verifier: {}", codeVerifier);
        log.debug("Code Challenge: {}", codeChallenge);
        assertThat(codeVerifier).hasSize(43);
        assertThat(codeChallenge).hasSize(43);
    }
}