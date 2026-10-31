package Random;

import java.util.*;

class Population {
    int countpopulation = 0;
    int malepopulation = 0;
    int femalepopulation = 0;

    public void update(List<Person> people) {
        countpopulation = 0;
        malepopulation = 0;
        femalepopulation = 0;
        for (Person p : people) {
            if (!p.died) {
                countpopulation++;
                if ("Male".equalsIgnoreCase(p.gender)) {
                    malepopulation++;
                } else if ("Female".equalsIgnoreCase(p.gender)) {
                    femalepopulation++;
                }
            }
        }
    }
}

class Person {
    public static final Random RAND = new Random();

    public String name;
    public int age;
    public String gender;
    public int bornyear;
    public boolean ismarried = false;
    public String marriedwith = "";
    public Person spouse = null;
    public boolean died = false;

    // Childbearing tracking
    public int targetChildren = 0;
    public int childrenCount = 0;
    public int lastBirthYear = -1;

    // Newborn constructor: 50% chance Male / Female
    public Person(String name, int bornyear) {
        this.name = name;
        this.age = 0;
        this.bornyear = bornyear;
        // 50% chance of male and female at random
        this.gender = RAND.nextBoolean() ? "Male" : "Female";
    }

    // Explicit constructor (for the initial starting couple)
    public Person(String name, String gender, int age, int bornyear) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.bornyear = bornyear;
    }

    // Marriage logic: pairs two people and sets random 3-5 children target
    public void marry(Person partner, int currentYear) {
        this.ismarried = true;
        this.spouse = partner;
        this.marriedwith = partner.name;

        partner.ismarried = true;
        partner.spouse = this;
        partner.marriedwith = this.name;

        // Random target number of children between 3 and 5 (inclusive)
        int target = 3 + RAND.nextInt(3); // 3, 4, or 5
        this.targetChildren = target;
        partner.targetChildren = target;

        // Set last birth year so they can have a child starting after marriage
        this.lastBirthYear = currentYear - 1;
        partner.lastBirthYear = currentYear - 1;
    }

    // Check if eligible to have another child
    public boolean canHaveChild(int currentYear) {
        if (died || !ismarried || spouse == null || spouse.died) {
            return false;
        }
        // Only evaluate on the female spouse to avoid duplicate birth triggers
        if (!"Female".equalsIgnoreCase(this.gender)) {
            return false;
        }
        // Must haven't reached target children, and must have at least 1 year gap since last birth
        return (childrenCount < targetChildren) && ((currentYear - lastBirthYear) >= 1);
    }

    // Give birth to a child
    public Person giveBirth(int currentYear, String childName) {
        Person child = new Person(childName, currentYear);
        this.childrenCount++;
        this.lastBirthYear = currentYear;
        if (this.spouse != null) {
            this.spouse.childrenCount++;
            this.spouse.lastBirthYear = currentYear;
        }
        return child;
    }

    // Advance age by 1 year and check for death past age 75
    public void ageOneYear(int currentYear) {
        if (!died) {
            age = currentYear - bornyear;
            if (age > 75) {
                dies(currentYear);
            }
        }
    }

    // Handle death
    public void dies(int currentYear) {
        this.died = true;
        if (this.spouse != null && !this.spouse.died) {
            // Surviving spouse becomes unmarried and can remarry if eligible
            this.spouse.ismarried = false;
            this.spouse.marriedwith = "";
            this.spouse.spouse = null;
        }
        this.spouse = null;
        this.ismarried = false;
    }
}

class Report extends Population {
    int curryear;
    int birthsInInterval = 0;
    int deathsInInterval = 0;
    int marriagesInInterval = 0;

    Report(int curryear) {
        this.curryear = curryear;
    }

    void showreport() {
        System.out.println("--------------------------------------------------");
        System.out.println("           5-YEAR REPORT - YEAR " + curryear);
        System.out.println("--------------------------------------------------");
        System.out.println("  Living Population     : " + countpopulation);
        System.out.println("  Male Population       : " + malepopulation);
        System.out.println("  Female Population     : " + femalepopulation);
        System.out.println("  Marriages (last 5 yrs): " + marriagesInInterval);
        System.out.println("  Births (last 5 yrs)   : " + birthsInInterval);
        System.out.println("  Deaths (last 5 yrs)   : " + deathsInInterval);
        System.out.println("--------------------------------------------------\n");
    }
}

class Timeline {
    int year = 0;
    int durationInYears;
    int delayPerYearMs;
    List<Person> people = new ArrayList<>();

    private static final String[] MALE_NAMES = {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
        "Thomas", "Charles", "Daniel", "Matthew", "Anthony", "Mark", "Steven", "Paul"
    };
    private static final String[] FEMALE_NAMES = {
        "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica",
        "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Margaret", "Sandra", "Emily"
    };
    private int nameIndex = 1;

    Timeline(int durationInYears, int delayPerYearMs) {
        this.durationInYears = durationInYears;
        this.delayPerYearMs = delayPerYearMs;
    }

    String generateName(String gender) {
        String base = "Male".equalsIgnoreCase(gender)
            ? MALE_NAMES[Person.RAND.nextInt(MALE_NAMES.length)]
            : FEMALE_NAMES[Person.RAND.nextInt(FEMALE_NAMES.length)];
        return base + "_" + (nameIndex++);
    }

    void startSimulation() {
        System.out.println("==================================================");
        System.out.println("           STARTING POPULATION SIMULATION         ");
        System.out.println("           Timeline Duration: " + durationInYears + " Years");
        System.out.println("           Speed: 1 Year = 1 Second");
        System.out.println("           Reports will show every 5 years");
        System.out.println("==================================================\n");

        // 1. Initial 2 people: 1 male, 1 female (both age 19, ready for marriage)
        Person adam = new Person("Adam", "Male", 19, 0);
        Person eve = new Person("Eve", "Female", 19, 0);
        people.add(adam);
        people.add(eve);

        Report report = new Report(0);

        // Simulation loop year by year (1 second per year)
        for (year = 1; year <= durationInYears; year++) {
//            System.out.println(">>> [Year " + year + "] <<<");
            report.curryear = year;

            // Step A: Marriage - Match unmarried alive males and females with age > 18
            List<Person> eligibleMales = new ArrayList<>();
            List<Person> eligibleFemales = new ArrayList<>();

            for (Person p : people) {
                if (!p.died && !p.ismarried && p.age > 18) {
                    if ("Male".equalsIgnoreCase(p.gender)) {
                        eligibleMales.add(p);
                    } else if ("Female".equalsIgnoreCase(p.gender)) {
                        eligibleFemales.add(p);
                    }
                }
            }

            int marriagesCount = Math.min(eligibleMales.size(), eligibleFemales.size());
            for (int i = 0; i < marriagesCount; i++) {
                Person groom = eligibleMales.get(i);
                Person bride = eligibleFemales.get(i);
                groom.marry(bride, year);
                report.marriagesInInterval++;
//                System.out.println("  [Wedding] " + groom.name + " (Age " + groom.age + ") married "
//                        + bride.name + " (Age " + bride.age + ") - Planned children: " + groom.targetChildren);
            }

            // Step B: Childbirth - Married couples with < targetChildren and >= 1 year gap
            List<Person> newborns = new ArrayList<>();
            for (Person p : people) {
                if (p.canHaveChild(year)) {
                    boolean isBoy = Person.RAND.nextBoolean();
                    String gender = isBoy ? "Male" : "Female";
                    String name = generateName(gender);

                    Person child = p.giveBirth(year, name);
                    child.gender = gender;
                    newborns.add(child);
                    report.birthsInInterval++;

//                    System.out.println("  [Birth] " + p.name + " & " + p.spouse.name
//                            + " had a child: " + child.name + " (" + child.gender + ") [Child #"
//                            + p.childrenCount + "/" + p.targetChildren + "]");
                }
            }
            people.addAll(newborns);

            // Step C: Aging & Mortality (age > 75)
            for (Person p : people) {
                if (!p.died) {
                    boolean wasAlive = !p.died;
                    p.ageOneYear(year);
                    if (p.died && wasAlive) {
                        report.deathsInInterval++;
//                        System.out.println("  [Death] " + p.name + " (" + p.gender + ") passed away at age " + p.age);
                    }
                }
            }

            // Step D: Show Report after every 5 years (and at the final year)
            if (year % 5 == 0 || year == durationInYears) {
                report.update(people);
                report.showreport();
                // Reset interval statistics for the next 5-year period
                report.marriagesInInterval = 0;
                report.birthsInInterval = 0;
                report.deathsInInterval = 0;
            }

            // 1 Year takes 1 Second (1000ms delay)
            if (delayPerYearMs > 0) {
                try {
                    Thread.sleep(delayPerYearMs);
                } catch (InterruptedException ignored) {}
            }
        }

        System.out.println("==================================================");
        System.out.println("            SIMULATION COMPLETED");
        System.out.println("==================================================");
    }
}

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter duration of timeline in years: ");
        int durationInYears = 25; // default fallback
        try {
            if (scanner.hasNextInt()) {
                durationInYears = scanner.nextInt();
            }
        } catch (Exception e) {
            System.out.println("Invalid input. Defaulting to 25 years.");
        }

        // 1 year takes 1 second (1000 milliseconds) for live timeline
        int delayPerYearMs = 100;

        // Initialize and start timeline simulation
        Timeline simulation = new Timeline(durationInYears, delayPerYearMs);
        simulation.startSimulation();
        scanner.close();
    }
}
