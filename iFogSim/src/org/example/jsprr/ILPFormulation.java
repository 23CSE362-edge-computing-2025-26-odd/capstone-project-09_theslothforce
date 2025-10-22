package org.example.jsprr;

import java.util.*;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.PointValuePair;

public class ILPFormulation {

    public double[][] solveLP(List<BaseStationDevice> bases, List<ServiceModule> services) throws Exception {
        int m = services.size();
        int n = bases.size();
        int totalVars = m * n;

        // Objective function coefficients
        double[] objective = new double[totalVars];
        for (int s = 0; s < m; s++) {
            ServiceModule svc = services.get(s);
            for (int b = 0; b < n; b++) {
                BaseStationDevice base = bases.get(b);
                double penalty = base instanceof CloudDevice ? 5.0 : 0.0; // high penalty for Cloud
                double cost = (svc.getStorageReq() / Math.max(1.0, base.getStorageCapacity()))
                            + (svc.getComputeReq() / Math.max(1.0, base.getComputeCapacity()))
                            + penalty;
                objective[s * n + b] = cost;
            }
        }

        Collection<LinearConstraint> constraints = new ArrayList<>();

        // Each service must be assigned to exactly one base
        for (int s = 0; s < m; s++) {
            double[] coeff = new double[totalVars];
            for (int b = 0; b < n; b++) coeff[s * n + b] = 1.0;
            constraints.add(new LinearConstraint(coeff, Relationship.EQ, 1.0));
        }

        // Capacity constraints
        addCapacity(constraints, services, bases, totalVars, "storage");
        addCapacity(constraints, services, bases, totalVars, "compute");
        addCapacity(constraints, services, bases, totalVars, "uplink");
        addCapacity(constraints, services, bases, totalVars, "downlink");

        // Variable bounds 0 <= x <= 1
        for (int i = 0; i < totalVars; i++) {
            double[] coeff = new double[totalVars];
            coeff[i] = 1.0;
            constraints.add(new LinearConstraint(coeff, Relationship.GEQ, 0.0));
            constraints.add(new LinearConstraint(coeff, Relationship.LEQ, 1.0));
        }

        SimplexSolver solver = new SimplexSolver();
        PointValuePair sol = solver.optimize(
                new LinearObjectiveFunction(objective, 0),
                new LinearConstraintSet(constraints),
                GoalType.MINIMIZE,
                new NonNegativeConstraint(true)
        );

        double[] raw = sol.getPoint();
        double[][] x = new double[m][n];
        for (int s = 0; s < m; s++)
            for (int b = 0; b < n; b++)
                x[s][b] = Math.max(0, Math.min(1, raw[s * n + b]));

        return x;
    }

    private void addCapacity(Collection<LinearConstraint> cons,
                             List<ServiceModule> services,
                             List<BaseStationDevice> bases,
                             int totalVars, String type) {
        int m = services.size();
        int n = bases.size();

        for (int b = 0; b < n; b++) {
            double[] coeff = new double[totalVars];
            for (int s = 0; s < m; s++) {
                ServiceModule svc = services.get(s);
                switch (type) {
                    case "storage" -> coeff[s * n + b] = svc.getStorageReq();
                    case "compute" -> coeff[s * n + b] = svc.getComputeReq();
                    case "uplink" -> coeff[s * n + b] = svc.getUplinkReq();
                    case "downlink" -> coeff[s * n + b] = svc.getDownlinkReq();
                }
            }
            double cap = switch (type) {
                case "storage" -> bases.get(b).getStorageCapacity() - bases.get(b).getStorageUsed();
                case "compute" -> bases.get(b).getComputeCapacity() - bases.get(b).getComputeUsed();
                case "uplink" -> bases.get(b).getUplinkCapacity() - bases.get(b).getUplinkUsed();
                case "downlink" -> bases.get(b).getDownlinkCapacity() - bases.get(b).getDownlinkUsed();
                default -> 0;
            };
            cons.add(new LinearConstraint(coeff, Relationship.LEQ, Math.max(0, cap)));
        }
    }
}

