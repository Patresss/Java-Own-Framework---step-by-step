package com.patres.ownframework.step4.application;

import com.patres.ownframework.step4.application.model.Company;
import com.patres.ownframework.step4.application.service.CompanyService;
import com.patres.ownframework.step4.framework.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step4App {

    private static final Logger logger = LoggerFactory.getLogger(Step4App.class);

    public static void main(String[] args) {

        final ApplicationContext applicationContext = new ApplicationContext(Step4App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        companyServiceProxy.createCompany(new Company());
    }
}
