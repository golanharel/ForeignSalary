package org.example.foreignsalary.repositories;

import org.example.foreignsalary.model.Vacation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VacationRepository extends MongoRepository<Vacation, String> {
}
