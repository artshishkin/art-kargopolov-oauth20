package net.shyshkin.study.oauth.ws.api.orders.controller;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.ws.api.orders.dto.OrderRest;
import net.shyshkin.study.oauth.ws.api.orders.service.OrderService;
import org.springframework.security.access.annotation.Secured;
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

    @GetMapping("/user/orders")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public List<OrderRest> getAllOrdersUser() {
        return orderService.getAllOrders();
    }

    @GetMapping("/admin/orders")
    @Secured({"ROLE_ADMIN"})
    public List<OrderRest> getAllOrdersAdmin() {
        return orderService.getAllOrders();
    }

}
