package com.cafe.listener;

import com.cafe.service.shared.StandardModifierService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Objects;

/** Repairs fixed Size, Sugar and Ice rows for products inserted outside the Admin workflow. */
public final class StandardModifierListener implements ServletContextListener {
    private final StandardModifierService service;

    public StandardModifierListener() {
        this(new StandardModifierService());
    }

    StandardModifierListener(StandardModifierService service) {
        this.service = Objects.requireNonNull(service);
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            int productCount = service.synchronizeAllProducts();
            event.getServletContext().log("[Catalog] Fixed Size/Sugar/Ice choices synchronized for "
                    + productCount + " products.");
        } catch (Exception error) {
            throw new IllegalStateException("Cannot synchronize fixed product choices.", error);
        }
    }
}
