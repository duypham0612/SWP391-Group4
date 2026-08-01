package com.cafe.service.shared;

import com.cafe.model.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/** Facade tương thích; các use case được tách sang service chuyên trách. */
public final class OrderService {
    private final OrderPlacementService placement;
    private final OrderQueryService query;
    private final KdsOrderWorkflowService kds;
    private final OrderIssueService issues;
    private final OrderHandoffService handoff;

    public OrderService() {
        OrderRepository repository = new OrderRepository();
        this.placement = new OrderPlacementService(repository);
        this.query = new OrderQueryService(repository);
        this.kds = new KdsOrderWorkflowService(repository);
        this.issues = new OrderIssueService(repository);
        this.handoff = new OrderHandoffService(repository);
    }
    public OrderService(OrderPlacementService placement, OrderQueryService query,
                        KdsOrderWorkflowService kds, OrderIssueService issues,
                        OrderHandoffService handoff) {
        this.placement = Objects.requireNonNull(placement);
        this.query = Objects.requireNonNull(query);
        this.kds = Objects.requireNonNull(kds);
        this.issues = Objects.requireNonNull(issues);
        this.handoff = Objects.requireNonNull(handoff);
    }

    public static class CartLine { public int productId; public int quantity; public String note; public List<Integer> optionIds = new ArrayList<>(); }
    public static class UnblockResult {
        private final boolean success; private final int remaining;
        public UnblockResult(boolean success, int remaining) { this.success=success; this.remaining=remaining; }
        public boolean isSuccess(){ return success; }
        public int getRemainingBlockedWithRecountedIngredients(){ return remaining; }
    }
    public static class BulkReadyResult {
        private final int completed; private final int skippedNoRecipe;
        public BulkReadyResult(int completed, int skippedNoRecipe){this.completed=completed;this.skippedNoRecipe=skippedNoRecipe;}
        public int getCompleted(){return completed;} public int getSkippedNoRecipe(){return skippedNoRecipe;}
    }

    public int placeOrder(int branchId,Integer tableId,String source,String orderType,Integer createdBy,List<CartLine> lines) throws SQLException {
        return placement.placeOrder(branchId,tableId,source,orderType,createdBy,lines);
    }
    public List<OrderItem> getKdsQueue(int b)throws SQLException{return query.getKdsQueue(b);} public List<OrderItem> getBaristaWorkbench(int b)throws SQLException{return query.getBaristaWorkbench(b);} public List<OrderItem> getBaristaWorkbench(int b,LocalDateTime d)throws SQLException{return query.getBaristaWorkbench(b,d);} public List<OrderItem> getStaleItems(int b,LocalDateTime d)throws SQLException{return query.getStaleItems(b,d);} public List<OrderItem> getRecentlyServed(int b,int m)throws SQLException{return query.getRecentlyServed(b,m);} public List<OrderItem> getPickedUpItems(int b)throws SQLException{return query.getPickedUpItems(b);} public List<OrderItem> getTableItemStatuses(int t)throws SQLException{return query.getTableItemStatuses(t);}
    public boolean startItem(int i,Integer u,int b)throws SQLException{return kds.startItem(i,u,b);} public boolean markItemReady(int i,Integer u,int b)throws SQLException{return kds.markItemReady(i,u,b);} public int startAllInOrder(int o,Integer u,int b)throws SQLException{return kds.startAllInOrder(o,u,b);} public BulkReadyResult markOrderReady(int o,Integer u,int b)throws SQLException{KdsOrderWorkflowService.BulkReadyResult r=kds.markOrderReady(o,u,b);return new BulkReadyResult(r.getCompleted(),r.getSkippedNoRecipe());} public int countMyMakingItems(int b,int u)throws SQLException{return kds.countMyMakingItems(b,u);} public boolean reclaimItem(int i,Integer u,int b,String n,Set<Integer> duty)throws SQLException{return kds.reclaimItem(i,u,b,n,duty);} public boolean returnItemToQueue(int i,Integer u,int b)throws SQLException{return kds.returnItemToQueue(i,u,b);}
    public boolean reportItemIssue(int i,String r,Integer u,int b)throws SQLException{return issues.reportItemIssue(i,r,u,b);} public boolean blockItem(int i,String r,Integer u,int b)throws SQLException{return issues.blockItem(i,r,u,b);} public boolean blockItemForDepletedIngredients(int i,List<Integer> ids,String r,Integer u,int b)throws SQLException{return issues.blockItemForDepletedIngredients(i,ids,r,u,b);} public boolean unblockItem(int i,Integer u,int b)throws SQLException{return issues.unblockItem(i,u,b);} public UnblockResult unblockItem(int i,List<StockAdjustment> rs,Integer u,int b)throws SQLException{OrderIssueService.UnblockResult x=issues.unblockItem(i,rs,u,b);return new UnblockResult(x.isSuccess(),x.getRemainingBlockedWithRecountedIngredients());} public boolean remakeItem(int i,String r,Integer u,int b)throws SQLException{return issues.remakeItem(i,r,u,b);} public String cancelItem(int i,String r,Integer u,int b)throws SQLException{return issues.cancelItem(i,r,u,b);} public boolean voidOrder(int o,Integer u,int b)throws SQLException{return issues.voidOrder(o,u,b);}
    public List<Recipe> getRecipeIngredients(int p)throws SQLException{return query.getRecipeIngredients(p);} public List<Recipe> getDepletedRecipeIngredients(int b,int p)throws SQLException{return query.getDepletedRecipeIngredients(b,p);}
    public boolean markItemPickedUp(int i,Integer u,int b)throws SQLException{return handoff.markItemPickedUp(i,u,b);} public boolean markItemServed(int i,Integer u,int b)throws SQLException{return handoff.markItemServed(i,u,b);} public int pickUpAllReady(int o,Integer u,int b)throws SQLException{return handoff.pickUpAllReady(o,u,b);} public int serveAllReady(int o,Integer u,int b)throws SQLException{return handoff.serveAllReady(o,u,b);} public int serveAllPickedUp(List<Integer> os,String t,Integer u,int b)throws SQLException{return handoff.serveAllPickedUp(os,t,u,b);} public boolean unserveItem(int i,Integer u,int b)throws SQLException{return handoff.unserveItem(i,u,b);}
    public List<PickupTicket> getPickupTickets(int b)throws SQLException{return query.getPickupTickets(b);} public Order getOrder(int o)throws SQLException{return query.getOrder(o);} public List<Order> getIncomingOrders(int b)throws SQLException{return query.getIncomingOrders(b);} public List<Order> getTableOrders(int t)throws SQLException{return query.getTableOrders(t);}
}
