package org.example.foreignsalary.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
@Data
@Builder
@Document(collection = "sicknessReports")
public class SicknessReports {
    //private Map<LocalDate,Integer> sicknessReports;
    @Id
    private String id;
    private double sicknessUsed;
    private double sicknessLeftEndOfMonth;
    private Double sicknessEntitledBeginOfMonth;
    private Double sicknessDaysPaidSum;
    private List<SalaryRequest.WorkingDays> prevMonthLastDaysWorkType;
    private Map<SicknessPayDaysPercentage, Integer> sicknessPayDaysPercentageSummary;//private Double sicknessLeftEndOfMonth;

    //private List<LocalDate>SicknessReports;

}
