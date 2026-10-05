# Data schema plan

Room database `brokemon.db`, currently **version 4**. Schemas are exported to `app/schemas/` (keep that folder committed). Every version bump so far has been an `AutoMigration`, so nobody's data gets wiped.

| Version | Change | Why |
|---|---|---|
| 1 | `bros`, `squads` tables | First release |
| 2 | `bros.look` (nullable TEXT) | Pixel character builder |
| 3 | `bros.voiceLine`, `bros.eventFrame` (nullable TEXT) | Voice lines, limited event frames |
| 4 | `bros.room` (nullable TEXT, list of option indices) | Decoratable bro rooms |

## How the planned fields map

| Planned field | Where it lives | Notes |
|---|---|---|
| `lastCheckInDate` | `Bro.lastCheckIn: Long?` | Set by every check-in (app, widget, notification) |
| `birthday` | `Fact.monthDay` ("MM-dd") on a Birthday fact | Lives in the facts JSON, so no column was needed. Year is optional by design |
| `isBonded` | **Computed**: `Evolution.info(bro).stage` | Never stored, same rule as evolution stage. Derive "bonded" from the stage |
| `avatarParts` | `Bro.look: BroLook?` (list of option indices) | Append-only option tables (`LookOptions`), so stored indices never shift |
| `isTradeable` | `Bro.isTradeable` | Locked cards can't QR-trade or post |
| `lastRecommended` | DataStore (`UserPrefs.lastRecommendedBroId`) | App state, not bro data |
| `MediaType.AUDIO` | `MediaType` enum (stored by name in memories JSON) | Voice memories (`.m4a`) |
| Voice line | `Bro.voiceLine: String?` | ~10s clip per bro |
| Event frame | `Bro.eventFrame: String?` ("RAMADAN\|2027") | Stamped at catch, travels in QR |

## Rules for future changes
1. **New column** → add it as nullable or with a default, bump `version`, and add `AutoMigration(from = N, to = N + 1)`. Build once so `N+1.json` appears in `app/schemas/`, then commit it.
2. **Renaming or deleting a column** → AutoMigration needs a spec (`@RenameColumn` / `@DeleteColumn`) or a manual `Migration`. Avoid if possible.
3. **Data inside JSON columns** (memories, facts, stats, look) can gain optional fields without a migration. Gson leaves missing fields at default. Never reuse or reorder enum names or option indices.
4. Never ship `fallbackToDestructiveMigration()`. Losing someone's Brodex is the one-star review.
5. When the schema changes, also update `BackupManager` (files referenced by new fields) and `QrCodec` (only if the field should travel in a trade).
