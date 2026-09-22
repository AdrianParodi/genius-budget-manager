package com.genius.budgetmanager.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCampaigns_returnsAllCampaigns() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

        @Test
        void getCampaignsByStatus_isCaseInsensitive() throws Exception {
                mockMvc.perform(get("/api/campaigns").param("status", "ACTIVE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        void getCampaignsByStatus_invalidStatus_returnsBadRequest() throws Exception {
                mockMvc.perform(get("/api/campaigns").param("status", "unknown"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value(
                                                "Estado inválido. Los valores permitidos son: active, paused, closed, draft"));
        }

    @Test
    void getCampaignById_existingId_returnsCampaign() throws Exception {
        mockMvc.perform(get("/api/campaigns/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Social Ads Q1 2026"))
                .andExpect(jsonPath("$.client").value("SuenoSimple"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void getCampaignById_nonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/api/campaigns/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getBudgetSummary_existingCampaign_returnsCorrectFields() throws Exception {
        mockMvc.perform(get("/api/campaigns/3/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(3))
                .andExpect(jsonPath("$.client").value("SuenoSimple"))
                .andExpect(jsonPath("$.totalBudget").value(120000.0))
                .andExpect(jsonPath("$.spent").value(67800.0))
                .andExpect(jsonPath("$.percentageUsed").exists());
    }

    @Test
    void getBudgetSummary_nonExistingCampaign_returns404() throws Exception {
        mockMvc.perform(get("/api/campaigns/999/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpenses_existingCampaign_returnsExpenseList() throws Exception {
        mockMvc.perform(get("/api/campaigns/3/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getExpenses_campaignWithNoExpenses_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/campaigns/6/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getExpenses_nonExistingCampaign_returns404() throws Exception {
        mockMvc.perform(get("/api/campaigns/999/expenses"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCampaign_validCampaign_returnsCreatedWithCorrectData() throws Exception {
        String body = """
                {
                  "id": 999,
                  "name": "Campaña Online Q3",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").value(not(999)))
                .andExpect(jsonPath("$.name").value("Campaña Online Q3"))
                .andExpect(jsonPath("$.client").value("SuenoSimple"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.budget").value(50000.0));
    }

    @Test
    void addCampaign_invalidClient_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Invalida",
                  "client": "Cliente Inexistente",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cliente no existe."));
    }

    @Test
    void addCampaign_invalidStatus_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Status",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "unknown",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Estado inválido. Los valores permitidos son: active, paused, closed, draft"));
    }

    @Test
    void addCampaign_budgetBelowOne_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Presupuesto",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 0.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El presupuesto debe ser mayor o igual a 1."));
    }

    @Test
    void addCampaign_missingType_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Sin Tipo",
                  "client": "SuenoSimple",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tipo es obligatorio."));
    }

    @Test
    void addCampaign_invalidDateFormat_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Fecha",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026/07/01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Las fechas deben tener formato yyyy-MM-dd."));
    }

    @Test
    void addCampaign_startDateAfterEndDate_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Orden Fecha",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "ARS",
                  "startDate": "2026-09-15",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha de inicio debe ser anterior a la fecha de fin."));
    }

    @Test
    void addCampaign_invalidCurrency_returnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Campaña Moneda",
                  "client": "SuenoSimple",
                  "type": "search_ads",
                  "status": "draft",
                  "budget": 50000.0,
                  "spent": 0.0,
                  "currency": "EUR",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La moneda no es válida. Los valores permitidos son: ars, usd."));
    }

    @Test
    void addExpense_validExpense_returnsCreatedWithCorrectData() throws Exception {
        String body = """
                {
                  "description": "TikTok Ads Abril",
                  "amount": 18000.0,
                  "category": "ads_spend",
                  "date": "2026-04-10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/2/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(2))
                .andExpect(jsonPath("$.description").value("TikTok Ads Abril"))
                .andExpect(jsonPath("$.amount").value(18000.0))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void addExpense_emptyDescription_returnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "",
                  "amount": 18000.0,
                  "category": "ads_spend",
                  "date": "2026-04-10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/2/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La descripción es obligatoria."));
    }

    @Test
    void addExpense_invalidAmount_returnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Gasto inválido",
                  "amount": 0.0,
                  "category": "ads_spend",
                  "date": "2026-04-10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/2/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El monto debe ser mayor a 0."));
    }

    @Test
    void addExpense_invalidCategory_returnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Categoría invalida",
                  "amount": 1500.0,
                  "category": "marketing",
                  "date": "2026-04-10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/2/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La categoría no es válida. Los valores permitidos son: ads_spend, creative, tools, agency_fee."));
    }

    @Test
    void addExpense_invalidDateFormat_returnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Fecha inválida",
                  "amount": 1200.0,
                  "category": "creative",
                  "date": "2026/04/10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/2/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha debe tener formato yyyy-MM-dd."));
    }

    @Test
    void addExpense_nonExistingCampaign_returns404() throws Exception {
        String body = """
                {
                  "description": "Gasto sin campana",
                  "amount": 5000.0,
                  "category": "tools",
                  "date": "2026-04-10"
                }
                """;

        mockMvc.perform(post("/api/campaigns/999/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBudget_validAmount_returnsUpdatedCampaign() throws Exception {
        mockMvc.perform(put("/api/campaigns/6/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 75000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.budget").value(75000.0));
    }

    @Test
    void updateBudget_nonExistingCampaign_returns404() throws Exception {
        mockMvc.perform(put("/api/campaigns/999/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 50000.0}"))
                .andExpect(status().isNotFound());
    }
}
