package com.rupeshbhurke.househunter.core.data.model

import com.rupeshbhurke.househunter.core.data.model.FieldDef.Companion.GROUP_BASICS
import com.rupeshbhurke.househunter.core.data.model.FieldDef.Companion.GROUP_EVALUATION
import com.rupeshbhurke.househunter.core.data.model.FieldDef.Companion.GROUP_MONEY

/** The built-in template seeded for every collection. Every field is optional. */
object HouseHuntTemplate {
    const val ID = "house_hunt"
    const val NAME = "House Hunt"

    object Keys {
        const val ADDRESS = "address"
        const val SOCIETY = "society"
        const val BHK = "bhk"
        const val FLOOR = "floor"
        const val AREA_SQFT = "area_sqft"
        const val FACING = "facing"
        const val PRICE = "price"
        const val DEPOSIT = "deposit"
        const val MAINTENANCE = "maintenance"
        const val BROKERAGE = "brokerage"
        const val PROS = "pros"
        const val CONS = "cons"
        const val VISIT_DATE = "visit_date"
    }

    val fields: List<FieldDef> = listOf(
        FieldDef(Keys.ADDRESS, "Address", FieldType.LONG_TEXT, GROUP_BASICS),
        FieldDef(Keys.SOCIETY, "Building / society", FieldType.TEXT, GROUP_BASICS),
        FieldDef(
            Keys.BHK, "BHK", FieldType.ENUM, GROUP_BASICS,
            options = listOf("1 RK", "1 BHK", "2 BHK", "3 BHK", "4 BHK", "5+ BHK"),
        ),
        FieldDef(Keys.FLOOR, "Floor", FieldType.TEXT, GROUP_BASICS),
        FieldDef(Keys.AREA_SQFT, "Area (sq ft)", FieldType.NUMBER, GROUP_BASICS),
        FieldDef(
            Keys.FACING, "Facing", FieldType.ENUM, GROUP_BASICS,
            options = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
        ),
        FieldDef(Keys.PRICE, "Rent / price", FieldType.MONEY, GROUP_MONEY),
        FieldDef(Keys.DEPOSIT, "Deposit", FieldType.MONEY, GROUP_MONEY),
        FieldDef(Keys.MAINTENANCE, "Maintenance", FieldType.MONEY, GROUP_MONEY),
        FieldDef(Keys.BROKERAGE, "Brokerage", FieldType.MONEY, GROUP_MONEY),
        FieldDef(Keys.PROS, "Pros", FieldType.LONG_TEXT, GROUP_EVALUATION),
        FieldDef(Keys.CONS, "Cons", FieldType.LONG_TEXT, GROUP_EVALUATION),
        FieldDef(Keys.VISIT_DATE, "Visit date", FieldType.DATE, GROUP_EVALUATION),
    )
}
