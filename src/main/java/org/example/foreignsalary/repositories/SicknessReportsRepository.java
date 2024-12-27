package org.example.foreignsalary.repositories;

import org.example.foreignsalary.model.SicknessReports;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SicknessReportsRepository extends MongoRepository<SicknessReports, String> {



}
