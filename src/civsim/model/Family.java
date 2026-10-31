package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Family {
    private final int id;
    private final String surname;
    private final int foundedYear;
    private Person founder;
    private final List<Person> members;

    public Family(int id, String surname, int foundedYear, Person founder) {
        this.id = id;
        this.surname = surname;
        this.foundedYear = foundedYear;
        this.founder = founder;
        this.members = new ArrayList<>();
        if (founder != null) {
            this.members.add(founder);
        }
    }

    public void addMember(Person person) {
        if (person != null && !members.contains(person)) {
            members.add(person);
        }
    }

    public int getLivingMemberCount() {
        int count = 0;
        for (Person p : members) {
            if (p.isAlive()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasLivingMembers() {
        return getLivingMemberCount() > 0;
    }

    public void pruneDeceased() {
        this.members.removeIf(p -> !p.isAlive());
        if (this.founder != null && !this.founder.isAlive()) {
            this.founder = null;
        }
    }

    public int getId() { return id; }
    public String getSurname() { return surname; }
    public int getFoundedYear() { return foundedYear; }
    public Person getFounder() { return founder; }
    public void setFounder(Person founder) { this.founder = founder; }
    public List<Person> getMembers() { return Collections.unmodifiableList(members); }
}
