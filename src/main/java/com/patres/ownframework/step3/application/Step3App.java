package com.patres.ownframework.step3.application;

import com.patres.ownframework.step3.application.model.Company;
import com.patres.ownframework.step3.application.service.CompanyService;
import com.patres.ownframework.step3.framework.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step3App {

    private static final Logger logger = LoggerFactory.getLogger(Step3App.class);

    public static void main(String[] args) {

        final ApplicationContext applicationContext = new ApplicationContext(Step3App.class.getPackage());
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        companyServiceProxy.createCompany(new Company());
    }
}
