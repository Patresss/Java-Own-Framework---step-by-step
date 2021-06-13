package com.patres.ownframework.step1.application;

import com.patres.ownframework.step1.application.dao.CompanyDaoImpl;
import com.patres.ownframework.step1.application.model.Company;
import com.patres.ownframework.step1.application.service.CompanyServiceImpl;

public class Step1App {

    public static void main(String[] args) {
        final CompanyDaoImpl companyDao = new CompanyDaoImpl();
        final CompanyServiceImpl companyService = new CompanyServiceImpl(companyDao);

        companyService.createCompany(new Company());
    }
}
