package net.shyshkin.study.oauth.ws.clients.orders.controller;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.clients.orders.dto.Order;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class OrdersController {

    @GetMapping("/orders")
    public String getAllOrders(Model model,
                               @RegisteredOAuth2AuthorizedClient("orders-web-oauth-client") OAuth2AuthorizedClient authorizedClient) {

        List<Order> orders = new ArrayList<>();
        model.addAttribute("orders", orders);

        String jwtToken = authorizedClient.getAccessToken().getTokenValue();
        log.debug("JWT Token: {}", jwtToken);

        return "orders-page";
    }

}
