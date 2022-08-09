package net.shyshkin.study.oauth.ws.clients.orders.controller;

import net.shyshkin.study.oauth.ws.clients.orders.dto.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class OrdersController {

    @GetMapping("/orders")
    public String getAllOrders(Model model) {

        List<Order> orders = new ArrayList<>();
        model.addAttribute("orders", orders);

        return "orders-page";
    }

}
