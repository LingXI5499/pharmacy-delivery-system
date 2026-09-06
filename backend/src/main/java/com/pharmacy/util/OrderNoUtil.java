
package com.pharmacy.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private OrderNoUtil() { }
    public static String next() {
        return "PD" + LocalDateTime.now().format(FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
