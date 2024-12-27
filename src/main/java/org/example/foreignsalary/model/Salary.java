package org.example.foreignsalary.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document(collection = "salary")
public class Salary {
    @Id
    private String id;

    private String employmentType;
    private String employmentCapacity;
    private String workStartDate;
    private String employeeName;
    private String employeeBankNumber;
    private String employeeBankBranchNumber;
    private String employeeBankAccountNumber;
    private String employeePassportOrId;
    private String employerName;
    private String employerId;
    private String employerAddress;


    private Integer daysWorkedCount;
    private Integer travelFeeCount;
    private Integer deductedDaysCount;
    private List<SalaryRequest.WorkingDays> workingDays;
    private Double holidayDaysWorkedSum;
    private Integer holidaysWorkedDays;

    private Double restDaysWorkedSum;
    private Integer restWorkedDays;

    private Double baseSalary;

    private Double grossSalary;
    private Double pensionDeposit;
    private Double compensationDeposit;
    private SummaryWorkingDays summaryWorkingDays;


    private boolean isClosed;
    private int month;
    private int year;
    private double dailyFee;

    private Integer MonthlyTravelExpenseTimes;
    private Double MonthlytravelFeeOnHoliday;
    private Double bituachLeumiSum;
    private Double restDayWorkedSum;

    private SalaryParams salaryParams;
    private String comments;


    @Data
    public static class YearDays {
        private int year;
        private int days;
    }

    @Data
    public static class HolidayDate {
        private int day;
        private int month;
    }

    @Data
    public static class AdditionalPayment {
        private String type;
        private double fee;
    }
    @Data
    public static class SummaryWorkingDays {
        private Double havraaDays;
        private Double havraaSum;
        private Vacation.VacationMonthlyUsed vacationMonthlyUsed;

        //private Double vacationEntitledBeginOfMonth;
        //private Double vacationUsed;
        private Double vacationDaysDeductedSum;
        private Double vacationEntitledBeginningOfMonth;
        //private Double vacationLeftEndOfMonth;
        private SicknessReports sicknessReports;
    }


}
