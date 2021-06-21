package net.shyshkin.study.oauth.ws.clients.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.clients.services.AlbumsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AlbumsController {

    private final AlbumsService albumsService;
    private final OAuth2AuthorizedClientService oauth2ClientService;

    @GetMapping("/albums")
    public String getAllAlbums(
            Model model,
            @AuthenticationPrincipal OidcUser principal) {

        log.debug("OpenId Connect User principal: {}", principal);

        String idTokenValue = principal.getIdToken().getTokenValue();
        log.debug("OpenId Connect token: {}", idTokenValue);

        model.addAttribute(
                "albums",
                albumsService.getAllAlbums()
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient oauth2Client = oauth2ClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        String jwtAccessToken = oauth2Client.getAccessToken().getTokenValue();
        log.debug("JWT Access Token: {}", jwtAccessToken);

        return "albums";
    }
}
