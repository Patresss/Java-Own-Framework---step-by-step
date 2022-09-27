package com.patres.ownframework.step3.application;

import com.patres.ownframework.step3.application.model.Company;
import com.patres.ownframework.step3.application.service.CompanyService;
import com.patres.ownframework.step3.framework.ApplicationContext;

public class Step3App {

    public static void main(String[] args) {
        final ApplicationContext applicationContext = new ApplicationContext(Step3App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        companyServiceProxy.createCompany(new Company());
    }
}
