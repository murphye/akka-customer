package lightbend.customer.persistence;

import akka.persistence.journal.Tagged;
import akka.persistence.journal.WriteEventAdapter;

import java.util.HashSet;
import java.util.Set;

public class CustomerTaggingEventAdapter implements WriteEventAdapter {

    @Override
    public Object toJournal(Object event) {
        if (event instanceof CustomerEvent) {
            Set<String> tags = new HashSet<String>();
            tags.add(CustomerEvent.TAG);
            return new Tagged(event, tags);
        }
        else {
            return event;
        }
    }

    @Override
    public String manifest(Object event) {
        return "";
    }
}
