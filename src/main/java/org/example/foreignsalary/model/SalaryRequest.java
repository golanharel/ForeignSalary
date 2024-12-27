package org.example.foreignsalary.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class SalaryRequest {
    @Id
    private String id;
    private Integer month;
    private Integer year;
    private List<WorkingDays> workingDays;
    private String comments;



    @Data
    public static class WorkingDays {
        private Integer day;
        private WorkType workType;
        private Double paymentPercentage;
        private Double paymentFee;
    }



}
