package com.cafe.controller.shared;

import com.cafe.service.shared.DatabaseHealthService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Smoke test Phase 0: kiểm tra kết nối DB qua pool (SELECT 1). Public. */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    private final DatabaseHealthService healthService;

    public HealthServlet() {
        this(new DatabaseHealthService());
    }

    HealthServlet(DatabaseHealthService healthService) {
        this.healthService = Objects.requireNonNull(healthService, "healthService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        DatabaseHealthService.HealthStatus status = healthService.check();
        if (!status.up()) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().println("DOWN - " + status.message());
            return;
        }
        resp.getWriter().println("UP - " + status.message()
                + "; schema version=" + status.schemaVersion());
    }
}
