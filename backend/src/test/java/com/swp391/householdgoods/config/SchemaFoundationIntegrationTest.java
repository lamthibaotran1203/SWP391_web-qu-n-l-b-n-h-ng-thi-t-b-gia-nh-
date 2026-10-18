package com.swp391.householdgoods.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.api.SalesOrderPage;
import com.swp391.householdgoods.orders.api.SalesOrderSummary;
import com.swp391.householdgoods.orders.application.SalesOrderQueryService;
import com.swp391.householdgoods.orders.application.SalesOrderSearch;
import com.swp391.householdgoods.orders.domain.OrderChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaFoundationIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SalesOrderQueryService salesOrderQueryService;

    @Test
    void existingIdentityIsTheOnlyAccountSourceAndAppliedMigrationsValidate() {
        assertThat(jdbc.queryForObject("select count(*) from app.flyway_schema_history "
                + "where version in ('1','2','3','4','5','6','7','8','9','10') and success", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema='app' and table_name in ('hm_user','hm_role','hm_userrole','hm_product')",
                Integer.class)).isZero();
    }

    @Test
    void adminRoleReplacesManagerRoleInTheDatabaseConstraint() {
        long adminCount = jdbc.queryForObject(
                "select count(*) from app.user_account where role='ADMIN'", Long.class);
        jdbc.update("insert into app.user_account(email,password_hash,full_name,role,status,email_verified_at,created_at,updated_at) "
                + "values (?,'{noop}test','Quản trị viên kiểm thử','ADMIN','ACTIVE',current_timestamp,current_timestamp,current_timestamp)",
                "admin-" + java.util.UUID.randomUUID() + "@example.invalid");
        assertThat(jdbc.queryForObject(
                "select count(*) from app.user_account where role='ADMIN'", Long.class)).isEqualTo(adminCount + 1);
        rejects(() -> jdbc.update("insert into app.user_account(email,password_hash,full_name,role,status,email_verified_at,created_at,updated_at) "
                + "values (?,'{noop}test','Quản lý cũ','MANAGER','ACTIVE',current_timestamp,current_timestamp,current_timestamp)",
                "manager-" + java.util.UUID.randomUUID() + "@example.invalid"));
    }

    @Test
    void categoryCodesAndNamesAreCaseInsensitiveUniqueAndSelfParentIsRejected() {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String code = "UC11-" + suffix;
        String name = "Danh mục UC11 " + suffix;
        long category = jdbc.queryForObject(
                "insert into app.hm_category(code,name,created_at) values (?,?,current_timestamp) returning id",
                Long.class, code, name);

        rejects(() -> jdbc.update(
                "insert into app.hm_category(code,name,created_at) values (?,?,current_timestamp)",
                "  " + code.toLowerCase() + "  ", "Tên khác " + suffix));
        rejects(() -> jdbc.update(
                "insert into app.hm_category(code,name,created_at) values (?,?,current_timestamp)",
                "OTHER-" + suffix, "  " + name.toUpperCase() + "  "));
        rejects(() -> jdbc.update("update app.hm_category set parent_id=id where id=?", category));
        rejects(() -> jdbc.update(
                "insert into app.hm_category(code,name,created_at) values ('   ','Tên hợp lệ',current_timestamp)"));
    }

    @Test
    void salesOrderSearchReadsSharedOrderPaymentAndSkuTables() {
        long user = account();
        long orderId = jdbc.queryForObject(
                "insert into app.hm_order(user_id,order_code,order_type,status,payment_status,total_amount,"
                        + "discount_amount,shipping_fee,final_amount,created_at) "
                        + "values (?,?,'ONLINE','COMPLETED','PAID',100000,0,0,100000,current_timestamp) "
                        + "returning id",
                Long.class,
                user,
                "ORDER-" + java.util.UUID.randomUUID());
        long sku = sku();
        jdbc.update("insert into app.hm_order_item(order_id,sku_id,quantity,unit_price,total_price,"
                + "sku_code_snapshot,sku_name_snapshot,created_at) "
                + "values (?,?,2,50000,100000,'SKU-TEST','Ấm kiểm thử',current_timestamp)",
                orderId,
                sku);
        jdbc.update("insert into app.hm_payment(order_id,transaction_code,idempotency_key,amount,payment_method,"
                + "provider,status,payment_time,created_at) "
                + "values (?,?,?,100000,'CASH','STORE','COMPLETED',current_timestamp,current_timestamp)",
                orderId,
                "PAY-" + java.util.UUID.randomUUID(),
                "IDEMP-" + java.util.UUID.randomUUID());
        jdbc.update("insert into app.hm_shipment(order_id,recipient_name,recipient_phone,shipping_address,"
                + "shipping_status,created_at) values (?,'Người nhận đơn','0912345678','Địa chỉ giao hàng',"
                + "'PENDING',current_timestamp)",
                orderId);

        String orderCode = jdbc.queryForObject(
                "select order_code from app.hm_order where id=?",
                String.class,
                orderId);
        SalesOrderSearch search = new SalesOrderSearch(
                "0912345678", OrderChannel.ONLINE, "COMPLETED", "PAID", null, null);
        SalesOrderPage<SalesOrderSummary> result = salesOrderQueryService.search(search, 0, 10);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(order -> {
            assertThat(order.id()).isEqualTo(orderId);
            assertThat(order.orderNumber()).isEqualTo(orderCode);
            assertThat(order.customerName()).isEqualTo("Người kiểm thử");
            assertThat(order.channel()).isEqualTo("ONLINE");
            assertThat(order.paymentMethod()).isEqualTo("CASH");
            assertThat(order.totalAmount()).isEqualByComparingTo("100000");
        });

        SalesOrderDetails details = salesOrderQueryService.getDetails(orderId);
        assertThat(details.shippingAddress()).isEqualTo("Địa chỉ giao hàng");
        assertThat(details.customerPhone()).isEqualTo("0912345678");
        assertThat(details.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Ấm kiểm thử");
            assertThat(item.productSku()).isEqualTo("SKU-TEST");
            assertThat(item.lineTotal()).isEqualByComparingTo("100000");
        });
    }

    @Test
    void deletedDefaultNoLongerBlocksReplacementAndTwoActiveDefaultsAreRejected() {
        long user = account();
        long address = address(user, true);
        jdbc.update("update app.hm_address set is_default=false, is_deleted=true where id=?", address);
        address(user, true);
        rejects(() -> address(user, true));
        assertThat(jdbc.queryForObject("select count(*) from app.hm_address "
                + "where user_id=? and is_default and not is_deleted", Integer.class, user)).isEqualTo(1);
    }

    @Test
    void publicationAndSkuIdentifiersAreProtectedAndInventoryIsGenerated() {
        long sku = sku();
        rejects(() -> jdbc.update("update app.product set image_url=null where product_id="
                + "(select product_id from app.hm_sku where id=?)", sku));
        rejects(() -> jdbc.update("update app.hm_sku set barcode='' where id=?", sku));
        rejects(() -> jdbc.update("update app.hm_sku set selling_price=-1 where id=?", sku));
        jdbc.update("insert into app.hm_inventory(sku_id,physical_quantity,reserved_quantity,created_at) "
                + "values (?,10,3,current_timestamp)", sku);
        assertThat(jdbc.queryForObject("select available_quantity from app.hm_inventory where sku_id=?",
                Integer.class, sku)).isEqualTo(7);
        rejects(() -> jdbc.update("update app.hm_inventory set reserved_quantity=11 where sku_id=?", sku));
    }

    @Test
    void returnLineMustBeFromTheSameOriginalOrderAndCannotRestockNonResalableGoods() {
        long sku = sku();
        long firstOrder = order("FIRST");
        long otherOrder = order("OTHER");
        long item = jdbc.queryForObject("insert into app.hm_order_item(order_id,sku_id,quantity,unit_price,"
                + "total_price,sku_code_snapshot,sku_name_snapshot,created_at) "
                + "values (?,?,1,100000,100000,'SKU','Món kiểm thử',current_timestamp) returning id",
                Long.class, firstOrder, sku);
        long request = request(firstOrder, "RETURN");
        rejects(() -> jdbc.update("insert into app.hm_return_item(return_request_id,order_id,order_item_id,"
                + "sku_id,requested_quantity,condition_status,refund_amount,created_at) "
                + "values (?,?,?,?,1,'UNINSPECTED',0,current_timestamp)", request, otherOrder, item, sku));
        long inspector = account();
        long line = jdbc.queryForObject("insert into app.hm_return_item(return_request_id,order_id,order_item_id,"
                + "sku_id,requested_quantity,approved_quantity,condition_status,refund_amount,inspected_by,"
                + "inspected_at,created_at) values (?,?,?,?,1,1,'NON_RESALABLE',100000,?,current_timestamp,"
                + "current_timestamp) returning id", Long.class, request, firstOrder, item, sku, inspector);
        rejects(() -> jdbc.update("update app.hm_return_item set restocked_quantity=1 where id=?", line));
    }

    @Test
    void refundMustUseItsOriginalOrderAndCompletedFinancialEvidenceIsImmutable() {
        long order = order("PAYMENT");
        long otherOrder = order("WRONG");
        long request = request(order, "REFUND");
        long payment = payment(order, "PAYMENT-REF");
        long wrongPayment = payment(otherOrder, "OTHER-PAYMENT-REF");
        rejects(() -> jdbc.update("insert into app.hm_refund(return_request_id,order_id,payment_id,refund_code,"
                + "amount,refund_method,provider,status,created_at) values (?,?,?,'WRONG',10000,'CASH','STORE',"
                + "'PENDING',current_timestamp)", request, order, wrongPayment));
        long actor = account();
        long refund = jdbc.queryForObject("insert into app.hm_refund(return_request_id,order_id,payment_id,"
                + "refund_code,amount,refund_method,provider,status,refunded_at,disbursed_by,payout_reference,"
                + "created_at) values (?,?,?,'REFUND-REF',10000,'CASH','STORE','COMPLETED',current_timestamp,?,"
                + "'PAYOUT-REF',current_timestamp) returning id", Long.class, request, order, payment, actor);
        rejects(() -> jdbc.update("update app.hm_refund set amount=20000 where id=?", refund));
        rejects(() -> jdbc.update("delete from app.hm_refund where id=?", refund));
        rejects(() -> jdbc.update("update app.hm_payment set amount=20000 where id=?", payment));
        rejects(() -> jdbc.update("insert into app.hm_payment(order_id,transaction_code,idempotency_key,amount,"
                + "payment_method,provider,status,created_at) values (?,'RETRY','PAYMENT-REF',100000,'CASH',"
                + "'STORE','PENDING',current_timestamp)", order));
    }

    private void rejects(Runnable operation) {
        jdbc.execute("SAVEPOINT rejected_operation");
        try {
            assertThatThrownBy(operation::run).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("ROLLBACK TO SAVEPOINT rejected_operation");
            jdbc.execute("RELEASE SAVEPOINT rejected_operation");
        }
    }

    private long account() {
        return jdbc.queryForObject("insert into app.user_account(email,full_name,role,status,created_at,updated_at) "
                + "values (?,'Người kiểm thử','CUSTOMER','PENDING_VERIFICATION',current_timestamp,"
                + "current_timestamp) returning user_id", Long.class,
                "schema-" + java.util.UUID.randomUUID() + "@example.invalid");
    }

    private long address(long user, boolean defaultAddress) {
        return jdbc.queryForObject("insert into app.hm_address(user_id,recipient_name,recipient_phone,province,"
                + "district,ward,street_address,is_default,created_at) values (?,'Người nhận','0000000000',"
                + "'Tỉnh','Quận','Phường','Địa chỉ',?,current_timestamp) returning id",
                Long.class, user, defaultAddress);
    }

    private long sku() {
        long category = jdbc.queryForObject("insert into app.hm_category(code,name,created_at) "
                + "values ('TEST-CAT','Danh mục kiểm thử',current_timestamp) returning id", Long.class);
        long brand = jdbc.queryForObject("insert into app.hm_brand(name,created_at) "
                + "values ('Nhãn hiệu kiểm thử',current_timestamp) returning id", Long.class);
        long product = jdbc.queryForObject("insert into app.product(name,status,category_id,brand_id,image_url,"
                + "created_at,updated_at) values ('Sản phẩm kiểm thử','ACTIVE',?,?,'/test-image.png',"
                + "current_timestamp,current_timestamp) returning product_id", Long.class, category, brand);
        return jdbc.queryForObject("insert into app.hm_sku(product_id,sku_code,barcode,sku_name,original_price,"
                + "selling_price,status,created_at) values (?,'TEST-SKU','TEST-BARCODE','SKU kiểm thử',100000,"
                + "100000,'ACTIVE',current_timestamp) returning id", Long.class, product);
    }

    private long order(String code) {
        return jdbc.queryForObject("insert into app.hm_order(order_code,order_type,status,payment_status,"
                + "total_amount,final_amount,created_at) values (?,'COUNTER','COMPLETED','PAID',100000,100000,"
                + "current_timestamp) returning id", Long.class, code);
    }

    private long request(long order, String code) {
        return jdbc.queryForObject("insert into app.hm_return_request(order_id,return_code,request_type,reason,"
                + "status,requested_by,created_at) values (?,?,'IN_STORE','Lý do kiểm thử','PENDING_INSPECTION',?,"
                + "current_timestamp) returning id", Long.class, order, code, account());
    }

    private long payment(long order, String reference) {
        return jdbc.queryForObject("insert into app.hm_payment(order_id,transaction_code,idempotency_key,amount,"
                + "payment_method,provider,status,payment_time,created_at) values (?,?,?,100000,'CASH','STORE',"
                + "'COMPLETED',current_timestamp,current_timestamp) returning id", Long.class, order, reference, reference);
    }
}
