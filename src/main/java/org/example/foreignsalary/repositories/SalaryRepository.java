package org.example.foreignsalary.repositories;
import org.example.foreignsalary.model.Salary;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SalaryRepository extends MongoRepository<Salary, String>{

    public Salary findSalaryByMonthAndYear(int month, int year);

}
