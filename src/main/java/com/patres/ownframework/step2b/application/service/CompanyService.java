package com.patres.ownframework.step2b.application.service;

import com.patres.ownframework.step2b.application.dao.CompanyDao;
import com.patres.ownframework.step2b.application.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyDao companyDao;

    public CompanyService(CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    public void createCompany(Company company) {
        logger.info("SERVICE:   START - create company");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - create company");
    }

    public void updateCompany(Company company) {
        logger.info("SERVICE:   START - update company");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - update company");
    }
}
