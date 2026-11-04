package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.persistence.Tuple;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Scalar-only API projection for an order report row. */
public final class OrderReportRowDto implements OrderReportRow {
    private final Long id;
    private final String orderNumber;
    private final String customerName;
    private final LocalDateTime orderDate;
    private final BigDecimal total;
    private final String orderStatus;
    private final String paymentStatus;
    private final Boolean invoiceAvailable;

    public OrderReportRowDto(Long id, String orderNumber, String customerName, LocalDateTime orderDate,
                             BigDecimal total, String orderStatus, String paymentStatus,
                             Boolean invoiceAvailable) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.orderDate = orderDate;
        this.total = total;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.invoiceAvailable = invoiceAvailable;
    }

    public static OrderReportRowDto from(Tuple tuple) {
        return new OrderReportRowDto(
                tuple.get("id", Long.class),
                tuple.get("orderNumber", String.class),
                tuple.get("customerName", String.class),
                tuple.get("orderDate", LocalDateTime.class),
                tuple.get("total", BigDecimal.class),
                tuple.get("orderStatus", String.class),
                tuple.get("paymentStatus", String.class),
                tuple.get("invoiceAvailable", Boolean.class));
    }

    @Override public Long getId() { return id; }
    @Override public String getOrderNumber() { return orderNumber; }
    @Override public String getCustomerName() { return customerName; }
    @Override public LocalDateTime getOrderDate() { return orderDate; }
    @Override public BigDecimal getTotal() { return total; }
    @Override public String getOrderStatus() { return orderStatus; }
    @Override public String getPaymentStatus() { return paymentStatus; }
    @Override public Boolean getInvoiceAvailable() { return invoiceAvailable; }
}
