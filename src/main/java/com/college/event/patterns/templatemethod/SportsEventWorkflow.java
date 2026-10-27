package com.college.event.patterns.templatemethod;

import com.college.event.model.Event;

public class SportsEventWorkflow extends EventWorkflowTemplate {

    @Override
    public String getWorkflowName() {
        return "Sports Tournament Organization Pipeline";
    }

    @Override
    public String getCategory() {
        return "SPORTS";
    }

    @Override
    protected void setupDomainPrerequisites(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 3: Medical & Referee Briefing",
                "Stationed Red Cross first-aid ambulance team, verified certified match referees and line umpires.",
                true
        );
    }

    @Override
    protected void provisionEquipmentAndResources(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 4: Turf & Gear Readiness",
                "Marked tournament turf boundaries, inspected match balls, goal nets, hydration stands, and electronic scoreboard.",
                true
        );
    }

    @Override
    protected void configureJudgingAndEvaluation(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 5: Bracket Seedings & Fairplay Code",
                "Generated single-elimination tournament bracket, established match penalty rules, and instituted Golden Boot/MVP trophies.",
                true
        );
    }
}
