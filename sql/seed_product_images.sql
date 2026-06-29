/* ============================================================================
   Thay ảnh sản phẩm: SVG cục bộ -> ảnh thật từ Unsplash (đã verify tải được + đúng đồ uống).
   Idempotent (UPDATE theo Name). Param ?w=700&q=80&auto=format&fit=crop để tối ưu kích thước.
   Muốn đổi ảnh khác: vào Admin > Sản phẩm, dán URL vào ô Ảnh (có preview).
   ============================================================================ */
USE CafeChain;
GO

UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Cà phê sữa';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Cold Brew';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Cà phê đen';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Bạc xỉu';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Cappuccino';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1541167760496-1628856ab772?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Latte';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Trà Đào';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1597318181409-cf64d0b5d8a2?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Trà sen vàng';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1499638673689-79a0b5115d87?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Trà vải';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Trà gừng mật ong';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Đá xay Cookie';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Matcha đá xay';
UPDATE catalog.Product SET ImageUrl = N'https://images.unsplash.com/photo-1619158401201-8fa932695178?w=700&q=80&auto=format&fit=crop' WHERE Name = N'Chocolate đá xay';
GO

SELECT CONCAT(Name, '  ->  ', ImageUrl) FROM catalog.Product ORDER BY CategoryId, Name;
GO
