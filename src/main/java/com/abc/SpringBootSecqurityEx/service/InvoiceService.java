package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.entity.customer.CustomerAddress;
import com.abc.SpringBootSecqurityEx.entity.order.Invoice;
import com.abc.SpringBootSecqurityEx.entity.order.InvoiceItem;
import com.abc.SpringBootSecqurityEx.entity.order.Order;
import com.abc.SpringBootSecqurityEx.entity.order.OrderItem;
import com.abc.SpringBootSecqurityEx.entity.payment.Payment;
import com.abc.SpringBootSecqurityEx.repository.InvoiceRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public Invoice createForCheckout(Order order, Payment payment, CustomerAddress address) {
        return invoiceRepository.findByOrder_Id(order.getId()).orElseGet(() -> {
            Invoice invoice = new Invoice();
            invoice.setOrder(order);
            invoice.setInvoiceNumber("INV-" + order.getOrderNumber());
            invoice.setCustomerName(order.getCustomer().getFullName());
            invoice.setCustomerPhone(order.getCustomer().getPhone());
            invoice.setDeliveryAddress(formatAddress(address));
            invoice.setPaymentMethod(payment.getPaymentMethod());
            invoice.setPaymentStatus(payment.getStatus());
            invoice.setOrderStatus(order.getStatus());
            invoice.setSubtotal(zeroIfNull(order.getSubtotal()));
            invoice.setDiscount(zeroIfNull(order.getDiscount()));
            invoice.setShippingFee(zeroIfNull(order.getShippingFee()));
            invoice.setGrandTotal(zeroIfNull(order.getTotalAmount()));
            invoice.setCreatedAt(order.getCreatedAt());
            for (OrderItem orderItem : order.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setItemName(orderItem.getProduct().getName());
                item.setQuantity(orderItem.getQuantity());
                item.setUnitPrice(orderItem.getUnitPrice());
                item.setLineTotal(orderItem.getTotalPrice());
                invoice.getItems().add(item);
            }
            return invoiceRepository.save(invoice);
        });
    }

    @Transactional(readOnly = true)
    public InvoicePdf getCustomerInvoicePdf(Long orderId, String username) {
        Invoice invoice = findInvoice(orderId);
        if (invoice.getOrder().getCustomer() == null
                || invoice.getOrder().getCustomer().getUser() == null
                || !username.equals(invoice.getOrder().getCustomer().getUser().getUserName())) {
            // Keep the existence of another customer's invoice private.
            throw new NotFoundException("Invoice not found");
        }
        return render(invoice);
    }

    @Transactional(readOnly = true)
    public InvoicePdf getInvoicePdf(Long orderId) {
        return render(findInvoice(orderId));
    }

    private Invoice findInvoice(Long orderId) {
        return invoiceRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private InvoicePdf render(Invoice invoice) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("invoiceNumber", invoice.getInvoiceNumber());
            parameters.put("orderNumber", invoice.getOrder().getOrderNumber());
            parameters.put("orderDate", invoice.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            parameters.put("customerName", invoice.getCustomerName());
            parameters.put("customerPhone", invoice.getCustomerPhone() == null ? "" : invoice.getCustomerPhone());
            parameters.put("deliveryAddress", invoice.getDeliveryAddress());
            parameters.put("paymentMethod", invoice.getPaymentMethod());
            parameters.put("paymentStatus", invoice.getPaymentStatus());
            parameters.put("orderStatus", invoice.getOrderStatus());
            parameters.put("subtotal", money(invoice.getSubtotal()));
            parameters.put("discount", money(invoice.getDiscount()));
            parameters.put("shippingFee", money(invoice.getShippingFee()));
            parameters.put("grandTotal", money(invoice.getGrandTotal()));
            var compiled = JasperCompileManager.compileReport(createReportDesign());
            var filled = JasperFillManager.fillReport(compiled, parameters,
                    new JRBeanCollectionDataSource(invoice.getItems()));
            return new InvoicePdf(JasperExportManager.exportReportToPdf(filled), invoice.getInvoiceNumber());
        } catch (JRException exception) {
            throw new IllegalStateException("Unable to generate invoice PDF", exception);
        }
    }

    private static String formatAddress(CustomerAddress address) {
        List<String> parts = new ArrayList<>();
        addAddressPart(parts, address.getAddressLine1());
        addAddressPart(parts, address.getCity());
        if (address.getState() != null && (address.getCity() == null
                || !address.getState().trim().equalsIgnoreCase(address.getCity().trim()))) {
            addAddressPart(parts, address.getState());
        }
        addAddressPart(parts, address.getPostalCode());
        addAddressPart(parts, address.getCountry());
        return String.join(", ", parts);
    }

    private static void addAddressPart(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value.trim());
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String money(BigDecimal value) {
        return zeroIfNull(value).toPlainString() + " BDT";
    }

    public record InvoicePdf(byte[] content, String filename) { }

    private static JasperDesign createReportDesign() throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("invoice");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(535);
        design.setLeftMargin(30);
        design.setRightMargin(30);
        design.setTopMargin(30);
        design.setBottomMargin(30);
        for (String name : new String[]{"invoiceNumber", "orderNumber", "orderDate", "customerName",
                "customerPhone", "deliveryAddress", "paymentMethod", "paymentStatus", "orderStatus",
                "subtotal", "discount", "shippingFee", "grandTotal"}) {
            JRDesignParameter parameter = new JRDesignParameter();
            parameter.setName(name);
            parameter.setValueClass(String.class);
            design.addParameter(parameter);
        }
        addField(design, "itemName", String.class);
        addField(design, "quantity", Integer.class);
        addField(design, "unitPrice", BigDecimal.class);
        addField(design, "lineTotal", BigDecimal.class);

        JRDesignBand title = new JRDesignBand();
        title.setHeight(210);
        title.addElement(staticText(0, 0, 535, 30, "INVOICE", 20, true));
        title.addElement(textField(0, 40, 535, 20, "\"Invoice: \" + $P{invoiceNumber}", 12, false));
        title.addElement(textField(0, 62, 535, 20, "\"Order: \" + $P{orderNumber} + \"    Date: \" + $P{orderDate}", 12, false));
        title.addElement(textField(0, 84, 535, 20, "\"Customer: \" + $P{customerName} + \"    Phone: \" + $P{customerPhone}", 12, false));
        title.addElement(textField(0, 106, 535, 32, "\"Delivery address: \" + $P{deliveryAddress}", 12, false));
        title.addElement(textField(0, 142, 535, 24, "\"Payment: \" + $P{paymentMethod} + \" (\" + $P{paymentStatus} + \")    Order status: \" + $P{orderStatus}", 11, false));
        title.addElement(staticText(0, 180, 250, 20, "Item", 11, true));
        title.addElement(staticText(270, 180, 55, 20, "Qty", 11, true));
        title.addElement(staticText(335, 180, 100, 20, "Unit price", 11, true));
        title.addElement(staticText(440, 180, 95, 20, "Line total", 11, true));
        design.setTitle(title);

        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(24);
        detail.addElement(textField(0, 2, 260, 20, "$F{itemName}", 10, false));
        detail.addElement(textField(270, 2, 55, 20, "$F{quantity}", 10, false));
        detail.addElement(textField(335, 2, 100, 20, "$F{unitPrice}.toPlainString() + \" BDT\"", 10, false));
        detail.addElement(textField(440, 2, 95, 20, "$F{lineTotal}.toPlainString() + \" BDT\"", 10, false));
        ((JRDesignSection) design.getDetailSection()).addBand(detail);

        JRDesignBand summary = new JRDesignBand();
        summary.setHeight(110);
        summary.addElement(textField(335, 5, 200, 20, "\"Subtotal: \" + $P{subtotal}", 10, false));
        summary.addElement(textField(335, 27, 200, 20, "\"Discount: \" + $P{discount}", 10, false));
        summary.addElement(textField(335, 49, 200, 20, "\"Shipping: \" + $P{shippingFee}", 10, false));
        summary.addElement(textField(335, 75, 200, 24, "\"Grand total: \" + $P{grandTotal}", 14, true));
        design.setSummary(summary);
        return design;
    }

    private static void addField(JasperDesign design, String name, Class<?> type) throws JRException {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClass(type);
        design.addField(field);
    }

    private static JRDesignStaticText staticText(int x, int y, int width, int height, String value,
                                                  float fontSize, boolean bold) {
        JRDesignStaticText text = new JRDesignStaticText();
        text.setX(x); text.setY(y); text.setWidth(width); text.setHeight(height);
        text.setText(value); text.setFontSize(fontSize); text.setBold(bold);
        return text;
    }

    private static JRDesignTextField textField(int x, int y, int width, int height, String expression,
                                                float fontSize, boolean bold) {
        JRDesignTextField text = new JRDesignTextField();
        text.setX(x); text.setY(y); text.setWidth(width); text.setHeight(height);
        text.setExpression(new JRDesignExpression(expression));
        text.setFontSize(fontSize); text.setBold(bold); text.setBlankWhenNull(true);
        return text;
    }

}
