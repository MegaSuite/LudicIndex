package com.ludicindex.app

/** Recorded from the live IGDB API (The Witcher 3, id 1942), trimmed to what the UI renders. */
const val WITCHER_DETAIL_JSON: String = """
[
  {
    "id": 1942,
    "name": "The Witcher 3: Wild Hunt",
    "summary": "An open-world action role-playing game.",
    "storyline": "Geralt searches for Ciri while the Wild Hunt closes in.",
    "first_release_date": 1431993600,
    "game_type": 0,
    "rating": 93.77415284802106,
    "rating_count": 5491,
    "aggregated_rating": 91.73076923076923,
    "aggregated_rating_count": 26,
    "total_rating": 92.75246103939514,
    "total_rating_count": 5517,
    "hypes": 179,
    "follows": 1200,
    "cover": { "id": 480513, "image_id": "coaarl" },
    "artworks": [{ "id": 303975, "image_id": "ar6ijr" }],
    "screenshots": [{ "id": 1377966, "image_id": "sctj8u" }],
    "videos": [{ "id": 9648, "name": "Developer Diary", "video_id": "yowv6_rspoM" }],
    "genres": [{ "id": 12, "name": "Role-playing (RPG)" }, { "id": 31, "name": "Adventure" }],
    "themes": [{ "id": 1, "name": "Action" }],
    "platforms": [{ "id": 6, "name": "PC (Microsoft Windows)" }],
    "game_modes": [{ "id": 1, "name": "Single player" }],
    "player_perspectives": [{ "id": 2, "name": "Third person" }],
    "game_engines": [{ "id": 24, "name": "REDengine" }],
    "involved_companies": [
      { "company": { "id": 1, "name": "CD Projekt RED" }, "developer": true, "publisher": false, "porting": false },
      { "company": { "id": 50, "name": "WB Games" }, "developer": false, "publisher": true, "porting": false },
      { "company": { "id": 99, "name": "Fishlabs" }, "developer": false, "publisher": false, "porting": true }
    ],
    "collection": { "id": 8, "name": "The Witcher" },
    "franchise": { "id": 5, "name": "The Witcher" },
    "similar_games": [
      { "id": 119133, "name": "Elden Ring", "cover": { "id": 212094, "image_id": "co4jni" } },
      { "id": 1887, "name": "Dragon Age: Inquisition", "cover": { "id": 122974, "image_id": "co2mvy" } }
    ],
    "websites": [
      { "id": 19103, "url": "https://www.facebook.com/CDPROJEKTRED", "type": 4 },
      { "id": 141346, "url": "https://www.epicgames.com/store/en-US/product/the-witcher-3-wild-hunt/home", "type": 16 }
    ],
    "language_supports": [
      { "id": 30, "language": { "id": 27, "name": "German" } },
      { "id": 31, "language": { "id": 19, "name": "Polish" } },
      { "id": 32, "language": { "id": 27, "name": "German" } }
    ]
  }
]
"""

/** A newer, unreleased title, used to check the derived release status. */
const val UNRELEASED_DETAIL_JSON: String = """
[
  { "id": 52189, "name": "Grand Theft Auto VI", "first_release_date": 4102444800 }
]
"""
