# OmniPOS - Multi-Tenant Offline-First Android Point of Sale

A production-grade, multi-tenant, offline-first Mobile POS system for universal retail shops and small businesses. Delivers instant barcode-ready checkout, immutable inventory ledgers, shift cash reconciliations, customer credit accounts, and multi-tenant cloud sync across branches.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural foundation and scope have been aligned based on your selections:

- **Business Vertical**: **Universal Multi-Category Retail Store** — configured with versatile catalog structures (electronics, apparel, FMCG, groceries), flexible tax rates (VAT/GST/inclusive/exclusive), barcode lookup, and discount management.
- **Tech Stack & Sync Backend**: **Android Jetpack Compose + Offline-First Room DB + Firebase Cloud Sync** — SQLite Room provides ultra-fast (<50ms) offline transactions and an immutable sync outbox, while Firebase Firestore & Auth enforce multi-tenant isolation, Google Sign-In security, and cloud sync.
- **Payment & Checkout Suite**: **Full Payment Engine** — supporting Cash with change calculation, QR / Digital Wallets, Cards, Split Payments, and a Customer Credit Ledger with outstanding due balances and debt repayment tracking.
- **Hardware Integrations**: Integrated camera barcode scanning, Bluetooth ESC/POS thermal receipt formatting (58mm/80mm), and printable/shareable digital invoices.

---

## 1. Overview & Core Concept

### What It Does
OmniPOS is a retail management and point-of-sale platform running natively on Android phones and tablets. It provides independent tenants (organizations) with isolated multi-branch management, role-based staff operations (Owner, Admin, Manager, Storekeeper, Cashier), swift cashier checkout, offline order parking, stock receiving and adjustments, cash float shift management, and customer due ledgers.

### Target Audience & Personas
- **Cashiers**: Need a high-speed, mistake-proof, keyboard/touch-optimized sales terminal with quick-cash shortcuts, instant barcode search, and parked cart switching.
- **Storekeepers**: Need mobile inventory audits, purchase order receiving, stock adjustments with reason codes, and low-stock alerts without exposure to sales profit margins.
- **Managers & Admins**: Require register shift approvals, cash in/out tracking, void/refund overrides, product pricing control, and staff permission assignments.
- **Business Owners**: Demand multi-branch visibility, aggregated Z-reports, profit & loss analytics, tax reports, and multi-tenant security assurances.

### Key Value
- **100% Offline Autonomy**: Outages never halt checkout lines. Sales and inventory adjustments write to the local ledger with deterministic register-prefixed IDs and sync automatically upon reconnection.
- **Zero-Trust Multi-Tenant Isolation**: Strict organization partitioning across every table, query, and security rule ensures cross-tenant data leaks are physically impossible.
- **Audit-Proof Accounting**: Append-only stock movements and immutable sales logs ensure cash balances and inventory counts match reality.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Organization Onboarding & Staff Sign-In**:
   - Google Sign-In via Jetpack Credential Manager.
   - Multi-step organization creation wizard (Business Name, Tax ID, Currency, Tax Mode, First Branch & Register).
   - Quick cashier lock / unlock via 4-digit PIN for rapid shift handovers.
2. **Cashier Sales & Checkout**:
   - Register shift opening with opening cash float verification.
   - Product catalog browsing with category tabs, live search, and camera barcode scanner.
   - Line items with quantity adjustments, custom discounts, and order notes.
   - Cart parking (held orders) and one-tap resume.
   - Payment modal: Cash, Card, QR Wallet, Customer Credit (Due), or Split.
   - Receipt generation: instant 58mm/80mm thermal preview, Bluetooth print dispatch, and receipt sharing.
3. **Inventory & Stock Management**:
   - Real-time stock on hand per branch derived from the immutable ledger.
   - Stock receiving against supplier shipments, cycle counting with variance reconciliation, and damage adjustments.
4. **Customer Credit Ledger**:
   - Customer profile with credit limit, total due balance, and transaction history.
   - Receiving partial or full due repayments with receipt generation.
5. **Shift Reconciliation & Reports**:
   - Pay-in and Pay-out cash drawer drops with audit reasons.
   - End-of-day shift closing: counted vs expected cash, over/short variance, and automatic Z-Report generation.

### Visual Identity & Theme
- **Aesthetic Direction**: **Industrial Modern Utility** — purposeful, dense, high-contrast, designed for rapid low-glare retail environments and high-traffic checkout counters.
- **Color Palette**:
  - `Primary`: Deep Slate Indigo (`#1E293B` / `#3B82F6` accent) for authoritative chrome and navigation.
  - `Secondary / Action`: Vibrant Emerald (`#10B981`) for completed checkouts, cash in, and affirmative actions.
  - `Warning / Attention`: Amber Gold (`#F59E0B`) for low stock, parked orders, and credit dues.
  - `Error / Destructive`: Coral Crimson (`#EF4444`) for voids, stock damages, and shift shortages.
  - `Surface & Background`: Crisp layered neutral slate (`#0F172A` in Dark mode, `#F8FAFC` in Light mode).
- **Typography & Hierarchy**:
  - Display numbers & Totals: Bold tabular sans numerals for instant readability of prices, subtotals, and cash changes.
  - Interface Headers & Body: Clean geometric sans-serif (Inter / Roboto) with strict typographic scale (`TitleLarge`, `BodyMedium`, `LabelSmall`).
- **Layout Adaptability**:
  - **Phone Mode (Handheld)**: Vertical single column with bottom checkout action sheet, floating cart drawer, and thumb-friendly 48dp+ hit targets.
  - **Tablet Mode (Countertop POS)**: Split-screen canonical view — catalog grid on the left (60%), sticky live cart, payment summary, and customer info on the right (40%).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Storage Hybrid (Room Local DB + Firebase Firestore Sync)**:
  - *Chosen Approach*: Android Room serves as the local single-source-of-truth for zero-latency offline reads and writes. A dedicated `sync_outbox` table queues operations. A reactive sync coordinator pushes queued mutations to Firestore and pulls remote changes when network is restored.
  - *Why*: POS systems cannot tolerate network lag during peak hours. Room guarantees sub-10ms UI responsiveness while Firestore guarantees cloud persistence and cross-branch synchronization.
- **Decision 2: Immutable Stock Movement Ledger vs Direct Stock Overwrite**:
  - *Chosen Approach*: Stock quantities are never overwritten directly in offline mode. Every sale, return, adjustment, or receipt writes a `StockMovement` entry with quantity delta (`+` or `-`). Stock on hand is computed as the sum of movements.
  - *Why*: Eliminates offline write-collision conflicts when multiple registers sell items concurrently.
- **Decision 3: Register-Prefixed Unique Invoice Generation**:
  - *Chosen Approach*: Invoices are generated as `[BRANCH_CODE]-[REGISTER_CODE]-[SEQUENCE_NUM]` (e.g., `BR1-REG1-000142`) alongside UUIDv7.
  - *Why*: Prevents invoice number duplication across independent offline devices without requiring server coordination.
- **Decision 4: Role-Based Access Control (RBAC) Data Model**:
  - *Chosen Approach*: Permissions are mapped dynamically (`RolePermission`), allowing organizations to assign custom privileges to Cashiers, Storekeepers, Managers, and Admins.
  - *Why*: Satisfies strict separation of duties (e.g., storekeepers cannot see margins; cashiers cannot approve refunds without manager PIN).

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│  ┌───────────────┐ ┌────────────────┐ ┌───────────────┐ ┌────────────┐ │
│  │ Cashier POS   │ │ Inventory Mgmt │ │ Shift & Cash  │ │ Customer   │ │
│  │ (Grid + Cart) │ │ (Ledger + POs) │ │ (Float / Z)   │ │ Credit Due │ │
│  └───────┬───────┘ └────────┬───────┘ └───────┬───────┘ └──────┬─────┘ │
└──────────┼──────────────────┼─────────────────┼────────────────┼───────┘
           ▼                  ▼                 ▼                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      ViewModel & Domain Services                       │
│  ┌───────────────────────┐ ┌───────────────────┐ ┌───────────────────┐ │
│  │ Cart & Tax Calculator │ │ Stock Ledger Math │ │ RBAC Security     │ │
│  └───────────────────────┘ └───────────────────┘ └───────────────────┘ │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Repository & Sync Layer                         │
│  ┌─────────────────────────────────┐ ┌───────────────────────────────┐ │
│  │ PosRepository / SyncRepository  │ │ AuthState & CredentialManager │ │
│  └────────────────┬────────────────┘ └───────────────┬───────────────┘ │
└───────────────────┼──────────────────────────────────┼─────────────────┘
                    ▼                                  ▼
      ┌───────────────────────────┐      ┌───────────────────────────┐
      │   Room Database (Local)   │      │   Firestore & Auth (Cloud)│
      │ ┌───────────────────────┐ │ Sync │ ┌───────────────────────┐ │
      │ │ Products / Customers  │ │◄────►│ │ /organizations/{orgId}│ │
      │ │ Sales / SaleItems     │ │Worker│ │   /products           │ │
      │ │ StockMovements        │ │      │ │   /sales              │ │
      │ │ SyncOutbox / Shifts   │ │      │ │   /stock_movements    │ │
      │ └───────────────────────┘ │      │ └───────────────────────┘ │
      └───────────────────────────┘      └───────────────────────────┘
```

### Core Entities & Database Schema

1. **Organization & Multi-Tenancy**:
   - `organizations` (`id`, `name`, `tax_id`, `currency`, `tax_mode`, `owner_uid`, `created_at`)
   - `branches` (`id`, `org_id`, `name`, `code`, `address`, `phone`)
   - `registers` (`id`, `org_id`, `branch_id`, `name`, `code`, `current_shift_id`)
   - `memberships` (`id`, `org_id`, `user_uid`, `role`, `pin_hash`, `branch_ids`)
2. **Catalog & Inventory**:
   - `categories` (`id`, `org_id`, `name`, `color`, `icon`)
   - `products` (`id`, `org_id`, `category_id`, `name`, `sku`, `barcode`, `cost_price`, `selling_price`, `tax_rate`, `unit`, `min_stock_alert`)
   - `stock_movements` (`id`, `org_id`, `branch_id`, `product_id`, `quantity_delta`, `type` [SALE, RETURN, RECEIPT, ADJUSTMENT], `reference_id`, `created_at`, `created_by`)
3. **Sales & Orders**:
   - `sales` (`id`, `org_id`, `branch_id`, `register_id`, `invoice_no`, `customer_id`, `subtotal`, `discount_total`, `tax_total`, `grand_total`, `payment_status`, `sync_status`, `created_at`, `created_by`)
   - `sale_items` (`id`, `sale_id`, `product_id`, `product_name`, `unit_price`, `quantity`, `discount_amount`, `tax_amount`, `line_total`)
   - `sale_payments` (`id`, `sale_id`, `payment_method` [CASH, CARD, QR, CREDIT], `amount`, `change_amount`, `reference_no`)
   - `held_orders` (`id`, `org_id`, `register_id`, `customer_name`, `items_json`, `created_at`)
4. **Customer Ledger & Credit**:
   - `customers` (`id`, `org_id`, `name`, `phone`, `email`, `credit_limit`, `current_due_balance`, `created_at`)
   - `customer_ledger` (`id`, `org_id`, `customer_id`, `sale_id`, `type` [DEBIT_SALE, CREDIT_PAYMENT], `amount`, `balance_after`, `notes`, `created_at`)
5. **Shifts & Cash Reconciliation**:
   - `shifts` (`id`, `org_id`, `register_id`, `cashier_uid`, `opened_at`, `opening_float`, `closed_at`, `counted_cash`, `expected_cash`, `variance`, `notes`)
   - `cash_movements` (`id`, `shift_id`, `type` [PAY_IN, PAY_OUT], `amount`, `reason`, `created_at`)
6. **Sync Engine**:
   - `sync_outbox` (`id`, `org_id`, `table_name`, `record_id`, `payload_json`, `operation` [INSERT, UPDATE, DELETE], `created_at`, `retry_count`, `last_error`)

---

## 5. Verification & Testing Strategy

- **Local Room Database Verification**: Automated verification of Room migrations, reactive Flow DAO observers, and immutable stock calculation queries.
- **Firestore Security Rules**: Zero-trust multi-tenant rules deployed and verified with emulator tests confirming user isolation.
- **Offline Resilience Check**: Verify transactions complete in airplane mode, persist to Room, enqueue into `sync_outbox`, and cleanly dispatch upon network reconnection.
- **POS Calculations & Math**: Exact integer/minor-unit math for discounts, percentage vs fixed discounts, tax-inclusive vs tax-exclusive math, and customer credit ledger balances.
