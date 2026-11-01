package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.ProductCatalogDTO;
import com.abc.SpringBootSecqurityEx.service.ProductCatalogService;
import com.abc.SpringBootSecqurityEx.service.ProductImageService;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Vendor-owned catalog operations; ownership is further scoped by the authenticated username. */
@RestController
@RequestMapping("/api/catalog/vendor-products")
@PreAuthorize("hasRole('VENDOR')")
public class VendorCatalogProductController {
    private final ProductCatalogService catalogService;
    private final ProductImageService imageService;

    public VendorCatalogProductController(ProductCatalogService catalogService, ProductImageService imageService) {
        this.catalogService = catalogService;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductCatalogDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(catalogService.findVendorProducts(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCatalogDTO> get(@PathVariable Long id, Authentication authentication) {
        return catalogService.findVendorProduct(id, authentication).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCatalogDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody ProductCatalogDTO request,
                                                     Authentication authentication) {
        if (request.getId() == null || !id.equals(request.getId())) return ResponseEntity.badRequest().build();
        return catalogService.updateVendorProduct(id, request, authentication).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/images/{imageId}/content")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long productId, @PathVariable Long imageId,
                                                     Authentication authentication) {
        try {
            ProductImageService.ImageContent content = imageService.openVendorContent(productId, imageId, authentication);
            return ResponseEntity.ok()
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.parseMediaType(content.contentType()))
                    .contentLength(content.sizeBytes())
                    .body(new InputStreamResource(content.stream()));
        } catch (NotFoundException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
