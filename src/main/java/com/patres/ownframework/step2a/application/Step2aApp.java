package com.patres.ownframework.step2a.application;

import com.patres.ownframework.step2a.application.dao.CompanyDao;
import com.patres.ownframework.step2a.application.dao.CompanyDaoImpl;
import com.patres.ownframework.step2a.application.model.Company;
import com.patres.ownframework.step2a.application.service.CompanyService;
import com.patres.ownframework.step2a.application.service.CompanyServiceImpl;
import com.patres.ownframework.step2a.framework.ProxyHandler;

import java.lang.reflect.Proxy;

public class Step2aApp {

    public static void main(String[] args) {
        final CompanyDao companyDao = new CompanyDaoImpl();

        final CompanyService companyServiceProxy = (CompanyService) Proxy.newProxyInstance(
                Step2aApp.class.getClassLoader(),
                new Class[]{CompanyService.class},
                new ProxyHandler(new CompanyServiceImpl(companyDao))
        );

        companyServiceProxy.createCompany(new Company());
    }
}
