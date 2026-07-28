# Five main sequence diagrams

Each diagram follows the implemented path: `View -> Controller -> Service -> DAO/JDBC -> SQL Server`.

## 1. Open a table

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Thu ngan
    participant View as table-map.jsp
    participant Controller as TableServlet
    participant Service as TableSessionService
    participant DAO as TableSessionDao + DiningTableDao
    participant Outbox as OutboxEventDao
    participant DB as SQL Server

    Cashier->>View: Chon ban va bam Mo ban
    View->>Controller: POST /cashier/table (action=openTable, tableId)
    Controller->>Service: openSession(branchId, tableId, cashierId)
    Note over Service,DB: One JDBC transaction

    Service->>DAO: findOpenByTable(tableId)
    DAO->>DB: SELECT sales.TableSession
    DB-->>DAO: OPEN session or null
    DAO-->>Service: existing session?

    alt Chua co phien OPEN
        Service->>DAO: insertOpen(branchId, tableId, cashierId)
        DAO->>DB: INSERT sales.TableSession (OPEN)
        DB-->>DAO: sessionId
        DAO-->>Service: sessionId
        Service->>DAO: updateStatus(tableId, OCCUPIED)
        DAO->>DB: UPDATE sales.DiningTable
    else Da co phien OPEN
        Note right of Service: Idempotent: dung lai session hien co
    end

    Service->>Outbox: markOpenRequestsProcessed(tableId)
    Outbox->>DB: UPDATE ops.OutboxEvent
    Note over Service,DB: COMMIT; SQLException thi ROLLBACK
    Service-->>Controller: sessionId
    Controller-->>View: Redirect POS hoac hien QR
    View-->>Cashier: Ban OCCUPIED
```

## 2. Place an order from POS or QR

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Thu ngan
    actor Customer as Khach hang
    participant PosView as pos.jsp
    participant QrView as customer/menu.jsp
    participant PosController as PosServlet
    participant QrController as QrMenuServlet
    participant QrService as QrOrderService
    participant OrderService
    participant TableDAO as TableSessionDao
    participant SalesDAO as Menu + Order + Item + Modifier DAOs
    participant Outbox as EventPublisher (JDBC gateway)
    participant DB as SQL Server

    alt Don tai quay
        Cashier->>PosView: Tao gio hang va submit
        PosView->>PosController: POST /cashier/pos (JSON cart)
        PosController->>OrderService: placeOrder(..., COUNTER, ..., userId, lines)
    else Don tu QR tren ban
        Customer->>QrView: Tao gio hang va submit
        QrView->>QrController: POST /qr/menu (JSON cart)
        QrController->>QrService: isSessionOrderable(sessionId)
        QrService->>TableDAO: findById(sessionId)
        TableDAO->>DB: SELECT sales.TableSession
        DB-->>TableDAO: OPEN session
        TableDAO-->>QrService: TableSession
        QrController->>QrService: placeCustomerOrder(branchId, sessionId, lines)
        QrService->>OrderService: placeOrder(..., QR, ..., null, lines)
    end

    Note over OrderService,DB: One JDBC transaction in OrderService.placeOrder
    OrderService->>SalesDAO: Load branch menu, 86/depleted state, modifiers
    SalesDAO->>DB: SELECT catalog.BranchMenu, ProductRecipe, Modifier*
    DB-->>SalesDAO: Server-side price and availability
    SalesDAO-->>OrderService: Validated catalog data

    alt Gio hang hop le
        OrderService->>SalesDAO: insert Order(ACTIVE) and pickupCode
        SalesDAO->>DB: INSERT/UPDATE sales.Orders
        DB-->>SalesDAO: orderId
        loop Moi cart line
            OrderService->>SalesDAO: insert OrderItem(WAITING)
            SalesDAO->>DB: INSERT sales.OrderItem
            opt Co modifier
                OrderService->>SalesDAO: insert OrderItemModifier
                SalesDAO->>DB: INSERT sales.OrderItemModifier
            end
        end
        OrderService->>Outbox: publish(order.created)
        Outbox->>DB: INSERT ops.OutboxEvent
        Note over OrderService,DB: COMMIT Order + Items + Modifiers + Event
    else Mon unpublished, stopped, 86, depleted, hoac option sai
        Note over OrderService,DB: ROLLBACK va tra validation error; khong tao don
    end

    alt COUNTER
        OrderService-->>PosController: orderId
        PosController-->>PosView: 200 JSON {orderId}
        PosView-->>Cashier: Don da tao
    else QR
        OrderService-->>QrService: orderId
        QrService-->>QrController: orderId
        QrController-->>QrView: 200 JSON {orderId}
        QrView-->>Customer: Hien trang thai don
    end
```

## 3. Barista prepares an item and deducts inventory

```mermaid
sequenceDiagram
    autonumber
    actor Barista
    participant View as barista/kds.jsp
    participant Controller as KdsServlet
    participant KdsService
    participant OrderService
    participant OrderDAO as OrderItemDao + OrderItemActionDao
    participant InventoryService
    participant InventoryDAO as Recipe + Ledger + Stock DAOs
    participant Outbox as EventPublisher (JDBC gateway)
    participant DB as SQL Server

    Barista->>View: Bam Nhan pha
    View->>Controller: POST /barista/kds (action=start)
    Controller->>KdsService: startItem(itemId, userId, branchId)
    KdsService->>OrderService: startItem(...)
    Note over OrderService,DB: One JDBC transaction
    OrderService->>OrderDAO: claim(itemId, branchId, baristaId)
    OrderDAO->>DB: Conditional UPDATE WAITING -> MAKING
    DB-->>OrderDAO: affectedRows
    alt affectedRows = 1
        OrderService->>OrderDAO: insert action log CLAIM
        OrderDAO->>DB: INSERT ops.OrderItemActionLog
        OrderService->>Outbox: publish status MAKING
        Outbox->>DB: INSERT ops.OutboxEvent
        Note over OrderService,DB: COMMIT
    else Mon da bi thay doi / khac chi nhanh
        Note over OrderService,DB: ROLLBACK; tra false
    end
    OrderService-->>KdsService: success
    KdsService-->>Controller: success
    Controller-->>View: Refresh KDS fragment

    Barista->>View: Bam Xong
    View->>Controller: POST /barista/kds (action=markReady)
    Controller->>KdsService: markReady(itemId, userId, branchId, location)
    KdsService->>OrderService: markItemReady(...)
    Note over OrderService,DB: One JDBC transaction
    OrderService->>OrderDAO: completeClaimed(itemId, branchId, userId)
    OrderDAO->>DB: Conditional UPDATE MAKING -> READY
    DB-->>OrderDAO: affectedRows

    alt Claim hop le va mon co cong thuc
        OrderService->>InventoryService: deductForOrderItem(connection, item)
        InventoryService->>InventoryDAO: Read recipe and modifier impacts
        InventoryDAO->>DB: SELECT ProductRecipe, OrderItemModifier, IngredientImpact
        DB-->>InventoryDAO: Required ingredients
        loop Moi nguyen lieu can tru
            InventoryService->>InventoryDAO: insert ledger(DEDUCT) + applyDelta
            InventoryDAO->>DB: INSERT InventoryTransaction; UPDATE BranchInventory
        end
        InventoryService->>Outbox: publish inventory.deducted / stock alert
        Outbox->>DB: INSERT ops.OutboxEvent
        OrderService->>OrderDAO: insert action log COMPLETE
        OrderDAO->>DB: INSERT ops.OrderItemActionLog
        OrderService->>Outbox: publish item.ready
        Outbox->>DB: INSERT ops.OutboxEvent
        Note over OrderService,DB: COMMIT READY + inventory + audit + events
    else Conflict hoac thieu cong thuc
        Note over OrderService,DB: ROLLBACK; khong de lai READY hoac tru kho mot phan
    end
    OrderService-->>KdsService: success
    KdsService-->>Controller: success
    Controller-->>View: Refresh KDS fragment
    View-->>Barista: Mon READY
```

## 4. Receive and serve a prepared item

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Thu ngan
    participant View as cashier/handoff/*
    participant Controller as PickupServlet
    participant PickupService
    participant OrderService
    participant OrderDAO as OrderItemDao + OrderDao + ActionDao
    participant Outbox as EventPublisher (JDBC gateway)
    participant DB as SQL Server

    Cashier->>View: Bam Da nhan mon
    View->>Controller: POST /cashier/handoff (action=pickUp)
    Controller->>PickupService: pickUpItem(itemId, userId, branchId)
    PickupService->>OrderService: markItemPickedUp(...)
    Note over OrderService,DB: One JDBC transaction
    OrderService->>OrderDAO: pickUp(itemId) + insert PICK_UP action
    OrderDAO->>DB: Conditional UPDATE READY -> PICKED_UP; INSERT action log
    DB-->>OrderDAO: success or conflict
    OrderService->>Outbox: publish(item.picked_up)
    Outbox->>DB: INSERT ops.OutboxEvent
    Note over OrderService,DB: COMMIT
    OrderService-->>PickupService: success
    PickupService-->>Controller: success
    Controller-->>View: Refresh handoff fragment

    Cashier->>View: Bam Da giao khach
    View->>Controller: POST /cashier/handoff (action=serve)
    Controller->>PickupService: serveItem(itemId, userId, branchId)
    PickupService->>OrderService: markItemServed(...)
    Note over OrderService,DB: One JDBC transaction
    OrderService->>OrderDAO: updateStatusIf(PICKED_UP -> SERVED)
    OrderDAO->>DB: UPDATE sales.OrderItem
    OrderService->>OrderDAO: completeIfAllItemsFinal(orderId)
    OrderDAO->>DB: UPDATE sales.Orders -> COMPLETED if all items final
    OrderService->>Outbox: publish status event
    Outbox->>DB: INSERT ops.OutboxEvent
    Note over OrderService,DB: COMMIT
    OrderService-->>PickupService: success
    PickupService-->>Controller: success
    Controller-->>View: Refresh handoff fragment
    View-->>Cashier: Mon SERVED; don COMPLETED neu da giao het
```

## 5. Build bill, pay, and release the table

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as Thu ngan
    participant View as checkout.jsp
    participant Controller as CheckoutServlet
    participant BillingService
    participant BillingDAO as BillDao + BillItemDao + VoucherDao
    participant TableDAO as TableSessionDao + DiningTableDao
    participant Outbox as OutboxEventDao + EventPublisher
    participant DB as SQL Server

    Cashier->>View: Mo checkout cho session
    View->>Controller: GET /cashier/checkout?sessionId
    Controller->>BillingService: buildSessionBill(sessionId, branchId, shiftId)
    Note over BillingService,DB: One JDBC transaction
    BillingService->>BillingDAO: find unpaid bills and unbilled items
    BillingDAO->>DB: SELECT payment.Bill, BillItem, sales.OrderItem
    DB-->>BillingDAO: Bill/item data
    opt Chua co default UNPAID bill
        BillingService->>BillingDAO: insert Bill(UNPAID)
        BillingDAO->>DB: INSERT payment.Bill
    end
    loop Moi OrderItem khong CANCELLED chua duoc bill
        BillingService->>BillingDAO: insert BillItem
        BillingDAO->>DB: INSERT payment.BillItem
    end
    BillingService->>BillingDAO: recompute subtotal, discount, VAT, total
    BillingDAO->>DB: UPDATE payment.Bill amounts
    Note over BillingService,DB: COMMIT
    BillingService-->>Controller: bills
    Controller-->>View: Forward bill data

    Cashier->>View: Xac nhan phuong thuc thanh toan
    View->>Controller: POST /cashier/checkout (action=pay)
    Controller->>BillingService: validate bill and payBill(billId, method)
    Note over BillingService,DB: One JDBC transaction
    BillingService->>BillingDAO: find bill, item, voucher and validate
    BillingDAO->>DB: SELECT payment.Bill, BillItem, Voucher
    BillingService->>BillingDAO: markPaid(billId, method)
    BillingDAO->>DB: Conditional UPDATE UNPAID -> PAID
    DB-->>BillingDAO: affectedRows

    alt Payment accepted
        opt Voucher applied
            BillingService->>BillingDAO: increment usage + insert redemption
            BillingDAO->>DB: UPDATE Voucher; INSERT VoucherRedemption
        end
        BillingService->>Outbox: publish(payment.completed)
        Outbox->>DB: INSERT ops.OutboxEvent
        BillingService->>BillingDAO: findUnpaidBySession(sessionId)
        BillingDAO->>DB: SELECT remaining UNPAID bills
        alt Khong con bill UNPAID
            BillingService->>TableDAO: close session and release table
            TableDAO->>DB: UPDATE TableSession -> CLOSED; DiningTable -> EMPTY
            BillingService->>Outbox: markBillRequestProcessed(sessionId)
            Outbox->>DB: UPDATE ops.OutboxEvent
        else Van con bill tach chua thanh toan
            Note right of BillingService: Keep session OPEN
        end
        Note over BillingService,DB: COMMIT payment, event, and table close
    else Invalid, PAID, VOID, or double-pay
        Note over BillingService,DB: ROLLBACK; return false
    end

    BillingService-->>Controller: payment result
    Controller-->>View: Redirect to table map if session CLOSED
    View-->>Cashier: Ban EMPTY when last bill is paid
```
