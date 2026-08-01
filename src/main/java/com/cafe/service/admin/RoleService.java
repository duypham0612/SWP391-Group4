package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.RoleDao;
import com.cafe.model.Role;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class RoleService {

    private final RoleDao dao;

    public RoleService() { this(new RoleDao()); }
    public RoleService(RoleDao dao) { this.dao = java.util.Objects.requireNonNull(dao); }

    public List<Role> getRoleList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }
}
