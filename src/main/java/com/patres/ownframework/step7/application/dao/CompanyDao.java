package com.patres.ownframework.step7.application.dao;

import com.patres.ownframework.step7.application.model.Company;

public interface CompanyDao {

    void createCompany(Company company);

    void updateCompany(Company company);
}
