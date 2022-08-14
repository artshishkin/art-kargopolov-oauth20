package net.shyshkin.study.oauth.ws.clients.orders.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.clients.orders.dto.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrdersController {

    public static final ParameterizedTypeReference<List<Order>> ORDER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private final RestTemplate restTemplate;

    @Value("${app.services.orders.default-uri}")
    private String ordersServiceDefaultUri;

    @Value("${app.services.orders.user-uri}")
    private String ordersServiceUserUri;

    @Value("${app.services.orders.admin-uri}")
    private String ordersServiceAdminUri;

    @GetMapping("/orders")
    public String getAllOrders(
            Model model,
            @RegisteredOAuth2AuthorizedClient("orders-web-oauth-client") OAuth2AuthorizedClient authorizedClient) {

        return getOrders(ordersServiceDefaultUri, model, authorizedClient);
    }

    @GetMapping("/user/orders")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String getAllOrdersRoleUser(
            Model model,
            @RegisteredOAuth2AuthorizedClient("orders-web-oauth-client") OAuth2AuthorizedClient authorizedClient) {

        return getOrders(ordersServiceUserUri, model, authorizedClient);
    }

    @GetMapping("/admin/orders")
    @Secured({"ROLE_ADMIN"})
    public String getAllOrdersRoleAdmin(
            Model model,
            @RegisteredOAuth2AuthorizedClient("orders-web-oauth-client") OAuth2AuthorizedClient authorizedClient) {

        return getOrders(ordersServiceAdminUri, model, authorizedClient);
    }

    private String getOrders(String ordersServiceUri, Model model, OAuth2AuthorizedClient authorizedClient) {
        String jwtToken = authorizedClient.getAccessToken().getTokenValue();
        log.debug("JWT Token: {}", jwtToken);
        RequestEntity<Void> requestEntity = RequestEntity.get(ordersServiceUri)
                .headers(h -> h.setBearerAuth(jwtToken))
                .build();

        ResponseEntity<List<Order>> responseEntity = restTemplate.exchange(requestEntity, ORDER_LIST_TYPE);
        List<Order> orders = responseEntity.getBody();

        model.addAttribute("orders", orders);

        return "orders-page";
    }

}
