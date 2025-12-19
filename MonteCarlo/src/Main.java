import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        double a = 0;
        double b = 100;
        int threads_num = 4;
        CalculatorMonteCarlo integrator = new CalculatorMonteCarlo(a, b, threads_num);
        try {
            double result = integrator.integrate();
            System.out.println("Result: " + result);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}