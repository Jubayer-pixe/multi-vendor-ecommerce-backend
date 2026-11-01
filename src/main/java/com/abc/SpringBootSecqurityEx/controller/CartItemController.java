package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.CartItemDTO;
import com.abc.SpringBootSecqurityEx.service.CartItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {
    private final CartItemService service;

    public CartItemController(CartItemService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CartItemDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CartItemDTO> findById(@PathVariable String id) {
        Long parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        return service.findById(parsedId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CartItemDTO> create(@RequestBody CartItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CartItemDTO> update(@PathVariable String id, @RequestBody CartItemDTO dto) {
        Long parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        if (!parsedId.equals(dto.getId())) return ResponseEntity.badRequest().build();
        if (service.findById(parsedId).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(service.save(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Long parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        if (service.findById(parsedId).isEmpty()) return ResponseEntity.notFound().build();
        service.deleteById(parsedId);
        return ResponseEntity.noContent().build();
    }

    private Long parsePathId(String id) {
        try { return Long.valueOf(id); } catch (IllegalArgumentException exception) { return null; }
    }
}
