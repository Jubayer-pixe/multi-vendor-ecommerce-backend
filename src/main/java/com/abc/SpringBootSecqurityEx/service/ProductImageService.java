package com.abc.SpringBootSecqurityEx.service;

import com.abc.SpringBootSecqurityEx.dtos.ProductImageDTO;
import com.abc.SpringBootSecqurityEx.dtos.ProductImageResponse;
import com.abc.SpringBootSecqurityEx.entity.product.Product;
import com.abc.SpringBootSecqurityEx.entity.product.ProductImage;
import com.abc.SpringBootSecqurityEx.entity.vendor.Vendor;
import com.abc.SpringBootSecqurityEx.repository.ProductCatalogRepository;
import com.abc.SpringBootSecqurityEx.repository.ProductImageRepository;
import com.abc.SpringBootSecqurityEx.repository.VendorRepository;
import com.abc.SpringBootSecqurityEx.util.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductImageService {
    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);
    private static final String STORAGE_PREFIX = "stored:";
    private static final String RESOURCE_PATH = "/api/product-images/%d/content";

    private final ProductImageRepository imageRepository;
    private final ProductCatalogRepository productRepository;
    private final VendorRepository vendorRepository;
    private final ProductImageStorage storage;
    private final int maxFilesPerUpload;

    public ProductImageService(ProductImageRepository imageRepository,
                               ProductCatalogRepository productRepository,
                               VendorRepository vendorRepository,
                               ProductImageStorage storage,
                               @Value("${app.product-images.max-files-per-upload:10}") int maxFilesPerUpload) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.storage = storage;
        if (maxFilesPerUpload <= 0) throw new IllegalArgumentException("Maximum image count must be positive");
        this.maxFilesPerUpload = maxFilesPerUpload;
    }

    public List<ProductImageResponse> uploadImages(Long productId, List<MultipartFile> files,
                                                   Authentication authentication) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one image file is required");
        }
        if (files.size() > maxFilesPerUpload) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many images in one upload");
        }

        Product product = lockProduct(productId);
        requireProductAccess(product, authentication);
        List<ProductImageStorage.StagedImage> staged = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image files must not be empty");
                }
                try (InputStream input = file.getInputStream()) {
                    staged.add(storage.stage(input));
                }
            }
            registerRollbackCleanup(staged);

            List<ProductImage> existing = imageRepository.findAllByProduct_IdOrderByIdAsc(productId);
            boolean hasPrimary = false;
            for (ProductImage image : existing) {
                if (Boolean.TRUE.equals(image.getPrimaryImage()) && !hasPrimary) {
                    hasPrimary = true;
                } else if (Boolean.TRUE.equals(image.getPrimaryImage())) {
                    image.setPrimaryImage(false);
                }
            }
            imageRepository.saveAll(existing);
            List<ProductImage> saved = new ArrayList<>();
            for (ProductImageStorage.StagedImage upload : staged) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageUrl(STORAGE_PREFIX + upload.storageKey());
                image.setPrimaryImage(!hasPrimary);
                image.setAltText(null);
                image = imageRepository.save(image);
                storage.promote(upload);
                saved.add(image);
                hasPrimary = true;
            }
            return saved.stream().map(this::toResponse).toList();
        } catch (IOException exception) {
            cleanupImmediately(staged);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image storage failed");
        } catch (RuntimeException exception) {
            cleanupImmediately(staged);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> listForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!Boolean.TRUE.equals(product.getActive())) throw new NotFoundException("Product not found");
        return imageRepository.findAllByProduct_IdOrderByIdAsc(productId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> listForVendorProduct(Long productId, Authentication authentication) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        requireProductAccess(product, authentication);
        return imageRepository.findAllByProduct_IdOrderByIdAsc(productId).stream().map(image -> {
            ProductImageResponse response = toResponse(image);
            response.setUrl("/api/catalog/vendor-products/" + productId + "/images/" + image.getId() + "/content");
            return response;
        }).toList();
    }

    @Transactional(readOnly = true)
    public ImageContent openVendorContent(Long productId, Long imageId, Authentication authentication) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        requireProductAccess(product, authentication);
        ProductImage image = imageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        String key = storageKey(image.getImageUrl());
        if (key == null) throw new NotFoundException("Product image content not found");
        try {
            ProductImageStorage.StoredImage metadata = storage.inspect(key);
            return new ImageContent(storage.open(key), metadata.contentType(), metadata.sizeBytes());
        } catch (IOException exception) {
            throw new NotFoundException("Product image content not found");
        }
    }

    public ProductImageResponse setPrimary(Long productId, Long imageId, Authentication authentication) {
        Product product = lockProduct(productId);
        requireProductAccess(product, authentication);
        ProductImage selected = imageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        List<ProductImage> images = imageRepository.findAllByProduct_IdOrderByIdAsc(productId);
        for (ProductImage image : images) {
            image.setPrimaryImage(image.getId().equals(selected.getId()));
        }
        imageRepository.saveAll(images);
        return toResponse(selected);
    }

    public void deleteImage(Long productId, Long imageId, Authentication authentication) {
        Product product = lockProduct(productId);
        requireProductAccess(product, authentication);
        ProductImage image = imageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        String key = storageKey(image.getImageUrl());
        imageRepository.delete(image);
        if (key != null) deleteAfterCommit(key);
    }

    public ImageContent openContent(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        if (!Boolean.TRUE.equals(image.getProduct().getActive())) {
            throw new NotFoundException("Product image not found");
        }
        String key = storageKey(image.getImageUrl());
        if (key == null) throw new NotFoundException("Product image content not found");
        try {
            ProductImageStorage.StoredImage metadata = storage.inspect(key);
            return new ImageContent(storage.open(key), metadata.contentType(), metadata.sizeBytes());
        } catch (IOException exception) {
            throw new NotFoundException("Product image content not found");
        }
    }

    // Preserve the existing ADMIN metadata CRUD route while preventing that route
    // from accepting internal storage keys or deleting files without coordination.
    @Transactional(readOnly = true)
    public List<ProductImageDTO> findAll() {
        return imageRepository.findAll().stream().map(this::toLegacyDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductImageDTO> findById(Long id) {
        return imageRepository.findById(id).map(this::toLegacyDto);
    }

    public ProductImageDTO saveExternal(ProductImageDTO request) {
        if (request.getProductId() == null || request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and imageUrl are required");
        }
        if (request.getImageUrl().startsWith(STORAGE_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage keys cannot be supplied by clients");
        }
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        ProductImage image;
        if (request.getId() == null) {
            image = new ProductImage();
        } else {
            image = imageRepository.findById(request.getId())
                    .orElseThrow(() -> new NotFoundException("Product image not found"));
            if (storageKey(image.getImageUrl()) != null) {
                if (!request.getImageUrl().equals(publicUrl(image))
                        || !image.getProduct().getId().equals(request.getProductId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Uploaded image storage and product association cannot be changed through metadata CRUD");
                }
            }
        }
        image.setProduct(product);
        if (storageKey(image.getImageUrl()) == null) image.setImageUrl(request.getImageUrl());
        image.setAltText(request.getAltText());
        boolean primary = Boolean.TRUE.equals(request.getPrimaryImage());
        if (primary) clearPrimary(product.getId());
        image.setPrimaryImage(primary);
        return toLegacyDto(imageRepository.save(image));
    }

    public void deleteById(Long id) {
        ProductImage image = imageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product image not found"));
        String key = storageKey(image.getImageUrl());
        imageRepository.delete(image);
        if (key != null) deleteAfterCommit(key);
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void requireProductAccess(Product product, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean vendorRole = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR"));
        boolean adminRole = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (adminRole) return;
        if (!vendorRole) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        Vendor vendor = vendorRepository.findByUser_UserName(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Vendor profile not found"));
        if (product.getVendor() == null || !vendor.getId().equals(product.getVendor().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
    }

    private void clearPrimary(Long productId) {
        List<ProductImage> images = imageRepository.findAllByProduct_IdOrderByIdAsc(productId);
        for (ProductImage image : images) image.setPrimaryImage(false);
        imageRepository.saveAll(images);
    }

    private ProductImageResponse toResponse(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.setId(image.getId());
        response.setProductId(image.getProduct().getId());
        response.setUrl(publicUrl(image));
        response.setAltText(image.getAltText());
        response.setPrimaryImage(image.getPrimaryImage());
        String key = storageKey(image.getImageUrl());
        if (key != null) {
            try {
                ProductImageStorage.StoredImage metadata = storage.inspect(key);
                response.setContentType(metadata.contentType());
                response.setSizeBytes(metadata.sizeBytes());
            } catch (IOException exception) {
                throw new NotFoundException("Product image content not found");
            }
        }
        return response;
    }

    private ProductImageDTO toLegacyDto(ProductImage image) {
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(image.getId());
        dto.setProductId(image.getProduct().getId());
        dto.setImageUrl(publicUrl(image));
        dto.setAltText(image.getAltText());
        dto.setPrimaryImage(image.getPrimaryImage());
        return dto;
    }

    private String publicUrl(ProductImage image) {
        return storageKey(image.getImageUrl()) == null
                ? image.getImageUrl()
                : RESOURCE_PATH.formatted(image.getId());
    }

    private String storageKey(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(STORAGE_PREFIX)
                ? imageUrl.substring(STORAGE_PREFIX.length()) : null;
    }

    private void registerRollbackCleanup(List<ProductImageStorage.StagedImage> uploads) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image uploads require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) cleanupImmediately(uploads);
                else uploads.forEach(upload -> quietly(() -> storage.discard(upload)));
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image deletion requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                quietly(() -> storage.delete(key));
            }
        });
    }

    private void cleanupImmediately(List<ProductImageStorage.StagedImage> uploads) {
        uploads.forEach(upload -> {
            quietly(() -> storage.delete(upload.storageKey()));
            quietly(() -> storage.discard(upload));
        });
    }

    private void quietly(IoAction action) {
        try {
            action.run();
        } catch (IOException exception) {
            log.warn("Product image cleanup failed; storage reconciliation is required");
        }
    }

    @FunctionalInterface
    private interface IoAction { void run() throws IOException; }

    public record ImageContent(InputStream stream, String contentType, long sizeBytes) { }
}
