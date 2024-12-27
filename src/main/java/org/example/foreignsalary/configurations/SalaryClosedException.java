package org.example.foreignsalary.configurations;

public class SalaryClosedException extends RuntimeException {
    public SalaryClosedException(String message) {
        super(message);
    }
}