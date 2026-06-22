package com.mycoffee.service;

import com.mycoffee.dao.VoucherDAO;
import com.mycoffee.model.Voucher;
import java.util.List;

public class VoucherService {
    private final VoucherDAO voucherDAO = new VoucherDAO();

    public List<Voucher> getValidVouchersForOrder(int orderId) {
        return voucherDAO.getValidVouchersForOrder(orderId);
    }
}