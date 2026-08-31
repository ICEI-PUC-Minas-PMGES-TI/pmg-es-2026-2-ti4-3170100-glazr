package com.glazr.configuration;

import com.glazr.user.application.AdministratorBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdministratorBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministratorBootstrapRunner.class);

    private final AdministratorBootstrapService service;
    private final Environment environment;

    public AdministratorBootstrapRunner(AdministratorBootstrapService service, Environment environment) {
        this.service = service;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdministratorBootstrapService.Outcome outcome = service.bootstrap(() -> new BootstrapCredentials(
                environment.getProperty("glazr.bootstrap.admin.email"),
                environment.getProperty("glazr.bootstrap.admin.password"),
                environment.getProperty("glazr.bootstrap.admin.name")));
        if (outcome == AdministratorBootstrapService.Outcome.CREATED) {
            LOGGER.info("Administrador inicial criado; remova as variáveis GLAZR_BOOTSTRAP_ADMIN_* do ambiente");
        } else {
            LOGGER.info("Bootstrap ignorado porque o banco já possui usuários");
        }
    }
}
