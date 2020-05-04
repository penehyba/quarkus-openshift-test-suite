package org.acme.getting.started;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetService {

    public String greeting(String name) {
        return "hello " + name;
    }

}
