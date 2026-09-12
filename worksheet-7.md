<!--
NOTE FROM DRAFTING SESSION (delete this comment before you submit):
This worksheet was drafted against the "Ecosystem Simulation" codebase built with Claude in this
session (see /ecosim-project). Every answer that points at the code, generics, exceptions, design
patterns, or class structure is factually accurate to that codebase and safe to submit as-is IF you
have read the code and agree with the reasoning -- the worksheet is explicitly asking YOU to explain
your design decisions, so skim World.java, Animal.java and the exceptions package before your
showcase so you can defend these answers out loud.

Sections I could NOT honestly fill in for you, because I have no visibility into them, are marked
with [[FILL IN: ...]] -- your name/ID, your actual git branch/PR workflow with your team, your real
commit-percentage, and personal reflections (what you personally learned, which week, what you found
hardest). Please replace every [[FILL IN: ...]] marker before submitting.
-->

# COMP2000 Worksheet 1 — Mid-Semester Submission

**Student name:** [[FILL IN: your full name]]

**Student ID:** [[FILL IN: your student ID]]

**GitHub repo URL:** [[FILL IN: the URL of your own fork of your team's repository — not your team's URL]]

---

## 1. Version Control

**1.1.** Paste the first 10 lines of the output of `git log --graph --oneline --all` from your repository:

```
[[FILL IN: after you push this code to your own fork, run
  git log --graph --oneline --all
in that repository and paste the first 10 lines here. A sample history for the
codebase as built in this session is provided in git-log-sample.txt in this
folder — if your team already has a shared repo, push these commits into your
fork of it (see push-instructions.md) and re-run the command, since your real
graph will also include your team's existing commits/branches.]]
```

**1.2.** Describe your workflow. Did you use branches? Pull directions? Pull requests?

[[FILL IN — describe your ACTUAL workflow with your team, not a generic answer. If you are unsure what to write: the codebase from this session was built as a straightforward sequence of commits on `main` (no branches), because it was written solo in one sitting as a starting scaffold for you to build on with your team. If your team is working from a shared repository with feature branches and pull requests, describe that real process instead — e.g. "each team member worked on a feature branch (feature/predator-ai, feature/gui, ...) and opened a pull request into main, reviewed by at least one other member before merging." Only claim a workflow you actually used — this is checked against your real commit graph.]]

**1.3.** Estimate the percentage of commits you contributed relative to the total in your repository.

[[FILL IN — run `git shortlog -sn --all` in your repository and calculate your commits ÷ total commits. If you are submitting the scaffold from this session largely as-is before collaborating further with your team, say so honestly (e.g. "100% of the current history, as I built the initial scaffold solo; my team and I will divide new feature work going forward").]]

---

## 2. Program Design

Don't forget to submit a pdf file of your program design along with this file.

*(See `design/program-design.pdf` — generated in this session, covers the same ground as this section in more depth, with a class diagram.)*

**2.1.** List every class in your project and write 1–2 sentences describing its responsibility.

- **`Position`** — Immutable (x, y) grid coordinate. Every "mutating" operation (`translate`) returns a new instance rather than changing the original, so a `Position` handed out by `getPosition()` can never be used to secretly relocate an organism.
- **`Locatable`** (interface) — Anything that occupies a cell: just `getPosition()`.
- **`Edible`** (interface) — Anything another organism can eat: `getEnergyValue()` and `markConsumed()`. Implemented by `Plant` and `Animal`.
- **`Organism`** (abstract) — Shared state for everything on the grid: position, alive flag, age. Declares `act(World)` and the rendering hooks `getColor()`/`getSymbol()` as abstract so every subclass must supply its own behaviour and appearance.
- **`Plant`** — Stationary `Edible`. Grows with age and, once mature, may spread a copy of itself into a free neighbouring cell.
- **`Animal`** (abstract) — Shared state/behaviour for anything that moves and eats: energy, upkeep cost, ageing, starvation, a `MovementStrategy`. `act(World)` is a **final template method**; `forage(World)` and `tryReproduce(World)` are the abstract hooks that `Herbivore`/`Carnivore` fill in.
- **`Herbivore`** — Eats `Plant`, flees `Carnivore` (via its `MovementStrategy`), reproduces into a free neighbouring cell.
- **`Carnivore`** — Eats `Herbivore`, has no predator of its own.
- **`MovementStrategy`** (interface) — Decides where an `Animal` moves next.
- **`RandomWalkStrategy`** — Wanders to a random free neighbouring cell.
- **`FleeOrSeekStrategy`** — Generic-token-based strategy: flee the nearest instance of a given threat type, else seek the nearest instance of a given food type, else fall back to random walk.
- **`OrganismFactory`** — Centralises how a fresh `Plant`/`Herbivore`/`Carnivore` is constructed (starting energy, assigned movement strategy), so that knowledge lives in one place instead of being duplicated at every call site that creates an organism.
- **`World`** — Owns the grid (`Map<Position,Organism>`) and the master `List<Organism>`. Runs `tick()`, enforces move/placement legality (throwing the custom exceptions below), and exposes generic helpers (`findNearest`, `countOfType`) used by the rest of the code.
- **`CircularBuffer<T>`** — A small generic ring buffer (implements `Iterable<T>`) holding the last 120 population counts per species, used to draw the sparkline.
- **`SimulationException`** (abstract, checked) — Base type for the two custom exceptions below.
- **`InvalidPositionException`** — A move/placement target is out of bounds or already occupied.
- **`OverpopulationException`** — The grid is full; a new organism has nowhere to go.
- **`WorldPanel`** — `JPanel` that draws the grid, calling each organism's polymorphic `getColor()`.
- **`SimulationFrame`** — The `JFrame`: hosts `WorldPanel`, a population sparkline, and start/pause/step/reset/speed controls driven by a `javax.swing.Timer`.
- **`SmokeTest`** — A headless (no GUI) console harness that ticks a `World` 400 times and prints population snapshots, useful for confirming model logic without watching the window.
- **`Main`** — Entry point; launches `SimulationFrame` on the Swing event thread.

**2.2.** Identify any inheritance relationships. For each parent–child pair, list what the child inherits and what it overrides.

- **`Organism` → `Plant`.** Inherits: `position`/`alive`/`age` bookkeeping, `getPosition()`, `die()`, `isAlive()`. Overrides: `act(World)` (grow, occasionally spread), `getColor()` (greener as it matures), `getSymbol()` (`'*'`). Also implements `Edible`.
- **`Organism` → `Animal`.** Inherits the same base bookkeeping. Overrides `act(World)` once, as a **final** template method (age → pay upkeep → check death → move → forage → maybe reproduce), and adds two new abstract hooks (`forage`, `tryReproduce`) for its own children to override. Also implements `Edible`.
- **`Animal` → `Herbivore`.** Inherits all of `Animal`'s energy/ageing/movement machinery and the fixed `act()` sequence. Overrides `forage(World)` (eat an adjacent `Plant`), `tryReproduce(World)` (spawn a `Herbivore` via `OrganismFactory`), `getColor()` (blue), `getSymbol()` (`'h'`).
- **`Animal` → `Carnivore`.** Same inheritance as `Herbivore`. Overrides `forage(World)` (eat an adjacent `Herbivore`), `tryReproduce(World)` (spawn a `Carnivore`), `getColor()` (red), `getSymbol()` (`'C'`).

**2.3.** Pick the class that you think has the best design. Explain why.

[[You can use this answer as written, or substitute your own opinion — this question is asking for your judgement.]]

`Animal` is the strongest-designed class in the project. It uses the **Template Method** pattern: `act(World)` is declared `final` and fixes the exact sequence every animal follows each tick (age, pay upkeep, check for death, move, forage, maybe reproduce), while the two genuinely species-specific decisions — *what counts as food* (`forage`) and *what a newborn looks like* (`tryReproduce`) — are left as abstract hooks for `Herbivore` and `Carnivore` to fill in. This is a better design than letting each subclass write its own `act()` because the ageing/starvation bookkeeping is exactly the kind of logic that quietly drifts out of sync between two independently maintained copies; sealing the sequence with `final` in one place removes that risk completely, while still leaving real room for each species to differ.

**2.4.** Paste one code snippet that demonstrates your use of polymorphism or encapsulation. Include an explanation of *how* this demonstrates polymorphism or encapsulation. Give a reference to a provided reading that talks about this type of polymorphism or encapsulation.

*Polymorphism* — from `WorldPanel.java`:

```java
for (Organism organism : world.getOrganisms()) {
    if (!organism.isAlive()) {
        continue;
    }
    g2.setColor(organism.getColor());
    int px = organism.getPosition().getX() * cellSize;
    int py = organism.getPosition().getY() * cellSize;
    if (organism instanceof Plant) {
        g2.fillRect(px + 2, py + 2, cellSize - 4, cellSize - 4);
    } else {
        g2.fillOval(px + 1, py + 1, cellSize - 2, cellSize - 2);
    }
}
```

This loop draws every `Plant`, `Herbivore` and `Carnivore` on the grid without ever checking which concrete species it is drawing a *colour* for — it just calls the polymorphic `organism.getColor()`, and each class's own override (see 2.2) supplies the right answer. Only the square-vs-circle shape needs one `instanceof` check, because that is a genuinely different rendering primitive, not a colour. Adding a fourth species later would need zero changes here. [[FILL IN: replace with the specific reading your course provided on dynamic dispatch / polymorphism — e.g. your unit's lecture notes or textbook chapter reference. A commonly used one for this exact idea is Gamma et al., *Design Patterns* (1994), the discussion of "program to an interface, not an implementation."]]

*Encapsulation* — from `Position.java`:

```java
public final class Position {
    private final int x;
    private final int y;

    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }
    // ... equals/hashCode/toString, no setters
}
```

Every field is `final` and the only way to get a "moved" position is `translate()`, which returns a **new** object rather than mutating `this`. That means any code holding a reference to an organism's `Position` (via `getPosition()`) can never use it to move the organism — the only legal way to change an organism's location is `World.moveOrganism()`, which is exactly where bounds and collision checks are enforced. [[FILL IN: reference your course's reading on immutability as encapsulation — e.g. Joshua Bloch, *Effective Java*, Item 17: "Minimize mutability," if that is (or is similar to) what was assigned.]]

---

## 3. Generics and Exceptions

**3.1.** List every place your code uses generics (e.g. `ArrayList<Actor>`, `Optional<Cell>`, `HashMap<String, Team>`). If you deliberately used none, explain why.

Off-the-shelf generics used throughout: `List<Organism>` and `Map<Position, Organism>` in `World`, `Optional<T>` as the return type of `World.findNearest`, and `List<Position>` for neighbour queries.

Two uses were designed specifically for this project rather than reached for by default:

- **`World.findNearest` / `World.countOfType`** — generic *methods* parameterised with a `Class<T>` token:
  ```java
  public <T extends Organism> Optional<T> findNearest(Position from, Class<T> type, int radius) {
      return organisms.stream()
              .filter(Organism::isAlive)
              .filter(type::isInstance)
              .filter(o -> from.distanceTo(o.getPosition()) <= radius)
              .min(Comparator.comparingInt(o -> from.distanceTo(o.getPosition())))
              .map(type::cast);
  }
  ```
  Before this, "find the nearest `Plant`" and "find the nearest `Carnivore`" would have needed two near-identical methods (or one method returning `Organism` with a manual cast at every call site — exactly what generics exist to avoid). `FleeOrSeekStrategy` calls this once for a threat type and once for a food type, fully typed both times.
- **`CircularBuffer<T>`** — a small generic ring-buffer type written for this project (not pulled from the JDK), used to hold the last 120 population counts for each of the three species without hard-coding it to `int`/`Integer`.

**Deliberately not generic:** `World` itself is *not* `World<T extends Organism>`, even though it looks like the obvious candidate. `World` manages a heterogeneous mix of `Plant`, `Herbivore` and `Carnivore` at the same time, so a single type parameter on the class would misdescribe what it actually stores. Type safety for a *specific* species is instead provided by the two targeted generic methods above, exactly where it's needed, without forcing an incorrect single-type constraint onto the whole class.

**3.2.** List every place your code handles exceptions (try/catch, throws, custom exception classes). What error is each protecting against?

Custom checked exception hierarchy: `SimulationException` (abstract base) → `InvalidPositionException` (a move/placement target is out of bounds or occupied) and `OverpopulationException` (the grid is full, no room for a new organism). Both are checked deliberately — a crowded grid is a normal, recoverable event in this simulation, not a bug, so the compiler forces every caller to decide how to react instead of letting a bad tick silently propagate and crash the Swing event thread.

`OverpopulationException` is handled three different ways in three different places, on purpose:

1. **`Plant.trySpread`, `Herbivore.tryReproduce`, `Carnivore.tryReproduce`** — caught immediately and silently ignored. A full grid just means "no offspring this tick," which happens routinely.
2. **`World.tick()`** — caught centrally around each organism's `act()` call and tallied into an `overpopulationEvents` counter (shown live in the GUI), so one crowded organism can't abort the whole tick, but the aggregate frequency is still visible.
3. **`SimulationFrame.seedWorld()`** — allowed to *propagate* out of the seeding loop to a top-level `catch` that shows a `JOptionPane` dialog, because too many initial organisms for the chosen grid size is a configuration mistake the user should be told about immediately, not a routine event to swallow.

`InvalidPositionException` is handled at one narrow point, `Animal.move()`:

```java
try {
    world.moveOrganism(this, target);
} catch (InvalidPositionException e) {
    // Recoverable: the strategy's chosen cell was taken by another organism
    // earlier in this tick's iteration, or is out of bounds. Standing still
    // for one tick is an acceptable fallback.
}
```

It is never declared on `act()` or allowed to propagate further, because by the time `World.moveOrganism` throws it, the only sensible recovery (try again next tick) is entirely local to the animal that was trying to move — no other part of the system needs to know about it. `World.addOrganism` and `World.moveOrganism` also each guard against `IllegalArgumentException` indirectly by validating bounds/occupancy *before* mutating any state, so a rejected move/placement never leaves the grid in a half-updated state.

**3.3.** Paste a code snippet showing either a generic class/method or a try/catch block.

```java
public class CircularBuffer<T> implements Iterable<T> {
    private final Object[] elements;
    private int start = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.elements = new Object[capacity];
    }

    public void add(T value) {
        int writeIndex = (start + size) % elements.length;
        elements[writeIndex] = value;
        if (size < elements.length) {
            size++;
        } else {
            start = (start + 1) % elements.length; // overwrite oldest
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " for size " + size);
        }
        return (T) elements[(start + index) % elements.length];
    }

    @Override
    public Iterator<T> iterator() { /* ... */ }
}
```

This is a fixed-capacity ring buffer parameterised over `T`, used three times in `World` (once per species) to keep bounded-length population history for the GUI's sparkline. Implementing `Iterable<T>` lets the sparkline-drawing code in `SimulationFrame` use a plain for-each loop instead of manual index arithmetic.

---

## 4. Log Book

Don't forget to submit an electronic version of your logbook.

*(See `logbook/logbook-template.md` in this folder for a week-by-week template you can back-fill and continue using through to Week 13.)*

**4.1.** Which week's activity taught you the most? What did you learn?

[[FILL IN — this has to be your own reflection on your own week-by-week learning; I can't write this one for you honestly. If it helps as a prompt: think about whichever week you personally found hardest to get working — for a lot of students building something like this, that's the week you first had to make inheritance/polymorphism decisions (should this be an abstract class or an interface? what goes in the parent vs. the child?), or the week exceptions stopped being "something Java makes me write" and started being a real design decision (where should this be caught, and why here and not somewhere else?). Say what you actually found hard, what you actually learned, and how it actually did or didn't change your code.]]

---

## 5. Uniqueness and Creativity

**5.1.** List everything you added to the project that was not part of the in-class activities.

- A full predator–prey–plant ecosystem (not the in-class Game of Life), with three interacting species instead of one uniform cell type.
- A **Template Method** in `Animal.act()` (final, with two abstract hooks) so ageing/upkeep/starvation logic is written exactly once and cannot drift out of sync between species.
- A **Strategy pattern** (`MovementStrategy` / `RandomWalkStrategy` / `FleeOrSeekStrategy`) so "flee my predator, otherwise seek my food, otherwise wander" is written once and reused by both animal species with different threat/food types.
- A **Simple Factory** (`OrganismFactory`) centralising species construction constants in one place.
- A generic `Class<T>`-token search method (`World.findNearest`) and a hand-written generic `CircularBuffer<T>` (not a JDK collection).
- A custom **checked exception hierarchy** (`SimulationException` → `InvalidPositionException`, `OverpopulationException`) handled three different, deliberate ways depending on context (silently ignored / centrally counted / surfaced to the user).
- An ambient plant-reseeding mechanic (`World.maybeSeedAmbientPlant`) added specifically to keep the boom/bust population cycle running for hundreds of ticks instead of collapsing to total extinction after one overgrazing event — a tuning decision made by watching the headless `SmokeTest` output.
- A live population-history **sparkline** chart drawn alongside the grid, reading from the `CircularBuffer` history.
- A headless `SmokeTest` console harness for validating ecosystem dynamics (400 ticks, population snapshots every 20) without needing the GUI running.

[[FILL IN: once you and your team have built on this further, add whatever you personally contributed on top of this scaffold.]]

**5.2.** Which feature required the most independent research or problem-solving? What did you learn from it?

[[FILL IN — personal reflection. One genuine candidate from this codebase, if you want a starting point to write from in your own words: getting the predator-prey-plant populations to oscillate for a long time instead of collapsing to extinction within ~150 ticks. The first version was tuned so herbivores boomed, ate every plant, then starved along with the carnivores that depended on them, and the ecosystem simply died — visible only by running the headless SmokeTest and watching all three counts hit zero. Fixing it meant adding a small ambient re-seeding chance for plants (see World.maybeSeedAmbientPlant) to model the fact that real plant populations are replenished by wind/animal-carried seed from beyond one grid cell's neighbours, which a purely local spreading rule can't capture.]]

**5.3.** Paste one code snippet that you are especially proud of. Explain why it goes beyond what was done in class.

```java
public class FleeOrSeekStrategy implements MovementStrategy {
    private final Class<? extends Organism> threatType; // nullable
    private final Class<? extends Organism> foodType;
    private final RandomWalkStrategy fallback = new RandomWalkStrategy();

    public FleeOrSeekStrategy(Class<? extends Organism> threatType, Class<? extends Organism> foodType) {
        this.threatType = threatType;
        this.foodType = foodType;
    }

    @Override
    public Position nextMove(Animal self, World world) {
        if (threatType != null) {
            Optional<? extends Organism> threat =
                    world.findNearest(self.getPosition(), threatType, SIGHT_RADIUS);
            if (threat.isPresent()) {
                return stepAwayFrom(self, world, threat.get().getPosition());
            }
        }
        Optional<? extends Organism> food =
                world.findNearest(self.getPosition(), foodType, SIGHT_RADIUS);
        if (food.isPresent()) {
            return stepToward(self, world, food.get().getPosition());
        }
        return fallback.nextMove(self, world);
    }
    // ...
}
```

This goes beyond the in-class material because it is a single, reusable "flee X, otherwise seek Y, otherwise wander" behaviour, generic over *which* organism types X and Y are, rather than one hard-coded "herbivore AI" method and a near-duplicate "carnivore AI" method. `OrganismFactory` wires up `new FleeOrSeekStrategy(Carnivore.class, Plant.class)` for herbivores and `new FleeOrSeekStrategy(null, Herbivore.class)` for carnivores (carnivores have no predator), and `Animal` never needs to know which concrete strategy it was given. Adding a third predator tier later (something that eats carnivores) would need a one-line change to a factory method and zero changes to `Animal`, `Herbivore`, or `Carnivore` themselves.

[[FILL IN: if you build something further and are prouder of that, swap it in here.]]
