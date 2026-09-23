package com.genius.budgetmanager.service;

import com.genius.budgetmanager.model.enums.CampaignStatus;
import com.genius.budgetmanager.model.enums.Currency;
import com.genius.budgetmanager.model.enums.ExpenseCategory;
import com.genius.budgetmanager.model.BudgetSummary;
import com.genius.budgetmanager.model.Campaign;
import com.genius.budgetmanager.model.CreateCampaignRequest;
import com.genius.budgetmanager.model.Expense;
import com.genius.budgetmanager.model.GlobalBudgetSummary;
import com.genius.budgetmanager.repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository repository;

    public List<Campaign> getAllCampaigns() {
        return repository.findAll();
    }

    public List<Campaign> getCampaignsByStatus(String status) {
        CampaignStatus validStatus = parseStatus(status);
        return repository.findAll().stream()
                .filter(c -> c.getStatus().equalsIgnoreCase(validStatus.name()))
                .collect(Collectors.toList());
    }

    private CampaignStatus parseStatus(String status) {
        return java.util.Arrays.stream(CampaignStatus.values())
                .filter(validStatus -> validStatus.name().equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado inválido. Los valores permitidos son: active, paused, closed, draft"));
    }

    public Campaign getCampaignById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public List<Campaign> getCampaignsByClient(String clientName) {
        return repository.findByClient(clientName);
    }

    public BudgetSummary getBudgetSummary(Long campaignId) {
        Campaign campaign = getCampaignById(campaignId);

        BudgetSummary summary = new BudgetSummary();
        summary.setCampaignId(campaign.getId());
        summary.setCampaignName(campaign.getName());
        summary.setClient(campaign.getClient());
        summary.setTotalBudget(campaign.getBudget());
        summary.setSpent(campaign.getSpent());
        summary.setRemaining(campaign.getBudget() - campaign.getSpent());
        summary.setPercentageUsed(
                Math.round((campaign.getSpent() / campaign.getBudget()) * 10000.0) / 100.0
        );

        return summary;
    }

    public List<Expense> getExpensesByCampaign(Long campaignId) {
        getCampaignById(campaignId);
        return repository.findExpensesByCampaignId(campaignId);
    }

    public Expense addExpense(Long campaignId, Expense expense) {
        validateExpense(campaignId, expense);

        Campaign campaign = getCampaignById(campaignId);
        expense.setCampaignId(campaignId);
        campaign.setSpent(campaign.getSpent() + expense.getAmount());
        return repository.saveExpense(expense);
    }

    private void validateExpense(Long campaignId, Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("El gasto es obligatorio.");
        }

        getCampaignById(campaignId);

        if (expense.getDescription() == null || expense.getDescription().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }

        if (expense.getAmount() == null || expense.getAmount() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        }

        if (expense.getCategory() == null || expense.getCategory().isBlank()) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }

        if (!isValidExpenseCategory(expense.getCategory())) {
            throw new IllegalArgumentException("La categoría no es válida. Los valores permitidos son: ads_spend, creative, tools, agency_fee.");
        }

        if (!isValidDate(expense.getDate())) {
            throw new IllegalArgumentException("La fecha debe tener formato yyyy-MM-dd.");
        }
    }

    private boolean isValidExpenseCategory(String category) {
        return java.util.Arrays.stream(ExpenseCategory.values())
                .anyMatch(validCategory -> validCategory.name().equalsIgnoreCase(category));
    }

    public GlobalBudgetSummary getGlobalBudgetSummary() {
        List<Campaign> active = repository.findAll().stream()
                .filter(c -> "active".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        double totalBudget    = active.stream().mapToDouble(Campaign::getBudget).sum();
        double totalSpent     = active.stream().mapToDouble(Campaign::getSpent).sum();
        double totalAvailable = totalBudget - totalSpent;
        double pct = totalBudget > 0
                ? Math.round((totalSpent / totalBudget) * 10000.0) / 100.0
                : 0.0;

        GlobalBudgetSummary summary = new GlobalBudgetSummary();
        summary.setActiveCampaigns(active.size());
        summary.setTotalBudget(totalBudget);
        summary.setTotalSpent(totalSpent);
        summary.setTotalAvailable(totalAvailable);
        summary.setConsumptionPercentage(pct);
        return summary;
    }

    public Campaign updateBudget(Long campaignId, Double newBudget) {
        Campaign campaign = getCampaignById(campaignId);
        campaign.setSpent(0.0);
        campaign.setBudget(newBudget);
        return campaign;
    }

    //Actualiza el estado de la campaña
    public Campaign updateCampaignStatus(Long campaignId, CampaignStatus status) {
        Campaign campaign = getCampaignById(campaignId);
        campaign.setStatus(status.name());
        return campaign;
    }

    //Agrega una nueva campaña
    public Campaign addCampaign(CreateCampaignRequest request) {
        validateCampaign(request);

        Campaign campaign = new Campaign();
        campaign.setClient(request.getClient().trim());
        campaign.setName(request.getName().trim());
        campaign.setType(request.getType().trim());
        campaign.setStatus(parseStatus(request.getStatus()).name());
        campaign.setBudget(request.getBudget());
        campaign.setSpent(request.getSpent() == null ? 0.0 : request.getSpent());
        campaign.setCurrency(request.getCurrency().toLowerCase());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());

        return repository.addCampaign(campaign);
    }

    private void validateCampaign(CreateCampaignRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La campaña es obligatoria.");
        }

        if (request.getClient() == null || request.getClient().isBlank()) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }

        boolean clientExists = repository.findAll().stream()
                .anyMatch(existingCampaign -> existingCampaign.getClient()
                .equalsIgnoreCase(request.getClient().trim()));
        if (!clientExists) {
            throw new IllegalArgumentException("El cliente no existe.");
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }
        parseStatus(request.getStatus());

        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("El tipo es obligatorio.");
        }

        if (request.getBudget() == null || request.getBudget() < 1) {
            throw new IllegalArgumentException("El presupuesto debe ser mayor o igual a 1.");
        }

        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria.");
        }

        if (!isValidCurrency(request.getCurrency())) {
            String allowedValues = Arrays.stream(Currency.values())
                .map(Enum::name) // convierte cada enum a String
                .map(String::toLowerCase) // opcional: en minúsculas
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("La moneda no es válida. Los valores permitidos son: " + allowedValues);
        }

        if (!isValidDate(request.getStartDate()) || !isValidDate(request.getEndDate())) {      
            throw new IllegalArgumentException("Las fechas deben tener formato yyyy-MM-dd.");
        }

        LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }

    private boolean isValidDate(String date) {
        if (date == null || date.isBlank()) {
            return false;
        }

        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean isValidCurrency(String currency) {
        return java.util.Arrays.stream(Currency.values())
                .anyMatch(validCurrency -> validCurrency.name().equalsIgnoreCase(currency));
    }
}
