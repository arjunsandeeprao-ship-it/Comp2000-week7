# COMP2000 Worksheet 2 — Final Submission

**Student name:** Arjun Sandeep Rao (60846534)

**GitHub repo URL:** https://github.com/arjunsandeeprao-ship-it/Comp2000-week7

---

## 1. Design Patterns

Don't forget to submit a pdf file which shows a diagram of your pattern implemenation to supplement this text description.

> Diagram: `design/design-patterns.pdf` (one page per pattern, showing participants, roles, the key interaction and the trade-off).

**1.1.** Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

| Pattern | Where | Participants |
|---|---|---|
| **Template Method** | `src/ecosim/Animal.java` — `act(World)` is `final` | AbstractClass: `Animal`; hooks: `forage(World)`, `tryReproduce(World)`; ConcreteClasses: `Herbivore`, `Carnivore` |
| **Strategy** | `src/ecosim/strategy/` | Strategy: `MovementStrategy`; ConcreteStrategies: `RandomWalkStrategy`, `FleeOrSeekStrategy`; Context: `Animal.move()` |
| **Simple Factory** | `src/ecosim/OrganismFactory.java` | Factory: `OrganismFactory`; products: `Plant`, `Herbivore`, `Carnivore`; clients: `SimulationFrame`, `SmokeTest`, `BatchExperiment`, `World`, and the two `tryReproduce` methods |
| **Observer** *(new since Week 7)* | `src/ecosim/SimulationListener.java`, `World.addListener/notifyListeners` | Subject: `World`; Observer interface: `SimulationListener`; ConcreteObservers: `SimulationFrame` (anonymous class), `SmokeTest` (lambda + anonymous class), `BatchExperiment` (one per run) |

**1.2.** For each pattern, paste a code snippet showing the key interaction between participants.

**Template Method** — `Animal.java`. The fixed sequence calls down into the subclass only for the two steps that differ:

```java
@Override
public final void act(World world) throws OverpopulationException {
    incrementAge();
    energy -= UPKEEP_COST;
    if (energy <= 0 || getAge() > maxAge) {
        die();
        return;
    }
    move(world);
    if (isAlive()) {
        forage(world);                 // hook -> Herbivore / Carnivore
    }
    if (isAlive() && energy >= reproductionThreshold) {
        tryReproduce(world);           // hook -> Herbivore / Carnivore
    }
}

protected abstract void forage(World world);
protected abstract void tryReproduce(World world) throws OverpopulationException;
```

**Strategy** — the context delegates (`Animal.java`) and the factory wires each species' strategy (`OrganismFactory.java`):

```java
// Animal.move(): the animal never knows which concrete strategy it has
Position target = movementStrategy.nextMove(this, world);

// OrganismFactory: same strategy class, different "flee / seek" types
new Herbivore(position, energy, new FleeOrSeekStrategy(Carnivore.class, Plant.class));
new Carnivore(position, energy, new FleeOrSeekStrategy(null, Herbivore.class));
```

**Simple Factory** — `Herbivore.tryReproduce` asks the factory for a newborn instead of calling a constructor:

```java
Position birthSite = freeNeighbours.get(world.getRandom().nextInt(freeNeighbours.size()));
energy -= REPRODUCTION_COST;
world.addOrganism(OrganismFactory.createHerbivore(birthSite, REPRODUCTION_COST));
```

**Observer** — the subject (`World.java`) and two observers registered in different styles:

```java
// World (subject)
private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

public void addListener(SimulationListener listener) {
    listeners.add(Objects.requireNonNull(listener));
}

private void notifyListeners() {          // last line of tick()
    for (Class<? extends Organism> species : List.of(Plant.class, Herbivore.class, Carnivore.class)) {
        if (countOfType(species) > 0) {
            extinctSpecies.remove(species);
        } else if (extinctSpecies.add(species)) {
            listeners.forEach(l -> l.onExtinction(this, species));
        }
    }
    listeners.forEach(l -> l.onTickCompleted(this));
}

// SmokeTest: observer as a lambda (SimulationListener is a @FunctionalInterface)
world.addListener(w -> {
    if (w.getTickCount() % 20 == 0) { /* print one row of the table */ }
});

// SimulationFrame: observer as an anonymous class; onTick() is now just world.tick()
w.addListener(new SimulationListener() {
    @Override public void onTickCompleted(World world) {
        updateStats(); worldPanel.repaint(); sparklinePanel.repaint();
    }
    @Override public void onExtinction(World world, Class<? extends Organism> species) {
        lastCrash = species.getSimpleName() + "s crashed at tick " + world.getTickCount() + " (" + world.getSeason() + ")";
    }
});
```

**1.3.** For each pattern, explain why you chose it over an alternative. What trade-off did you make?

- **Template Method vs. each subclass writing its own `act()`.** The ageing/upkeep/starvation steps are identical for every animal; two hand-written copies would drift out of sync the first time one was tuned. Making `act()` `final` removes that risk. **Trade-off:** a species can no longer change the *order* of steps (e.g. eat before moving) without changing `Animal` for everyone. I accepted that because no species needs a different order, and a fixed order also makes the simulation easier to reason about.
- **Strategy vs. an `if/else` (or `instanceof`) block inside `Animal.move()`.** Herbivores and carnivores need the same "flee, else seek, else wander" behaviour with different types plugged in. With Strategy that is written once in `FleeOrSeekStrategy`, and a new species just gets a new strategy object. **Trade-off:** an extra object per animal and one level of indirection, which makes the code slightly harder to follow for someone reading `Animal` alone. Worth it to avoid duplicate or type-switching movement code.
- **Simple Factory vs. calling constructors directly / a full Abstract Factory.** Construction details (starting energy, which strategy, what to flee and seek) were needed in six places. A full Abstract Factory (a factory interface with swappable implementations) would be over-engineering, since there is only one "family" of organisms. **Trade-off:** static methods can't be swapped out for testing and the class must change when a species is added. That paid off in Week 12: fixing the newborn-energy bug touched only the factory and two call sites.
- **Observer vs. the GUI calling `world.tick()` and then refreshing everything by hand.** Before Week 8, `World` was effectively tied to the GUI's refresh sequence, and `SmokeTest` duplicated its own "after each tick, print" logic. With Observer, `World` announces "tick done" and doesn't care who listens, so the same class runs in the GUI, in the console harness, and many times in parallel in `BatchExperiment`. **Trade-off:** the order observers are called in isn't part of the design, a slow observer slows every tick, and a forgotten registration fails silently (for example, `reset()` must re-register the frame on the new `World`). I used `CopyOnWriteArrayList` so an observer can unregister itself during a callback without a `ConcurrentModificationException`.

---

## 2. Streams / Lambdas / Parallelism

**2.1.** List every file in your project that uses lambda expressions or the Streams API.

| File | What it uses |
|---|---|
| `src/ecosim/World.java` | Streams in `findNearest`, `countOfType`, `getNeighbours`, `freeNeighbours`, `neighbourPositions`, `edgeCells`; lambdas/method refs in `removeIf`, `listeners.forEach`, `maybeMigrate` factory parameter |
| `src/ecosim/PopulationStats.java` | `groupingBy` + `counting` into an `EnumMap`; a custom `Collector.of(...)` built from four lambdas; `energyOf` stream |
| `src/ecosim/BatchExperiment.java` | **Parallel** `IntStream.range(...).parallel()`; `groupingBy`, `flatMap`, `average`, `allMatch`; method references `OrganismFactory::createPlant` etc. passed as `Function<Position, ? extends Organism>` |
| `src/ecosim/Herbivore.java` | `forage`: `filter → map → findFirst → ifPresent` with a lambda body |
| `src/ecosim/Carnivore.java` | `forage`: same pipeline for prey |
| `src/ecosim/SmokeTest.java` | Observer registered as a lambda |
| `src/ecosim/SimulationFrame.java` | Method refs/lambdas for Swing listeners (`this::onTick`, `this::toggleRunning`, `e -> timer.setDelay(...)`) |
| `src/ecosim/Main.java` | `SwingUtilities.invokeLater(() -> ...)` |

**2.2.** For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

From `BatchExperiment.main`:

```java
List<RunResult> results = IntStream.range(0, runs)
        .parallel()
        .mapToObj(seed -> runOne(seed, ticks))
        .collect(Collectors.toList());

// Which season do herbivore crashes happen in?
Map<Season, Long> crashSeason = results.stream()
        .flatMap(r -> r.herbivoreCrashes.stream())
        .collect(Collectors.groupingBy(BatchExperiment::seasonAt,
                () -> new EnumMap<>(Season.class), Collectors.counting()));
```

**What it does:** the first pipeline runs the whole simulation once per seed (100 seeds × 600 ticks by default), in parallel across the CPU's cores, and collects one immutable `RunResult` per run. The second flattens every run's list of herbivore crash ticks into one stream, works out which season each crash happened in, and counts them per season in an `EnumMap` (so the output is in season order).

**Why better than a loop:**
- **Parallelism is one word.** The loop version would need an `ExecutorService`, a list of `Future`s, and code to wait for and gather them. Here, removing `.parallel()` gives the sequential version with identical results, which made it easy to check the parallel version was correct.
- **The aggregation says *what*, not *how*.** The loop equivalent of the second pipeline is a nested loop, a `switch` or map lookup on the season, and a manual "get, add one, put back" on a counter map. `groupingBy(seasonAt, counting())` states the intent directly, and the downstream collector can be swapped (e.g. `averagingLong`) without restructuring.
- **No shared mutable state.** A loop that adds into an outside `HashMap` is a data race the moment it is made parallel; collectors build per-thread partial results and merge them.

**2.3.** Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

**Yes — both a parallel stream and a custom collector.**

*Parallel stream* (`BatchExperiment`, above). Thread safety comes from confinement rather than locks:
1. **Nothing is shared between runs.** Each call to `runOne(seed, ticks)` creates its own `World`, which owns its own `java.util.Random` (seeded with the run number), grid and organism list. There is no static mutable state anywhere in the model: every `static` field is `final` (`MIGRATION_CHANCE` is read once from a system property when `World` is first loaded and never changes).
2. **Per-run observers are thread-confined.** The listener that records crash ticks writes into `ArrayList`s created *inside* `runOne`, so only the thread running that seed ever sees them.
3. **Results are immutable and combined only by collectors.** `RunResult` has only `final` fields and copies its lists with `List.copyOf`. Results are merged with `Collectors.toList()` / `groupingBy`, which combine per-thread partial results themselves. There is no `forEach` writing into an outside collection and no shared counter.
4. **Deterministic output.** Because each run is seeded by its index, the parallel and sequential versions produce identical results; only the timing changes. On a 2-core machine the 100-run batch took about 178 seconds.

*Custom collector* (`PopulationStats.energyCollector()`):

```java
Supplier<int[]> supplier = () -> new int[4];              // [count, sum, min, starving]
BiConsumer<int[], Animal> accumulator = (acc, animal) -> {
    int e = animal.getEnergy();
    acc[2] = acc[0] == 0 ? e : Math.min(acc[2], e);
    acc[0]++;
    acc[1] += e;
    if (e <= EnergyReport.STARVING_THRESHOLD) acc[3]++;
};
BinaryOperator<int[]> combiner = (a, b) -> {
    if (a[0] == 0) return b;
    if (b[0] == 0) return a;
    return new int[] {a[0] + b[0], a[1] + b[1], Math.min(a[2], b[2]), a[3] + b[3]};
};
Function<int[], EnergyReport> finisher =
        acc -> new EnergyReport(acc[0], acc[0] == 0 ? 0.0 : acc[1] / (double) acc[0], acc[2], acc[3]);
return Collector.of(supplier, accumulator, combiner, finisher);
```

It is used sequentially today (inside one `World`, which is single-threaded), but it is written to be parallel-safe: the stream framework gives each thread its own accumulator array from the supplier, and the combiner merges two partial results without mutating shared state (the empty-side checks stop an empty partial's `min = 0` from corrupting the real minimum).

*Where more parallelism could go — and why I didn't:* `World.tick()` itself is **not** parallel. Organisms act in sequence and read/write the shared grid (moving, eating neighbours, giving birth into free cells), so running `act()` in parallel would cause races such as two herbivores eating the same plant. Making it safe would need either a two-phase tick (every organism *proposes* a move in parallel, then conflicts are resolved sequentially) or locking per grid region. The `findNearest` scans inside the movement strategies are read-only and could use `parallelStream()`, but with only a few hundred organisms the overhead would be larger than the gain.

---

## 3. Uniqueness and Creativity

**3.1.** What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

Since Week 7 (in addition to the Worksheet 1 list: three-species ecosystem, Template Method, Strategy, factory, generics, exception hierarchy, sparkline, headless SmokeTest):

- **Seasons** (`Season` enum). Each season scales plant spread and ambient seeding (spring ×1.4 spread, winter ×0.25), the grid is tinted by season, and the season is shown in the stats line. *Inspired by* real predator–prey data (e.g. the classic lynx–hare cycles), where food supply isn't constant through the year.
- **Observer-based event system** (`SimulationListener`) with an extinction event, so the GUI, the console harness and the experiment runner all react to the same `World` without it knowing about any of them.
- **Parallel batch experiment** (`BatchExperiment`): runs 100 seeds in parallel and reports survival, coexistence, average crashes, and crashes by season. *Inspired by* realising I had been tuning the simulation by watching one seed, which is an anecdote rather than evidence.
- **Energy-conserving reproduction** and **migration from off-map**, both found *using* the batch experiment (see 3.2).
- **Live stats from streams**: average herbivore energy in the stats line (custom collector), and the last crash plus migrant count in the window title.

**3.2.** Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

**Surprising outcome: the ecosystem was always going to die.** In Week 7 I wrote that ambient plant seeding keeps the boom/bust cycle "running for hundreds of ticks". Running the original rules across many seeds in parallel showed that was true only for the seed I had been watching:

| Version (600 ticks, 100 seeds) | Runs with animals alive at the end | Share of ticks with all 3 species alive | Avg. crashes per run (herbivore / carnivore) |
|---|---|---|---|
| Migration off (`-Decosim.migration=0`) | 0 / 100 | 23% | 1.0 / 1.0 (one crash = permanent) |
| Final version (migration on) | 85 / 100 | 78% | 5.4 / 3.0 (crash, then recolonise) |

Two causes, both fixed:

1. **A hidden energy bug.** A newborn got the full `INITIAL_ENERGY` (herbivore 40) while the parent paid only `REPRODUCTION_COST` (30), so *every birth created 10 energy out of nothing*. That turbo-charged the herbivore boom, which wiped out the plants, which starved everything. Fix, in the factory:

   ```java
   // A newborn carries exactly what its parent paid -- energy is conserved across birth.
   public static Herbivore createHerbivore(Position position, int energy) {
       return new Herbivore(position, energy,
               new FleeOrSeekStrategy(Carnivore.class, Plant.class));
   }
   ```

2. **A sealed box can't recover.** Once one crash removes a species, nothing can bring it back. Real valleys get recolonised from neighbouring ones, so I added migration: while a species is locally extinct, there's a 10% chance per tick that one animal walks in on a free edge cell.

   ```java
   private void maybeMigrate(Class<? extends Animal> species,
                             Function<Position, ? extends Animal> factory) {
       if (countOfType(species) > 0 || random.nextDouble() >= MIGRATION_CHANCE) {
           return;
       }
       List<Position> freeEdge = edgeCells().filter(this::isFree).collect(Collectors.toList());
       ...
       addOrganism(factory.apply(freeEdge.get(random.nextInt(freeEdge.size()))));
   }
   ```

**Emergent behaviour:** nothing in the code says *when* species should crash, yet herbivore crashes are not spread evenly over the year. In the final 100-run batch, herbivore crashes by season were SPRING 124, SUMMER 92, AUTUMN 138, WINTER 183 — about 60% in autumn and winter and fewest in summer. The likely mechanism is a lag: herbivores boom on the spring/summer plant surplus, overshoot, and then run out of food as plant growth slows to a quarter of normal in winter. Carnivore crashes are spread much more evenly (66 / 83 / 65 / 88), which suggests carnivores die from the herbivore cycle itself rather than directly from the seasons. Watching the GUI shows the same story: a grid that turns almost completely green after the herbivores have gone, then a herbivore migrant arrives on the edge and the boom starts again.

I also tried "satiation" (animals don't eat when full) as a stabiliser. The batch numbers showed it made no real difference, so I removed it rather than keep tuning code that didn't earn its place.

**3.3.** If you had another 6 weeks, what would you add or change?

- **A two-phase, parallel tick.** Every organism proposes its action in parallel, then conflicts (two animals choosing the same cell or the same plant) are resolved sequentially. This would let `World.tick()` itself use all cores and would remove the current bias where organisms earlier in the list get first pick of food.
- **Real unit tests (JUnit)** for `CircularBuffer`, `World.moveOrganism` legality and the energy-conservation rule, instead of relying on the SmokeTest output.
- **Genetics:** each animal carries heritable traits (sight radius, speed, reproduction threshold) that mutate slightly at birth, so the populations can evolve. The batch experiment could then measure whether, for example, herbivores evolve longer sight under predator pressure.
- **A spatial refuge** (terrain cells such as dense bushes that carnivores can't enter), as an in-world alternative to migration for keeping prey alive.
- **Parameter sweeps in `BatchExperiment`** with CSV output and a chart, to find the most stable settings systematically instead of by hand.

---

## 4. Log Book and Final Reflection

**4.1.** Attach or link your log book entries for Weeks 7–13.

Submitted as a single file alongside this worksheet: `Logbook-Arjun-Sandeep-Rao.pdf` (Weeks 1–7 followed by Weeks 8–13). The Weeks 8–13 entries are also in the repository at https://github.com/arjunsandeeprao-ship-it/Comp2000-week7/blob/main/logbook/logbook-weeks-8-13.md

**4.2.** Reflect on the semester:

- What was the most important concept you learned?

  Programming to interfaces so that the core of a program doesn't need to know about the parts around it. `MovementStrategy` meant `Animal` doesn't know how it moves; `SimulationListener` meant `World` doesn't know it is being drawn. The payoff was concrete: once `World` stopped depending on the GUI, I could run it 100 times in parallel to test it — and that is how I found the bugs described in 3.2.

- What design trade-off did you face, and how did you resolve it?

  Realism versus a simulation that keeps running. The "pure" rules (organisms only interact with their neighbours on a closed grid) always ended in total extinction; adding migration from off-map is less pure but matches how real ecosystems recover. I resolved it with evidence rather than by eye: the batch experiment measured survival with migration on and off, and I kept only the changes that measurably helped (migration and the energy fix) and removed the one that didn't (satiation).

- If you started over, what would you do differently?

  Build the headless, multi-seed experiment in the first few weeks, not the last. I spent weeks tuning constants against one seed in the GUI and wrote claims in Worksheet 1 that a 30-second batch run would have disproved. I would also add unit tests early for the rules that must always hold, such as energy conservation, which would have caught the newborn-energy bug immediately.

- What feedback from the Week 7 submission did you act on?

  The Week 7 feedback was very positive, so there was no specific weakness to fix. I treated it as a sign the foundations (the class hierarchy, Template Method, Strategy, generics and the exception hierarchy) were solid, and built on them rather than reworking them. Instead, I went after the weakest part of my own Week 7 submission: claims I hadn't tested. In Worksheet 1, I said ambient plant seeding kept the ecosystem cycling "for hundreds of ticks", based on watching one seed. I wrote `BatchExperiment` specifically to check that claim across many seeds, found it was wrong (every run eventually went extinct), and fixed the two causes (the newborn-energy bug and the lack of migration). I also added the Observer pattern and the stream/collector work, so the second half of the semester extends the design rather than only describing it.
