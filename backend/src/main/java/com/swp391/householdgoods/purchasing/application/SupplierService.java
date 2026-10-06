package com.swp391.householdgoods.purchasing.application;

import com.swp391.householdgoods.purchasing.application.dto.CreateSupplierRequest;
import com.swp391.householdgoods.purchasing.application.dto.SupplierResponse;
import com.swp391.householdgoods.purchasing.application.dto.UpdateSupplierRequest;
import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import com.swp391.householdgoods.purchasing.infrastructure.SupplierRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String keyword, SupplierStatus status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return supplierRepository.searchSuppliers(cleanKeyword, status, pageable)
                .map(SupplierResponse::fromDomain);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findAllByStatusAndIsDeletedFalseOrderByNameAsc(SupplierStatus.ACTIVE)
                .stream()
                .map(SupplierResponse::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        return supplierRepository.findByIdAndIsDeletedFalse(id)
                .map(SupplierResponse::fromDomain)
                .orElseThrow(() -> new SupplierNotFoundException("Không tìm thấy nhà cung cấp với ID: " + id));
    }

    public SupplierResponse createSupplier(CreateSupplierRequest request, String actor) {
        String code = request.code().trim();
        String taxCode = request.taxCode().trim();

        if (supplierRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new SupplierDuplicateException("Mã nhà cung cấp đã tồn tại: " + code);
        }
        if (supplierRepository.existsByTaxCodeAndIsDeletedFalse(taxCode)) {
            throw new SupplierDuplicateException("Mã số thuế đã tồn tại: " + taxCode);
        }

        Supplier supplier = new Supplier(
                code,
                taxCode,
                request.name().trim(),
                request.contactName() != null ? request.contactName().trim() : null,
                request.phone() != null ? request.phone().trim() : null,
                request.email() != null ? request.email().trim() : null,
                request.address() != null ? request.address().trim() : null,
                actor
        );

        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponse.fromDomain(saved);
    }

    public SupplierResponse updateSupplier(Long id, UpdateSupplierRequest request, String actor) {
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new SupplierNotFoundException("Không tìm thấy nhà cung cấp với ID: " + id));

        supplier.setName(request.name().trim());
        supplier.setContactName(request.contactName() != null ? request.contactName().trim() : null);
        supplier.setPhone(request.phone() != null ? request.phone().trim() : null);
        supplier.setEmail(request.email() != null ? request.email().trim() : null);
        supplier.setAddress(request.address() != null ? request.address().trim() : null);
        supplier.setStatus(request.status());
        supplier.setUpdatedBy(actor);

        Supplier updated = supplierRepository.save(supplier);
        return SupplierResponse.fromDomain(updated);
    }

    public void deleteSupplier(Long id, String actor) {
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new SupplierNotFoundException("Không tìm thấy nhà cung cấp với ID: " + id));

        supplier.setDeleted(true);
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplier.setUpdatedBy(actor);
        supplierRepository.save(supplier);
    }
}
