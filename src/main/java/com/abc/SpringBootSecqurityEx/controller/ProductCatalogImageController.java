package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.ProductImageResponse;
import com.abc.SpringBootSecqurityEx.service.ProductImageService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Image operations for canonical commerce products; legacy /api/products remains untouched. */
@RestController
@RequestMapping("/api/catalog/products/{productId}/images")
public class ProductCatalogImageController {
    private final ProductImageService productImageService;

    public ProductCatalogImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.listForProduct(productId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<List<ProductImageResponse>> upload(
            @PathVariable Long productId,
            @RequestPart("images") @NotEmpty List<MultipartFile> images,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productImageService.uploadImages(productId, images, authentication));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long imageId,
                                       Authentication authentication) {
        productImageService.deleteImage(productId, imageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<ProductImageResponse> setPrimary(@PathVariable Long productId,
                                                            @PathVariable Long imageId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(productImageService.setPrimary(productId, imageId, authentication));
    }
}
