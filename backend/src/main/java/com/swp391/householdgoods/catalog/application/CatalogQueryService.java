package com.swp391.householdgoods.catalog.application;

import com.swp391.householdgoods.catalog.api.*;
import com.swp391.householdgoods.catalog.infrastructure.CatalogRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class CatalogQueryService {
    private final CatalogRepository repository;
    public CatalogQueryService(CatalogRepository repository) { this.repository=repository; }
    public CatalogPage page(int page,int size) {
        if(page<0 || size<1 || size>100) throw new CatalogException(HttpStatus.BAD_REQUEST,
            "CATALOG_PAGE_INVALID","Trang phải từ 0; số mục mỗi trang từ 1 đến 100.");
        return new CatalogPage(repository.page(page,size),page,size,repository.count());
    }
    public CatalogSku sku(long id) { return require(repository.bySku(id)).getFirst(); }
    public List<CatalogSku> product(long id) { return require(repository.byProduct(id)); }
    public List<CatalogReference> categories() { return repository.categories(); }
    public List<CatalogReference> brands() { return repository.brands(); }
    private List<CatalogSku> require(List<CatalogSku> items) {
        if(items.isEmpty()) throw new CatalogException(HttpStatus.NOT_FOUND,
            "CATALOG_NOT_FOUND","Không tìm thấy sản phẩm đang được công bố.");
        return items;
    }
}
