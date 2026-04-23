package ge.tbcacademy;

import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class User implements Serializable {
    private static final long serialVersionUID = 1L;

    String username;
    String password;
    int theoryPoints;
    int practicePoints;
    int theoryAttempts;
    int practiceAttempts;
    LocalDate thirdFailDate; // null თუ არ არის მითითებული
    LocalDate registrationDate;
    LocalDate lastActivityDate;

    User(String username, String password) {
        this.username = username;
        this.password = password;
        this.theoryPoints = 0;
        this.practicePoints = 0;
        this.theoryAttempts = 0;
        this.practiceAttempts = 0;
        this.thirdFailDate = null;
        this.registrationDate = LocalDate.now();
        this.lastActivityDate = LocalDate.now();
    }

    boolean isLockedByThirdFail() {
        if (thirdFailDate == null) return false;
        long days = ChronoUnit.DAYS.between(thirdFailDate, LocalDate.now());
        return days < 365;
    }

    void resetAfterYear() {
        this.thirdFailDate = null;
        this.theoryAttempts = 0;
        this.practiceAttempts = 0;
        this.lastActivityDate = LocalDate.now();
    }

    void touchActivity() {
        this.lastActivityDate = LocalDate.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", theoryPoints=" + theoryPoints +
                ", practicePoints=" + practicePoints +
                ", theoryAttempts=" + theoryAttempts +
                ", practiceAttempts=" + practiceAttempts +
                ", thirdFailDate=" + thirdFailDate +
                ", registrationDate=" + registrationDate +
                ", lastActivityDate=" + lastActivityDate +
                '}';
    }
}

public class DrivingLicenseSystem {
    private final Map<String, User> users = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private final File storageFile = new File("users.dat");

    public static void main(String[] args) {
        DrivingLicenseSystem app = new DrivingLicenseSystem();
        app.run();
    }

    public DrivingLicenseSystem() {
        loadUsers();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveUsers));
    }

    private void run() {
        System.out.println("სტარტი");
        while (true) {
            System.out.println("\n--- ავტოსკოლის სისტემა: ავტორიზაცია ---");
            System.out.print("მომხმარებლის სახელი: ");
            String username = scanner.nextLine().trim();
            System.out.print("პაროლი: ");
            String password = scanner.nextLine().trim();

            User user = users.get(username);
            if (user != null && user.password.equals(password)) {
                System.out.println("მომხმარებელი მოიძებნა.");
                System.out.print("გსურთ ახალი მომხმარებლის რეგისტრაცია? (დიახ/არა): ");
                String wantRegister = scanner.nextLine().trim().toLowerCase();
                if (wantRegister.equals("დიახ") || wantRegister.equals("y") || wantRegister.equals("yes")) {
                    createNewUserFlow();
                } else {
                    handleUserFlow(user);
                }
            } else {
                System.out.println("მომხმარებელი არ მოიძებნა.");
                System.out.print("გსურთ ახალი მომხმარებლის შექმნა? (დიახ/არა): ");
                String create = scanner.nextLine().trim().toLowerCase();
                if (create.equals("დიახ") || create.equals("y") || create.equals("yes")) {
                    createNewUser(username, password);
                } else {
                    System.out.println("გასვლა");
                    saveUsers();
                    break;
                }
            }
        }
    }

    private void createNewUserFlow() {
        System.out.print("შეიყვანეთ ახალი მომხმარებლის სახელი: ");
        String newUsername = scanner.nextLine().trim();
        System.out.print("შეიყვანეთ ახალი პაროლი: ");
        String newPassword = scanner.nextLine().trim();
        createNewUser(newUsername, newPassword);
    }

    private void createNewUser(String username, String password) {
        if (users.containsKey(username)) {
            System.out.println("მომხმარებლის სახელი უკვე არსებობს. რეგისტრაცია გაუქმდა.");
            return;
        }
        User newUser = new User(username, password);
        users.put(username, newUser);
        System.out.println("ახალი მომხმარებელი შექმნილია. ქულები და მცდელობები ნულდება.");
        saveUsers();
        handleUserFlow(newUser);
    }

    private void handleUserFlow(User user) {
        user.touchActivity();
        saveUsers();
        while (true) {
            // განახლება: თუ ერთი წელი გავიდა, დააბრუნე მცდელობები
            if (user.thirdFailDate != null) {
                long days = ChronoUnit.DAYS.between(user.thirdFailDate, LocalDate.now());
                if (days >= 365) {
                    user.resetAfterYear();
                    System.out.println("ერთი წელი გავიდა მესამე წარუმატებლობის შემდეგ. მცდელობები განახლდა.");
                    saveUsers();
                }
            }

            // ნაბიჯი 9: თეორიის შემოწმება
            if (user.theoryPoints >= 15) {
                // ნაბიჯი 10: პრაქტიკის შემოწმება
                if (user.practicePoints >= 20) {
                    // ნაბიჯი 27: ჯამური ქულა
                    if (user.theoryPoints + user.practicePoints >= 55) {
                        System.out.println("გილოცავთ! თქვენ მიიღეთ საკმარისი ქულა მართვის მოწმობის მისაღებად.");
                        System.out.println("გასვლა");
                        user.touchActivity();
                        saveUsers();
                        return;
                    } else {
                        // ნაბიჯი 19: სურვილი გადაბარების
                        if (!askYesNo("გსურთ თეორიის ხელახლა ჩაბარება? (დიახ/არა): ")) {
                            if (!askYesNo("გსურთ პრაქტიკის ხელახლა ჩაბარება? (დიახ/არა): ")) {
                                System.out.println("გასვლა");
                                user.touchActivity();
                                saveUsers();
                                return;
                            } else {
                                practiceExamFlow(user);
                            }
                        } else {
                            theoryExamFlow(user);
                        }
                    }
                } else {
                    // practicePoints < 20 -> ნაბიჯი 15
                    if (user.practiceAttempts < 3) {
                        practiceExamFlow(user);
                    } else {
                        // ნაბიჯი 22
                        handleThirdFailDate(user);
                        // ბრუნდება ნაბიჯ 9
                    }
                }
            } else {
                // theoryPoints < 15 -> ნაბიჯი 11
                if (user.theoryAttempts < 3) {
                    theoryExamFlow(user);
                } else {
                    // ნაბიჯი 22
                    handleThirdFailDate(user);
                    // ბრუნდება ნაბიჯ 9
                }
            }
        }
    }

    private void theoryExamFlow(User user) {
        if (user.isLockedByThirdFail()) {
            System.out.println("თქვენ დაბლოკილი ხართ გამოცდებზე ერთი წლის განმავლობაში: " + user.thirdFailDate);
            return;
        }
        System.out.println("თეორიის გამოცდა. მიმდინარე საუკეთესო ქულა: " + user.theoryPoints + ". გამოყენებული მცდელობები: " + user.theoryAttempts);
        int result = readExamResult("შეიყვანეთ თეორიის ქულა (0-50): ");
        if (result > user.theoryPoints) {
            user.theoryPoints = result;
            System.out.println("ახალი საუკეთესო თეორიის ქულა შენახულია: " + user.theoryPoints);
        } else {
            System.out.println("ქულა არ აღემატება წინა საუკეთესო შედეგს. საუკეთესო რჩება: " + user.theoryPoints);
        }
        user.theoryAttempts++;
        user.touchActivity();
        System.out.println("თეორიის მცდელობები: " + user.theoryAttempts);
        // თუ თეორიის მცდელობები >=3 და არ არის ჩაბარებული, დააყენე thirdFailDate
        if (user.theoryAttempts >= 3 && user.theoryPoints < 15) {
            if (user.thirdFailDate == null) {
                user.thirdFailDate = LocalDate.now();
                System.out.println("მესამე წარუმატებლობის თარიღი დარეგისტრირდა: " + user.thirdFailDate);
            }
        }
        saveUsers();
    }

    private void practiceExamFlow(User user) {
        if (user.theoryPoints < 15) {
            System.out.println("პრაქტიკის გამოცდაზე გასვლა შეუძლებელია სანამ თეორია არ იქნება მინიმუმ 15 ქულა.");
            return;
        }
        if (user.isLockedByThirdFail()) {
            System.out.println("თქვენ დაბლოკილი ხართ გამოცდებზე ერთი წლის განმავლობაში: " + user.thirdFailDate);
            return;
        }
        System.out.println("პრაქტიკის გამოცდა. მიმდინარე საუკეთესო ქულა: " + user.practicePoints + ". გამოყენებული მცდელობები: " + user.practiceAttempts);
        int result = readExamResult("შეიყვანეთ პრაქტიკის ქულა (0-50): ");
        if (result > user.practicePoints) {
            user.practicePoints = result;
            System.out.println("ახალი საუკეთესო პრაქტიკის ქულა შენახულია: " + user.practicePoints);
        } else {
            System.out.println("ქულა არ აღემატება წინა საუკეთესო შედეგს. საუკეთესო რჩება: " + user.practicePoints);
        }
        user.practiceAttempts++;
        user.touchActivity();
        System.out.println("პრაქტიკის მცდელობები: " + user.practiceAttempts);
        // თუ პრაქტიკის მცდელობები >=3 და არ არის ჩაბარებული, დააყენე thirdFailDate
        if (user.practiceAttempts >= 3 && user.practicePoints < 20) {
            if (user.thirdFailDate == null) {
                user.thirdFailDate = LocalDate.now();
                System.out.println("მესამე წარუმატებლობის თარიღი დარეგისტრირდა: " + user.thirdFailDate);
            }
        }
        saveUsers();
    }

    private void handleThirdFailDate(User user) {
        if (user.thirdFailDate == null) {
            user.thirdFailDate = LocalDate.now();
            System.out.println("მესამე წარუმატებლობის თარიღი დარეგისტრირდა: " + user.thirdFailDate);
            saveUsers();
        } else {
            long days = ChronoUnit.DAYS.between(user.thirdFailDate, LocalDate.now());
            if (days >= 365) {
                user.resetAfterYear();
                System.out.println("ერთი წელი გავიდა მესამე წარუმატებლობის შემდეგ. მცდელობები განახლდა.");
                saveUsers();
            } else {
                System.out.println("ერთი წელი არ არის გასული. დარჩენილი დღეები: " + (365 - days));
            }
        }
    }

    private boolean askYesNo(String prompt) {
        System.out.print(prompt);
        String ans = scanner.nextLine().trim().toLowerCase();
        return ans.equals("დიახ") || ans.equals("y") || ans.equals("yes");
    }

    private int readExamResult(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val < 0 || val > 50) {
                    System.out.println("ქულა უნდა იყოს 0-დან 50-მდე.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("გთხოვთ შეიყვანოთ სწორი მთელი რიცხვი.");
            }
        }
    }

    // Persistence methods
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        if (!storageFile.exists()) {
            System.out.println("მომხმარებელთა მონაცემები არ მოიძებნა. სისტემა იწყება ახალი მონაცემებით.");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                Map<?, ?> loaded = (Map<?, ?>) obj;
                for (Map.Entry<?, ?> entry : loaded.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof User) {
                        users.put((String) entry.getKey(), (User) entry.getValue());
                    }
                }
                System.out.println("ჩატვირთულია " + users.size() + " მომხმარებელი.");
            } else {
                System.out.println("შენახული ფაილის ფორმატი არასწორია. იწყება ცარიელი სია.");
            }
        } catch (Exception e) {
            System.out.println("მომხმარებელთა ჩატვირთვა ვერ მოხერხდა: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.out.println("მომხმარებელთა შენახვა ვერ მოხერხდა: " + e.getMessage());
        }
    }
}
