package com.karwa.mdtnavigation

import com.mapbox.navigation.base.route.NavigationRoute

class Utils {
    companion object{
        fun updateContent(routes: List<NavigationRoute>): List<NavigationRoute> {
            val updatedRoutes = mutableListOf<NavigationRoute>()

            routes.forEach { route ->
                val updatedDirectionRoutes = route.directionsResponse.routes().map { directionRoute ->
                    val updatedLegs = directionRoute.legs()?.map { leg ->
                        val updatedSteps = leg.steps()?.map { step ->

                            // Update the maneuver instruction
                            val updatedManeuver = step.maneuver().let { maneuver ->
                                if (maneuver.instruction()!!
                                        .contains("You will arrive at your destination", true)
                                ) {
                                    maneuver.toBuilder().instruction(
                                        maneuver.instruction()!!.replace(
                                            "You will arrive at your destination", "Continue", true
                                        )
                                    ).build()
                                } else if (maneuver.instruction()!!
                                        .contains("You have arrived at your destination", true)
                                ) {
                                    maneuver.toBuilder().instruction(
                                        maneuver.instruction()!!.replace(
                                            "You have arrived at your destination", "Continue", true
                                        )
                                    ).build()
                                } else {
                                    maneuver
                                }
                            }

                            val updatedBannerInstructions =
                                step.bannerInstructions()?.map { instruction ->
                                    // Update primary instruction text
                                    val updatedPrimary = instruction.primary().let { primary ->
                                        if (primary.text()
                                                .contains("You will arrive at your destination", true)
                                        ) {
                                            primary.toBuilder().text(
                                                primary.text().replace(
                                                    "You will arrive at your destination",
                                                    "Continue",
                                                    true
                                                )
                                            ).build()
                                        } else if (primary.text()
                                                .contains("You have arrived at your destination", true)
                                        ) {
                                            primary.toBuilder().text(
                                                primary.text().replace(
                                                    "You have arrived at your destination",
                                                    "Continue",
                                                    true
                                                )
                                            ).build()
                                        } else {
                                            primary
                                        }
                                    }

                                    // Update components if necessary
                                    val updatedComponents =
                                        updatedPrimary.components()?.map { component ->
                                            if (component.text().contains(
                                                    "You will arrive at your destination", true
                                                )
                                            ) {
                                                component.toBuilder().text(
                                                    component.text().replace(
                                                        "You will arrive at your destination",
                                                        "Continue",
                                                        true
                                                    )
                                                ).build()
                                            } else if (component.text().contains(
                                                    "You have arrived at your destination", true
                                                )
                                            ) {
                                                component.toBuilder().text(
                                                    component.text().replace(
                                                        "You have arrived at your destination",
                                                        "Continue",
                                                        true
                                                    )
                                                ).build()
                                            } else {
                                                component
                                            }
                                        }

                                    // Rebuild the primary instruction with updated components
                                    val finalPrimaryInstruction = updatedPrimary.toBuilder()
                                        .components(updatedComponents ?: updatedPrimary.components())
                                        .build()

                                    // Rebuild the banner instruction with the updated primary instruction
                                    instruction.toBuilder().primary(finalPrimaryInstruction).build()
                                }

                            // Update voice instructions
                            val updatedVoiceInstructions =
                                step.voiceInstructions()?.map { voiceInstruction ->
                                    val updatedAnnouncement = if (voiceInstruction.announcement()!!
                                            .contains("You will arrive at your destination", true)
                                    ) {
                                        voiceInstruction.toBuilder().announcement(
                                            voiceInstruction.announcement()!!.replace(
                                                "You will arrive at your destination", "Continue", true
                                            )
                                        ).ssmlAnnouncement(
                                            voiceInstruction.ssmlAnnouncement()!!.replace(
                                                "You will arrive at your destination", "Continue", true
                                            )
                                        ).build()
                                    } else if (voiceInstruction.announcement()!!
                                            .contains("You have arrived at your destination", true)
                                    ) {
                                        voiceInstruction.toBuilder().announcement(
                                            voiceInstruction.announcement()!!.replace(
                                                "You have arrived at your destination", "Continue", true
                                            )
                                        ).ssmlAnnouncement(
                                            voiceInstruction.ssmlAnnouncement()!!.replace(
                                                "You have arrived at your destination", "Continue", true
                                            )
                                        ).build()
                                    } else {
                                        voiceInstruction
                                    }
                                    updatedAnnouncement
                                }

                            // Rebuild the step with the updated banner instructions
                            step.toBuilder().bannerInstructions(updatedBannerInstructions!!)
                                .maneuver(updatedManeuver).voiceInstructions(updatedVoiceInstructions!!)
                                .build()

                        }

                        // Rebuild the leg with the updated steps
                        leg.toBuilder().steps(updatedSteps).build()
                    }

                    // Rebuild the direction route with the updated legs
                    directionRoute.toBuilder().legs(updatedLegs).build()
                }

                // Rebuild the directions response with the updated routes
                val updatedDirectionsResponse =
                    route.directionsResponse.toBuilder().routes(updatedDirectionRoutes).build()

                // Recreate the NavigationRoute using the updated DirectionsResponse
                val newRoutes = NavigationRoute.create(
                    updatedDirectionsResponse, route.routeOptions, route.origin
                )

                // Add the new routes to the updated list
                updatedRoutes.addAll(newRoutes)
            }

            return updatedRoutes
        }
    }
}