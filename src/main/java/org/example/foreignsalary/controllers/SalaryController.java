package org.example.foreignsalary.controllers;

import org.example.foreignsalary.configurations.SalaryClosedException;
import org.example.foreignsalary.configurations.SalaryParamsConfiguration;
import org.example.foreignsalary.configurations.VacationConfiguration;
import org.example.foreignsalary.model.Salary;
import org.example.foreignsalary.model.SalaryParams;
import org.example.foreignsalary.model.SalaryRequest;
import org.example.foreignsalary.services.SalaryService;
import org.example.foreignsalary.services.SicknessReportsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/salary")
public class SalaryController {


    private final SalaryService salaryService;
    private final SalaryParamsConfiguration salaryParamsConfiguration;
    private final SicknessReportsService sicknessReportsService;
    private final VacationConfiguration vacationConfiguration;



    @Autowired
    public SalaryController(SalaryService salaryService, SalaryParamsConfiguration salaryParamsConfiguration, SicknessReportsService sicknessReportsService, VacationConfiguration vacationConfiguration) {
        this.salaryService = salaryService;
        this.salaryParamsConfiguration = salaryParamsConfiguration;
        this.sicknessReportsService = sicknessReportsService;
        this.vacationConfiguration = vacationConfiguration;

    }

    @CrossOrigin(origins = "*")
    @GetMapping("/{month}/{year}")
    public ResponseEntity<Salary> getSalary(@PathVariable int month, @PathVariable int year) {

        return ResponseEntity.ok().body(salaryService.getSalary(month, year));
    }

    @CrossOrigin(origins = "*")
    @PostMapping
    public ResponseEntity<?> postSalary(@RequestBody SalaryRequest salaryRequest) {
        try {
            ResponseEntity.ok().body(salaryService.saveSalary(salaryRequest, sicknessReportsService));
            return getSalary(salaryRequest.getMonth(), salaryRequest.getYear());
        } catch (SalaryClosedException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()); // Respond with 400 Bad Request
        }
    }

    @ExceptionHandler(SalaryClosedException.class)
    public ResponseEntity<String> handleSalaryClosedException(SalaryClosedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/params")
    public ResponseEntity postSalaryParams(@RequestBody SalaryParams salaryParams) {
        return ResponseEntity.ok().body(salaryParamsConfiguration.saveSalaryParams(salaryParams));
    }
    @CrossOrigin(origins = "*")
    @GetMapping("/params")
    public ResponseEntity<SalaryParams> getSalaryParams() {
        return ResponseEntity.ok().body(salaryParamsConfiguration.salaryParams());
    }

    //@CrossOrigin(origins = "http://localhost:3001")
    @CrossOrigin(origins = "*")
    @PostMapping("/close/{month}/{year}")
    public void postClose(@PathVariable int month, @PathVariable int year) {
        salaryService.close(month,year);

    }

}
