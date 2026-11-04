package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.OrderReportRow;
import com.abc.SpringBootSecqurityEx.service.OrderReportService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class OrderReportController {
    private final OrderReportService reportService;

    public OrderReportController(OrderReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Page<OrderReportRow> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.findOrders(startDate, endDate, status, search, page, size);
    }
}
