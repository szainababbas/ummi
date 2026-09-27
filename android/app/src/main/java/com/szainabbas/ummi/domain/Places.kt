package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.Place

/**
 * The places prayer times can be worked out for. A short fixed list rather
 * than the phone's location: it needs no permission, and a city's times are
 * close enough for a reminder a few minutes after the adhān.
 */
object Places {
    val ALL: List<Place> = listOf(
        Place.DEFAULT,
        Place("Birmingham", 52.4862, -1.8904),
        Place("Bradford", 53.7960, -1.7594),
        Place("Bristol", 51.4545, -2.5879),
        Place("Cardiff", 51.4816, -3.1791),
        Place("Edinburgh", 55.9533, -3.1883),
        Place("Glasgow", 55.8642, -4.2518),
        Place("Leeds", 53.8008, -1.5491),
        Place("Leicester", 52.6369, -1.1398),
        Place("Liverpool", 53.4084, -2.9916),
        Place("Luton", 51.8787, -0.4200),
        Place("Manchester", 53.4808, -2.2426),
        Place("Sheffield", 53.3811, -1.4701),
        Place("Dublin", 53.3498, -6.2603),
        Place("Toronto", 43.6532, -79.3832),
        Place("New York", 40.7128, -74.0060),
        Place("Dar es Salaam", -6.7924, 39.2083),
        Place("Dubai", 25.2048, 55.2708),
        Place("Karachi", 24.8607, 67.0011),
        Place("Lahore", 31.5204, 74.3587),
        Place("Islamabad", 33.6844, 73.0479),
        Place("Mumbai", 19.0760, 72.8777),
        Place("Qom", 34.6416, 50.8746),
        Place("Mashhad", 36.2605, 59.6168),
        Place("Najaf", 32.0003, 44.3354),
        Place("Karbala", 32.6160, 44.0249),
    )
}
