package civsim.economy;

public class Bank {
    private double totalDeposits;
    private double totalLoansOutstanding;
    private final double interestRate;

    public Bank(double interestRate) {
        this.totalDeposits = 0.0;
        this.totalLoansOutstanding = 0.0;
        this.interestRate = interestRate;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.totalDeposits += amount;
        }
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && this.totalDeposits >= amount) {
            this.totalDeposits -= amount;
            return true;
        }
        return false;
    }

    public boolean issueLoan(double amount) {
        if (amount > 0) {
            this.totalLoansOutstanding += amount;
            return true;
        }
        return false;
    }

    public void repayLoan(double amount) {
        if (amount > 0) {
            this.totalLoansOutstanding = Math.max(0.0, this.totalLoansOutstanding - amount);
        }
    }

    public double getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(double totalDeposits) { this.totalDeposits = Math.max(0.0, totalDeposits); }
    public double getTotalLoansOutstanding() { return totalLoansOutstanding; }
    public void setTotalLoansOutstanding(double totalLoansOutstanding) { this.totalLoansOutstanding = Math.max(0.0, totalLoansOutstanding); }
    public double getInterestRate() { return interestRate; }
}
