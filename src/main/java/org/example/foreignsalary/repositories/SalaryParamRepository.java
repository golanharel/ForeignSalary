package org.example.foreignsalary.repositories;

import org.example.foreignsalary.model.SalaryParams;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SalaryParamRepository extends MongoRepository<SalaryParams, String> {
}
