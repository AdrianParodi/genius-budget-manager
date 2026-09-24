package com.genius.budgetmanager.controller;

import com.genius.budgetmanager.model.BudgetSummary;
import com.genius.budgetmanager.model.BudgetUpdateRequest;
import com.genius.budgetmanager.model.Campaign;
import com.genius.budgetmanager.model.CreateCampaignRequest;
import com.genius.budgetmanager.model.Expense;
import com.genius.budgetmanager.model.GlobalBudgetSummary;
import com.genius.budgetmanager.model.StatusUpdateRequest;
import com.genius.budgetmanager.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Gestion de campanas y presupuestos")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @GetMapping
    @Operation(summary = "Listar campanas", description = "Retorna todas las campanas. Acepta filtro opcional por status.")
    public ResponseEntity<List<Campaign>> getCampaigns(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(campaignService.getCampaignsByStatus(status));
        }
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen global de presupuesto", description = "Agrega KPIs de todas las campanas activas: total asignado, gastado, disponible y porcentaje de consumo.")
    public ResponseEntity<GlobalBudgetSummary> getGlobalBudgetSummary() {
        return ResponseEntity.ok(campaignService.getGlobalBudgetSummary());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener campana por ID")
    public ResponseEntity<Campaign> getCampaignById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Resumen de presupuesto", description = "Retorna el resumen de uso de presupuesto de la campana.")
    public ResponseEntity<BudgetSummary> getBudgetSummary(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getBudgetSummary(id));
    }

    @GetMapping("/{id}/expenses")
    @Operation(summary = "Listar gastos de una campana")
    public ResponseEntity<List<Expense>> getExpenses(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getExpensesByCampaign(id));
    }

    //Agregar una nueva campana
    @PostMapping
    @Operation(summary = "Dar de alta una nueva campana")
    public ResponseEntity<Campaign> addCampaign(@RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.addCampaign(request));
    }

    @PostMapping("/{id}/expenses")
    @Operation(summary = "Registrar un gasto en la campana")
    public ResponseEntity<Expense> addExpense(@PathVariable Long id, @RequestBody Expense expense) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.addExpense(id, expense));
    }

    @PutMapping("/{id}/budget")
    @Operation(summary = "Actualizar presupuesto de la campana")
    public ResponseEntity<Campaign> updateBudget(@PathVariable Long id, @RequestBody BudgetUpdateRequest request) {
        return ResponseEntity.ok(campaignService.updateBudget(id, request.getBudget()));
    }

    //Obterner campana por nombre de cliente
    @GetMapping("/client/{clientName}")
    @Operation(summary = "Obtener campana por nombre de cliente")
    public ResponseEntity<List<Campaign>> getCampaignByClientName(@PathVariable String clientName) {
        return ResponseEntity.ok(campaignService.getCampaignsByClient(clientName));
    }

    //Actualizar el estado de la campaña
    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado de una campana")
    public ResponseEntity<Campaign> updateStatus(
        @PathVariable Long id,
        @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(campaignService.updateCampaignStatus(id, request.getStatus()));
    }

    //Listar campanas por estado
    @GetMapping("/status/{status}")
    @Operation(summary = "Obtener campana por status")
    public ResponseEntity<List<Campaign>> getCampaignsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(campaignService.getCampaignsByStatus(status));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidEnum(HttpMessageNotReadableException ex) {
    return ResponseEntity
        .badRequest()
        .body("Estado inválido. Los valores permitidos son: active, paused, closed, draft");
    }
}
