package org.example.foreignsalary.services;

import org.example.foreignsalary.configurations.SalaryClosedException;
import org.example.foreignsalary.configurations.VacationConfiguration;
import org.example.foreignsalary.model.*;
import org.example.foreignsalary.repositories.SalaryRepository;
import org.example.foreignsalary.repositories.VacationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;


import java.util.List;





@Service
public class SalaryService {
    private final SalaryRepository salaryRepository;
    private final SalaryParams salaryParams;
    private final Vacation vacation;
    private final SicknessReportsService sicknessReportsService ;
    private final VacationRepository vacationRepository;
    private final VacationConfiguration vacationConfiguration;
    private final Environment environment;
    private SicknessReports sicknessReports;
    private static final DecimalFormat df = new DecimalFormat("0.00");


    @Autowired
    public SalaryService(SalaryRepository salaryRepository, SalaryParams salaryParams, SicknessReportsService sicknessReportsService, Vacation vacation, VacationRepository vacationRepository, VacationConfiguration vacationConfiguration, Environment environment) {
        this.salaryRepository = salaryRepository;
        this.salaryParams = salaryParams;
        this.sicknessReportsService = sicknessReportsService;
        this.vacation = vacation;
        this.vacationRepository = vacationRepository;
        this.vacationConfiguration = vacationConfiguration;
        this.environment = environment;
    }

    public Salary getSalary(int month, int year)  {

        return  salaryRepository.findSalaryByMonthAndYear(month, year);
    }

    public Salary saveSalary(SalaryRequest salaryRequest, SicknessReportsService sicknessReportsService) {
        if (isSalaryClosed (salaryRequest.getMonth() ,salaryRequest.getYear()))
            throw new SalaryClosedException("Salary for the specified month and year is already closed.");

        Salary salary = generateSalary(salaryRequest);
        return salaryRepository.save(salary);
    }

    private boolean isSalaryClosed(Integer month, Integer year) {
        Salary salaryCloseCheck = getSalary(month, year);
        if (salaryCloseCheck != null)
            return salaryCloseCheck.isClosed();
        return false;
    }

    private Salary generateSalary(SalaryRequest salaryRequest) {
        //clean workingDays in case too many records sent
        int lastDayOfMonth = (calcLastDayOfMonth(salaryRequest.getMonth(), salaryRequest.getYear())).getDayOfMonth();
        LocalDate startDate =LocalDate.parse(salaryParams.getWorkStartDate());
        LocalDate endDate;
        int lastDayOfWork = 31;
        if (isDate (salaryParams.getWorkEndDate())) {
            endDate = LocalDate.parse(salaryParams.getWorkEndDate());
            if (salaryRequest.getMonth() == endDate.getMonthValue() && salaryRequest.getYear() == endDate.getYear()) {
                lastDayOfWork = endDate.getDayOfMonth();
            }
        }
        int firstDayOfWork = 1;
        if (salaryRequest.getMonth() == startDate.getMonthValue() && salaryRequest.getYear() == startDate.getYear()) {
            firstDayOfWork = startDate.getDayOfMonth();
        }
        int day;
        int deductDays=0;
        for (int i=salaryRequest.getWorkingDays().size()-1; i>=0;i--) {
            day = salaryRequest.getWorkingDays().get(i).getDay();
            if (day > lastDayOfMonth || day>lastDayOfWork) {
                salaryRequest.getWorkingDays().remove(i );
            }
            if (day < firstDayOfWork)  {
                salaryRequest.getWorkingDays().remove(i );
                if (!isHoliday(day,salaryRequest.getMonth(),salaryRequest.getYear()) && LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), day).getDayOfWeek() != DayOfWeek.SATURDAY)
                    deductDays++;
            }
        }




        int holidaysWorkedDays = getHolidayDaysWorked(salaryRequest, false);
        int restWorkedDays = getHolidayDaysWorked(salaryRequest, true);
        double entitleYearlyVacation = calculateVacationEntitleThisYear(salaryRequest.getYear(), salaryRequest.getMonth());
        //double usedVacationDays = calcUsedVacationDays(salaryRequest, entitleYearlyVacation);


        double daySalary = salaryParams.getBaseSalary()/salaryParams.getDaysInMonth();



        Salary.SummaryWorkingDays summaryWorkingDays = new Salary.SummaryWorkingDays();
        summaryWorkingDays.setSicknessReports(calcSicknessUsedThisMonth(salaryRequest));
        summaryWorkingDays.setVacationMonthlyUsed(calcUsedVacationDays(salaryRequest, entitleYearlyVacation));
        summaryWorkingDays.setHavraaDays(calcHavraaDays(salaryRequest.getMonth(), salaryRequest.getYear()));
        summaryWorkingDays.setHavraaSum(summaryWorkingDays.getHavraaDays()*salaryParams.getBaseHavraaSalary());
        summaryWorkingDays.setVacationDaysDeductedSum(calcVacationDeductSum(salaryRequest, daySalary));
        summaryWorkingDays.setVacationEntitledBeginningOfMonth(entitleYearlyVacation);
        double grossSalary = calcGrossSalary(salaryRequest, summaryWorkingDays.getHavraaSum() ,deductDays);


        return Salary.builder()
                 .id(handleSalaryId(salaryRequest))
                .employeeName(salaryParams.getEmployeeName())
                .employeeBankNumber(salaryParams.getEmployeeBankNumber())
                .employeeBankBranchNumber(salaryParams.getEmployeeBankBranchNumber())
                .employeeBankAccountNumber(salaryParams.getEmployeeBankAccountNumber())
                .employeePassportOrId(salaryParams.getEmployeePassportOrId())
                .employerName(salaryParams.getEmployerName())
                .employerId(salaryParams.getEmployerId())
                .employerAddress(salaryParams.getEmployerAddress())
                .employmentType(salaryParams.getEmploymentType())
                .employmentCapacity(salaryParams.getEmploymentCapacity())
                .workStartDate(salaryParams.getWorkStartDate())
                .month(salaryRequest.getMonth())
                .year(salaryRequest.getYear())
                .daysWorkedCount(calcDaysWorked(salaryRequest))
                .travelFeeCount(calcTravelFeeCount(salaryRequest))
                .baseSalary(salaryParams.getBaseSalary())
                .workingDays(salaryRequest.getWorkingDays())
                .holidaysWorkedDays(holidaysWorkedDays)
                .holidayDaysWorkedSum(calcHolidayFee(restWorkedDays, salaryParams.getHolidayDaySalary()))
                .restWorkedDays(restWorkedDays)
                .restDaysWorkedSum(calcHolidayFee(restWorkedDays, salaryParams.getHolidayDaySalary()))
                .summaryWorkingDays(summaryWorkingDays)
                .deductedDaysCount(deductDays)
                .grossSalary(grossSalary)
                .bituachLeumiSum(calcBL(grossSalary))
                .pensionDeposit(calcPensionSum(salaryRequest, grossSalary))
                .compensationDeposit(calcCompensation(grossSalary))
                .dailyFee(salaryParams.getBaseSalary()/salaryParams.getDaysInMonth())

                .comments(salaryRequest.getComments())

                .salaryParams(salaryParams)
                .build();

    }

    private boolean isDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    private int calcDaysWorked (SalaryRequest salaryRequest) {
        int daysWorked = 0;

        for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
            if (workingDay.getWorkType().equals(WorkType.WORK)) {
                daysWorked++;
            }
        }
        return daysWorked;
    }

    private int calcTravelFeeCount (SalaryRequest salaryRequest) {
        int TravelFee = 0;
        double travelFee = salaryParams.getTravelVacationFee();

        for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
            if (workingDay.getPaymentFee() != null)
                if (workingDay.getPaymentFee()== travelFee) {
                    TravelFee++;
                }
        }
        return TravelFee;
    }


    private String handleSalaryId (SalaryRequest salaryRequest) {
        String salaryId ;
        salaryId = null;
        getSalary(salaryRequest.getMonth(), salaryRequest.getYear());
        Salary salary = getSalary(salaryRequest.getMonth(), salaryRequest.getYear());
        if (salary != null)
            salaryId = salary.getId();
        return salaryId;
    }


    private double calcGrossSalary (SalaryRequest salaryRequest, double havraaSum, int deductDays) {
        double grossSalary = salaryParams.getBaseSalary();
        double daySalary = salaryParams.getBaseSalary()/salaryParams.getDaysInMonth();

        for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
            if (workingDay.getPaymentPercentage() != null) {
                grossSalary += daySalary* (workingDay.getPaymentPercentage()/100);
            } if (workingDay.getPaymentFee() != null) {
                // payment fee is only for holidays to get travel fee
                grossSalary +=  workingDay.getPaymentFee();
            }
        }
        grossSalary -= deductDays*daySalary;
        return Double.parseDouble(new DecimalFormat("#.##").format(grossSalary+havraaSum));
    }

/////////////////////////  SICKNESS /////////////////////////////////////////////

    public SicknessReports calcSicknessUsedThisMonth (SalaryRequest salaryRequest) {
        double  monthlySicknessUsed=getMonthlySicknessUsedThisMonth( salaryRequest);
        double accumulatedDaysBeforeThisMonth = getLastMonthSicknessAccumulated(salaryRequest.getMonth(),salaryRequest.getYear());
        double accumulatedDays = getAccumulatedSicknessDaysAfterDeduction(accumulatedDaysBeforeThisMonth,monthlySicknessUsed);

        return sicknessReportsService.saveSicknessReports(
                SicknessReports.builder()
                        .id(salaryRequest.getMonth().toString() + salaryRequest.getYear().toString())
                        .sicknessUsed(monthlySicknessUsed)
                        .sicknessPayDaysPercentageSummary(calcSicknessUsed(salaryRequest, accumulatedDaysBeforeThisMonth))
                        .sicknessLeftEndOfMonth(accumulatedDays)
                        .sicknessEntitledBeginOfMonth(accumulatedDaysBeforeThisMonth)
                        .sicknessDaysPaidSum(calcSicknessPaidSum(salaryRequest))
                        .build());
    }



    public LocalDate calcLastDayOfMonth(int month, int year) {
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1);
        lastDayOfMonth = lastDayOfMonth.withDayOfMonth(lastDayOfMonth.getMonth().length(lastDayOfMonth.isLeapYear()));;
        return lastDayOfMonth;
    }


    public double getMonthlySicknessUsedThisMonth(SalaryRequest salaryRequest) {
        double monthlySicknessUsed=0;
        DayOfWeek dayOfWeek;
        for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
            dayOfWeek = LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), workingDay.getDay()).getDayOfWeek();
            if (workingDay.getWorkType().equals(WorkType.SICKNESS))
                if (!(dayOfWeek == DayOfWeek.SATURDAY || isHoliday(workingDay.getDay(),salaryRequest.getMonth(),salaryRequest.getYear()))) {
                    monthlySicknessUsed++;
            }
        }
        return monthlySicknessUsed;
    }

    public double getAccumulatedSicknessDaysAfterDeduction (double accumulatedDays, double monthlySicknessUsed) {

        accumulatedDays = accumulatedDays - monthlySicknessUsed + salaryParams.getMonthlySicknessDaysToAccumulate();
        if (accumulatedDays<0) accumulatedDays = 0;
        if (accumulatedDays>salaryParams.getMaxSicknessAccumulation()) accumulatedDays = salaryParams.getMaxSicknessAccumulation();

        return accumulatedDays;
    }

    public Map<SicknessPayDaysPercentage, Integer> calcSicknessUsed (SalaryRequest salaryRequest, double accumulatedDays
    ) {
        Map<SicknessPayDaysPercentage,Integer> sicknessPayPercentage = new HashMap<>();
        sicknessPayPercentage.put(SicknessPayDaysPercentage.FIRST0,0);
        sicknessPayPercentage.put(SicknessPayDaysPercentage.SECOND50,0);
        sicknessPayPercentage.put(SicknessPayDaysPercentage.THIRD50,0);
        sicknessPayPercentage.put(SicknessPayDaysPercentage.REST100,0);
        DayOfWeek dayOfWeek;
        // get last month's 3 days of sickness
        SicknessPayDaysPercentage nextSicknessPercentage; //= SicknessPayDaysPercentage.FIRST0;
        nextSicknessPercentage = getLastMonthSicknessSickDaysPercentage(salaryRequest.getMonth(), salaryRequest.getYear(),salaryRequest);

        for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
            if (workingDay.getWorkType().equals(WorkType.SICKNESS)) {
                dayOfWeek = LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), workingDay.getDay()).getDayOfWeek();
                switch (nextSicknessPercentage) {
                    case FIRST0:

                        if (dayOfWeek == DayOfWeek.SATURDAY || (isHoliday(workingDay.getDay(),salaryRequest.getMonth(),salaryRequest.getYear())) ) {
                            workingDay.setPaymentPercentage(0.0);
                        } else {
                            sicknessPayPercentage.put(SicknessPayDaysPercentage.FIRST0,sicknessPayPercentage.get(SicknessPayDaysPercentage.FIRST0)+1);
                            nextSicknessPercentage = SicknessPayDaysPercentage.SECOND50;
                            workingDay.setPaymentPercentage(-100.0);
                            accumulatedDays--;
                        }

                        break;
                    case SECOND50:

                        if (dayOfWeek == DayOfWeek.SATURDAY || (isHoliday(workingDay.getDay(),salaryRequest.getMonth(),salaryRequest.getYear())) ) {
                            workingDay.setPaymentPercentage(0.0);
                        } else {
                            sicknessPayPercentage.put(SicknessPayDaysPercentage.SECOND50,sicknessPayPercentage.get(SicknessPayDaysPercentage.SECOND50)+1);
                            nextSicknessPercentage = SicknessPayDaysPercentage.THIRD50;
                            if (accumulatedDays <= 0.5 )
                                workingDay.setPaymentPercentage(-100.0);
                            else
                                workingDay.setPaymentPercentage(-50.0);
                            accumulatedDays--;
                        }
                        break;
                    case THIRD50:

                        if (dayOfWeek == DayOfWeek.SATURDAY || (isHoliday(workingDay.getDay(),salaryRequest.getMonth(),salaryRequest.getYear())) ) {
                            workingDay.setPaymentPercentage(0.0);
                        } else {
                            sicknessPayPercentage.put(SicknessPayDaysPercentage.THIRD50,sicknessPayPercentage.get(SicknessPayDaysPercentage.THIRD50)+1);
                            nextSicknessPercentage = SicknessPayDaysPercentage.REST100;
                            if (accumulatedDays <= 0.5 )
                                workingDay.setPaymentPercentage(-100.0);
                            else
                                workingDay.setPaymentPercentage(-50.0);
                            accumulatedDays--;
                        }
                        break;
                    case REST100:

                        if (accumulatedDays<=0.5 || LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), workingDay.getDay()).getDayOfWeek() == DayOfWeek.SATURDAY)
                            workingDay.setPaymentPercentage(-100.0);
                        else
                            sicknessPayPercentage.put(SicknessPayDaysPercentage.REST100,sicknessPayPercentage.get(SicknessPayDaysPercentage.REST100)+1);
                            workingDay.setPaymentPercentage(100.0);
                        accumulatedDays--;
                        break;
                }
            } else
                nextSicknessPercentage = SicknessPayDaysPercentage.FIRST0;
        }
        return sicknessPayPercentage;
    }



    public SicknessPayDaysPercentage getLastMonthSicknessSickDaysPercentage (Integer  month, Integer year, SalaryRequest salaryRequest) {
        // this function calculates the last 3 days workType of the previous month and return the next payment for sickness in case the first of the month is sickness

        if (month == 1) {
            month = 12;
            year--;
        }else
            month--;

    Salary salaryPrevMonth = getSalary(month, year);
//sicknessReportsRepository.findAllById (Collections.singleton(month.toString() + year.toString()));

        if (salaryPrevMonth == null) {
            return SicknessPayDaysPercentage.FIRST0;
        }
        SicknessPayDaysPercentage nextSicknessPercentage = SicknessPayDaysPercentage.FIRST0;


        for (SalaryRequest.WorkingDays workingDay : salaryPrevMonth.getWorkingDays()) {
            if (workingDay.getWorkType().equals(WorkType.SICKNESS)) {
                nextSicknessPercentage = switch (nextSicknessPercentage) {
                    case FIRST0 -> SicknessPayDaysPercentage.SECOND50;
                    case SECOND50 -> SicknessPayDaysPercentage.THIRD50;
                    case THIRD50, REST100 -> SicknessPayDaysPercentage.REST100;
                };
            } else
                nextSicknessPercentage = SicknessPayDaysPercentage.FIRST0;
        }
        return nextSicknessPercentage;

    }

    public double getLastMonthSicknessAccumulated (Integer  month, Integer year) {
        // This function returns how many days accumulated last month after deductions if any.

        if (month == 1) {
            month = 12;
            year--;
        } else
            month--;

        Salary salaryPrevMonth = getSalary(month, year);
//sicknessReportsRepository.findAllById (Collections.singleton(month.toString() + year.toString()));

        if (salaryPrevMonth == null) {
            return 0;
        }
        return salaryPrevMonth.getSummaryWorkingDays().getSicknessReports().getSicknessLeftEndOfMonth();

    }

    private double calcSicknessPaidSum (SalaryRequest salaryRequest){
        double sicknessSum = 0;
        double daySalary = salaryParams.getBaseSalary()/salaryParams.getDaysInMonth();

        for(SalaryRequest.WorkingDays workingDays : salaryRequest.getWorkingDays()) {
            if (workingDays.getWorkType()==WorkType.SICKNESS && workingDays.getPaymentPercentage() <0) {
                    sicknessSum += daySalary * (workingDays.getPaymentPercentage()/100);
            }
        }
        return sicknessSum;

    }


/////////////////////////  HOLIDAY /////////////////////////////////////////////

    private int getHolidayDaysWorked (SalaryRequest salaryRequest, boolean isSaturday) {
        int intHolidayWorked = 0;



        if (isSaturday) {
            for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
                // check if saturday

                if (LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), workingDay.getDay()).getDayOfWeek() == DayOfWeek.SATURDAY)
                    if (WorkType.WORK.equals(workingDay.getWorkType())) {
                        workingDay.setPaymentFee(salaryParams.getHolidayDaySalary());
                        if (!isHoliday(workingDay.getDay(), salaryRequest.getMonth(), salaryRequest.getYear()))
                            intHolidayWorked++;
                    } else if (WorkType.HOLIDAY.equals(workingDay.getWorkType()) || WorkType.REST.equals(workingDay.getWorkType())) {
                        workingDay.setWorkType(WorkType.REST);
                        workingDay.setPaymentFee(salaryParams.getTravelVacationFee());
                    }
            }
        } else {

            for (SalaryRequest.WorkingDays workingDay : salaryRequest.getWorkingDays()) {
                if (isHoliday(workingDay.getDay(), salaryRequest.getMonth(), salaryRequest.getYear()))
                    if (WorkType.WORK.equals(workingDay.getWorkType())) {
                        workingDay.setPaymentFee(salaryParams.getHolidayDaySalary()); // holiday salary is fixed per Marina from Ayelet
                        //workingDay.setPaymentFee(getHolidayWorkDayPercentage());
                        intHolidayWorked++;
                    } else if (WorkType.REST.equals(workingDay.getWorkType())) {
                        workingDay.setWorkType(WorkType.HOLIDAY);
                        workingDay.setPaymentFee(salaryParams.getTravelVacationFee());
                    } else if (WorkType.HOLIDAY.equals(workingDay.getWorkType())) {
                        workingDay.setPaymentFee(salaryParams.getTravelVacationFee());
                    }
            }
        }

        return intHolidayWorked;
    }

    private boolean isHoliday (int day, int month, int year) {
        //check if the current year exists in holiday, if not, we use the previous year
        if (salaryParams.getHolidayDates().get(year) == null)
            year = year-1;

        for (Salary.HolidayDate holidayDate : salaryParams.getHolidayDates().get(year)) {
            int currentMonth;
            if (month == holidayDate.getMonth())
                if (day == holidayDate.getDay()) {
                    return true;
                }
        }

        return false;
    }

    private double getHolidayWorkDayPercentage() {
        double dayHolidaySalary =  Math.ceil((salaryParams.getBaseSalary()/salaryParams.getDaysInMonth()+salaryParams.getBaseSalary()/salaryParams.getHoursInMonth())*1.5);
        double daySalary = salaryParams.getBaseSalary()/salaryParams.getDaysInMonth();
        return dayHolidaySalary / daySalary * 100;

    }

    private double calcHolidayFee(double NumberOfDays, double daySalary) {
        // IMPORTANT: after Galit discussing with Marina  from Ayelet 24/11/24 she said that one day of working in holiday worth 401.25 regardless the salary so day salary is not the below calcualtion
        //return Math.ceil(NumberofDays * daySalary * (getHolidayWorkDayPercentage()/100));

        return daySalary * NumberOfDays;

    }

///////////////////////////// VACATION ///////////////////////////////////////////////////////////////
    // 11/27 - discussion with Galit this is what we have decided:
    //On January the employee gets all the days for the year and may use them from day one
    // The vacation days will be accumulated from year to year


private Vacation.VacationMonthlyUsed calcUsedVacationDays(SalaryRequest salaryRequest, double entitleYearlyVacation) {
    double totalVacationDays=0;
    double unpaidVacationDays=0;
    double paidVacationDays=0;

    for(SalaryRequest.WorkingDays workingDays : salaryRequest.getWorkingDays()) {
        DayOfWeek dayOfWeek = LocalDate.of(salaryRequest.getYear(), salaryRequest.getMonth(), workingDays.getDay()).getDayOfWeek();
        if (workingDays.getWorkType()==WorkType.VACATION ) {
            if (dayOfWeek == DayOfWeek.SATURDAY || (isHoliday(workingDays.getDay(),salaryRequest.getMonth(),salaryRequest.getYear())) )
                workingDays.setPaymentPercentage(0.0);
            else {


                if (entitleYearlyVacation > 0) {
                    workingDays.setPaymentPercentage(0.0); // get the salary as this percentage is multiplied by the day salary -
                    paidVacationDays++;
                } else {
                    workingDays.setPaymentPercentage(-100.0); // doesn't get paid - deducted as doesn't have enough days
                    unpaidVacationDays++;
                }
                entitleYearlyVacation--;
            }
            totalVacationDays++;
        }
    }
    boolean vacationUpdated = false;
    Vacation.VacationMonthlyUsed vacationMonthlyUsed = new Vacation.VacationMonthlyUsed();

    for (Vacation.VacationActual vacationActual : vacation.getVacationActual()) {
        if (Objects.equals(vacationActual.getYear(), salaryRequest.getYear())) {
            for (Vacation.VacationMonthlyUsed LocalVacationMonthlyUsed : vacationActual.getVacationUsed()) {
                if (Objects.equals(LocalVacationMonthlyUsed.getMonth(), salaryRequest.getMonth())) {
                    LocalVacationMonthlyUsed.setTotalVacationDays(totalVacationDays);
                    LocalVacationMonthlyUsed.setUnpaidVacationDays(unpaidVacationDays);
                    LocalVacationMonthlyUsed.setPaidVacationDays(paidVacationDays);
                    vacationConfiguration.saveVacation(vacation);
                    vacationMonthlyUsed = LocalVacationMonthlyUsed;
                    vacationUpdated = true;
                    break;
                }
            }
            if (!vacationUpdated) {
                vacationMonthlyUsed.setMonth(salaryRequest.getMonth());
                vacationMonthlyUsed.setTotalVacationDays(totalVacationDays);
                vacationMonthlyUsed.setUnpaidVacationDays(unpaidVacationDays);
                vacationMonthlyUsed.setPaidVacationDays(paidVacationDays);
                vacationActual.getVacationUsed().add(vacationMonthlyUsed);
                vacationConfiguration.saveVacation(vacation);
            }
            break;
        }
    }

    return vacationMonthlyUsed;
}

    private double calcVacationEntitledPrevYears (int year) { //2
        double totalVacationUsedUntilThisYear = 0;
        double totalYearlyEntitled = 0;

        List<Vacation.VacationActual> vacationActualSorted =  vacation.getVacationActual().stream().sorted((o1, o2)->o1.getYear().compareTo(o2.getYear())).toList();
        for (Vacation.VacationActual vacationActual : vacationActualSorted) {
            if (vacationActual.getYear() < year) {
                totalYearlyEntitled += vacationActual.getEntitled();
                for(Vacation.VacationMonthlyUsed vacationMonthlyUsed : vacationActual.getVacationUsed()){
                    totalVacationUsedUntilThisYear+=vacationMonthlyUsed.getPaidVacationDays();
                }
                break;
            }
        }
        if (totalYearlyEntitled - totalVacationUsedUntilThisYear <= 0)
            return 0.0;
        return totalYearlyEntitled - totalVacationUsedUntilThisYear;
    }

    private double calculateVacationEntitleThisYear(int year, Integer month) { //1
        LocalDate startWork = LocalDate.parse(salaryParams.getWorkStartDate());
        boolean startedThisYear = (year == startWork.getYear());
        boolean endedThisYear;
        LocalDate endWork;
        if (isDate (salaryParams.getWorkEndDate()) ) {
            endWork = LocalDate.parse(salaryParams.getWorkEndDate());
            endedThisYear = (year == endWork.getYear());
        } else {
            endWork = LocalDate.parse("2034-12-31");
            endedThisYear = false;
        }
        LocalDate janFirstThisYear = LocalDate.of(year,1,1);

        double periodToCalcVacation=0;
        double pastYearLeftOver = 0;
        double totalVacationUsedUntilNow= 0;
        double totalVacationDaysEntitled = 0;
        double days = 0;

        if (vacation.getVacationActual()!=null) {
             pastYearLeftOver = calcVacationEntitledPrevYears(year);
            for (Vacation.VacationActual actualVacation : vacation.getVacationActual()) {
                if (actualVacation.getYear() == year) {
                    if (endedThisYear) {
                        // recalcualte the entitled since the end working was just added and needs recalc
                        if (startedThisYear) {
                            periodToCalcVacation=ChronoUnit.DAYS.between(startWork, endWork);
                        } else {
                            periodToCalcVacation=ChronoUnit.DAYS.between(janFirstThisYear, endWork);
                        }
                        days = salaryParams.getDaysVacation().get(0).getDays();
                        actualVacation.setEntitled((double)Math.round((days/365.0*periodToCalcVacation)));
                    } else {
                        totalVacationDaysEntitled = actualVacation.getEntitled() + pastYearLeftOver;
                    }
                        for(Vacation.VacationMonthlyUsed vacationMonthlyUsed : actualVacation.getVacationUsed()){
                        if (vacationMonthlyUsed.getMonth() < month)
                            totalVacationUsedUntilNow+=vacationMonthlyUsed.getPaidVacationDays();
                    }
                    return totalVacationDaysEntitled - totalVacationUsedUntilNow;
                }
            }
        } else {
            vacation.setVacationActual(new ArrayList<>());
        }
        //if we got here, this year doesn't exist - we add it
        Vacation.VacationActual vacationActual = new Vacation.VacationActual();


        if (startedThisYear || endedThisYear) {
            if (startedThisYear) {
                periodToCalcVacation=ChronoUnit.DAYS.between(startWork, endWork);
            } else {
                periodToCalcVacation=ChronoUnit.DAYS.between(janFirstThisYear, endWork);
            }
            days = salaryParams.getDaysVacation().get(0).getDays();
            vacationActual.setEntitled((double)Math.round((days/365.0*periodToCalcVacation)));
        } else {

            // started to work prior to current year - entitled on january for the whole year
            int yearsWorked = Period.between(startWork, endWork).getYears() ;
            vacationActual.setEntitled((double)(salaryParams.getDaysVacation().get(yearsWorked-1).getDays()));
        }
        vacationActual.setYear(year);
        vacationActual.setVacationUsed(new ArrayList<Vacation.VacationMonthlyUsed>());
        vacation.getVacationActual().add(vacationActual);
        vacationRepository.save(vacation);

        return vacationActual.getEntitled()+ pastYearLeftOver;

    }










    private double calcVacationDeductSum(SalaryRequest salaryRequest, double daySalary) {//3
        double vacationDeductSum = 0;
        for(SalaryRequest.WorkingDays workingDays : salaryRequest.getWorkingDays()) {
            if (workingDays.getWorkType()==WorkType.VACATION) {
                    if (workingDays.getPaymentPercentage() < 0.0)
                        vacationDeductSum += daySalary * (workingDays.getPaymentPercentage()/100);
            }
        }
        return vacationDeductSum;
    }


///////////////////////////////////// BITUACH LEUMI ///////////////////////////////////////////////////////////


    private double calcBL(double grossSalary) {
        // need to calc total salary before reductions and pay 2% to BL. not on the employee

        return Double.parseDouble(new DecimalFormat("#.##").format(grossSalary* (salaryParams.getBituachLeumiPercentage()/100)));

    }


    ///////////////////////// PENSION ///////////////////////////////////////////

    private double calcPensionSum(SalaryRequest salaryRequest, double grossSalary) {
        // pension is available only 6 months after start working

        LocalDate startWork = LocalDate.parse(salaryParams.getWorkStartDate());
        LocalDate salaryDateToCalc = LocalDate.of(salaryRequest.getYear(),salaryRequest.getMonth(),startWork.getDayOfMonth());
        int periodBetween = Period.between(startWork, salaryDateToCalc).getMonths();
        if (periodBetween<7)
            return 0;
        else
            return Math.round(Double.parseDouble(new DecimalFormat("#.##").format(grossSalary * (salaryParams.getPensionPercentage()/100))));

    }

    ///////////////////////// COMPENSATION /////////////////////////////////////////////////

    private double calcCompensation (double grossSalary) {
        // one salary per year
        return Math.round(Double.parseDouble(new DecimalFormat("#.##").format(grossSalary/12)));
    }


    ///////////////////// HAVRAA ///////////////////////////////////////////////
    private double calcHavraaDays(int month, int year) {

        LocalDate startWork = LocalDate.parse(salaryParams.getWorkStartDate());
        if (month == startWork.getMonthValue()+1) {
            LocalDate salaryDateToCalc = LocalDate.of(year, month, 1); //8 of this salary's month in this year when start working date is the 8th of the month
            int yearsAtWork = Period.between(startWork, salaryDateToCalc).getYears();
            int daysHavraa = 0;

            if (yearsAtWork == 0)
                return 0;
            else {
                for (Salary.YearDays yearDays : salaryParams.getDaysHavraa()) {
                    if (yearDays.getYear() == yearsAtWork) {
                        daysHavraa = yearDays.getDays();
                        break;
                    }
                }
            }
            return Double.parseDouble(new DecimalFormat("#.##").format(daysHavraa));
        }else
            return 0;
    }

    public void close(int month, int year) {
        Salary salary = getSalary(month, year);
        salary.setClosed(true);
        salaryRepository.save(salary);
    }
}
