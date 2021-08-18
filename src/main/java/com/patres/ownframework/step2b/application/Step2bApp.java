package com.patres.ownframework.step2b.application;

import com.patres.ownframework.step2b.application.dao.CompanyDao;
import com.patres.ownframework.step2b.application.model.Company;
import com.patres.ownframework.step2b.application.service.CompanyService;
import com.patres.ownframework.step2b.framework.ProxyMethodInterceptor;
import net.sf.cglib.proxy.Enhancer;

public class Step2bApp {

    public static void main(String[] args) {
        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(CompanyService.class);
        enhancer.setCallback(new ProxyMethodInterceptor());

        CompanyService companyService = (CompanyService) enhancer.create(new Class[]{CompanyDao.class}, new Object[]{new CompanyDao()});
        companyService.createCompany(new Company());
    }
}