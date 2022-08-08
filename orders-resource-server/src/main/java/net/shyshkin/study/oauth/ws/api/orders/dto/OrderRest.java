package net.shyshkin.study.oauth.ws.api.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRest {

    private String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private OrderStatus orderStatus;

}
