package com.patres.ownframework.step1.application.service;

import com.patres.ownframework.step1.application.dao.CompanyDao;
import com.patres.ownframework.step1.application.model.Company;
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
        try {
            logger.info("TRANSACTION:   BEGIN");

            logger.info("SERVICE:   START - create company");
            companyDao.createCompany(company);
            logger.info("SERVICE:   END - create company");

            logger.info("TRANSACTION:   COMMIT");
        } catch (Exception e) {
            logger.info("TRANSACTION:   ROLLBACK");
        }
    }

    @Override
    public void updateCompany(Company company) {
        try {
            logger.info("TRANSACTION:   BEGIN");

            logger.info("SERVICE:   START - update company");
            companyDao.createCompany(company);
            logger.info("SERVICE:   END - update company");

            logger.info("TRANSACTION:   COMMIT");
        } catch (Exception e) {
            logger.info("TRANSACTION:   ROLLBACK");
        }
    }
}
