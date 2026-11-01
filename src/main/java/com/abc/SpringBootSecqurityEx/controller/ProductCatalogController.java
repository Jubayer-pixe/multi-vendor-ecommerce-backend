package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.ProductCatalogDTO;
import com.abc.SpringBootSecqurityEx.service.ProductCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.abc.SpringBootSecqurityEx.dtos.ProductCreateRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductCatalogController {
    private final ProductCatalogService service;

    public ProductCatalogController(ProductCatalogService service) {
        this.service = service;
    }

    @GetMapping("/public")
    public ResponseEntity<List<ProductCatalogDTO>> findPublicActive() {
        return ResponseEntity.ok(service.findPublicActiveProducts());
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProductCatalogDTO> findPublicActiveById(@PathVariable Long id) {
        return service.findPublicActiveProduct(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductCatalogDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCatalogDTO> findById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCatalogDTO> createJson(@Valid @RequestBody ProductCatalogDTO request) {
        ProductCatalogDTO created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Multipart form is selected separately from the existing JSON POST contract. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<ProductCatalogDTO> createWithImages(
            @Valid @RequestPart("product") ProductCreateRequest product,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        ProductCatalogDTO created = service.createWithImages(product, images, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCatalogDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody ProductCatalogDTO request) {
        if (request.getId() == null || !id.equals(request.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service.update(id, request).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (com.abc.SpringBootSecqurityEx.util.NotFoundException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
