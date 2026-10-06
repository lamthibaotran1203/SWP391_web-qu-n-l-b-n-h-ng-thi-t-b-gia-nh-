package com.swp391.householdgoods.purchasing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.purchasing.application.dto.CreateSupplierRequest;
import com.swp391.householdgoods.purchasing.application.dto.SupplierResponse;
import com.swp391.householdgoods.purchasing.application.dto.UpdateSupplierRequest;
import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import com.swp391.householdgoods.purchasing.infrastructure.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private SupplierService service;

    @Test
    void createSupplierSavesWhenCodeAndTaxCodeAreUnique() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-01",
                "0312345678",
                "Cong ty Dien May Sunhouse",
                "Nguyen Van A",
                "0901234567",
                "sunhouse@example.com",
                "Ha Noi"
        );

        when(repository.existsByCodeAndIsDeletedFalse("SUP-01")).thenReturn(false);
        when(repository.existsByTaxCodeAndIsDeletedFalse("0312345678")).thenReturn(false);
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        SupplierResponse response = service.createSupplier(request, "admin@homemart.vn");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("SUP-01");
        assertThat(response.taxCode()).isEqualTo("0312345678");
        assertThat(response.name()).isEqualTo("Cong ty Dien May Sunhouse");
        assertThat(response.status()).isEqualTo(SupplierStatus.ACTIVE);
    }

    @Test
    void createSupplierThrowsWhenCodeAlreadyExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-DUP",
                "0399999999",
                "Cong ty B",
                null,
                null,
                null,
                null
        );

        when(repository.existsByCodeAndIsDeletedFalse("SUP-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.createSupplier(request, "admin@homemart.vn"))
                .isInstanceOf(SupplierDuplicateException.class)
                .hasMessageContaining("Mã nhà cung cấp đã tồn tại");
    }

    @Test
    void createSupplierThrowsWhenTaxCodeAlreadyExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-NEW",
                "0388888888",
                "Cong ty C",
                null,
                null,
                null,
                null
        );

        when(repository.existsByCodeAndIsDeletedFalse("SUP-NEW")).thenReturn(false);
        when(repository.existsByTaxCodeAndIsDeletedFalse("0388888888")).thenReturn(true);

        assertThatThrownBy(() -> service.createSupplier(request, "admin@homemart.vn"))
                .isInstanceOf(SupplierDuplicateException.class)
                .hasMessageContaining("Mã số thuế đã tồn tại");
    }

    @Test
    void updateSupplierUpdatesAllowedFields() {
        Supplier existing = new Supplier("SUP-01", "0312345678", "Sunhouse Old", "Nguyen A", "0901", "a@mail.com", "HN", "admin");
        existing.setId(1L);

        when(repository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSupplierRequest updateRequest = new UpdateSupplierRequest(
                "Sunhouse New",
                "Tran B",
                "0988776655",
                "b@mail.com",
                "TP HCM",
                SupplierStatus.INACTIVE
        );

        SupplierResponse response = service.updateSupplier(1L, updateRequest, "manager@homemart.vn");

        assertThat(response.name()).isEqualTo("Sunhouse New");
        assertThat(response.contactName()).isEqualTo("Tran B");
        assertThat(response.phone()).isEqualTo("0988776655");
        assertThat(response.status()).isEqualTo(SupplierStatus.INACTIVE);
    }

    @Test
    void deleteSupplierSetsSoftDeleteAndInactive() {
        Supplier existing = new Supplier("SUP-01", "0312345678", "Sunhouse", "Nguyen A", "0901", "a@mail.com", "HN", "admin");
        existing.setId(1L);

        when(repository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteSupplier(1L, "admin@homemart.vn");

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getStatus()).isEqualTo(SupplierStatus.INACTIVE);
        verify(repository).save(existing);
    }

    @Test
    void searchSuppliersDelegatesToRepository() {
        Supplier s = new Supplier("SUP-01", "0312345678", "Sunhouse", "Nguyen A", "0901", "a@mail.com", "HN", "admin");
        s.setId(1L);

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(repository.searchSuppliers("sunhouse", SupplierStatus.ACTIVE, pageRequest))
                .thenReturn(new PageImpl<>(List.of(s), pageRequest, 1));

        Page<SupplierResponse> result = service.searchSuppliers("sunhouse", SupplierStatus.ACTIVE, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Sunhouse");
    }
}
