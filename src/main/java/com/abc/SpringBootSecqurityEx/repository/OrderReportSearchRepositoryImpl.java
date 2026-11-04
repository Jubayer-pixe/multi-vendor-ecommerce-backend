package com.abc.SpringBootSecqurityEx.repository;

import com.abc.SpringBootSecqurityEx.dtos.OrderReportRow;
import com.abc.SpringBootSecqurityEx.dtos.OrderReportRowDto;
import com.abc.SpringBootSecqurityEx.entity.customer.Customer;
import com.abc.SpringBootSecqurityEx.entity.order.Invoice;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.payment.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class OrderReportSearchRepositoryImpl implements OrderReportSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<OrderReportRow> searchReport(LocalDateTime from, LocalDateTime until,
                                             String status, String search, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> resultQuery = cb.createTupleQuery();
        Root<Order> order = resultQuery.from(Order.class);
        Join<Order, Customer> customer = order.join("customer");

        Subquery<Long> invoiceExists = resultQuery.subquery(Long.class);
        Root<Invoice> invoice = invoiceExists.from(Invoice.class);
        invoiceExists.select(invoice.get("id"))
                .where(cb.equal(invoice.get("order").get("id"), order.get("id")));

        Subquery<String> paymentStatus = resultQuery.subquery(String.class);
        Root<Payment> payment = paymentStatus.from(Payment.class);
        Subquery<Long> latestPaymentId = paymentStatus.subquery(Long.class);
        Root<Payment> paymentCandidate = latestPaymentId.from(Payment.class);
        latestPaymentId.select(cb.max(paymentCandidate.<Long>get("id")))
                .where(cb.equal(paymentCandidate.get("order").get("id"), order.get("id")));
        paymentStatus.select(payment.get("status")).where(
                cb.equal(payment.get("order").get("id"), order.get("id")),
                cb.equal(payment.get("id"), latestPaymentId));

        resultQuery.multiselect(
                order.get("id").alias("id"),
                order.get("orderNumber").alias("orderNumber"),
                customer.get("fullName").alias("customerName"),
                order.get("createdAt").alias("orderDate"),
                order.get("totalAmount").alias("total"),
                order.get("status").alias("orderStatus"),
                cb.<Boolean>selectCase().when(cb.exists(invoiceExists), true).otherwise(false).alias("invoiceAvailable"),
                paymentStatus.alias("paymentStatus"));
        resultQuery.where(predicates(cb, order, customer, from, until, status, search));
        resultQuery.orderBy(cb.desc(order.get("createdAt")));

        TypedQuery<Tuple> query = entityManager.createQuery(resultQuery);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<OrderReportRow> content = query.getResultList().stream()
                .map(tuple -> (OrderReportRow) OrderReportRowDto.from(tuple))
                .toList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> countOrder = countQuery.from(Order.class);
        Join<Order, Customer> countCustomer = countOrder.join("customer");
        countQuery.select(cb.count(countOrder))
                .where(predicates(cb, countOrder, countCustomer, from, until, status, search));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Order> order, Join<Order, Customer> customer,
                                   LocalDateTime from, LocalDateTime until, String status, String search) {
        List<Predicate> predicates = new ArrayList<>();
        if (from != null) predicates.add(cb.greaterThanOrEqualTo(order.get("createdAt"), from));
        if (until != null) predicates.add(cb.lessThan(order.get("createdAt"), until));
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(cb.lower(order.get("status")), status.trim().toLowerCase(Locale.ROOT)));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(order.get("orderNumber")), pattern),
                    cb.like(cb.lower(customer.get("fullName")), pattern)));
        }
        return predicates.toArray(Predicate[]::new);
    }

}
