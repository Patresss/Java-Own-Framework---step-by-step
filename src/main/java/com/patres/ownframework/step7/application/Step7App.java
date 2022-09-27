package com.patres.ownframework.step7.application;

import com.patres.ownframework.step7.application.model.Company;
import com.patres.ownframework.step7.application.service.CompanyService;
import com.patres.ownframework.step7.framework.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step7App {

    private static final Logger logger = LoggerFactory.getLogger(Step7App.class);

    public static void main(String[] args) {
        final ApplicationContext applicationContext = new ApplicationContext(Step7App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        logger.info("======== Transactional ========");
        companyServiceProxy.createCompany(new Company());
        logger.info("===============================");


        logger.info("========== Cacheable ==========");
        final Company company1 = new Company();
        logger.info(companyServiceProxy.generateToken(company1));
        logger.info(companyServiceProxy.generateToken(company1));

        final Company company2 = new Company();
        logger.info(companyServiceProxy.generateToken(company2));
        logger.info("===============================");


        logger.info("============= Scope ===========");
        final CompanyService companyServiceProxy1 = applicationContext.getBean(CompanyService.class);
        final CompanyService companyServiceProxy2 = applicationContext.getBean(CompanyService.class);

        logger.info(String.valueOf(companyServiceProxy1 == companyServiceProxy2));
        logger.info("===============================");
    }
}
