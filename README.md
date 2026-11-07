# Ecosystem Simulation — COMP2000

A grid-based predator–prey–plant ecosystem simulation built with **only standard JRE classes**
(Swing for the GUI, `java.util`/`java.util.stream` for the model). Plants grow and spread,
herbivores eat plants and flee carnivores, and carnivores hunt herbivores — the three populations
rise and fall in an emergent boom/bust cycle, shaped by four seasons and kept going by occasional
migration from off-map when a species is locally wiped out.

See `worksheet-13.md` (final, Worksheet 2) and `worksheet-7.md` (mid-semester) for the worksheet
answers, `design/design-patterns.pdf` for the pattern diagrams, and `design/program-design.pdf` for the full design
write-up (class list, diagram, and the reasoning behind the inheritance hierarchy, the Template
Method and Strategy patterns, the generics, and the exception handling).

## Build & run

Requires a JDK (developed against Java 21, but only uses language features safe on much older
versions too). No build tool, no external dependencies.

```bash
javac -d bin $(find src -name "*.java")

# Watch it run (Swing GUI):
java -cp bin ecosim.Main

# Or check the model logic headlessly (prints population counts every 20 ticks):
java -cp bin ecosim.SmokeTest

# Run 100 seeds x 600 ticks in parallel and summarise survival / crashes by season:
java -cp bin ecosim.BatchExperiment            # optional args: [runs] [ticks]
java -Decosim.migration=0 -cp bin ecosim.BatchExperiment   # compare with migration switched off
```

## Project layout

```
src/ecosim/              core model classes (Position, Organism, Plant, Animal, Herbivore,
                          Carnivore, World, Season, CircularBuffer, OrganismFactory), the Observer
                          interface (SimulationListener), stream-based PopulationStats, the Swing
                          GUI (WorldPanel, SimulationFrame, Main), SmokeTest and BatchExperiment
src/ecosim/strategy/      MovementStrategy and its two implementations (Strategy pattern)
src/ecosim/exceptions/    SimulationException hierarchy (InvalidPositionException,
                          OverpopulationException)
design/                   class diagram + program-design.pdf, design-patterns.pdf (and their scripts)
logbook/                  weekly log book (template + Weeks 8-13 entries)
worksheet-7.md            mid-semester worksheet answers
worksheet-13.md           final worksheet answers
```
