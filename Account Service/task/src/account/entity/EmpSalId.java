package account.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import javax.persistence.Column;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


public class EmpSalId implements Serializable {

    @Column(name = "employee", nullable = false)
    private String employee;

    @Column(name = "period", nullable = false)
    /*@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-yyyy")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)*/
    private String period;

    public EmpSalId() {
    }


    public EmpSalId(String employee, String period) {
        this.employee = employee;
        this.period = period;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getPeriod() {
        return period;
    }


    public void setPeriod(String period) {
        this.period = period;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmpSalId empSalId = (EmpSalId) o;
        return employee.equals(empSalId.employee) && period.equals(empSalId.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employee, period);
    }
}
