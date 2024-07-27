package com.crio.warmup.stock.exception;


public class AlphaVantageRateLimitException extends StockQuoteServiceException {

    public AlphaVantageRateLimitException(String message) {
        super(message);
    }

    public AlphaVantageRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
