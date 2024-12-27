package org.example.foreignsalary.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Document(collection = "salaryParams")
public class SalaryParams {
    @Id
    private String id;
    private double baseSalary;
    private String employmentType;
    private String employmentCapacity;
    private double weeksInMonth;
    private double hoursInMonth;
    private double daysInMonth;
    private double saturdayFee;
    private String workStartDate;
    private String workEndDate;
    private double baseHavraaSalary;
    private double pensionPercentage;
    private int pensionWaitingMonths;


    private List<Salary.YearDays> daysHavraa;
    private double baseWorkOnHolidayFee;
    private int holidayDays;
    private Map<Integer, List<Salary.HolidayDate>> holidayDates;
    private double holidayDaySalary;
    private double monthlySicknessDaysToAccumulate;
    private int maxSicknessAccumulation;
    private double travelVacationFee;
    private List<Salary.YearDays> daysVacation;
    private double bituachLeumiPercentage;
    private String employeeName;
    private String employeeBankNumber;
    private String employeeBankBranchNumber;
    private String employeeBankAccountNumber;
    private String employeePassportOrId;
    private String employerName;
    private String employerId;
    private String employerAddress;


    private List<Salary.AdditionalPayment> additionalMonthlyPaymentsOutsideSalary;




}
