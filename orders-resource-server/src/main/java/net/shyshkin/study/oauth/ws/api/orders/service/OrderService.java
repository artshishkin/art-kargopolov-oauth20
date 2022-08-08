package net.shyshkin.study.oauth.ws.api.orders.service;

import net.shyshkin.study.oauth.ws.api.orders.dto.OrderRest;
import net.shyshkin.study.oauth.ws.api.orders.dto.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OrderService {

    private final List<OrderRest> orders =
            IntStream.rangeClosed(1, 5)
                    .mapToObj(this::createOrder)
                    .collect(Collectors.toList());

    public List<OrderRest> getAllOrders() {
        return orders;
    }

    private OrderRest createOrder(int i) {
        return OrderRest.builder()
                .orderId(UUID.randomUUID().toString())
                .productId("product-id-" + i)
                .userId("user-id-" + (i % 3))
                .quantity(ThreadLocalRandom.current().nextInt(1, 10))
                .orderStatus(OrderStatus.NEW)
                .build();
    }

}
