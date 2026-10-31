package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.systems.OrganizationSystem;

import java.util.*;

public class Test200YearsWithOrganizations {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   RUNNING 200-YEAR INTEGRATION SIMULATION WITH ORGANIZATIONS & PROGRESSION");
        System.out.println("================================================================================");

        SimulationConfig config = SimulationConfig.createDefault();

        ProgressionConfig progConfig = ProgressionConfig.createDefault();

        SimulationEngine engine = new SimulationEngine(config, progConfig);
        engine.initializeWorld();

        OrganizationSystem orgSys = engine.getOrganizationSystem();

        // Seed initial mundane clans in Year 5
        boolean seededYear5Clans = false;
        boolean seededAwakenedOrgs = false;

        for (int year = 1; year <= 200; year++) {
            for (int month = 0; month < 12; month++) {
                engine.processMonthlyTick();

                // Dynamic mundane clan founding at Year 5
                if (year == 5 && month == 0 && !seededYear5Clans) {
                    seededYear5Clans = true;
                    List<Person> people = engine.getAllPeople();
                    if (people.size() >= 2) {
                        Person founder1 = people.get(0);
                        Person founder2 = people.get(1);

                        orgSys.foundOrganization(new OrganizationFoundingEvent(
                                "FOUND_CLAN_A", "CLAN_A", "Sunstone Clan",
                                OrganizationType.CLAN, OrganizationAlignment.NEUTRAL,
                                String.valueOf(founder1.getId()), Collections.emptyList(),
                                OrganizationPolicyProfile.createDefault(OrganizationType.CLAN), 5, 0
                        ), people, config, null);

                        orgSys.foundOrganization(new OrganizationFoundingEvent(
                                "FOUND_GUARD_B", "GUARD_B", "Iron Bastion Guard",
                                OrganizationType.SETTLEMENT_GUARD, OrganizationAlignment.NEUTRAL,
                                String.valueOf(founder2.getId()), Collections.emptyList(),
                                OrganizationPolicyProfile.createDefault(OrganizationType.SETTLEMENT_GUARD), 5, 0
                        ), people, config, null);
                    }
                }

                // Dynamic supernatural organization founding post Year 50
                if (year >= 52 && !seededAwakenedOrgs) {
                    List<Person> people = engine.getAllPeople();
                    Person awakenedFounder = null;
                    Person darkFounder = null;

                    for (Person p : people) {
                        if (p.isAlive() && p.isAwakened()) {
                            if (p.getAetherProfile().isDarkAwakened() && darkFounder == null) {
                                darkFounder = p;
                            } else if (!p.getAetherProfile().isDarkAwakened() && awakenedFounder == null) {
                                awakenedFounder = p;
                            }
                        }
                    }

                    if (awakenedFounder != null) {
                        orgSys.foundOrganization(new OrganizationFoundingEvent(
                                "FOUND_ORDER_52", "ORDER_SOLAR", "Order of the Sun",
                                OrganizationType.AWAKENED_ORDER, OrganizationAlignment.AETHERIC_ALIGNED,
                                String.valueOf(awakenedFounder.getId()), Collections.emptyList(),
                                OrganizationPolicyProfile.createDefaultAwakenedOrderPolicy(), year, month
                        ), people, config, null);
                    }

                    if (darkFounder != null) {
                        orgSys.foundOrganization(new OrganizationFoundingEvent(
                                "FOUND_COVEN_52", "COVEN_SHADOW", "Shadow Coven",
                                OrganizationType.DARK_COVEN, OrganizationAlignment.DARK_ALIGNED,
                                String.valueOf(darkFounder.getId()), Collections.emptyList(),
                                OrganizationPolicyProfile.createDefaultDarkCovenPolicy(), year, month
                        ), people, config, null);
                    }

                    if (awakenedFounder != null || darkFounder != null) {
                        seededAwakenedOrgs = true;
                    }
                }
            }

            if (year % 25 == 0 || year == 50 || year == 51) {
                List<AnnualReport> reports = engine.getAnnualReports();
                AnnualReport rep = reports.get(reports.size() - 1);
                System.out.println(String.format("Year %3d: Living Pop: %d | %s | %s",
                        year, rep.getTotalLivingPopulation(),
                        rep.getOrganizationSummary() != null ? rep.getOrganizationSummary() : "No Orgs",
                        rep.getCombatSummary() != null ? rep.getCombatSummary() : "No Combat"
                ));
            }
        }

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("200-YEAR SIMULATION COMPLETED SUCCESSFULLY WITHOUT ERRORS!");
        System.out.println("Total Active Organizations: " + orgSys.getActiveOrganizations().size());
        System.out.println("Total Dissolved Organizations: " + orgSys.getDissolvedOrganizations().size());
        System.out.println("--------------------------------------------------------------------------------");
    }
}
