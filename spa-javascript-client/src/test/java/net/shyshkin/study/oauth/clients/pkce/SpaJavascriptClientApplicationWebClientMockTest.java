package net.shyshkin.study.oauth.clients.pkce;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug"
})
@AutoConfigureMockMvc
@DisplayName("Simple Unit test to workaround htmlunit Javascript issue during testing")
class SpaJavascriptClientApplicationWebClientMockTest {

    @Autowired
    private WebClient webClient;

    @BeforeEach
    void setUp() {

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setThrowExceptionOnScriptError(false);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        this.webClient.setJavaScriptErrorListener(new DefaultJavaScriptErrorListener() {
            @Override
            public void scriptException(HtmlPage page, ScriptException scriptException) {
                log.debug("JS error in line {} column {} Failing line\n`{}`\nscript source code {}",
                        scriptException.getFailingLineNumber(),
                        scriptException.getFailingColumnNumber(),
                        scriptException.getFailingLine(),
                        scriptException.getScriptSourceCode()
                );
                scriptException.printStackTrace();
            }
        });

        this.webClient.getCookieManager().clearCookies();    // log out

    }

    @Test
    void fullWorkflowTest() throws IOException {
        //given
        String indexPageUrl = "/";

        //visit index page should show correct content
        HtmlPage page = webClient.getPage(indexPageUrl);

        webClient.waitForBackgroundJavaScript(1000L);

        assertThat(page.getTitleText()).isEqualTo("Javascript Application with PKCE");

        assertThat(page.getHtmlElementById("redirectHostUri").getTextContent()).isEqualTo("http://localhost:8181");
        assertThat(page.getHtmlElementById("oAuthServerUri").getTextContent()).isEqualTo("http://localhost:8080");
        assertThat(page.getHtmlElementById("usersApiUri").getTextContent()).isEqualTo("http://localhost:8666");
        assertThat(page.getHtmlElementById("gatewayUri").getTextContent()).isEqualTo("http://localhost:8090");
        assertThat(page.getHtmlElementById("resourceServerUri").getAttribute("value")).isEqualTo("http://localhost:8666");

        //click on button `Generate Random State Value` should change text in `stateValue` field
        page.getHtmlElementById("generateStateBtn").click();
        assertThat(page.getHtmlElementById("stateValue").getTextContent()).isNotEqualTo("Some Value");

        //click on button `Generate Code Verifier Value` should change text in `codeVerifierValue` field
        page.getHtmlElementById("generateCodeVerifierBtn").click();
        assertThat(page.getHtmlElementById("codeVerifierValue").getTextContent()).isNotEqualTo("Code Verifier Value");

        //click on button `Generate Code Challenge Value` should change text in `codeVerifierValue` field
        HtmlButton generateCodeChallengeBtn = page.getHtmlElementById("generateCodeChallengeBtn");
        generateCodeChallengeBtn.click();
        log.debug("generateCodeChallengeBtn: {}", generateCodeChallengeBtn);

        assertThat(page.getHtmlElementById("codeChallengeValue").getTextContent()).isNotEqualTo("Code Challenge Value");

        //click on button `Get Auth Code` should pop up new window for signing into keycloak
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        var getAuthCodeBtnClick = page.getHtmlElementById("getAuthCodeBtn").click();

        //assert calling external Authorization Server
        WebResponse webResponse = getAuthCodeBtnClick.getWebResponse();

        assertThat(webResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        URL requestUrl = webResponse.getWebRequest().getUrl();
        assertThat(requestUrl.toString())
                .startsWith("http://localhost:8080/auth/realms/katarinazart/protocol/openid-connect/auth?client_id=photo-app-pkce-client&response_type=code&scope=openid%20profile&redirect_uri=http://localhost:8181/authcodeReader.html&state=")
                .contains("code_challenge=", "code_challenge_method=S256");
    }

}