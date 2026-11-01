package com.abc.SpringBootSecqurityEx.controller;

import com.abc.SpringBootSecqurityEx.dtos.StudentModelDTO;
import com.abc.SpringBootSecqurityEx.service.StudentModelService;
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
@RequestMapping("/api/students")
public class StudentModelController {
    private final StudentModelService service;

    public StudentModelController(StudentModelService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentModelDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentModelDTO> findById(@PathVariable String id) {
        Integer parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        return service.findById(parsedId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentModelDTO> create(@RequestBody StudentModelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentModelDTO> update(@PathVariable String id, @RequestBody StudentModelDTO dto) {
        Integer parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        if (!parsedId.equals(dto.getStudentId())) return ResponseEntity.badRequest().build();
        if (service.findById(parsedId).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(service.save(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Integer parsedId = parsePathId(id);
        if (parsedId == null) return ResponseEntity.badRequest().build();
        if (service.findById(parsedId).isEmpty()) return ResponseEntity.notFound().build();
        service.deleteById(parsedId);
        return ResponseEntity.noContent().build();
    }

    private Integer parsePathId(String id) {
        try { return Integer.valueOf(id); } catch (IllegalArgumentException exception) { return null; }
    }
}
