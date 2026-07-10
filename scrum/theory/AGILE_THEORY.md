# AGILE Theory — Coding Agent Knowledge Base

> Sources: [Agile Manifesto (2001)](https://agilemanifesto.org/), [GeeksforGeeks — Agile Methodology](https://www.geeksforgeeks.org/software-testing/what-is-agile-methodology/)

---

## 1. What is Agile?

Agile is a **philosophy and set of principles** for software development that prioritizes:

- **Individuals and interactions** over processes and tools
- **Working software** over comprehensive documentation
- **Customer collaboration** over contract negotiation
- **Responding to change** over following a plan

It emerged in 2001 from the **Agile Manifesto** as a response to heavyweight, plan-driven methodologies (Waterfall). Agile is **not a single methodology** — it is an umbrella term for multiple frameworks that share the same values and principles.

---

## 2. The 12 Agile Principles

| # | Principle |
|---|---|
| 1 | **Customer satisfaction** through early and continuous delivery of valuable software |
| 2 | **Welcome changing requirements**, even late in development |
| 3 | **Deliver working software frequently** (weeks rather than months) |
| 4 | **Business people and developers work together** daily throughout the project |
| 5 | **Build projects around motivated individuals**; give them trust, support, and environment |
| 6 | **Face-to-face conversation** is the most efficient communication method |
| 7 | **Working software is the primary measure of progress** |
| 8 | **Sustainable pace** — sponsors, developers, users should be able to maintain constant pace |
| 9 | **Continuous attention to technical excellence** and good design enhances agility |
| 10 | **Simplicity** — maximizing work not done is essential |
| 11 | **Self-organizing teams** produce the best architectures, requirements, and designs |
| 12 | **Regularly reflect** on how to become more effective, then tune and adjust behavior |

---

## 3. Agile vs Waterfall

| Aspect | Agile | Waterfall |
|---|---|---|
| Requirements | Emergent, changeable | Fully defined upfront |
| Delivery | Iterative, incremental (small batches) | Single big-bang release |
| Customer involvement | Continuous throughout | Only at beginning and end |
| Risk | Lower (early feedback catches issues) | Higher (issues found late) |
| Documentation | Just enough, working software prioritized | Comprehensive upfront docs |
| Team structure | Cross-functional, self-organizing | Siloed by role |

---

## 4. Agile Development Lifecycle Stages

1. **Requirement Gathering** — Identify stakeholder needs, scope, budget, schedule
2. **Design** — Architecture, detailed specs, UI design
3. **Development (Coding)** — Write code with unit testing
4. **Testing** — Integration, system, UAT, performance testing
5. **Deployment** — Release to production, user training
6. **Review (Maintenance)** — Post-release fixes, updates, patches

Unlike Waterfall, these stages are **not strictly sequential** — they repeat in each iteration.

---

## 5. Major Agile Frameworks

| Framework | Key Characteristics |
|---|---|
| **Scrum** | Fixed-length Sprints; 3 roles (PO, SM, Devs); 5 events; 3 artifacts |
| **Kanban** | Continuous flow; visualize workflow (board); limit WIP; no prescribed roles |
| **Extreme Programming (XP)** | Pair programming, TDD, continuous integration, collective ownership, frequent releases |
| **Feature Driven Development (FDD)** | Build feature list → plan by feature → design by feature → build by feature |
| **Adaptive Software Development (ASD)** | Speculate → Collaborate → Learn cycle |
| **Dynamic Systems Development Method (DSDM)** | Full lifecycle Agile with feasibility/business study, functional model, design/build, implementation |
| **Lean Software Development** | Eliminate waste, amplify learning, decide as late as possible, deliver fast, empower team, build integrity in, see the whole |

---

## 6. Common Agile Practices (not framework-specific)

- **User Stories** — Short, simple descriptions of a feature from end-user perspective
- **Backlog Grooming** — Refining and prioritizing the product backlog
- **Timeboxing** — Fixed time windows for work/events
- **Stand-ups** — Daily short sync meetings
- **Retrospectives** — Regular team reflection for improvement
- **Test-Driven Development (TDD)** — Write tests before code
- **Continuous Integration (CI)** — Merge code changes frequently, automated builds/tests
- **Continuous Delivery (CD)** — Automated deployment pipeline
- **Burndown/Burnup Charts** — Visual progress tracking
- **Story Points / Velocity** — Relative estimation techniques

---

## 7. When to Use (and Not Use) Agile

**Good fit:**
- Unclear or evolving requirements
- Complex work that benefits from iteration/feedback
- Customer collaboration is feasible and valued
- Fast time-to-market is important
- Small-to-medium cross-functional teams

**Less suitable:**
- Highly regulated environments requiring extensive documentation upfront
- Fixed-price/fixed-scope contracts with no flexibility
- Distributed teams with significant timezone/cultural barriers
- Large organizations without Agile adoption support

---

## 8. Misconceptions (for Agent Awareness)

| Misconception | Correction |
|---|---|
| Agile = no planning | Agile plans continuously; plans are updated with new information |
| Agile = no documentation | Agile values "just enough" documentation; working software is the priority |
| Agile = Scrum | Scrum is one framework *under* the Agile umbrella; there are many others |
| Agile means no deadlines | Agile uses timeboxes (Sprints/iterations) with fixed durations |
| Agile is only for software | Originated in software, but now used in marketing, HR, hardware, etc. |
| Agile guarantees faster delivery | Agile enables faster *feedback*; speed depends on team, domain, complexity |
| Agile = cowboy coding | Agile emphasizes technical excellence, testing, and sustainable pace |

---

## 9. Answering "Agile" Queries

When a user asks about Agile, the coding agent should:

1. **Start with the Agile Manifesto** — the 4 value statements are the foundation
2. **Explain the 12 principles** — these are what make a method "Agile"
3. **Distinguish Agile from frameworks** — Agile is the philosophy; Scrum/Kanban/XP are implementations
4. **Contrast with Waterfall** — iterative vs sequential, emergent vs fixed requirements
5. **Avoid prescribing one framework** — ask about their context (team size, project type, constraints)
6. **Clarify common misconceptions** (section 8)
7. **Reference practices** (stand-ups, retrospectives, TDD, CI/CD) as *tools*, not requirements of Agile
8. **If asked "is this Agile?"** — check against the 4 values and 12 principles; is the team delivering iteratively, collaborating with customers, welcoming change, and self-organizing?
