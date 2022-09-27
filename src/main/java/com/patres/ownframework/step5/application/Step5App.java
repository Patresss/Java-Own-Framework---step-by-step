package com.patres.ownframework.step5.application;

import com.patres.ownframework.step5.application.model.Company;
import com.patres.ownframework.step5.application.service.CompanyService;
import com.patres.ownframework.step5.framework.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step5App {

    private static final Logger logger = LoggerFactory.getLogger(Step5App.class);

    public static void main(String[] args) {

        final ApplicationContext applicationContext = new ApplicationContext(Step5App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        final Company company1 = new Company();
        logger.info(companyServiceProxy.generateToken(company1));
        logger.info(companyServiceProxy.generateToken(company1));

        final Company company2 = new Company();
        logger.info(companyServiceProxy.generateToken(company2));
    }
}
