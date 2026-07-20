# E-Commerce Order Tracking System

A Low-Level Design (LLD) mini project implementing order lifecycle tracking
using a **Queue** and a **Finite State Machine (FSM)**.

## Problem Statement
An order moves through a fixed set of stages (Placed → Confirmed → Shipped →
Out for Delivery → Delivered), with side branches for Cancellation and
Returns. Instead of updating an order's status directly, every status-change
request is placed into a queue. A processor drains the queue one request at
a time, validates each transition against a rulebook, and applies or rejects
it — invalid transitions are logged and skipped without stopping the system.

## Why Queue + State Machine
- **Queue**: decouples "a change was requested" from "the change was
  processed," and guarantees requests are handled in order, one at a time.
- **State Machine**: enforces that an order can only move to specific valid
  next-states from its current state — arbitrary jumps (e.g. Delivered →
  Placed) are rejected.

## Order States
`PLACED, CONFIRMED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, RETURNED`

## Transition Rules
- Placed → Confirmed / Cancelled
- Confirmed → Shipped / Cancelled
- Shipped → Out for Delivery / Cancelled
- Out for Delivery → Delivered
- Delivered → Returned
- Cancelled / Returned → (terminal states, no further transitions)

## Class Diagram
<img width="1307" height="1195" alt="LLD_Diagram" src="https://github.com/user-attachments/assets/940aeb50-ebdb-49ff-aeb0-254883626081" />

## Classes
| Class | Responsibility |
|---|---|
| `OrderStatus` (enum) | The fixed set of valid order states |
| `Order` | Holds an order's ID, current status, and status history |
| `StateChangeRequest` | A queued request: which order, which new status |
| `StateChangeValidator` | Holds the transition rulebook; checks if a move is legal |
| `OrderStore` | Stores and retrieves `Order` objects by ID |
| `StateProcessor` | Drains the queue, validates each request, applies or rejects it |

**Relationships:**
| From | To | Type | Relationship |
|---|---|---|---|
| `StateProcessor` | `StateChangeValidator` | Composition | validates via |
| `StateProcessor` | `OrderStore` | Composition | stores via |
| `StateProcessor` | `StateChangeRequest` | Association | consumes |
| `OrderStore` | `Order` | Composition | holds |
| `Order` | `OrderStatus` | Association | has current status |
| `StateChangeRequest` | `OrderStatus` | Association | requests |

## How to Run (Core Logic)
Enter an Order ID (existing or new), then the status you want to move it to.
Invalid transitions are rejected with a message; valid ones are applied
immediately. Type `exit` to quit and see final order histories.

## Frontend
A standalone HTML/CSS/JS demo UI is included at `docs/index.html`, deployed
live via GitHub Pages. It opens on a role-select screen and branches into
two views:

| View | Purpose |
|---|---|
| Seller / admin | Search any order, push it to its next status, and watch the manifest log update as the queue processes it |
| Customer | Read-only — search an order and see its current status and delivery timeline, with no ability to change it |

Both views currently share local mock order data within the page (not yet
wired to the Java backend) — see **Design Notes** below.

**Live demo (GitHub Pages):**
[Order_Tracking_Syatem](https://jenifervincya.github.io/Order_Tracking_System/)

## Design Notes
- Rejected transitions don't stop the queue — the system logs and continues
  processing the next request, since one bad event shouldn't halt the whole
  pipeline.
- History is tracked per order so the full lifecycle can be audited.
- The order store and the queue processor are kept as separate classes
  (rather than one combined class) to follow single-responsibility — storage
  concerns and queue-processing concerns can change independently.
- The seller and customer roles are intentionally separated in the frontend
  — a customer should never have access to status-change controls.
- The frontend currently explores an additional `NOT_DELIVERED` branch state
  (for a failed delivery attempt) that exists only in the JS demo layer and
  is not yet part of the core `OrderStatus` enum or transition table — a
  candidate extension if adopted into the core design.
