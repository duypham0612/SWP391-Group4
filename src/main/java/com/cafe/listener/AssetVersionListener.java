package com.cafe.listener;

import com.cafe.web.viewmodel.ViewFormatter;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Gắn một phiên bản cho CSS/JS tĩnh, đổi sau mỗi lần khởi động lại app.
 *
 * <p>Tomcat chỉ trả ETag/Last-Modified cho file tĩnh, không có Cache-Control, nên trình duyệt
 * được phép dùng bản cache cũ mà không hỏi lại server. Hậu quả: redeploy xong giao diện vẫn ăn
 * CSS cũ, phải Cmd+Shift+R mới thấy — rất dễ tưởng nhầm là code chưa vào.
 * Thêm {@code ?v=<mốc khởi động>} khiến URL đổi sau mỗi lần deploy, trình duyệt buộc tải lại.
 */
public class AssetVersionListener implements ServletContextListener {

    private final ViewFormatter viewFormatter;

    public AssetVersionListener() {
        this(new ViewFormatter());
    }

    AssetVersionListener(ViewFormatter viewFormatter) {
        this.viewFormatter = java.util.Objects.requireNonNull(viewFormatter, "viewFormatter");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("assetVersion", String.valueOf(System.currentTimeMillis()));
        sce.getServletContext().setAttribute("view", viewFormatter);
    }
}
