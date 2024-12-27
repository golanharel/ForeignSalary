package org.example.foreignsalary.configurations;

import org.example.foreignsalary.model.SalaryParams;
import org.example.foreignsalary.repositories.SalaryParamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SalaryParamsConfiguration {

    private final SalaryParamRepository salaryParamRepository;

    @Autowired
    public SalaryParamsConfiguration(SalaryParamRepository salaryParamRepository) {
        this.salaryParamRepository = salaryParamRepository;
    }

    @Bean
    public SalaryParams salaryParams() {
        if (salaryParamRepository.findAll().isEmpty())
                return null;
        return salaryParamRepository.findAll().get(0);
    }

    public SalaryParams saveSalaryParams(SalaryParams salaryParams) {
        return salaryParamRepository.save(salaryParams);
    }
}
