package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.CategoryDTO;
import com.abc.SpringBootSecqurityEx.entity.product.Category;
import com.abc.SpringBootSecqurityEx.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryServiceTest {

    @Test
    void createsTrimmedCategoryAndRejectsCaseInsensitiveDuplicateName() {
        Map<Long, Category> categories = new HashMap<>();
        CategoryRepository repository = (CategoryRepository) Proxy.newProxyInstance(
                CategoryRepository.class.getClassLoader(), new Class<?>[]{CategoryRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByNameIgnoreCase" -> categories.values().stream().anyMatch(category -> category.getName().equalsIgnoreCase((String) args[0]));
                    case "existsBySlugIgnoreCase" -> categories.values().stream().anyMatch(category -> category.getSlug().equalsIgnoreCase((String) args[0]));
                    case "existsByNameIgnoreCaseAndIdNot" -> categories.values().stream().anyMatch(category -> !category.getId().equals(args[1]) && category.getName().equalsIgnoreCase((String) args[0]));
                    case "existsBySlugIgnoreCaseAndIdNot" -> categories.values().stream().anyMatch(category -> !category.getId().equals(args[1]) && category.getSlug().equalsIgnoreCase((String) args[0]));
                    case "save" -> {
                        Category category = (Category) args[0];
                        if (category.getId() == null) category.setId(1L);
                        categories.put(category.getId(), category);
                        yield category;
                    }
                    case "findById" -> Optional.ofNullable(categories.get(args[0]));
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        CategoryService service = new CategoryService(repository, null);
        CategoryDTO category = new CategoryDTO();
        category.setName("  Home Goods ");
        category.setSlug("  home-goods ");

        CategoryDTO saved = service.save(category);

        assertEquals("Home Goods", saved.getName());
        assertEquals("home-goods", saved.getSlug());

        CategoryDTO duplicate = new CategoryDTO();
        duplicate.setName("HOME GOODS");
        duplicate.setSlug("home-goods-2");
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.save(duplicate));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
