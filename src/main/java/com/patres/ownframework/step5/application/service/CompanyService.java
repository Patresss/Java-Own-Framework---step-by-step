package com.patres.ownframework.step5.application.service;

import com.patres.ownframework.step5.application.model.Company;

public interface CompanyService {

    void createCompany(Company company);

    String generateToken(Company company);

    void updateCompany(Company company);
}
