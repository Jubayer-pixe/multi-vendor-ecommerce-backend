package com.abc.SpringBootSecqurityEx.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderReportRow {
    Long getId();
    String getOrderNumber();
    String getCustomerName();
    LocalDateTime getOrderDate();
    BigDecimal getTotal();
    String getOrderStatus();
    String getPaymentStatus();
    Boolean getInvoiceAvailable();
    default String getPaymentMethod() { return null; }
}
