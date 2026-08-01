# Bộ công cụ quan sát database bằng SSMS

Thư mục này nhóm 25 bảng nghiệp vụ của CafeChain theo luồng sử dụng thay vì theo
thứ tự tên bảng. Các file chỉ là công cụ dành cho SSMS, không thuộc Flyway và
không làm thay đổi schema mà ứng dụng đang sử dụng.

## Cách dùng

1. Mở SSMS và kết nối SQL Server của dự án.
2. Chọn đúng database, mặc định local là `CafeChain_v2`.
3. Mở file cần xem và sửa các biến lọc ở đầu file như `@BranchId`, `@FromDate`,
   `@ToDate`.
4. Chạy từng result set hoặc chạy toàn bộ file.
5. Với `90_safe_operations.sql`, để `@ApplyChanges = 0` khi thử. Script sẽ
   `ROLLBACK`; chỉ đổi thành `1` khi đã kiểm tra kết quả preview.

Không chạy các file này trong database `master`. Mọi timestamp nghiệp vụ lưu UTC;
các cột có hậu tố `Local` trong bộ truy vấn đã được đổi sang giờ Việt Nam (UTC+7).

## Các cụm thông tin

| File | Cụm nghiệp vụ | Bảng chính |
|---|---|---|
| `00_database_map.sql` | Bản đồ tổng thể | Toàn bộ schema, bảng, FK, trigger, row count |
| `01_people_shifts.sql` | Chi nhánh, tài khoản, ca làm | `org.Branch`, `iam.UserAccount`, `hr.ShiftAssignment` |
| `02_catalog_menu.sql` | Sản phẩm, công thức, menu | Toàn bộ schema `catalog` |
| `03_inventory.sql` | Tồn kho, nhập, prep, hao hụt | Toàn bộ schema `inventory` |
| `04_sales_kds.sql` | Bàn, đơn hàng, KDS | Toàn bộ schema `sales` |
| `05_payment.sql` | Két thu ngân, hóa đơn | Toàn bộ schema `payment` |
| `06_operations.sql` | Audit và outbox | `ops.ActivityLog`, `ops.OutboxEvent` |
| `90_safe_operations.sql` | Mẫu thay đổi có bảo vệ | Menu, tài khoản, xếp ca |
| `99_integrity_check.sql` | Kiểm tra toàn vẹn | FK, CHECK, dữ liệu lệch nghiệp vụ |

## Gợi ý tạo Database Diagram trong SSMS

Nếu cần kéo-thả sơ đồ, tạo các diagram riêng theo đúng nhóm dưới đây:

- `01_People_Shifts`: `org.Branch`, `iam.UserAccount`, `hr.ShiftAssignment`.
- `02_Catalog_Menu`: 7 bảng thuộc schema `catalog`.
- `03_Inventory`: 7 bảng `inventory` cùng `catalog.Ingredient`, `org.Branch`,
  `iam.UserAccount`.
- `04_Sales_Payment`: 4 bảng `sales`, 2 bảng `payment`, cộng
  `catalog.Product`, `org.Branch`, `iam.UserAccount`.
- `05_Operations`: 2 bảng `ops`, cộng `org.Branch`, `iam.UserAccount`.

Schema `dbo` không chứa bảng nghiệp vụ. Bảng `dbo.sysdiagrams` là bảng kỹ thuật
do tính năng Database Diagrams của SSMS tạo ra và không tính vào 25 bảng của hệ thống.

## Quy tắc thao tác quan trọng

- Không sửa trực tiếp `inventory.BranchInventory.QuantityOnHand`; mọi biến động tồn
  phải có dòng đối ứng trong `inventory.InventoryTransaction` và nên đi qua ứng dụng.
- Không sửa snapshot tên/giá ở `sales.OrderItem` và
  `sales.OrderItemModifier` sau khi đã tạo đơn.
- Không sửa hóa đơn `PAID` hay ca thu ngân đã đóng bằng SQL thủ công.
- Không thay `PasswordHash` bằng mật khẩu thuần.
- Luôn giới hạn thao tác vận hành theo `BranchId`.

