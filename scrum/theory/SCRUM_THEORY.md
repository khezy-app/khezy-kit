# SCRUM Theory — Coding Agent Knowledge Base

> Source: [Scrum Guide 2020](https://scrumguides.org/scrum-guide.html) — Ken Schwaber & Jeff Sutherland

---

## 1. What is Scrum?

Scrum is a **lightweight framework** for solving complex, adaptive problems and delivering value. It is founded on:

- **Empiricism** — knowledge comes from experience and observation, not prediction
- **Lean thinking** — eliminate waste, focus on essentials

Scrum does **not** prescribe specific engineering practices, tools, or techniques. It defines only the minimal rules for roles, events, artifacts, and their interactions. Teams fill in the rest.

---

## 2. The Three Pillars

| Pillar | Meaning |
|---|---|
| **Transparency** | Work, progress, and artifacts must be visible to everyone. Low transparency → bad decisions. |
| **Inspection** | Frequently examine artifacts and progress toward goals to detect problems. |
| **Adaptation** | Adjust immediately when deviations are found. Delayed adaptation increases risk. |

These pillars are operationalized through the five Scrum Events.

---

## 3. The Five Values

**Commitment, Focus, Openness, Respect, Courage**

When the team embodies these values, the three pillars come to life and trust is built.

---

## 4. Scrum Team (Three Accountabilities)

| Role | Key Accountability |
|---|---|
| **Product Owner** | Maximizes value; manages Product Backlog (ordering, clarity, transparency). One person, not a committee. |
| **Scrum Master** | Ensures Scrum is understood and enacted; coaches team, removes impediments, facilitates events. |
| **Developers** | Create a usable Increment each Sprint; plan work, maintain quality via Definition of Done, adapt daily. |

The team is **cross-functional** (all skills needed to deliver value) and **self-managing** (decides who does what, when, how). Typically ≤10 people.

---

## 5. The Five Events (Timeboxed)

| Event | Max Duration | Purpose |
|---|---|---|
| **Sprint** | ≤1 month | Container event; turns ideas into value. |
| **Sprint Planning** | 8h (1mo) | Define Sprint Goal (why), select backlog items (what), plan delivery (how). |
| **Daily Scrum** | 15 min | Inspect progress toward Sprint Goal; adapt plan for next 24h. |
| **Sprint Review** | 4h (1mo) | Inspect outcome with stakeholders; adapt Product Backlog. |
| **Sprint Retrospective** | 3h (1mo) | Inspect team processes, interactions, tools; plan quality/effectiveness improvements. |

*Shorter Sprints → smaller timeboxes proportionally.*

---

## 6. The Three Artifacts & Their Commitments

| Artifact | Description | Commitment |
|---|---|---|
| **Product Backlog** | Emergent, ordered list of what is needed to improve the product. | **Product Goal** (long-term target) |
| **Sprint Backlog** | Sprint Goal + selected items + plan for delivery. | **Sprint Goal** (single Sprint objective) |
| **Increment** | Usable, verified stepping stone toward Product Goal. | **Definition of Done** (quality standard) |

The commitments reinforce empiricism: they provide a clear basis for inspection and adaptation.

---

## 7. Key Principles for Software Development Teams

1. **Sprints are fixed-length** — no extensions; if Sprint Goal becomes obsolete, Product Owner may cancel.
2. **Quality never decreases during a Sprint** — Definition of Done is non-negotiable.
3. **Scope can be renegotiated** with Product Owner as learning emerges.
4. **Forecasting tools** (burn-downs, burn-ups) are useful but do **not** replace empiricism.
5. **Scrum is immutable** — partial implementation is not Scrum; the framework functions as a complete container.
6. **Cross-functionality** means the team has all skills to deliver; self-management means no external assignment of tasks.
7. **The Sprint Review is a working session**, not a presentation/demo.
8. **Improvements from Retrospective** can be added to the next Sprint Backlog immediately.

---

## 8. Common Misconceptions (for Agent Awareness)

| Misconception | Correction |
|---|---|
| Scrum = software development methodology | Scrum is a **framework**; it wraps around any engineering practices. |
| Product Owner = manager/boss | The PO is accountable for value maximization, not people management. |
| Daily Scrum = status report | It is an **inspection & adaptation** event for Developers to plan the next 24h. |
| Scrum Master = project manager | The SM is a **servant leader** and coach, not a task assigner. |
| Sprint = release cycle | An Increment may be released multiple times per Sprint; Sprint Review is not a release gate. |
| Scrum guarantees faster delivery | Scrum increases **transparency and adaptability**, enabling better decisions about delivery. |

---

## 9. Answering "Scrum/Agile" Queries

When a user asks about Scrum or Agile, the coding agent should:

1. **Distinguish Scrum from Agile** — Agile is a philosophy (Manifesto); Scrum is a specific framework that embodies Agile principles.
2. **Reference the three pillars** — explain why empiricism matters.
3. **Describe the roles** — emphasize self-management and cross-functionality.
4. **Explain the events and their purpose** — especially the Sprint as the heartbeat.
5. **Connect artifacts to commitments** — Product Goal, Sprint Goal, Definition of Done.
6. **Avoid prescribing specific tools** — Scrum leaves technique choices open.
7. **Clarify common misconceptions** (see section 8).
8. **If asked "is this Scrum?"** — check against the immutable framework: all three accountabilities present? All five events held? All three artifacts with commitments? If any element is missing, it's not Scrum.
