# HopLedger Android — Implementation Plan & Issue Tracker

This directory contains planning documents for the Android app phases.

---

## Phases

| Phase |                                Title                                |                           GitHub Issue                            |          Depends On           |
|-------|---------------------------------------------------------------------|-------------------------------------------------------------------|-------------------------------|
| 7     | Android App Scaffolding (Kotlin + Jetpack Compose)                  | [#2](https://github.com/haertibraeu/HopLedger-Android/issues/2) ✅ | Backend Phases 1–5            |
| 8     | Settings Screens: Manage Brewers, Container Types, Beers, Locations | [#1](https://github.com/haertibraeu/HopLedger-Android/issues/1) ✅ | Phase 7 + Backend Phase 2     |
| 9     | Inventory Tab: Container List, Actions & Batch Fill                 | [#3](https://github.com/haertibraeu/HopLedger-Android/issues/3) ✅ | Phases 7, 8 + Backend Phase 5 |
| 10    | Accounting Tab: Balances, Entries & Settlements                     | [#4](https://github.com/haertibraeu/HopLedger-Android/issues/4) ✅ | Phases 7, 8 + Backend Phase 4 |

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Networking:** Retrofit + Kotlin Serialization
- **Local Storage:** Room (optional caching), DataStore (preferences)
- **Navigation:** Compose NavHost with bottom navigation (Inventory | Accounting | Settings)

## Architecture

```
app/src/main/java/com/haertibraeu/hopledger/
├── data/api/          # Retrofit interfaces
├── data/model/        # DTOs
├── data/repository/   # Repository implementations
├── domain/model/      # Domain entities
├── ui/inventory/      # Inventory screens & ViewModels
├── ui/accounting/     # Accounting screens & ViewModels
├── ui/settings/       # Config screens
├── ui/actions/        # Action dialogs (sell, self-consume, return)
├── ui/components/     # Shared composables
├── ui/navigation/     # NavHost setup
└── di/                # Hilt modules
```

## Cross-Repo Dependencies

- All API calls target the **HopLedger-Backend** REST API
- Backend URL is configurable via app settings (persisted in DataStore)
- Backend must be running and accessible for the app to function

