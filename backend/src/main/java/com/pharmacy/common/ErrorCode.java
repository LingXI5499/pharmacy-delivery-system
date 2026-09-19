
package com.pharmacy.common;

public final class ErrorCode {
    private ErrorCode() {}
    public static final int PARAM_INVALID = 40001;
    public static final int USERNAME_EXISTS = 40002;
    public static final int PASSWORD_ERROR = 40003;
    public static final int ACCOUNT_DISABLED = 40004;
    public static final int UNAUTHORIZED = 40101;
    public static final int FORBIDDEN = 40301;
    public static final int NOT_FOUND = 40401;
    public static final int STOCK_OR_STATUS_CONFLICT = 40901;
    public static final int ORDER_STATUS_CONFLICT = 40902;
    public static final int CATEGORY_NOT_EMPTY = 40903;
    public static final int ADDRESS_NOT_OWNED = 40904;
    public static final int RATE_LIMITED = 42901;
    public static final int SYSTEM_ERROR = 50000;
}
