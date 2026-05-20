package model.bean;

import java.io.Serializable;
import java.util.List;

public class AssignmentResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Assignment> assignments;
    private List<Supervisor> supervisors;

    public AssignmentResult() {
    }

    public AssignmentResult(List<Assignment> assignments, List<Supervisor> supervisors) {
        this.assignments = assignments;
        this.supervisors = supervisors;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public List<Supervisor> getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(List<Supervisor> supervisors) {
        this.supervisors = supervisors;
    }

    @Override
    public String toString() {
        return "AssignmentResult{" +
                "assignments=" + assignments.size() +
                ", supervisors=" + supervisors.size() +
                '}';
    }
}
