
// package com.abc.SpringBootSecqurityEx.controller;

// import com.abc.SpringBootSecqurityEx.dtos.DeliveryDTO;
// import com.abc.SpringBootSecqurityEx.service.DeliveryService;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/deliveries")
// public class DeliveryController {

//     private final DeliveryService service;

//     public DeliveryController(DeliveryService service) {
//         this.service = service;
//     }

//     @GetMapping
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<DeliveryDTO>> findAll() {
//         return ResponseEntity.ok(service.findAll());
//     }

//     @GetMapping("/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<DeliveryDTO> findById(
//         @PathVariable Long id
//     ) {
//         return service.findById(id)
//             .map(ResponseEntity::ok)
//             .orElseGet(() -> ResponseEntity.notFound().build());
//     }

//     @GetMapping("/agent/{agentId}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<DeliveryDTO>> findByAgent(
//         @PathVariable Long agentId
//     ) {
//         return ResponseEntity.ok(service.findByAgent(agentId));
//     }

//     @PostMapping
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<DeliveryDTO> create(
//         @RequestBody DeliveryDTO dto
//     ) {
//         return ResponseEntity.status(HttpStatus.CREATED)
//             .body(service.create(dto));
//     }

//     @PutMapping("/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<DeliveryDTO> update(
//         @PathVariable Long id,
//         @RequestBody DeliveryDTO dto
//     ) {
//         return ResponseEntity.ok(service.update(id, dto));
//     }

//     @DeleteMapping("/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<Void> delete(@PathVariable Long id) {
//         service.deleteById(id);
//         return ResponseEntity.noContent().build();
//     }
// }
