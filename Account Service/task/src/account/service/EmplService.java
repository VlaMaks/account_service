package account.service;

import account.entity.EmpSal;
import account.entity.User;
import account.exception.EmplSalExceptionReason;
import account.repository.EmplRepository;
import account.validation.EmplPayrollsValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class EmplService {
    private final EmplRepository emplRepository;
    private final UserService userService;
    private final Map<String, String> emplSalMap;

    private boolean isMonthCorrect(int month) {
        return month > 0 && month < 13;
    }

    private int strMonthConvertToInt(String period) {
        String numberMonth = period.split("-")[0];
        if ("0".equals(numberMonth.charAt(0))) {
            numberMonth = String.valueOf(numberMonth.charAt(1));
        }

        int month = Integer.parseInt(numberMonth);

        if (isMonthCorrect(month)) {
            return month;
        }
        throw new RuntimeException("incorrect period");
    }

    private String getMonth(int month) throws RuntimeException {
        List<String> monthList = List.of("January", "February", "March", "April",
                "May",       "June",    "July",     "August",
                "September", "October", "November", "December");

        return monthList.get(month - 1);
    }

    public EmplService(EmplRepository emplRepository, UserService userService) {
        this.emplRepository = emplRepository;
        this.emplSalMap = new HashMap<>();
        this.userService = userService;
    }

    public boolean changeUserSalary(EmpSal empSal, UserService userService) {
        EmplPayrollsValidator.ValidationResult result = EmplPayrollsValidator
                .isEmplUser(userService)
                .and(EmplPayrollsValidator.isSalaryNotNegative())
                .and(EmplPayrollsValidator.isPeriodCorrect())
                .apply(empSal);
        if (result != EmplPayrollsValidator.ValidationResult.SUCCESS) {
            throw new RuntimeException(EmplSalExceptionReason.getEmplSalExceptionReason(result));
        }
        emplRepository.save(empSal);
        return true;
    }

    @Transactional
    public boolean uploadPayrolls(List<EmpSal> payrolls, UserService userService) {
        StringBuilder messageException = new StringBuilder();
        boolean isException = false;
        int i = 0;
        for (EmpSal empSal : payrolls) {
            EmplPayrollsValidator.ValidationResult result = EmplPayrollsValidator
                    .isEmplUser(userService)
                    .and(EmplPayrollsValidator.isPeriodUniq(emplSalMap, this))
                    .and(EmplPayrollsValidator.isSalaryNotNegative())
                    .and(EmplPayrollsValidator.isPeriodCorrect())
                    .apply(empSal);
            if (result != EmplPayrollsValidator.ValidationResult.SUCCESS) {
                messageException.append("item[" + ++i + "] " + EmplSalExceptionReason.getEmplSalExceptionReason(result));
                isException = true;
            }
            emplSalMap.put(empSal.getEmployee(), empSal.getPeriod());
        }

        emplSalMap.clear();
        if (!isException) {
            for (EmpSal empSal : payrolls) {
                emplRepository.save(empSal);
            }
        } else {
            throw new RuntimeException(messageException.toString());
        }

        return true;
    }

    public List<Map<String, String>> findAllByEmployee(String employee) {
       List<EmpSal> empSals = emplRepository.findAllByEmployeeIgnoreCaseOrderByPeriodDesc(employee);
       //Collections.sort(empSals, (o1, o2) -> o2.getPeriod().compareTo(o1.getPeriod()));
       return getEmployeeInfoList(empSals);
    }

    public List<Map<String, String>> findAllByEmployeeWithPeriod(String employee, String period) {
        int month = strMonthConvertToInt(period);
        List<EmpSal> empSals = emplRepository.findAllByEmployeeAndPeriodIgnoreCase(employee, period);
        return getEmployeeInfoList(empSals);
    }

    private String getSalary(Long salary) {
        return String.format("%s dollar(s) %s cent(s)", salary / 100, salary % 100);
    }

    private List<Map<String, String>> getEmployeeInfoList(List<EmpSal> empSals) {
        if (empSals == null || empSals.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> resultList = new ArrayList<>();
        for (EmpSal empSal : empSals) {
            Map<String, String> resultMap = new LinkedHashMap<>();
            User user = userService.findUserByEmail(empSal.getEmployee()).get();
            resultMap.put("name", user.getName());
            resultMap.put("lastname", user.getLastName());
            resultMap.put("period", getMonth(strMonthConvertToInt(empSal.getPeriod())) + "-" + empSal.getPeriod().split("-")[1]);
            resultMap.put("salary", getSalary(empSal.getSalary()));
            resultList.add(resultMap);
        }
        return resultList;
    }

    public Optional<EmpSal> findByEmailAndPeriod(String employee, String period) {
        return emplRepository.findByEmployeeAndPeriodIgnoreCase(employee, period);
    }

}
