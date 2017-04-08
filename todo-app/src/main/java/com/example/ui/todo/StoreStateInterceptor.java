package com.example.ui.todo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class StoreStateInterceptor {

    private final Logger log = LoggerFactory.getLogger(StoreStateInterceptor.class);

    @AroundInvoke
    private Object logCurrentStoreState(InvocationContext ic) throws Exception {
        Object returnVal = ic.proceed();

        if (ic.getTarget() instanceof Store) {
            Store store = (Store) ic.getTarget();
            StringBuilder sb = new StringBuilder();
            sb.append("Current store content: ");
            if (store.getStore().isEmpty()) sb.append("empty");
            else sb.append(System.lineSeparator());
            store.getStore()
                    .forEach(todo -> sb.append(" - ").append(todo).append(System.lineSeparator()));

            log.info(sb.toString());
        }

        return returnVal;
    }

}
