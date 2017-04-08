package com.example.ui.todo.rs;

import au.com.dius.pact.model.RequestResponseInteraction;
import com.example.ui.todo.Store;
import com.example.ui.todo.StoreStateInterceptor;
import com.example.ui.todo.Todo;
import org.arquillian.pact.provider.core.httptarget.Target;
import org.arquillian.pact.provider.core.loader.PactFolder;
import org.arquillian.pact.provider.spi.CurrentInteraction;
import org.arquillian.pact.provider.spi.Provider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

@RunWith(Arquillian.class)
@Provider("Todo API")
@PactFolder("pacts")
public class TodoResourceProviderIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
                .addPackage(TodoResource.class.getPackage())
                .addClass(StoreStateInterceptor.class)
                .addClasses(Todo.class, Store.class);
    }

    @ArquillianResource
    Target target;

    @Test
    public void should_provide_valid_answers() throws MalformedURLException {
        target.testInteraction(new URL("http://127.0.0.1:8080"));
    }

}