package com.cloudkitchen.order_engine.dto;

/** What the frontend needs to open the Razorpay checkout for an order. */
public class PaymentCreateResponse {

    private final Long orderId;
    private final String keyId;
    private final String razorpayOrderId;
    private final long amountPaise;
    private final String currency;

    public PaymentCreateResponse(Long orderId, String keyId, String razorpayOrderId,
                                 long amountPaise, String currency) {
        this.orderId = orderId;
        this.keyId = keyId;
        this.razorpayOrderId = razorpayOrderId;
        this.amountPaise = amountPaise;
        this.currency = currency;
    }

    public Long getOrderId() { return orderId; }
    public String getKeyId() { return keyId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
}
