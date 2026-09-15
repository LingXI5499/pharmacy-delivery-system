
package com.pharmacy.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PageData<T>(List<T> records, long current, long size, long total, long pages) implements Serializable {
    public static <S, T> PageData<T> from(IPage<S> page, Function<S, T> converter) {
        return new PageData<>(
                page.getRecords().stream().map(converter).collect(Collectors.toList()),
                page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }
}
