package com.patres.ownframework.step2b.application.dao;

import com.patres.ownframework.step2b.application.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompanyDao {

    private static final Logger logger = LoggerFactory.getLogger(CompanyDao.class);

    public void createCompany(Company company) {
        logger.info("DAO:   START - create company");

        logger.info("DAO:   END - create company");
    }

    public void updateCompany(Company company) {
        logger.info("DAO:   START - update company");

        logger.info("DAO:   END - update company");
    }
}
