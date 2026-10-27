package com.college.event.patterns.templatemethod;

import com.college.event.model.Event;

/**
 * Concrete Template Method implementation for Cultural Events.
 * Implements audio-visual checks, stage/acoustic provisioning, and performance rubrics.
 */
public class CulturalEventWorkflow extends EventWorkflowTemplate {

    @Override
    public String getWorkflowName() {
        return "Cultural Event Production Pipeline";
    }

    @Override
    public String getCategory() {
        return "CULTURAL";
    }

    @Override
    protected void setupDomainPrerequisites(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 3: AV & Green Room Preparation",
                "Assigned 4 backstage green rooms, inspected 32-channel acoustic mixing console and soundproof baffles.",
                true
        );
    }

    @Override
    protected void provisionEquipmentAndResources(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 4: Stage & Lighting Provisioning",
                "Rigged programmable stage LED spotlights, 8 wireless Sennheiser mics, fog generators, and high-definition stage backdrop.",
                true
        );
    }

    @Override
    protected void configureJudgingAndEvaluation(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 5: Performance Rubric & Audience Polling",
                "Configured jury scoring matrix (Choreography/Melody 40%, Synchronization 30%, Audience Engagement 30%) with QR voting.",
                true
        );
    }
}
