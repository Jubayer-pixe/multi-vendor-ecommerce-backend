package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.OrderReportRow;
import com.abc.SpringBootSecqurityEx.dtos.CustomerOrderRow;
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class OrderReportService {
    private final OrderRepository orderRepository;

    public OrderReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderReportRow> findOrders(LocalDate startDate, LocalDate endDate, String status,
                                           String search, int page, int size) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be between 1 and 100");
        }
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        // The next day's midnight is exclusive, making the selected end date inclusive.
        LocalDateTime until = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        return orderRepository.searchReport(from, until, blankToNull(status), blankToNull(search),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderRow> findCustomerOrders(String username, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be between 1 and 100");
        }
        return orderRepository.findCustomerOrders(username,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
