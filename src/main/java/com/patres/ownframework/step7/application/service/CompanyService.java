package com.patres.ownframework.step7.application.service;

import com.patres.ownframework.step7.application.model.Company;

public interface CompanyService {

    void createCompany(Company company);

    String generateToken(Company company);

    void updateCompany(Company company);
}
