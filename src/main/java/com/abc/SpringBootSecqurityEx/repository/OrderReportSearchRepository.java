package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.dtos.OrderReportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderReportSearchRepository {
    Page<OrderReportRow> searchReport(LocalDateTime from, LocalDateTime until,
                                      String status, String search, Pageable pageable);
}
