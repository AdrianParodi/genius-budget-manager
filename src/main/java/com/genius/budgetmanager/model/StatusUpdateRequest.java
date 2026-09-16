package com.genius.budgetmanager.model;

import com.genius.budgetmanager.model.enums.CampaignStatus;

public class StatusUpdateRequest {
    private CampaignStatus status;

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }
}