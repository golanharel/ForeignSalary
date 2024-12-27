package org.example.foreignsalary.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document(collection = "vacation")
public class Vacation {

    @Id
    private String id;
    private List<VacationActual> vacationActual;

    @Data
    public static class VacationMonthlyUsed {
        private Integer month;
        private Double totalVacationDays;
        private Double unpaidVacationDays;
        private Double paidVacationDays;
    }

    @Data
    public static class VacationActual {
        private Integer year;
        private Double entitled;
        List<VacationMonthlyUsed> vacationUsed;
    }
}
