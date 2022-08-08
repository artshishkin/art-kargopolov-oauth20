package net.shyshkin.study.oauth.ws.api.orders.controller;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.ws.api.orders.dto.OrderRest;
import net.shyshkin.study.oauth.ws.api.orders.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public List<OrderRest> getAllOrders() {
        return orderService.getAllOrders();
    }

}
