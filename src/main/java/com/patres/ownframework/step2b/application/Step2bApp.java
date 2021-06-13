package com.patres.ownframework.step2b.application;

import com.patres.ownframework.step2b.application.dao.CompanyDao;
import com.patres.ownframework.step2b.application.model.Company;
import com.patres.ownframework.step2b.application.service.CompanyService;
import com.patres.ownframework.step2b.framework.ProxyMethodInterceptor;
import net.sf.cglib.proxy.Enhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Step2bApp {

    private static final Logger logger = LoggerFactory.getLogger(Step2bApp.class);

    public static void main(String[] args) {
        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(CompanyService.class);
        enhancer.setCallback(new ProxyMethodInterceptor());

        CompanyService companyService = (CompanyService) enhancer.create(new Class[]{CompanyDao.class}, new Object[]{new CompanyDao()});
        companyService.createCompany(new Company());
    }
}

// Since JDK 16, the default for the --illegal-access option is deny, so “deep reflection” to JDK classes fails.
// https://github.com/cglib/cglib/issues/191
// --illegal-access=permit

/*
        enhancer.setCallback((MethodInterceptor) (obj, method, methodArgs, proxy) -> {
            try {
                logger.info("TRANSACTION:   BEGIN");
                final Object invoke = proxy.invokeSuper(obj, methodArgs);
                logger.info("TRANSACTION:   COMMIT");
                return invoke;
            } catch (Exception e) {
                logger.info("TRANSACTION:   ROLLBACK");
                throw e;
            }
        });
 */