package dev.cfaz.timeclicker.ui.icons

import android.content.res.Resources
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TileIcon

/** Translated names of the default palette's icons, current and past (tiles and palettes may still use them). */
internal val translatedNames: Map<String, Int> = mapOf(
    "circle-check" to R.string.icon_check,
    "star" to R.string.icon_star,
    "heart" to R.string.icon_heart,
    "house" to R.string.icon_house,
    "sparkles" to R.string.icon_sparkles,
    "trash" to R.string.icon_trash,
    "washing-machine" to R.string.icon_laundry,
    "bed" to R.string.icon_bed,
    "utensils" to R.string.icon_utensils,
    "coffee" to R.string.icon_coffee,
    "shopping-cart" to R.string.icon_cart,
    "droplet" to R.string.icon_drop,
    "leaf" to R.string.icon_leaf,
    "sprout" to R.string.icon_grass,
    "flower-2" to R.string.icon_flower,
    "snowflake" to R.string.icon_snow,
    "pill" to R.string.icon_pill,
    "dumbbell" to R.string.icon_dumbbell,
    "book-open" to R.string.icon_book,
    "phone" to R.string.icon_phone,
    "paw-print" to R.string.icon_paw,
    "scissors" to R.string.icon_scissors,
    "paintbrush" to R.string.icon_brush,
    "wrench" to R.string.icon_wrench,
    "car" to R.string.icon_car,
    "gauge" to R.string.icon_gauge,
    "broom" to R.string.icon_broom,
    "shirt" to R.string.icon_shirt,
    "bath" to R.string.icon_bath,
    "refrigerator" to R.string.icon_fridge,
    "lightbulb" to R.string.icon_lightbulb,
    "air-vent" to R.string.icon_air_vent,
    "battery" to R.string.icon_battery,
    "toothbrush" to R.string.icon_toothbrush,
    "stethoscope" to R.string.icon_stethoscope,
    "syringe" to R.string.icon_syringe,
    "footprints" to R.string.icon_footprints,
    "bike" to R.string.icon_bike,
    "dog" to R.string.icon_dog,
    "cat" to R.string.icon_cat,
    "fish" to R.string.icon_fish,
    "users" to R.string.icon_people,
    "gift" to R.string.icon_gift,
    "cake" to R.string.icon_cake,
    "receipt" to R.string.icon_receipt,
    "key" to R.string.icon_key,
    "mail" to R.string.icon_mail,
    "music" to R.string.icon_music,
    "plane" to R.string.icon_plane,
)

/**
 * What screen readers (and search) call an icon. The default icons have translated names;
 * the rest of Lucide's icons only have English ones, made readable: "paw-print" → "Paw print".
 */
fun iconLabel(resources: Resources, icon: TileIcon): String = when {
    icon.isNone -> resources.getString(R.string.icon_none)
    else -> translatedNames[icon.key]?.let(resources::getString)
        ?: icon.key.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

/** Lucide's category ids, with their translated names. Unknown ids (from a newer icon set) aren't shown as sections. */
val categoryNames: Map<String, Int> = mapOf(
    "accessibility" to R.string.category_accessibility,
    "account" to R.string.category_account,
    "animals" to R.string.category_animals,
    "arrows" to R.string.category_arrows,
    "buildings" to R.string.category_buildings,
    "charts" to R.string.category_charts,
    "communication" to R.string.category_communication,
    "connectivity" to R.string.category_connectivity,
    "cursors" to R.string.category_cursors,
    "design" to R.string.category_design,
    "development" to R.string.category_development,
    "devices" to R.string.category_devices,
    "emoji" to R.string.category_emoji,
    "files" to R.string.category_files,
    "finance" to R.string.category_finance,
    "food-beverage" to R.string.category_food_beverage,
    "gaming" to R.string.category_gaming,
    "home" to R.string.category_home,
    "layout" to R.string.category_layout,
    "mail" to R.string.category_mail,
    "math" to R.string.category_math,
    "medical" to R.string.category_medical,
    "multimedia" to R.string.category_multimedia,
    "nature" to R.string.category_nature,
    "navigation" to R.string.category_navigation,
    "notifications" to R.string.category_notifications,
    "people" to R.string.category_people,
    "photography" to R.string.category_photography,
    "science" to R.string.category_science,
    "seasons" to R.string.category_seasons,
    "security" to R.string.category_security,
    "shapes" to R.string.category_shapes,
    "shopping" to R.string.category_shopping,
    "social" to R.string.category_social,
    "sports" to R.string.category_sports,
    "sustainability" to R.string.category_sustainability,
    "text" to R.string.category_text,
    "time" to R.string.category_time,
    "tools" to R.string.category_tools,
    "transportation" to R.string.category_transportation,
    "travel" to R.string.category_travel,
    "weather" to R.string.category_weather,
)
