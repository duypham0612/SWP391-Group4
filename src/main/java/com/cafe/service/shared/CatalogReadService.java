package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierOption;
import com.cafe.model.PosMenuItem;
import com.cafe.model.ProductModifierGroup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc menu để dựng màn POS / QR (chỉ món available, chưa 86). */
public class CatalogReadService {

    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();
    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();

    /** Menu bán được của chi nhánh: published + available + chưa 86, kèm nhóm modifier. */
    public List<PosMenuItem> getPosMenu(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<PosMenuItem> out = new ArrayList<>();
            for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                if (!bm.isPublished() || !bm.isAvailable() || bm.isIs86()) continue;
                PosMenuItem item = new PosMenuItem();
                item.setProductId(bm.getProductId());
                item.setName(bm.getProductName());
                item.setPrice(bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());

                for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, bm.getProductId())) {
                    ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                    if (g == null) continue;
                    PosMenuItem.Group grp = new PosMenuItem.Group();
                    grp.setGroupId(g.getModifierGroupId());
                    grp.setName(g.getName());
                    grp.setRequired(g.isRequired());
                    grp.setMinSelect(g.getMinSelect());
                    grp.setMaxSelect(g.getMaxSelect());
                    for (ModifierOption o : optionDao.findByGroup(conn, g.getModifierGroupId())) {
                        if (o.isActive()) grp.getOptions().add(o);
                    }
                    if (!grp.getOptions().isEmpty()) item.getGroups().add(grp);
                }
                out.add(item);
            }
            return out;
        }
    }
}
