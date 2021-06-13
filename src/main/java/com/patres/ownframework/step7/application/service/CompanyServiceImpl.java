package com.patres.ownframework.step7.application.service;

import com.patres.ownframework.step7.application.dao.CompanyDao;
import com.patres.ownframework.step7.application.model.Company;
import com.patres.ownframework.step7.framework.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component(scope = Scope.SINGLETON)
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
