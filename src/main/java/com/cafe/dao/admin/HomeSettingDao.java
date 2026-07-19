package com.cafe.dao.admin;

import com.cafe.model.HomeSetting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** catalog.HomeSetting — 1 dòng duy nhất (Id=1) chứa nội dung hero trang Home. */
public class HomeSettingDao {

    /** Lấy cấu hình hero (Id=1); null nếu chưa có dòng nào. */
    public HomeSetting find(Connection conn) throws SQLException {
        final String sql = "SELECT HeroEyebrow, HeroTitle, HeroSubtitle, HeroImageUrl " +
                           "FROM catalog.HomeSetting WHERE Id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            HomeSetting s = new HomeSetting();
            s.setHeroEyebrow(rs.getString("HeroEyebrow"));
            s.setHeroTitle(rs.getString("HeroTitle"));
            s.setHeroSubtitle(rs.getString("HeroSubtitle"));
            s.setHeroImageUrl(rs.getString("HeroImageUrl"));
            return s;
        }
    }

    /** Ghi nội dung hero (Id=1). Tự INSERT nếu dòng singleton chưa tồn tại. */
    public void update(Connection conn, HomeSetting s) throws SQLException {
        final String upd = "UPDATE catalog.HomeSetting SET HeroEyebrow=?, HeroTitle=?, HeroSubtitle=?, " +
                          "HeroImageUrl=?, UpdatedAt=SYSUTCDATETIME() WHERE Id = 1";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setString(1, s.getHeroEyebrow());
            ps.setString(2, s.getHeroTitle());
            ps.setString(3, s.getHeroSubtitle());
            ps.setString(4, s.getHeroImageUrl());
            if (ps.executeUpdate() > 0) return;
        }
        final String ins = "INSERT INTO catalog.HomeSetting(Id, HeroEyebrow, HeroTitle, HeroSubtitle, HeroImageUrl) " +
                          "VALUES (1, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setString(1, s.getHeroEyebrow());
            ps.setString(2, s.getHeroTitle());
            ps.setString(3, s.getHeroSubtitle());
            ps.setString(4, s.getHeroImageUrl());
            ps.executeUpdate();
        }
    }
}
