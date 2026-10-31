package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ProductCatalogDTO;
import com.abc.SpringBootSecqurityEx.dtos.ProductCreateRequest;
import com.abc.SpringBootSecqurityEx.dtos.ProductImageResponse;
import com.abc.SpringBootSecqurityEx.entity.product.Category;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.product.SubCategory;
import com.abc.SpringBootSecqurityEx.repository.BrandRepository;
import com.abc.SpringBootSecqurityEx.repository.CategoryRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.SubCategoryRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductCatalogService {
    private final ProductCatalogRepository catalogRepository;
    private final ProductImageService productImageService;
    private final VendorRepository vendorRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;

    public ProductCatalogService(ProductCatalogRepository catalogRepository,
                                 ProductImageService productImageService,
                                 VendorRepository vendorRepository,
                                 CategoryRepository categoryRepository,
                                 SubCategoryRepository subCategoryRepository,
                                 BrandRepository brandRepository) {
        this.catalogRepository = catalogRepository;
        this.productImageService = productImageService;
        this.vendorRepository = vendorRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogDTO> findAll() {
        return catalogRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogDTO> findPublicActiveProducts() {
        return catalogRepository.findAllByActiveTrueOrderByNameAsc().stream().map(product -> {
            ProductCatalogDTO dto = toDto(product);
            dto.setImages(productImageService.listForProduct(product.getId()));
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductCatalogDTO> findPublicActiveProduct(Long id) {
        return catalogRepository.findByIdAndActiveTrue(id).map(product -> {
            ProductCatalogDTO dto = toDto(product);
            dto.setImages(productImageService.listForProduct(product.getId()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Optional<ProductCatalogDTO> findById(Long id) {
        return catalogRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogDTO> findVendorProducts(Authentication authentication) {
        String username = requireVendorUsername(authentication);
        return catalogRepository.findAllByVendor_User_UserNameOrderByNameAsc(username).stream()
                .map(product -> toVendorDto(product, authentication)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductCatalogDTO> findVendorProduct(Long id, Authentication authentication) {
        String username = requireVendorUsername(authentication);
        return catalogRepository.findByIdAndVendor_User_UserName(id, username)
                .map(product -> toVendorDto(product, authentication));
    }

    public Optional<ProductCatalogDTO> updateVendorProduct(Long id, ProductCatalogDTO request,
                                                            Authentication authentication) {
        String username = requireVendorUsername(authentication);
        return catalogRepository.findByIdAndVendor_User_UserName(id, username).map(product -> {
            validateCore(request.getName(), request.getSlug(), request.getSku(),
                    request.getBasePrice(), request.getSalePrice());
            if (request.getStockQuantity() != null && request.getStockQuantity() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockQuantity must not be negative");
            }
            ensureUnique(request.getSlug(), request.getSku(), id);
            product.setName(request.getName().trim());
            product.setSlug(request.getSlug().trim());
            product.setSku(request.getSku().trim());
            product.setDescription(request.getDescription());
            product.setBasePrice(request.getBasePrice());
            product.setSalePrice(request.getSalePrice());
            if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
            if (request.getActive() != null) product.setActive(request.getActive());
            applyCatalogRelations(product, request);
            return toVendorDto(catalogRepository.save(product), authentication);
        });
    }

    public ProductCatalogDTO create(ProductCatalogDTO request) {
        if (request.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id must not be supplied on create");
        }
        validateCore(request.getName(), request.getSlug(), request.getSku(),
                request.getBasePrice(), request.getSalePrice());
        if (request.getStockQuantity() != null && request.getStockQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockQuantity must not be negative");
        }
        ensureUnique(request.getSlug(), request.getSku(), null);
        Product product = new Product();
        applyCatalogRequest(request, product);
        return toDto(catalogRepository.save(product));
    }

    public Optional<ProductCatalogDTO> update(Long id, ProductCatalogDTO request) {
        return catalogRepository.findById(id).map(product -> {
            validateCore(request.getName(), request.getSlug(), request.getSku(),
                    request.getBasePrice(), request.getSalePrice());
            if (request.getStockQuantity() != null && request.getStockQuantity() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockQuantity must not be negative");
            }
            ensureUnique(request.getSlug(), request.getSku(), id);
            applyCatalogRequest(request, product);
            return toDto(catalogRepository.save(product));
        });
    }

    @Transactional
    public ProductCatalogDTO createWithImages(ProductCreateRequest request, List<MultipartFile> files,
                                              Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        boolean hasVendorRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VENDOR"));
        boolean hasAdminRole = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!hasVendorRole && !hasAdminRole) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        validateCore(request.getName(), request.getSlug(), request.getSku(),
                request.getBasePrice(), request.getSalePrice());
        ensureUnique(request.getSlug(), request.getSku(), null);
        Product product = new Product();
        product.setName(request.getName().trim());
        product.setSlug(request.getSlug().trim());
        product.setSku(request.getSku().trim());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setStockQuantity(request.getStockQuantity() == null ? 0 : request.getStockQuantity());
        product.setActive(request.getActive() == null || request.getActive());
        applyCreateRelations(product, request);

        if (hasVendorRole) {
            var vendor = vendorRepository.findByUser_UserName(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Authenticated vendor profile is unavailable"));
            product.setVendor(vendor);
        }

        ProductCatalogDTO created = toDto(catalogRepository.save(product));
        List<ProductImageResponse> images = files == null || files.isEmpty() ? List.of()
                : productImageService.uploadImages(created.getId(), files, authentication);
        created.setImages(hasVendorRole
                ? productImageService.listForVendorProduct(created.getId(), authentication)
                : images);
        created.setImageIds(images.stream().map(ProductImageResponse::getId).toList());
        return created;
    }

    public void deleteById(Long id) {
        Product product = catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        catalogRepository.delete(product);
    }

    private void applyCatalogRequest(ProductCatalogDTO request, Product product) {
        product.setName(request.getName().trim());
        product.setSlug(request.getSlug().trim());
        product.setSku(request.getSku().trim());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null) product.setActive(request.getActive());
        applyRelations(product, request.getCategoryId(), request.getSubCategoryId(),
                request.getBrandId(), request.getVendorId());
    }

    private void applyRelations(Product product, Long categoryId, Long subCategoryId,
                                Long brandId, Long vendorId) {
        Category category = categoryId == null ? null : categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        SubCategory subCategory = subCategoryId == null ? null : subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subcategory not found"));
        if (subCategory != null) {
            if (category != null && !subCategory.getCategory().getId().equals(category.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Subcategory does not belong to the selected category");
            }
            if (category == null) category = subCategory.getCategory();
        }
        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setBrand(brandId == null ? null : brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand not found")));
        product.setVendor(vendorId == null ? null : vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found")));
    }

    private void applyCreateRelations(Product product, ProductCreateRequest request) {
        Long categoryId = request.getCategoryId();
        if (categoryId == null && request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            categoryId = categoryRepository.findByNameIgnoreCase(request.getCategoryName().trim())
                    .map(Category::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }
        Long brandId = request.getBrandId();
        if (brandId == null && request.getBrandName() != null && !request.getBrandName().isBlank()) {
            brandId = brandRepository.findByNameIgnoreCase(request.getBrandName().trim())
                    .map(brand -> brand.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand not found"));
        }
        applyRelations(product, categoryId, request.getSubCategoryId(), brandId, null);
    }

    private void applyCatalogRelations(Product product, ProductCatalogDTO request) {
        var vendor = product.getVendor();
        Long categoryId = request.getCategoryId();
        if (categoryId == null && request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            categoryId = categoryRepository.findByNameIgnoreCase(request.getCategoryName().trim())
                    .map(Category::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }
        Long brandId = request.getBrandId();
        if (brandId == null && request.getBrandName() != null && !request.getBrandName().isBlank()) {
            brandId = brandRepository.findByNameIgnoreCase(request.getBrandName().trim())
                    .map(brand -> brand.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand not found"));
        }
        applyRelations(product, categoryId, request.getSubCategoryId(), brandId, null);
        product.setVendor(vendor);
    }

    private String requireVendorUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_VENDOR"))) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return authentication.getName();
    }

    private ProductCatalogDTO toVendorDto(Product product, Authentication authentication) {
        ProductCatalogDTO dto = toDto(product);
        dto.setImages(productImageService.listForVendorProduct(product.getId(), authentication));
        dto.setImageIds(dto.getImages().stream().map(ProductImageResponse::getId).toList());
        return dto;
    }

    private void validateCore(String name, String slug, String sku, BigDecimal basePrice, BigDecimal salePrice) {
        if (name == null || name.isBlank() || slug == null || slug.isBlank() || sku == null || sku.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, slug, and sku are required");
        }
        if (basePrice == null || basePrice.signum() <= 0
                || (salePrice != null && (salePrice.signum() < 0 || salePrice.compareTo(basePrice) > 0))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product prices are invalid");
        }
    }

    private void ensureUnique(String slug, String sku, Long excludedId) {
        String normalizedSlug = slug.trim();
        String normalizedSku = sku.trim();
        boolean duplicateSlug = excludedId == null ? catalogRepository.existsBySlug(normalizedSlug)
                : catalogRepository.existsBySlugAndIdNot(normalizedSlug, excludedId);
        boolean duplicateSku = excludedId == null ? catalogRepository.existsBySku(normalizedSku)
                : catalogRepository.existsBySkuAndIdNot(normalizedSku, excludedId);
        if (duplicateSlug) throw new ResponseStatusException(HttpStatus.CONFLICT, "Product slug already exists");
        if (duplicateSku) throw new ResponseStatusException(HttpStatus.CONFLICT, "Product SKU already exists");
    }

    private ProductCatalogDTO toDto(Product product) {
        ProductCatalogDTO dto = new ProductCatalogDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setDescription(product.getDescription());
        dto.setSku(product.getSku());
        dto.setBasePrice(product.getBasePrice());
        dto.setSalePrice(product.getSalePrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setActive(product.getActive());
        dto.setCategoryId(product.getCategory() == null ? null : product.getCategory().getId());
        dto.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
        dto.setSubCategoryId(product.getSubCategory() == null ? null : product.getSubCategory().getId());
        dto.setBrandId(product.getBrand() == null ? null : product.getBrand().getId());
        dto.setBrandName(product.getBrand() == null ? null : product.getBrand().getName());
        dto.setVendorId(product.getVendor() == null ? null : product.getVendor().getId());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setImageIds(product.getImages() == null ? List.of()
                : product.getImages().stream().map(image -> image.getId()).toList());
        dto.setVariantIds(product.getVariants() == null ? List.of()
                : product.getVariants().stream().map(variant -> variant.getId()).toList());
        return dto;
    }
}
