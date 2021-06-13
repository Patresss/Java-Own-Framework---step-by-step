package com.patres.ownframework.step5.application.service;

import com.patres.ownframework.step5.application.dao.CompanyDao;
import com.patres.ownframework.step5.application.model.Company;
import com.patres.ownframework.step5.framework.annotation.Autowired;
import com.patres.ownframework.step5.framework.annotation.Cacheable;
import com.patres.ownframework.step5.framework.annotation.Component;
import com.patres.ownframework.step5.framework.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyDao companyDao;

    @Autowired
    public CompanyServiceImpl(CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    @Override
    public void createCompany(Company company) {
        logger.info("SERVICE:   START - create company");
//         companyDao.createCompany(company);
        createWithTransaction(company);
        logger.info("SERVICE:   END - create company");
    }

    @Override
    @Cacheable
    public String generateToken(Company company) {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public void createWithTransaction(Company company) {
        logger.info("SERVICE:   START - createWithTransaction");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - createWithTransaction");
    }

    @Override
    @Transactional
    public void updateCompany(Company company) {
        logger.info("SERVICE:   START - update company");
        companyDao.createCompany(company);
        logger.info("SERVICE:   END - update company");
    }
}
