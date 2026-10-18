
package com.swp391.householdgoods.catalog.brand.application;

import com.swp391.householdgoods.catalog.brand.application.dto.BrandResponse;
import com.swp391.householdgoods.catalog.brand.application.dto.CreateBrandRequest;
import com.swp391.householdgoods.catalog.brand.application.dto.UpdateBrandRequest;
import com.swp391.householdgoods.catalog.brand.domain.Brand;
import com.swp391.householdgoods.catalog.brand.infrastructure.BrandRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public Page<BrandResponse> searchBrands(String keyword, String status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : "";
        Boolean isDeletedFilter = null;

        if (status != null && !status.trim().isEmpty()) {
            String s = status.trim().toUpperCase();
            if ("ACTIVE".equals(s)) {
                isDeletedFilter = false;
            } else if ("INACTIVE".equals(s) || "DEACTIVATED".equals(s)) {
                isDeletedFilter = true;
            }
        }

        return brandRepository.searchBrands(cleanKeyword, isDeletedFilter, pageable)
                .map(BrandResponse::fromDomain);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        return brandRepository.findById(id)
                .map(BrandResponse::fromDomain)
                .orElseThrow(() -> new BrandNotFoundException(
                        "Không tìm thấy thương hiệu với ID: " + id));
    }

    public BrandResponse createBrand(CreateBrandRequest request, String actor) {
        String trimmedName = request.name().trim();

        if (brandRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BrandDuplicateException(
                    "Tên thương hiệu đã tồn tại: " + trimmedName);
        }

        String country = request.country() != null && !request.country().isBlank()
                ? request.country().trim() : null;

        String description = request.description() != null && !request.description().isBlank()
                ? request.description().trim() : null;

        String logoUrl = request.logoUrl() != null && !request.logoUrl().isBlank()
                ? request.logoUrl().trim() : null;

        Brand brand = new Brand(trimmedName, country, description, logoUrl, actor);

        try {
            Brand saved = brandRepository.saveAndFlush(brand);
            return BrandResponse.fromDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BrandDuplicateException(
                    "Tên thương hiệu đã tồn tại: " + trimmedName);
        }
    }

    public BrandResponse updateBrand(Long id, UpdateBrandRequest request, String actor) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(
                        "Không tìm thấy thương hiệu với ID: " + id));

        // Khong cho phep sua thuong hieu da vo hieu hoa
        if (brand.isDeleted()) {
            throw new BrandDeactivatedException(
                    "Không thể cập nhật thương hiệu đã bị vô hiệu hóa.");
        }

        String trimmedName = request.name().trim();

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new BrandDuplicateException(
                    "Tên thương hiệu đã tồn tại: " + trimmedName);
        }

        brand.setName(trimmedName);

        brand.setCountry(
                request.country() != null && !request.country().isBlank()
                        ? request.country().trim() : null
        );

        brand.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim() : null
        );

        brand.setLogoUrl(
                request.logoUrl() != null && !request.logoUrl().isBlank()
                        ? request.logoUrl().trim() : null
        );

        brand.setUpdatedBy(actor);

        try {
            Brand updated = brandRepository.saveAndFlush(brand);
            return BrandResponse.fromDomain(updated);
        } catch (DataIntegrityViolationException e) {
            throw new BrandDuplicateException(
                    "Tên thương hiệu đã tồn tại: " + trimmedName);
        }
    }

    public BrandResponse deactivateBrand(Long id, String actor) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(
                        "Không tìm thấy thương hiệu với ID: " + id));

        // Neu da vo hieu hoa thi tra ve luon
        if (brand.isDeleted()) {
            return BrandResponse.fromDomain(brand);
        }

        // Khong vo hieu hoa neu van con san pham dang hoat dong
        long activeProducts = brandRepository.countActiveProductsByBrandId(id);

        if (activeProducts > 0) {
            throw new BrandHasActiveProductsException(
                    "Không thể vô hiệu hóa thương hiệu vì vẫn còn "
                            + activeProducts
                            + " sản phẩm đang hoạt động.");
        }

        brand.setDeleted(true);
        brand.setUpdatedBy(actor);

        Brand updated = brandRepository.saveAndFlush(brand);
        return BrandResponse.fromDomain(updated);
    }

    // ==========================================
    // REACTIVATE BRAND - KHOI PHUC THUONG HIEU
    // ==========================================
    public BrandResponse reactivateBrand(Long id, String actor) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(
                        "Không tìm thấy thương hiệu với ID: " + id));

        // Neu da hoat dong thi tra ve luon
        if (!brand.isDeleted()) {
            return BrandResponse.fromDomain(brand);
        }

        // Kiem tra trung ten voi thuong hieu khac
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(brand.getName(), id)) {
            throw new BrandDuplicateException(
                    "Không thể khôi phục vì tên thương hiệu đã tồn tại: "
                            + brand.getName());
        }

        // Chuyen trang thai tu INACTIVE sang ACTIVE
        brand.setDeleted(false);
        brand.setUpdatedBy(actor);

        try {
            Brand updated = brandRepository.saveAndFlush(brand);
            return BrandResponse.fromDomain(updated);
        } catch (DataIntegrityViolationException e) {
            throw new BrandDuplicateException(
                    "Không thể khôi phục thương hiệu do xung đột dữ liệu.");
        }
    }
}
