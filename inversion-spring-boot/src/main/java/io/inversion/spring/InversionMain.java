/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.spring;

import io.inversion.Api;
import io.inversion.Engine;
import io.inversion.utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Launches your Api in an SpringBoot embedded Tomcat.
 * <p>
 * This is a super simple way to launch an Api with an embedded Tomcat but if you are a
 * regular Spring Boot users and would like to wire your Api up an a more "spring-ish"
 * way, please check out <code>io.inversion.service.spring.config.EnableInversion</code>
 */
public class InversionMain {

    protected static Engine engine = null;

    protected static ApplicationContext context = null;

    public static void main(String[] args) {
        run(new Engine());
    }

    public static void exit() {
        if (context != null)
            SpringApplication.exit(context);
        context = null;
    }

    /**
     * Convenience method for launching a Engine that will be configured via config files.
     */
    public static void run() {
        run(new Engine());
    }

    /**
     * Convenience method for launching a Engine with a single API.
     *
     * @param api the Api to run
     * @return the SpringBoot ApplicationContext for the running server
     */
    public static ApplicationContext run(Api api) {
        return run(new Engine().withApi(api));
    }

    public static ApplicationContext run(Engine engine) {
        try {

            if (context != null)
                exit();

            InversionMain.engine = engine;
            context = SpringApplication.run(InversionMain.class);
        } catch (Throwable e) {
            e = Utils.getCause(e);
            if (Utils.getStackTraceString(e).contains("A child container failed during start")) {
                String msg;
                msg = " README FOR HELP!!!!!!!";
                msg += "\n";
                msg += "\n It looks like you are getting a frustrating Tomcat startup error.";
                msg += "\n";
                msg += "\n This error may be casused if URL.setURLStreamHandlerFactory()";
                msg += "\n is somehow called before Spring Boot starts Tomcat. ";
                msg += "\n";
                msg += "\n This seems to be a frustrating undocumented \"no no\" of Tomcat with ";
                msg += "\n Spring Boot. Using H2 db before Spring Boot starts Tomcat seems to ";
                msg += "\n be one known cause of this error.";
                msg += "\n";
                msg += "\n SOLUTION: Override Engine.startup0() and place all of your Api wiring";
                msg += "\n and other setup code there.  That way Tomcat will load before ";
                msg += "\n the part of your code that is causing this unintended side effect.";
                msg += "\n\n\n";

                System.err.println(msg);
                throw new RuntimeException(msg, e);
            } else {
                e.printStackTrace();
            }
            Utils.rethrow(e);
        }

        return context;
    }

    public ApplicationContext getContext() {
        return context;
    }

    @Bean
    public ServletRegistrationBean inversionServlet() {
        return InversionServletConfig.createDefaultInversionServlet(InversionMain.engine);
    }

    @Bean
    public ConfigurableServletWebServerFactory servletContainer() {
        return InversionServletConfig.createDefaultServletContainer();
    }

}