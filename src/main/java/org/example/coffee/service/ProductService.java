package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.cloudinary.CloudinaryHelper;
import org.example.coffee.common.Common;
import org.example.coffee.common.ProductSite;
import org.example.coffee.dto.product.ProductInput;
import org.example.coffee.dto.product.ProductOutput;
import org.example.coffee.dto.productsize.ProductSizeOutput;
import org.example.coffee.entity.*;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.mapper.ProductMapper;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final CustomRepository customRepository;
    private final ProductMapper productMapper;
    private final ProductCategoryRepository productCategoryRepository;
    private final CommentRepository commentRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductPageConfigService productPageConfigService;

    @Transactional
    public void createProduct(String accessToken, ProductInput productInput, MultipartFile multipartFile) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        ProductEntity productEntity = productMapper.getEntityFromInput(productInput);
        if (Objects.nonNull(multipartFile) && !multipartFile.isEmpty()) {
            productEntity.setImage(CloudinaryHelper.uploadAndGetFileUrl(multipartFile));
        }
        productEntity.setCreatedAt(LocalDateTime.now());
        productRepository.save(productEntity);
    }

    @Transactional
    public void updateProduct(String accessToken, ProductInput productInput, Long productId, MultipartFile multipartFile) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        ProductEntity productEntity = customRepository.getProductBy(productId);
        productMapper.updateEntityFromInput(productEntity, productInput);
        if (Objects.nonNull(multipartFile) && !multipartFile.isEmpty()) {
            productEntity.setImage(CloudinaryHelper.uploadAndGetFileUrl(multipartFile));
        }
        productEntity.setCreatedAt(LocalDateTime.now());
        productRepository.save(productEntity);
    }

    @Transactional
    public void deleteProduct(String accessToken, Long productId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        productRepository.deleteById(productId);
        productCategoryRepository.deleteAllByProductId(productId);
        productSizeRepository.deleteAllByProductId(productId);
    }

    @Transactional(readOnly = true)
    public Page<ProductOutput> getProducts(ProductSite site, Integer requestedSize, Pageable pageable) {
        Pageable resolvedPageable = resolveProductPageable(site, requestedSize, pageable);
        Page<ProductEntity> products = productRepository.findAll(resolvedPageable);
        if (Objects.isNull(products) || products.isEmpty()) {
            return Page.empty();
        }

        List<Long> productIds = products.stream()
                .map(ProductEntity::getId).collect(Collectors.toList());
        Map<Long, List<ProductSizeEntity>> sizeMap = productSizeRepository.findAllByProductIdIn(productIds)
                .stream().collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        return products.map(productEntity -> buildProductOutput(productEntity, sizeMap.get(productEntity.getId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductOutput> getProductsByCategory(ProductSite site, Integer requestedSize, Pageable pageable, Long categoryId) {
        List<Long> productIds = productCategoryRepository.findAllByCategoryId(categoryId)
                .stream().map(ProductCategoryMapEntity::getProductId).collect(Collectors.toList());

        Pageable resolvedPageable = resolveProductPageable(site, requestedSize, pageable);
        Page<ProductEntity> products = productRepository.findAllByIdIn(productIds, resolvedPageable);
        if (Objects.isNull(products) || products.isEmpty()) {
            return Page.empty();
        }

        Map<Long, List<ProductSizeEntity>> sizeMap = productSizeRepository.findAllByProductIdIn(
                products.stream().map(ProductEntity::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        return products.map(productEntity -> buildProductOutput(productEntity, sizeMap.get(productEntity.getId())));
    }

    @Transactional
    public void addProductToCategory(String accessToken, Long productId, Long categoryId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        if (Boolean.TRUE.equals(productCategoryRepository.existsByCategoryIdAndProductId(categoryId, productId))) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }

        ProductCategoryMapEntity productCategoryMapEntity = ProductCategoryMapEntity.builder()
                .categoryId(categoryId)
                .productId(productId)
                .build();
        productCategoryRepository.save(productCategoryMapEntity);
    }

    @Transactional
    public List<ProductOutput> getProductsNotInCategory(String accessToken, Long categoryId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        List<Long> productIds = productCategoryRepository.findAllByCategoryId(categoryId)
                .stream().map(ProductCategoryMapEntity::getProductId).collect(Collectors.toList());

        List<ProductEntity> productEntities = (productIds.isEmpty())
                ? productRepository.findAll() : productRepository.findAllByIdNotIn(productIds);

        List<Long> allProductIds = productEntities.stream()
                .map(ProductEntity::getId).collect(Collectors.toList());
        Map<Long, List<ProductSizeEntity>> sizeMap = productSizeRepository.findAllByProductIdIn(allProductIds)
                .stream().collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        List<ProductOutput> productOutputs = new ArrayList<>();
        for (ProductEntity productEntity : productEntities) {
            productOutputs.add(buildProductOutput(productEntity, sizeMap.get(productEntity.getId())));
        }
        return productOutputs;
    }

    @Transactional
    public void removeProductFromCategory(String accessToken, Long categoryId, Long productId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(userId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
        productCategoryRepository.deleteByCategoryIdAndProductId(categoryId, productId);
    }

    @Transactional(readOnly = true)
    public ProductOutput getProductDetails(Long productId) {
        log.info("getProductDetails {}", productId);
        ProductEntity productEntity = customRepository.getProductBy(productId);
        List<ProductSizeEntity> sizes = productSizeRepository.findAllByProductId(productId);
        List<CommentEntity> commentEntities = commentRepository.findAllByProductId(productId);
        double averageRating = (double) Math.round(commentEntities
                .stream()
                .mapToLong(CommentEntity::getRating)
                .average().orElse(0.0) * 10) / 10;

        ProductOutput output = buildProductOutput(productEntity, sizes);
        output.setAverageRatting(averageRating);
        return output;
    }

    @Transactional(readOnly = true)
    public Page<ProductOutput> getProductsBySearch(String search, ProductSite site, Integer requestedSize, Pageable pageable) {
        Pageable resolvedPageable = resolveProductPageable(site, requestedSize, pageable);
        Page<ProductEntity> productEntities = productRepository.searchProductEntitiesByString(search, resolvedPageable);
        if (Objects.isNull(productEntities) || productEntities.isEmpty()) {
            return Page.empty();
        }

        Map<Long, List<ProductSizeEntity>> sizeMap = productSizeRepository.findAllByProductIdIn(
                productEntities.stream().map(ProductEntity::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        return productEntities.map(productEntity -> buildProductOutput(productEntity, sizeMap.get(productEntity.getId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductOutput> getAdminProducts(String accessToken, String search, Long categoryId,
                                                String sortBy, String direction, Integer requestedSize, Pageable pageable) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        String resolvedSortBy = normalizeProductSortBy(sortBy);
        String resolvedDirection = "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
        Pageable resolvedPageable = resolveProductPageable(ProductSite.ADMIN, requestedSize, pageable);
        Page<ProductEntity> productEntities = productRepository.searchAdminProducts(
                normalizeSearch(search), categoryId, resolvedSortBy, resolvedDirection, resolvedPageable);
        if (Objects.isNull(productEntities) || productEntities.isEmpty()) {
            return Page.empty();
        }

        Map<Long, List<ProductSizeEntity>> sizeMap = productSizeRepository.findAllByProductIdIn(
                productEntities.stream().map(ProductEntity::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        return productEntities.map(productEntity -> buildProductOutput(productEntity, sizeMap.get(productEntity.getId())));
    }

    private Pageable resolveProductPageable(ProductSite site, Integer requestedSize, Pageable pageable) {
        int pageSize = productPageConfigService.resolvePageSize(site, requestedSize);
        return PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
    }

    private String normalizeProductSortBy(String sortBy) {
        if ("name".equals(sortBy) || "price".equals(sortBy)) {
            return sortBy;
        }
        return "createdAt";
    }

    private String normalizeSearch(String search) {
        return Objects.isNull(search) ? "" : search.trim();
    }

    private ProductOutput buildProductOutput(ProductEntity productEntity, List<ProductSizeEntity> sizes) {
        List<ProductSizeOutput> sizeOutputs = null;
        Integer minPrice = null;

        if (sizes != null && !sizes.isEmpty()) {
            sizeOutputs = sizes.stream()
                    .map(s -> ProductSizeOutput.builder()
                            .id(s.getId())
                            .productId(s.getProductId())
                            .size(s.getSize())
                            .price(s.getPrice())
                            .description(s.getDescription())
                            .build())
                    .collect(Collectors.toList());
            minPrice = sizes.stream().mapToInt(ProductSizeEntity::getPrice).min().orElse(0);
        }

        return ProductOutput.builder()
                .productId(productEntity.getId())
                .name(productEntity.getName())
                .description(productEntity.getDescription())
                .image(productEntity.getImage())
                .minPrice(minPrice)
                .sizes(sizeOutputs)
                .build();
    }
}
