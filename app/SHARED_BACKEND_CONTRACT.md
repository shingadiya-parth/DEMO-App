# PlayRewards: User App ↔ Shared Backend Contract

## 1. Overview & System Relationship
This document defines the authoritative data models, transaction ledger invariants, domain engine contracts, and security boundaries shared between the **PlayRewards User Android Application** and the **Future Separate Admin Panel / Backend Services**.

```
┌──────────────────────────────┐              ┌──────────────────────────────┐
│  PlayRewards User App (M3)   │              │  Separate Admin Panel (Web)  │
│  - Casual Gamers / Members   │              │  - Operations & Support      │
│  - Restricted User Auth      │              │  - Privileged Admin Auth     │
└──────────────┬───────────────┘              └──────────────┬───────────────┘
               │                                             │
               │ REST / gRPC / Room SQLite                   │ HTTPS Admin Endpoints / Cloud DB
               ▼                                             ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                    CENTRAL AUTHORITATIVE DATA LAYER                        │
│  - Users (`user_account`)             - Ledger (`coin_transactions`)      │
│  - Game Stats (`game_play_stats`)     - Redemptions (`redemption_requests`)│
│  - Referrals (`referral_record`)      - Notifications (`app_notifications`)│
│  - Activities (`user_activities`)     - Security Events (`security_events`)│
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Entities & Table Contracts

### 2.1. User Account (`user_account`)
| Field | Type | Description | Mutability |
| :--- | :--- | :--- | :--- |
| `user_id` | `String` (PK) | Unique user identifier | Immutable |
| `display_name` | `String` | User-chosen display name | Editable by User / Admin |
| `email` | `String` | Registered email address | User / Admin |
| `avatar` | `String` | Selected avatar identifier | User Editable |
| `country` | `String` | Country code (e.g., `"IN"`) | User / System |
| `date_of_birth` | `String?` | Optional date of birth | User / System |
| `coin_balance` | `Long` | Mirror of aggregate ledger balance | **LEDGER ONLY** |
| `total_coins_earned` | `Long` | Lifetime earned coins | **LEDGER ONLY** |
| `total_coins_spent` | `Long` | Lifetime spent/redeemed coins | **LEDGER ONLY** |
| `referral_code` | `String` | Unique user referral code (`REF-XXXXXX`) | Immutable |
| `referred_by` | `String?` | Referrer user code (if applied) | Single-write only |
| `account_status` | `Enum` | `ACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`, `DEACTIVATED` | **ADMIN ONLY** |
| `account_created_date`| `Long` | Epoch timestamp of signup | Immutable |
| `last_login_at` | `Long` | Epoch timestamp of last authentication | System Updated |
| `last_activity` | `Long` | Epoch timestamp of latest user event | System Updated |

---

### 2.2. Coin Transaction Ledger (`coin_transactions`)
**Core Invariant:** The ledger is **append-only and immutable**. Direct modifications or deletions are strictly prohibited. Every balance mutation must create a discrete transaction.

| Field | Type | Description |
| :--- | :--- | :--- |
| `transaction_id` | `String` (PK) | Unique transaction ID (`tx_uuid`) |
| `user_id` | `String` (Indexed) | Owning user ID |
| `wallet_id` | `String` (Indexed) | Associated wallet reference |
| `type` | `Enum` | Valid `TransactionType` (see below) |
| `source` | `String` | Originating source (e.g. `"SPIN_WHEEL"`, `"DAILY_BONUS"`, `"AD_MOB"`) |
| `amount` | `Long` | Signed integer: Positive (+) for Credits, Negative (-) for Debits |
| `balance_before` | `Long` | Calculated balance snapshot before application |
| `balance_after` | `Long` | Calculated balance snapshot after application |
| `status` | `Enum` | `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`, `REFUNDED` |
| `reference_id` | `String?` | External order / game session ID |
| `idempotency_key` | `String?` (Indexed)| Cryptographic / UUID idempotency token |
| `metadata` | `String?` | JSON metadata (score, multipliers, order info) |
| `created_at` | `Long` (Indexed) | Epoch timestamp |

#### Supported `TransactionType` Enums:
- `DAILY_BONUS` (Credit)
- `GAME_REWARD` (Credit)
- `SPIN_REWARD` (Credit)
- `SCRATCH_REWARD` (Credit)
- `PUZZLE_REWARD` (Credit)
- `COIN_TOSS_REWARD` (Credit)
- `TIC_TAC_TOE_REWARD` (Credit)
- `BUBBLE_POP_REWARD` (Credit)
- `AD_REWARD` (Credit)
- `REFERRAL_REWARD` (Credit)
- `GIVEAWAY_REWARD` (Credit)
- `REDEMPTION_DEDUCTION` (Debit)
- `ADMIN_ADJUSTMENT` (Credit/Debit - Admin Only)
- `REVERSAL` (Credit - Refund Reversal)

---

### 2.3. Game Sessions & Stats (`game_play_stats` / `CommonGameSession`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `sessionId` | `String` (PK) | Unique session UUID |
| `userId` | `String` | Participating user |
| `gameId` | `String` | Game identifier (`spin`, `scratch`, `puzzle`, `cointoss`, `tictactoe`, `bubblepop`) |
| `startedAt` | `Long` | Game start timestamp |
| `completedAt` | `Long?` | Game completion timestamp |
| `status` | `Enum` | `CREATED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `EXPIRED`, `REWARDED` |
| `result` | `String?` | Outcome summary (e.g. `"WIN"`, `"LOSS"`, `"DRAW"`) |
| `score` | `Int` | Points or raw game score |
| `rewardAmount` | `Long` | Coins awarded |
| `idempotencyKey` | `String` | Enforces single reward per play session |

---

### 2.4. Redemption Requests (`redemption_requests`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `redemption_id` | `String` (PK) | Unique order reference (`red_uuid`) |
| `user_id` | `String` (Indexed) | Requesting user |
| `reward_id` | `String` | Catalog item ID (`rew_amazon_500`, `rew_gplay_250`, etc.) |
| `reward_name_snapshot` | `String` | Preserved reward title at time of order |
| `reward_value_snapshot`| `Double` | Monetary value (e.g. `500.0`) |
| `required_coins_snapshot`| `Long` | Required coins deducted at time of order |
| `currency_snapshot` | `String` | Monetary currency code (`INR`, `USD`) |
| `destination_account` | `String` | Delivery email or UPI ID destination |
| `status` | `Enum` | `PENDING`, `PROCESSING`, `APPROVED`, `FULFILLED`, `REJECTED`, `CANCELLED`, `REFUNDED` |
| `transaction_id` | `String?` | Linked debit transaction ID |
| `idempotency_key` | `String` (Unique) | Idempotency token preventing double debit |
| `admin_note` | `String?` | Optional internal note by operations team |
| `failure_reason` | `String?` | User-facing explanation if rejected |
| `created_at` | `Long` (Indexed) | Submission timestamp |
| `updated_at` | `Long` | Last status update timestamp |
| `processed_at` | `Long?` | Fulfillment/Rejection timestamp |

---

### 2.5. Referral Records (`referral_record`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `referral_id` | `String` (PK) | Unique referral record ID |
| `referrer_user_id` | `String` | Inviting user's ID |
| `referred_user_id` | `String` | Invited user's ID |
| `referral_code` | `String` | Referral code applied |
| `status` | `Enum` | `PENDING`, `QUALIFYING`, `QUALIFIED`, `REWARDED`, `REJECTED`, `EXPIRED` |
| `qualification_progress`| `Int` | Completed games towards threshold |
| `qualification_target` | `Int` | Required games (Default: `3`) |
| `referrer_reward_amount`| `Long` | Referrer reward (Default: `500L`) |
| `referred_user_reward_amount`| `Long` | Referred user reward (Default: `100L`) |
| `referrer_reward_transaction_id`| `String?` | Linked referrer reward transaction |
| `referred_user_reward_transaction_id`| `String?` | Linked referred reward transaction |
| `risk_state` | `Enum` | `NORMAL`, `REVIEW`, `BLOCKED` |
| `created_at` | `Long` | Registration timestamp |
| `qualified_at` | `Long?` | Qualification timestamp |

---

### 2.6. In-App Notifications (`app_notifications`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `notificationId` | `String` (PK) | Unique notification UUID |
| `userId` | `String` (Indexed) | Recipient user ID |
| `title` | `String` | Notification header |
| `message` | `String` | Body text |
| `type` | `Enum` | `DAILY_BONUS`, `GAME_REWARD`, `AD_REWARD`, `REFERRAL`, `REDEMPTION`, `GIVEAWAY`, `SYSTEM`, `SECURITY` |
| `deepLink` | `String?` | Optional destination route |
| `isRead` | `Boolean` (Indexed) | Read/Unread flag |
| `createdAt` | `Long` (Indexed) | Creation timestamp |
| `expiresAt` | `Long?` | Optional expiration timestamp |

---

### 2.7. User Activities (`user_activities`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `activityId` | `String` (PK) | Unique activity UUID |
| `userId` | `String` (Indexed) | Owning user ID |
| `activityType` | `Enum` | `SPIN_COMPLETED`, `DAILY_BONUS_CLAIMED`, `REDEMPTION_REQUESTED`, etc. |
| `category` | `Enum` | `ALL`, `GAMES`, `REWARDS`, `REFERRALS`, `REDEMPTIONS` |
| `title` | `String` | Display title in activity log |
| `description` | `String` | Descriptive details |
| `relatedId` | `String?` (Indexed) | Associated transaction or game session ID |
| `result` | `String?` | Outcome snapshot (e.g. `"+50 Coins"`) |
| `createdAt` | `Long` (Indexed) | Creation timestamp |

---

## 3. Central Reward Engine Pipeline
Every coin grant operation MUST execute through the authoritative `RewardEngine` contract:

```
[User Action]
     │
     ▼
1. Eligibility Verification (Check active session, account status != SUSPENDED)
     │
     ▼
2. Rate Limiting & Fraud Risk Engine (Velocity checks, duplicate click throttle)
     │
     ▼
3. Idempotency Check (Look up existing transaction with `idempotencyKey`)
     ├──> If Match: Return `RewardGrantResult.AlreadyClaimed` (No-Op)
     │
     ▼
4. Atomic Ledger Mutation (Insert `CoinTransaction` with `balance_before` & `balance_after`)
     │
     ▼
5. Update Cached User Account Stats (`coin_balance`, `total_coins_earned`)
     │
     ▼
6. Record Activity Record (`user_activities`)
     │
     ▼
7. Return `RewardGrantResult.Success(coinsGranted, newBalance)`
```

---

## 4. Future Admin Panel Permissions & Access Model

The separate Admin Panel will operate under **Privileged Administrative Scopes** and must **NEVER** use normal client tokens. Required permissions include:

| Admin Permission Scope | Description | Allowed Actions |
| :--- | :--- | :--- |
| `USER_READ` | View member profiles | View account stats, balances, device info |
| `USER_UPDATE_STATUS` | Moderation & security | Suspend, reactivate, or blacklist accounts |
| `LEDGER_READ` | Financial auditing | Search and inspect raw transaction records |
| `LEDGER_ADJUST` | Manual corrections | Execute credit/debit with `ADMIN_ADJUSTMENT` |
| `REDEMPTION_READ` | Order queue inspection | Filter pending, approved, and fulfilled redemptions |
| `REDEMPTION_PROCESS` | Order fulfillment | Change status to `APPROVED`, `FULFILLED`, or `REJECTED` (with auto-refund) |
| `GAME_CONFIG_READ` | Read game parameters | Inspect daily limits, multipliers, cooldowns |
| `GAME_CONFIG_UPDATE` | Dynamic tuning | Update reward sectors, scratch tiers, puzzle points |
| `CATALOG_UPDATE` | Catalog management | Add, edit, disable, or adjust pricing of gift vouchers |
| `NOTIFICATION_BROADCAST`| Communications | Dispatch system/promotional notifications to users |
| `FRAUD_LOG_READ` | Risk management | View security events, velocity spikes, multi-account alerts |

---

## 5. Configuration & Environment Variables

| Variable Name | Environment | Description |
| :--- | :--- | :--- |
| `BACKEND_PROJECT_ID` | Production / Dev | Cloud project / Firebase project ID |
| `ADMOB_APP_ID` | Production | Google AdMob App ID (`ca-app-pub-...~...`) |
| `ADMOB_BANNER_UNIT_ID` | Production | Banner Ad Unit ID |
| `ADMOB_INTERSTITIAL_UNIT_ID`| Production | Interstitial Ad Unit ID |
| `ADMOB_REWARDED_UNIT_ID` | Production | Rewarded Video Ad Unit ID |
| `ADMOB_APP_OPEN_UNIT_ID` | Production | App Open Ad Unit ID |
| `KEYSTORE_PATH` | Release Build | Path to release signing `.jks` |
| `STORE_PASSWORD` | Release Build | Keystore password |
| `KEY_PASSWORD` | Release Build | Signing key password |
| `API_BASE_URL` | Production | Base HTTPS URL for cloud backend microservices |

---

## 6. Configurable vs. Hardcoded Parameter Inventory

| System Parameter | Current Location | Status | Target Recommendation for Admin Panel |
| :--- | :--- | :--- | :--- |
| **Coin Conversion (1000 = ₹10)**| `CoinConfig.kt` | Config File | Make dynamic via Remote Config API |
| **Daily Bonus Tiers (10→100 Coins)**| `DailyBonusConfig.kt` | Config File | Make dynamic via Remote Config API |
| **Mini-Game Daily Limits & Rewards** | `core/config/*Config.kt` | Config Files | Synchronize from Cloud Firestore / Admin API |
| **Redemption Catalog & Pricing** | `RewardCatalogConfig.kt`| Config File | Store in Cloud Catalog database with Admin CMS |
| **Referral Reward (500 + 100)** | `ReferralConfig.kt` | Config File | Make dynamic via Admin Dashboard |
| **AdMob Ad Unit IDs** | `AdMobConfig.kt` | Configured | Support dynamic unit rotation if needed |
