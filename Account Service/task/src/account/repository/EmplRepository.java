package account.repository;

import account.entity.EmpSal;
import account.entity.EmpSalId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmplRepository extends JpaRepository<EmpSal, EmpSalId> {
    Optional<EmpSal> findByEmployeeAndPeriodIgnoreCase(String employee, String period);
    List<EmpSal> findAllByEmployeeIgnoreCaseOrderByPeriodDesc(String employee);
    List<EmpSal> findAllByEmployeeAndPeriodIgnoreCase(String employee, String period);

}
