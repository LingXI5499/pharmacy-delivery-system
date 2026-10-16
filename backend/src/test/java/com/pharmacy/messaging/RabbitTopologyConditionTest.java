package com.pharmacy.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitTopologyConditionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RabbitTopologyConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void disabledMessagingDoesNotDeclareTopology() {
        runner.withPropertyValues("app.messaging.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RabbitTopologyConfig.class);
            assertThat(context).doesNotHaveBean(Queue.class);
        });
    }

    @Test
    void enabledMessagingDeclaresBothQueues() {
        runner.withPropertyValues("app.messaging.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(RabbitTopologyConfig.class);
            assertThat(context.getBeansOfType(Queue.class)).hasSize(2);
        });
    }
}
