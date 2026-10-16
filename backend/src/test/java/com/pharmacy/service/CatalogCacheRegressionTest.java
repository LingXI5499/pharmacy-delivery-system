package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.entity.Medicine;
import com.pharmacy.mapper.MedicineCategoryMapper;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.service.impl.PublicMedicineServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCacheRegressionTest {
    @Test
    void differentKeywordsWithSameHashDoNotReuseCachedPage() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        try (var context = new AnnotationConfigApplicationContext(Caching.class)) {
            MedicineMapper mapper = context.getBean(MedicineMapper.class);
            when(mapper.selectPage(any(), any())).thenAnswer(invocation -> {
                Page<Medicine> page = invocation.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });
            PublicMedicineService service = context.getBean(PublicMedicineService.class);
            service.pageMedicines(1, 10, "Aa", null, "default");
            service.pageMedicines(1, 10, "BB", null, "default");
            service.pageMedicines(1, 10, "Aa", null, "default");
            verify(mapper, times(2)).selectPage(any(), any());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class Caching {
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager("catalogPages"); }
        @Bean MedicineMapper medicineMapper() { return mock(MedicineMapper.class); }
        @Bean MedicineCategoryMapper categoryMapper() { return mock(MedicineCategoryMapper.class); }
        @Bean PublicMedicineService service(MedicineMapper medicines, MedicineCategoryMapper categories) {
            return new PublicMedicineServiceImpl(medicines, categories);
        }
    }
}
