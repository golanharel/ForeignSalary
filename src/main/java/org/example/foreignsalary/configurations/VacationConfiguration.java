package org.example.foreignsalary.configurations;

import org.example.foreignsalary.model.Vacation;
import org.example.foreignsalary.repositories.VacationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VacationConfiguration {
    private final VacationRepository vacationRepository;

    @Autowired
    public VacationConfiguration(VacationRepository vacationRepository) {
        this.vacationRepository = vacationRepository;
    }

    @Bean
    public Vacation getVacationRepository() {
        if (vacationRepository.findAll().isEmpty())
            return Vacation.builder().build();
        return vacationRepository.findAll().get(0);
    }
    public Vacation saveVacation(Vacation vacation) {
        return vacationRepository.save(vacation);
    }
}
