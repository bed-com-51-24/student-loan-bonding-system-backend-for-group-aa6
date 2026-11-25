package mw.loancompliance.common;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class BusinessEventPublisher {
    private final ApplicationEventPublisher publisher;

    public BusinessEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(BusinessEvent event) {
        publisher.publishEvent(event);
    }
}
