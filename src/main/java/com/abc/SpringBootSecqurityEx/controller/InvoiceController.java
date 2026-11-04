package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.service.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping(value = "/orders/{orderId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> customerInvoice(@PathVariable Long orderId, Authentication authentication) {
        return pdf(invoiceService.getCustomerInvoicePdf(orderId, authentication.getName()), false);
    }

    @GetMapping(value = "/admin/orders/{orderId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> adminInvoice(@PathVariable Long orderId) {
        return pdf(invoiceService.getInvoicePdf(orderId), true);
    }

    private ResponseEntity<byte[]> pdf(InvoiceService.InvoicePdf invoice, boolean attachment) {
        String disposition = attachment ? "attachment" : "inline";
        String filename = invoice.filename().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder(disposition)
                .filename(filename, StandardCharsets.US_ASCII).build());
        headers.setContentLength(invoice.content().length);
        return ResponseEntity.ok().headers(headers).body(invoice.content());
    }
}
