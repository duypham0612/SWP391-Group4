package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.model.Ingredient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngredientValidationTest {

    @Test
    void normalizesValidPurchaseConversion() {
        Ingredient ingredient = rawIngredient();
        ingredient.setName("  Bánh   chuối lát  ");
        ingredient.setPurchaseUnitName("  hộp  ");
        ingredient.setPurchaseFactorToBase(new BigDecimal("12"));

        IngredientService.validateAndNormalizeFields(ingredient);

        assertEquals("Bánh chuối lát", ingredient.getName());
        assertEquals("hộp", ingredient.getPurchaseUnitName());
        assertEquals(new BigDecimal("12"), ingredient.getPurchaseFactorToBase());
    }

    @Test
    void requiresPurchaseUnitAndQuantityTogether() {
        Ingredient ingredient = rawIngredient();
        ingredient.setPurchaseUnitName("hộp");

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    @Test
    void rejectsMarkupInPurchaseUnit() {
        Ingredient ingredient = rawIngredient();
        ingredient.setPurchaseUnitName("<script>");
        ingredient.setPurchaseFactorToBase(BigDecimal.TEN);

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    @Test
    void rejectsPurchaseFactorAboveLimit() {
        Ingredient ingredient = rawIngredient();
        ingredient.setPurchaseUnitName("thùng");
        ingredient.setPurchaseFactorToBase(new BigDecimal("1000000.000001"));

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    @Test
    void rejectsFractionalPurchaseFactor() {
        Ingredient ingredient = rawIngredient();
        ingredient.setPurchaseUnitName("chai");
        ingredient.setPurchaseFactorToBase(new BigDecimal("12.5"));

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    @Test
    void rejectsPurchaseFactorEqualToOne() {
        Ingredient ingredient = rawIngredient();
        ingredient.setPurchaseUnitName("chai");
        ingredient.setPurchaseFactorToBase(BigDecimal.ONE);

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    @Test
    void rawIngredientClearsShelfLife() {
        Ingredient ingredient = rawIngredient();
        ingredient.setShelfLifeMinutes(120);

        IngredientService.validateAndNormalizeFields(ingredient);

        assertNull(ingredient.getShelfLifeMinutes());
    }

    @Test
    void preppedIngredientRequiresShelfLifeWithinRange() {
        Ingredient ingredient = rawIngredient();
        ingredient.setIngredientType("PREPPED");
        ingredient.setShelfLifeMinutes(null);

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));

        ingredient.setShelfLifeMinutes(60);
        IngredientService.validateAndNormalizeFields(ingredient);
        assertEquals(60, ingredient.getShelfLifeMinutes());
    }

    @Test
    void rejectsUnitOutsideSupportedList() {
        Ingredient ingredient = rawIngredient();
        ingredient.setUnit("xô");

        assertThrows(BusinessException.class,
                () -> IngredientService.validateAndNormalizeFields(ingredient));
    }

    private Ingredient rawIngredient() {
        Ingredient ingredient = new Ingredient();
        ingredient.setName("Bột matcha");
        ingredient.setUnit("g");
        ingredient.setIngredientType("RAW");
        ingredient.setActive(true);
        return ingredient;
    }
}
