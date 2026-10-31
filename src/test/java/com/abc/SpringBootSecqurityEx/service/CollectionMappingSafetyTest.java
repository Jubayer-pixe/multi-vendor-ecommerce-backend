package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.*;
import com.abc.SpringBootSecqurityEx.entity.customer.*;
import com.abc.SpringBootSecqurityEx.entity.order.*;
import com.abc.SpringBootSecqurityEx.entity.product.*;
import com.abc.SpringBootSecqurityEx.entity.shopping.*;
import com.abc.SpringBootSecqurityEx.entity.vendor.*;
import com.abc.SpringBootSecqurityEx.entity.User;
import com.abc.SpringBootSecqurityEx.enums.ERole;
import com.abc.SpringBootSecqurityEx.repository.*;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;

class CollectionMappingSafetyTest {
    @Test
    void cartNullAndEmptyPreserveCollectionSemanticsAndReadIds() {
        Cart cart = cart(1L);
        CartItem item = cartItem(10L, cart);
        cart.setItems(new ArrayList<>(List.of(item)));
        Map<Class<?>, Map<Object, Object>> related = related(CartItem.class, 10L, item);
        CartService service = new CartService(repository(CartRepository.class, cart), entityManager(related));

        CartDTO nullResult = service.save(cartDto(1L, null));
        assertEquals(List.of(10L), nullResult.getItemIds());
        assertSame(cart, item.getCart());

        Cart empty = cart(2L);
        CartService emptyService = new CartService(repository(CartRepository.class, empty), entityManager(Map.of()));
        assertEquals(List.of(), emptyService.save(cartDto(2L, List.of())).getItemIds());
    }

    @Test
    void cartRejectsUnknownForeignAndDestructiveReplacementWithoutMutation() {
        Cart current = cart(1L);
        Cart other = cart(2L);
        CartItem retained = cartItem(10L, current);
        CartItem foreign = cartItem(20L, other);
        current.setItems(new ArrayList<>(List.of(retained)));
        Map<Class<?>, Map<Object, Object>> related = related(CartItem.class, 20L, foreign);
        CartRepository repo = repository(CartRepository.class, current);
        CartService service = new CartService(repo, entityManager(related));

        assertThrows(NotFoundException.class, () -> service.save(cartDto(1L, List.of(99L))));
        assertThrows(ResponseStatusException.class, () -> service.save(cartDto(1L, List.of(20L))));
        assertThrows(ResponseStatusException.class, () -> service.save(cartDto(1L, List.of())));
        assertEquals(List.of(retained), current.getItems());
        assertSame(other, foreign.getCart());
    }

    @Test
    void orderAndVendorOrderDoNotOrphanHistoricalChildren() {
        Order order = new Order(); order.setId(5L);
        OrderItem orderItem = new OrderItem(); orderItem.setId(50L); orderItem.setOrder(order);
        order.setItems(new ArrayList<>(List.of(orderItem)));
        OrderService orderService = new OrderService(repository(OrderRepository.class, order),
                entityManager(related(OrderItem.class, 50L, orderItem)));
        OrderDTO orderDto = new OrderDTO(); orderDto.setId(5L); orderDto.setItemIds(List.of(50L));
        assertEquals(List.of(50L), orderService.save(orderDto).getItemIds());
        orderDto.setItemIds(List.of());
        assertThrows(ResponseStatusException.class, () -> orderService.save(orderDto));
        assertSame(order, orderItem.getOrder());

        VendorOrder vendorOrder = new VendorOrder(); vendorOrder.setId(6L);
        VendorOrderItem vendorItem = new VendorOrderItem(); vendorItem.setId(60L); vendorItem.setVendorOrder(vendorOrder);
        vendorOrder.setItems(new ArrayList<>(List.of(vendorItem)));
        VendorOrderService vendorOrderService = new VendorOrderService(
                repository(VendorOrderRepository.class, vendorOrder),
                entityManager(related(VendorOrderItem.class, 60L, vendorItem)));
        VendorOrderDTO dto = new VendorOrderDTO(); dto.setId(6L); dto.setItemIds(List.of(60L));
        assertEquals(List.of(60L), vendorOrderService.save(dto).getItemIds());
        dto.setItemIds(List.of(61L));
        assertThrows(NotFoundException.class, () -> vendorOrderService.save(dto));
        assertSame(vendorOrder, vendorItem.getVendorOrder());
    }

    @Test
    void variantAndWishlistChildrenAreValidatedAndNeverReparented() {
        ProductVariant variant = new ProductVariant(); variant.setId(3L);
        ProductAttributeValue value = new ProductAttributeValue(); value.setId(30L); value.setProductVariant(variant);
        variant.setAttributeValues(new ArrayList<>(List.of(value)));
        ProductVariantService variantService = new ProductVariantService(
                repository(ProductVariantRepository.class, variant),
                entityManager(related(ProductAttributeValue.class, 30L, value)));
        ProductVariantDTO variantDto = new ProductVariantDTO(); variantDto.setId(3L); variantDto.setAttributeValueIds(List.of(30L));
        assertEquals(List.of(30L), variantService.save(variantDto).getAttributeValueIds());
        variantDto.setAttributeValueIds(null);
        assertEquals(List.of(30L), variantService.save(variantDto).getAttributeValueIds());
        variantDto.setAttributeValueIds(List.of(99L));
        assertThrows(NotFoundException.class, () -> variantService.save(variantDto));
        assertSame(variant, value.getProductVariant());

        Wishlist wishlist = new Wishlist(); wishlist.setId(4L);
        WishlistItem wishlistItem = new WishlistItem(); wishlistItem.setId(40L); wishlistItem.setWishlist(wishlist);
        wishlist.setItems(new ArrayList<>(List.of(wishlistItem)));
        WishlistService wishlistService = new WishlistService(
                repository(WishlistRepository.class, wishlist),
                entityManager(related(WishlistItem.class, 40L, wishlistItem)));
        WishlistDTO wishlistDto = new WishlistDTO(); wishlistDto.setId(4L); wishlistDto.setItemIds(List.of(40L));
        assertEquals(List.of(40L), wishlistService.save(wishlistDto).getItemIds());
        wishlistDto.setItemIds(List.of());
        assertThrows(ResponseStatusException.class, () -> wishlistService.save(wishlistDto));
        assertSame(wishlist, wishlistItem.getWishlist());
    }

    @Test
    void customerWishlistsAndVendorReferenceCollectionsRequireExistingOwnership() {
        Customer customer = new Customer(); customer.setId(7L);
        Wishlist wishlist = new Wishlist(); wishlist.setId(70L); wishlist.setCustomer(customer);
        customer.setWishlists(new ArrayList<>(List.of(wishlist)));
        CustomerService customerService = new CustomerService(
                repository(CustomerRepository.class, customer),
                entityManager(related(Wishlist.class, 70L, wishlist)));
        CustomerDTO customerDto = new CustomerDTO(); customerDto.setId(7L); customerDto.setWishlistIds(List.of(70L));
        assertEquals(List.of(70L), customerService.save(customerDto).getWishlistIds());
        customerDto.setWishlistIds(List.of(71L));
        assertThrows(NotFoundException.class, () -> customerService.save(customerDto));
        assertSame(customer, wishlist.getCustomer());

        Vendor vendor = new Vendor(); vendor.setId(8L);
        User vendorUser = new User(); vendorUser.setUserName("vendor"); vendorUser.setRoles(EnumSet.of(ERole.VENDOR));
        vendor.setUser(vendorUser);
        VendorBankAccount account = new VendorBankAccount(); account.setId(80L); account.setVendor(vendor);
        VendorDocument document = new VendorDocument(); document.setId(81L); document.setVendor(vendor);
        vendor.setBankAccounts(new ArrayList<>(List.of(account)));
        vendor.setDocuments(new ArrayList<>(List.of(document)));
        VendorService vendorService = new VendorService(
                repository(VendorRepository.class, vendor),
                repository(UserRepository.class, vendorUser),
                entityManager(Map.of(VendorBankAccount.class, Map.of(80L, account), VendorDocument.class, Map.of(81L, document))));
        VendorDTO vendorDto = new VendorDTO(); vendorDto.setId(8L); vendorDto.setUserName("vendor");
        vendorDto.setBusinessName("Example Store"); vendorDto.setContactPerson("Store contact"); vendorDto.setEmail("store@example.test");
        vendorDto.setBankAccountIds(List.of(80L)); vendorDto.setDocumentIds(List.of(81L));
        VendorDTO result = vendorService.save(vendorDto);
        assertEquals(List.of(80L), result.getBankAccountIds());
        assertEquals(List.of(81L), result.getDocumentIds());
        vendorDto.setBankAccountIds(List.of());
        assertThrows(ResponseStatusException.class, () -> vendorService.save(vendorDto));
        assertSame(vendor, account.getVendor());
        assertSame(vendor, document.getVendor());
    }

    private static Cart cart(Long id) { Cart value = new Cart(); value.setId(id); return value; }
    private static CartItem cartItem(Long id, Cart cart) { CartItem item = new CartItem(); item.setId(id); item.setCart(cart); return item; }
    private static CartDTO cartDto(Long id, List<Long> ids) { CartDTO dto = new CartDTO(); dto.setId(id); dto.setItemIds(ids); return dto; }

    private static <R extends JpaRepository<?, ?>> R repository(Class<R> type, Object... initial) {
        Map<Object, Object> rows = new HashMap<>();
        for (Object entity : initial) rows.put(identifier(entity), entity);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "findById" -> Optional.ofNullable(rows.get(args[0]));
                case "findByUserName" -> Optional.ofNullable(rows.get(args[0]));
                case "existsByUser_UserNameAndIdNot", "existsByEmailAndIdNot", "existsByUser_UserName", "existsByEmail" -> false;
                case "save" -> { Object entity = args[0]; rows.put(identifier(entity), entity); yield entity; }
                case "findAll" -> new ArrayList<>(rows.values());
                case "existsById" -> rows.containsKey(args[0]);
                case "deleteById" -> rows.remove(args[0]);
                case "toString" -> type.getSimpleName() + " test fake";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }));
    }

    private static Object identifier(Object entity) {
        if (entity instanceof User value) return value.getUserName();
        if (entity instanceof Cart value) return value.getId();
        if (entity instanceof Order value) return value.getId();
        if (entity instanceof VendorOrder value) return value.getId();
        if (entity instanceof ProductVariant value) return value.getId();
        if (entity instanceof Customer value) return value.getId();
        if (entity instanceof Vendor value) return value.getId();
        if (entity instanceof Wishlist value) return value.getId();
        throw new IllegalArgumentException("Unexpected repository test entity: " + entity.getClass());
    }
    private static Map<Class<?>, Map<Object, Object>> related(Class<?> type, Object id, Object entity) {
        return Map.of(type, Map.of(id, entity));
    }

    private static EntityManager entityManager(Map<Class<?>, Map<Object, Object>> rows) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[]{EntityManager.class}, (proxy, method, args) -> {
            if (method.getName().equals("find")) return rows.getOrDefault(args[0], Map.of()).get(args[1]);
            if (method.getName().equals("getReference")) return rows.getOrDefault(args[0], Map.of()).get(args[1]);
            if (method.getName().equals("toString")) return "EntityManager test fake";
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            throw new UnsupportedOperationException(method.toString());
        });
    }
}

