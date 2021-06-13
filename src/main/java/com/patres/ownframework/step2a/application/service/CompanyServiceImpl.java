package com.patres.ownframework.step2a.application.service;

import com.patres.ownframework.step2a.application.dao.CompanyDao;
import com.patres.ownframework.step2a.application.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyDao companyDao;

    public CompanyServiceImpl(CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    @Override
    public void createCompany(Company company) {
        logger.info("SERVICE:   START - create company");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - create company");
    }

    @Override
    public void updateCompany(Company company) {
        logger.info("SERVICE:   START - update company");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - update company");
    }
}
