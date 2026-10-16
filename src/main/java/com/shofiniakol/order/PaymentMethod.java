package com.shofiniakol.order;

public enum PaymentMethod {
    /** "دفع عند الاستلام" */
    CASH_ON_DELIVERY,
    /** "شام كاش" - customer transfers, then enters the transaction number; staff verifies it. */
    SHAM_CASH
}
