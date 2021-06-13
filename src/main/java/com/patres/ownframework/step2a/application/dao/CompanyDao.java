package com.patres.ownframework.step2a.application.dao;

import com.patres.ownframework.step2a.application.model.Company;

public interface CompanyDao {

    void createCompany(Company company);

    void updateCompany(Company company);
}
