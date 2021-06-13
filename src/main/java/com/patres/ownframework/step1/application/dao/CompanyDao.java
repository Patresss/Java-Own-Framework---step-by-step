package com.patres.ownframework.step1.application.dao;

import com.patres.ownframework.step1.application.model.Company;

public interface CompanyDao {

    void createCompany(Company company);

    void updateCompany(Company company);
}
