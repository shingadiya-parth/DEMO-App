# PlayRewards Android Application — Architecture & Developer Documentation

## 1. Executive Summary & System Overview
**PlayRewards** is an Android mobile loyalty and casual gaming reward application built with **Kotlin** and **Jetpack Compose (Material Design 3)**. The application implements an authoritative **Reward Engine**, an **immutable double-entry-style Coin Ledger**, and multi-layered fraud/rate-limiting defenses.

---

## 2. Core Architecture & Layers

The application strictly follows Clean Architecture / MVVM principles:

```
┌───────────────────────────────────────────────────────────┐
│              Jetpack Compose UI (Screens & Theme)         │
│     (Home, Play/Games, Rewards, Wallet, Earn, Settings)   │
└─────────────────────────────┬─────────────────────────────┘
                              │ Collects StateFlow / Triggers Events
┌─────────────────────────────▼─────────────────────────────┐
│                       ViewModels                          │
│   (HomeViewModel, GameViewModel, WalletViewModel, etc.)   │
└─────────────────────────────┬─────────────────────────────┘
                              │ Calls Domain Engines & Repositories
┌─────────────────────────────▼─────────────────────────────┐
│                 Domain Engines & Services                 │
│  - RewardEngine (Authoritative Validation & Idempotency)  │
│  - FraudRiskEngine & RateLimiter (Velocity Defense)       │
│  - AdMobService (AdMob Lifecycle & Reward Validation)     │
│  - RedemptionEngine (Voucher Validation & Deduction)      │
└─────────────────────────────┬─────────────────────────────┘
                              │ Interacts with Repositories
┌─────────────────────────────▼─────────────────────────────┐
│                      Data Repositories                    │
│   (AuthRepo, WalletRepo, GameRepo, ActivityRepo, etc.)    │
└─────────────────────────────┬─────────────────────────────┘
                              │ Reads / Writes via DAOs
┌─────────────────────────────▼─────────────────────────────┐
│                   Room SQLite Local Database              │
│  (Users, CoinTransactions, GamePlayStats, Activities,     │
│   Redemptions, Notifications, SecurityEvents)             │
└───────────────────────────────────────────────────────────┘
```

---

## 3. Database Schema & Tables

All persistence is managed via Room (`AppDatabase.kt`):
- `users`: User identity, display name, email, referral code, account status, timestamps.
- `coin_transactions`: Immutable transaction ledger (`transactionId`, `userId`, `amount`, `type`, `description`, `idempotencyKey`, `createdAt`).
- `game_play_stats`: Per-user game attempts, scores, and daily count tracking.
- `activities`: Historic activity feed records for user visibility.
- `redemptions`: Reward redemption voucher orders with statuses (`PENDING`, `APPROVED`, `DELIVERED`, `REJECTED`).
- `notifications`: User inbox items with read/unread tracking.
- `security_events`: Audit log for rate limits, velocity spikes, and anti-fraud heuristics.

---

## 4. Authoritative Reward Pipeline

Every reward (Daily Bonus, Mini-Games, AdMob Bonus, Referrals) MUST strictly traverse the following pipeline:

$$\text{User Action} \rightarrow \text{Eligibility Verification} \rightarrow \text{Rate Limiter \& Fraud Check} \rightarrow \text{Idempotency Key Resolution} \rightarrow \text{Atomic Ledger Update} \rightarrow \text{Activity Logging} \rightarrow \text{UI Refresh}$$

**Core Invariant:** The client UI cannot inject or modify wallet balances directly. The wallet balance is derived from atomic transactions committed to the ledger.

---

## 5. Mini-Games Specifications (6 Games)

1. **Spin & Win**: Physics-based wheel rotation; configurable coin sectors, daily free spin limits, and cooldown timers.
2. **Scratch & Reveal**: Interactive scratch-off canvas with 60% reveal threshold detection and authoritative reward calculation.
3. **Brain Puzzle**: Timed arithmetic and pattern challenges with strict server-side answer verification.
4. **Coin Toss**: 3D flip animation with 50/50 probability engine and streak logging.
5. **Tic-Tac-Toe**: Interactive 3x3 grid with Minimax AI decision engine.
6. **Bubble Pop**: 30-second target popping round with score-to-coin reward curves.

---

## 6. AdMob Integration Architecture

- **Service**: Centralized `AdMobService` managing lifecycle, test-mode switching, and rewarded callbacks.
- **Configured Production IDs**:
  - **App ID**: `ca-app-pub-6519190170203543~2092384205`
  - **Banner Ad**: `ca-app-pub-6519190170203543/4114138818`
  - **Interstitial Ad**: `ca-app-pub-6519190170203543/7153139198`
  - **Rewarded Ad**: `ca-app-pub-6519190170203543/7805971811`
  - **App Open Ad**: `ca-app-pub-6519190170203543/9587730845`
- **Safety**: No reward is credited if an ad is skipped, dismissed early, or fails to load.

---

## 7. Environment Separation (Dev vs. Production)

| Feature | Development Mode (`DEVELOPMENT_TEST`) | Production Mode (`PRODUCTION`) |
| :--- | :--- | :--- |
| **AdMob Ad Units** | Google Sample Test IDs | User's Production AdMob Unit IDs |
| **Logging** | Verbose debug output | Minimal non-sensitive audit logs |
| **Reward Velocity** | Relaxed for QA testing | Strict daily limits and cooldowns |
| **Keystore Signing** | Android Debug Keystore | Production Upload Keystore (`.jks`) |

---

## 8. Release & Deployment Checklist (For Future Steps)
- Provide release upload key (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).
- Deploy `assetlinks.json` on production web domain for verified app deep linking.
- Register physical test devices in Google AdMob Console.
- Submit Data Safety and Content Declarations in Google Play Developer Console.
