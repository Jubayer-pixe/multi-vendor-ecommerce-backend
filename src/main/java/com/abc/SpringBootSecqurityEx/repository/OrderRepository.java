package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import com.abc.SpringBootSecqurityEx.dtos.CustomerOrderRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderReportSearchRepository {
    long countByStatusIgnoreCase(String status);

    @Query(value = "select o.id as id, o.orderNumber as orderNumber, c.fullName as customerName, " +
            "o.createdAt as orderDate, o.totalAmount as total, o.status as orderStatus, " +
            "case when invoice.id is not null then true else false end as invoiceAvailable, " +
            "(select p.status from Payment p where p.order = o and p.id = " +
            "(select max(p2.id) from Payment p2 where p2.order = o)) as paymentStatus, " +
            "(select p.paymentMethod from Payment p where p.order = o and p.id = " +
            "(select max(p2.id) from Payment p2 where p2.order = o)) as paymentMethod " +
            "from Order o join o.customer c left join Invoice invoice on invoice.order = o where c.user.userName = :username",
            countQuery = "select count(o) from Order o join o.customer c where c.user.userName = :username")
    Page<CustomerOrderRow> findCustomerOrders(@Param("username") String username, Pageable pageable);
}
