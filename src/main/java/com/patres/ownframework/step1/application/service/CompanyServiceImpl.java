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
            beginTransaction();

            logger.info("SERVICE:   START - create company");
            companyDao.createCompany(company);
            logger.info("SERVICE:   END - create company");

            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }

    @Override
    public void updateCompany(Company company) {
        try {
            beginTransaction();

            logger.info("SERVICE:   START - update company");
            companyDao.createCompany(company);
            logger.info("SERVICE:   END - update company");

            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }

    private void beginTransaction() {
        logger.debug("BEGIN TRANSACTION");
    }

    private void commitTransaction() {
        logger.debug("COMMIT TRANSACTION");
    }

    private void rollbackTransaction() {
        logger.error("ROLLBACK TRANSACTION");
    }
}
