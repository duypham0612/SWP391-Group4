# Phan tich sequence diagram - flow chinh

## 1. Kien truc thuc te

Du an la ung dung Java Web dong goi WAR, dung Jakarta Servlet/JSP va JDBC voi SQL Server.
Moi request nghiep vu di theo chuoi chinh:

`JSP/JavaScript -> Servlet Controller -> Service -> DAO/JDBC gateway -> SQL Server`

- **View**: JSP trong `src/main/webapp/WEB-INF/views` va JavaScript gui form/fetch.
- **Controller**: Servlet khai bao bang `@WebServlet`, doc request, CSRF/session/branch scope,
  goi service va forward/redirect.
- **Service**: chua nghiep vu, validation, trang thai va transaction JDBC.
- **DAO**: nhan `Connection` tu service, thuc thi SQL theo schema.
- **Database**: SQL Server, tach schema `sales`, `catalog`, `inventory`, `payment`, `ops`.

Chuoi filter `Charset -> Auth -> RBAC -> BranchScope -> CashierDutyGuard` chay truoc
controller. Diagram khong mo rong filter de giu dung trong tam ma nguoi dung yeu cau:
`View -> Controller -> Service -> DAO -> Database`.

## 2. Flow chinh duoc chon

Flow dai dien tot nhat cho he thong la vong doi mot don dine-in:

1. Thu ngan mo ban, tao `sales.TableSession` va doi ban sang `OCCUPIED`.
2. Don duoc tao tu POS hoac tu QR. Ca hai nhanh deu hoi tu vao
   `OrderService.placeOrder(...)`.
3. Moi `OrderItem` bat dau o `WAITING`; barista nhan pha de chuyen sang `MAKING`.
4. Khi barista bam xong, item chuyen `MAKING -> READY`. Cung transaction nay,
   he thong doc cong thuc/modifier, ghi ledger va tru ton kho.
5. Thu ngan nhan mon tai quay (`READY -> PICKED_UP`) va giao khach
   (`PICKED_UP -> SERVED`). Khi tat ca item ket thuc, don thanh `COMPLETED`.
6. Checkout tao/dong bo bill, thu tien, doi bill sang `PAID`. Neu khong con bill
   `UNPAID`, phien ban thanh `CLOSED` va ban tro ve `EMPTY`.

Day la flow chinh vi no noi cac bounded area quan trong nhat cua du an:
`sales -> inventory -> payment`, dong thoi di qua day du View, Controller,
Service, DAO va Database.

## 3. Doi chieu voi code

| Buoc | View | Controller | Service chinh | Persistence |
|---|---|---|---|---|
| Mo ban | `cashier/table-map.jsp` | `TableServlet` | `TableSessionService.openSession` | `TableSessionDao`, `DiningTableDao`, `OutboxEventDao` |
| Dat mon POS | `cashier/pos.jsp` | `PosServlet` | `OrderService.placeOrder` | `BranchMenuDao`, `OrderDao`, `OrderItemDao`, cac modifier DAO |
| Dat mon QR | `customer/menu.jsp` | `QrMenuServlet` | `QrOrderService.placeCustomerOrder -> OrderService.placeOrder` | Cung cac DAO cua don POS |
| Pha che | `barista/kds.jsp` va fragment | `KdsServlet` | `KdsService -> OrderService` | `OrderItemDao`, `OrderItemActionDao` |
| Tru kho | Cung thao tac "Xong" tren KDS | `KdsServlet` | `OrderService -> InventoryService.deductForOrderItem` | Recipe/modifier DAO, `InventoryTransactionDao`, `BranchInventoryDao` |
| Ban giao | `cashier/handoff/*` | `PickupServlet` | `PickupService -> OrderService` | `OrderItemDao`, `OrderDao`, `OrderItemActionDao` |
| Thanh toan | `cashier/checkout.jsp` | `CheckoutServlet` | `BillingService` | `BillDao`, `BillItemDao`, voucher DAO, table/session DAO |

## 4. Cac diem bat bien the hien trong diagram

- Don POS va don QR khong co hai pipeline tach biet; ca hai dung chung bang
  `sales.Orders`, `sales.OrderItem` va `OrderService`.
- Gia duoc tinh lai server-side tu menu chi nhanh va modifier; client khong quyet
  dinh gia cuoi.
- `OrderItemDao` dung conditional update de tranh hai barista cung nhan/xong mot mon.
- Ton kho chi bi tru tai transition `MAKING -> READY`, khong tru luc dat mon.
- Ghi ledger `inventory.InventoryTransaction` va cap nhat
  `inventory.BranchInventory` nam trong cung transaction voi transition `READY`.
- Thanh toan dung conditional update de chong double-pay.
- Event domain duoc ghi vao `ops.OutboxEvent` trong cung transaction nghiep vu.

`EventPublisher` trong code la mot JDBC persistence gateway goi SQL truc tiep,
khong phai class DAO. File PlantUML dat no trong nhom Persistence va ghi ro
`JDBC gateway` de diagram trung thuc voi implementation hien tai.

## 5. Pham vi khong mo rong

Diagram khong ve chi tiet login, attendance, manager/admin CRUD, prep batch, waste,
86/depleted handling, split/merge bill, refund va cac nhanh sua loi KDS. Chung la
flow phu hoac exception flow, neu dua het vao mot sequence se che mat vong doi don
hang chinh.
