package com.pharmacy.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

public final class MybatisPlusLambdaInit {
    private MybatisPlusLambdaInit() {
    }

    public static void entities(Class<?>... types) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> type : types) {
            TableInfoHelper.initTableInfo(assistant, type);
        }
    }
}
