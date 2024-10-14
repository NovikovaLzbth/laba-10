import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Account {
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition sufficientFunds = lock.newCondition();
    double depositAmount;

    Account(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
            System.out.printf("Пополнено на: %.2f, Новый баланс: %.2f%n", amount, balance);
            sufficientFunds.signalAll(); // Уведомляем ожидающие потоки о пополнении
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(double amount) throws InterruptedException {
        lock.lock();
        try {
            while (balance < amount) {
                System.out.printf("Недостаточно средств для снятия %.2f. Ожидание пополнения...%n", amount);
                sufficientFunds.await(); // Ждем, пока баланс не пополнится
            }
            balance -= amount;
            System.out.printf("Снято: %.2f, Остаток на балансе: %.2f%n", amount, balance);
        } finally {
            lock.unlock();
        }
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
}

class Depositor implements Runnable {
    private final Account account;

    public Depositor(Account account) {
        this.account = account;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 1008657; i++) {
                account.deposit(account.depositAmount);
                Thread.sleep(5); // Увеличена задержка между пополнениями
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Введите сумму, на которую пополнять: ");
        double depositSum = sc.nextDouble();

        System.out.print("Введите сумму, которую снимать: ");
        double withdrawalSum = sc.nextDouble();

        Account account = new Account(depositSum);
        Thread depositorThread = new Thread(new Depositor(account));
        depositorThread.start();

        try {
            // Снимаем деньги в цикле
            for (int i = 0; i < 5; i++) {
                account.withdraw(withdrawalSum); // Попробуем снять указанную сумму
                Thread.sleep(5); // Увеличена задержка между снятиями
            }
            depositorThread.interrupt();
            System.out.printf("Итоговый баланс: %.2f%n", account.getBalance());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
