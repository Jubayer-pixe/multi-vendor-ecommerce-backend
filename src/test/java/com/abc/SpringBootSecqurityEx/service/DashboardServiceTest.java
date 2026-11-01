package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.DashboardDTO;
import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductRepository;
import com.abc.SpringBootSecqurityEx.repository.UserRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {

    @Test
    void adminDashboardUsesRealRepositoryMetricsAndDoesNotMergeProductModels() {
        ProductRepository legacyProducts = proxy(ProductRepository.class, (method, arguments) -> switch (method) {
            case "count" -> 12L;
            case "countByActiveTrue" -> 10L;
            case "countByActiveFalse" -> 2L;
            case "findAllByActiveTrueOrderByNameAsc" -> List.of();
            default -> 0L;
        });
        ProductCatalogRepository catalogProducts = proxy(ProductCatalogRepository.class, (method, arguments) -> switch (method) {
            case "count" -> 4L;
            case "countByActiveTrue" -> 3L;
            case "countByActiveFalse" -> 1L;
            default -> 0L;
        });
        UserRepository users = proxy(UserRepository.class, (method, arguments) -> switch (method) {
            case "count" -> 7L;
            case "countByEnabledTrue" -> 6L;
            case "findAll" -> new PageImpl<>(List.of());
            default -> 0L;
        });
        VendorRepository vendors = proxy(VendorRepository.class, (method, arguments) -> switch (method) {
            case "count" -> 2L;
            case "countByVerifiedFalse" -> 1L;
            default -> 0L;
        });
        OrderRepository orders = proxy(OrderRepository.class, (method, arguments) -> switch (method) {
            case "count" -> 5L;
            case "countByStatusIgnoreCase" -> switch ((String) arguments[0]) {
                case "PENDING" -> 2L;
                case "DELIVERED" -> 2L;
                case "CANCELLED" -> 1L;
                default -> 0L;
            };
            case "findAll" -> new PageImpl<>(List.of());
            default -> 0L;
        });

        DashboardService service = new DashboardService(legacyProducts, catalogProducts, users, vendors, orders);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        DashboardDTO dashboard = service.adminDashboard(authentication);

        assertEquals(7L, dashboard.getMetrics().get("totalUsers"));
        assertEquals(2L, dashboard.getMetrics().get("totalVendors"));
        assertEquals(1L, dashboard.getMetrics().get("pendingVendorApprovals"));
        assertEquals(12L, dashboard.getMetrics().get("totalLegacyProducts"));
        assertEquals(4L, dashboard.getMetrics().get("totalCatalogProducts"));
        assertEquals(5L, dashboard.getMetrics().get("totalOrders"));
        assertEquals(2L, dashboard.getMetrics().get("pendingOrders"));
        assertEquals(0, dashboard.getRecentOrders().size());
        assertEquals(0, dashboard.getRecentRegistrations().size());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> repositoryType, RepositoryMethod methodHandler) {
        return (T) Proxy.newProxyInstance(repositoryType.getClassLoader(), new Class<?>[]{repositoryType},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("toString")) return repositoryType.getSimpleName() + " test proxy";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == arguments[0];
                    return methodHandler.invoke(method.getName(), arguments == null ? new Object[0] : arguments);
                });
    }

    @FunctionalInterface
    private interface RepositoryMethod {
        Object invoke(String method, Object[] arguments);
    }
}
