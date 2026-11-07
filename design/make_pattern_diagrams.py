"""Builds design/design-patterns.pdf: one page per design pattern used in the
ecosystem simulation (Template Method, Strategy, Simple Factory, Observer),
showing the participants, their roles and the key interaction.

Run from the repo root:  python3 design/make_pattern_diagrams.py
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyArrowPatch
from matplotlib.backends.backend_pdf import PdfPages

IFACE = ("#fff6e3", "#8a6d1a")
ABSTRACT = ("#e8f0ff", "#2c4a7c")
CONCRETE = ("#eef6ee", "#2f6d31")
CLIENT = ("#f3eefb", "#5b3a8c")
NOTE = ("#fbfbf6", "#9a9a8a")


def new_page(title, subtitle):
    fig, ax = plt.subplots(figsize=(11.7, 8.27))  # A4 landscape
    ax.set_xlim(0, 12)
    ax.set_ylim(0, 8.5)
    ax.axis("off")
    ax.text(0.2, 8.15, title, fontsize=17, fontweight="bold", va="center")
    ax.text(0.2, 7.75, subtitle, fontsize=10, color="#555555", va="center")
    return fig, ax


def box(ax, cx, cy, w, h, title, lines, colours, stereotype=None, italic=False):
    fc, ec = colours
    x, y = cx - w / 2, cy - h / 2
    ax.add_patch(mpatches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.06",
                                         linewidth=1.4, edgecolor=ec, facecolor=fc))
    head = 0.5 if stereotype else 0.38
    ax.plot([x, x + w], [y + h - head, y + h - head], color=ec, linewidth=1.0)
    if stereotype:
        ax.text(cx, y + h - 0.15, stereotype, ha="center", va="center", fontsize=7.5, fontstyle="italic")
        ax.text(cx, y + h - 0.35, title, ha="center", va="center", fontsize=10.5, fontweight="bold",
                fontstyle="italic" if italic else "normal")
    else:
        ax.text(cx, y + h - 0.19, title, ha="center", va="center", fontsize=10.5, fontweight="bold",
                fontstyle="italic" if italic else "normal")
    ax.text(x + 0.12, y + h - head - 0.12, "\n".join(lines), ha="left", va="top",
            fontsize=8, family="monospace", linespacing=1.45)
    return {"top": (cx, y + h), "bottom": (cx, y), "left": (x, cy), "right": (x + w, cy)}


def arrow(ax, p1, p2, kind="dep", label=None, rad=0.0, label_offset=(0, 0.12)):
    styles = {
        "extends": dict(arrowstyle="-|>", linestyle="solid", color="#2c4a7c", mutation_scale=18),
        "implements": dict(arrowstyle="-|>", linestyle="dashed", color="#8a6d1a", mutation_scale=18),
        "dep": dict(arrowstyle="->", linestyle="dashed", color="#5b3a8c", mutation_scale=14),
        "assoc": dict(arrowstyle="->", linestyle="solid", color="#5b3a8c", mutation_scale=14),
        "call": dict(arrowstyle="->", linestyle="solid", color="#b5452b", mutation_scale=14),
    }
    st = styles[kind]
    ax.add_patch(FancyArrowPatch(p1, p2, connectionstyle=f"arc3,rad={rad}", linewidth=1.3, **st))
    if label:
        mx, my = (p1[0] + p2[0]) / 2 + label_offset[0], (p1[1] + p2[1]) / 2 + label_offset[1]
        ax.text(mx, my, label, fontsize=8, color=st["color"], ha="center", va="center",
                bbox=dict(facecolor="white", edgecolor="none", pad=1))


def note(ax, x, y, w, h, text):
    box_ = mpatches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.04",
                                   linewidth=1, edgecolor=NOTE[1], facecolor=NOTE[0])
    ax.add_patch(box_)
    ax.text(x + 0.15, y + h - 0.15, text, fontsize=8.6, va="top", ha="left", wrap=True, linespacing=1.4)


def legend(ax):
    y = 0.25
    items = [("extends", "generalisation (extends)"), ("implements", "realisation (implements)"),
             ("assoc", "holds a reference"), ("dep", "uses / creates"), ("call", "runtime call")]
    x = 0.3
    for kind, label in items:
        arrow(ax, (x, y), (x + 0.55, y), kind)
        ax.text(x + 0.65, y, label, fontsize=7.5, va="center")
        x += 2.35


# ------------------------------------------------------------------ pages
with PdfPages("design/design-patterns.pdf") as pdf:

    # 1. Template Method -------------------------------------------------
    fig, ax = new_page("1. Template Method — Animal.act()",
                       "src/ecosim/Animal.java (template)  ·  Herbivore.java, Carnivore.java (hooks)")
    org = box(ax, 3.0, 6.6, 3.4, 1.0, "Organism", ["+ act(World) {abstract}"], ABSTRACT, "abstract", True)
    ani = box(ax, 3.0, 4.45, 4.2, 2.45, "Animal", [
        "+ act(World) {final}   <- template",
        "    incrementAge()",
        "    energy -= UPKEEP_COST",
        "    if starved/old: die(); return",
        "    move(world)   -> MovementStrategy",
        "    forage(world)            <- hook",
        "    if energy>=threshold:",
        "        tryReproduce(world)  <- hook",
    ], ABSTRACT, "abstract", True)
    herb = box(ax, 1.4, 1.55, 2.6, 1.2, "Herbivore", ["# forage(): eat adjacent Plant", "# tryReproduce()"], CONCRETE)
    carn = box(ax, 4.6, 1.55, 2.6, 1.2, "Carnivore", ["# forage(): eat adjacent", "  Herbivore", "# tryReproduce()"], CONCRETE)
    arrow(ax, ani["top"], org["bottom"], "extends")
    arrow(ax, herb["top"], (2.2, ani["bottom"][1]), "extends")
    arrow(ax, carn["top"], (3.8, ani["bottom"][1]), "extends")
    note(ax, 7.0, 3.2, 4.7, 3.9,
         "Roles\n"
         "• AbstractClass = Animal: act() is final and fixes\n"
         "  the order of the six steps for every species.\n"
         "• Primitive operations (hooks) = forage(),\n"
         "  tryReproduce(): abstract, filled in by each species.\n"
         "• ConcreteClasses = Herbivore, Carnivore.\n\n"
         "Key interaction\n"
         "World.tick() calls organism.act(world). The fixed\n"
         "sequence runs in Animal; it calls *down* into the\n"
         "subclass only for the two steps that genuinely\n"
         "differ (\"Hollywood principle\": don't call us,\n"
         "we'll call you).")
    note(ax, 7.0, 1.15, 4.7, 1.7,
         "Trade-off: subclasses lose the freedom to reorder\n"
         "steps (e.g. eat before moving). Accepted, because\n"
         "ageing/starvation logic written twice would drift\n"
         "out of sync — final makes that impossible.")
    legend(ax)
    pdf.savefig(fig); plt.close(fig)

    # 2. Strategy --------------------------------------------------------
    fig, ax = new_page("2. Strategy — MovementStrategy",
                       "src/ecosim/strategy/  ·  context: Animal.move()")
    ani = box(ax, 2.2, 5.6, 3.4, 1.5, "Animal  (Context)", [
        "- movementStrategy",
        "- move(world):",
        "    target = strategy",
        "      .nextMove(this, world)",
    ], ABSTRACT, "abstract", True)
    ms = box(ax, 7.6, 6.3, 3.6, 1.0, "MovementStrategy", ["+ nextMove(Animal, World): Position"], IFACE, "interface")
    rw = box(ax, 6.0, 3.6, 3.0, 1.2, "RandomWalkStrategy", ["random free neighbour", "or stay put"], CONCRETE)
    fs = box(ax, 9.9, 3.5, 3.6, 2.1, "FleeOrSeekStrategy", [
        "- threatType: Class<? ext Org>",
        "- foodType:   Class<? ext Org>",
        "- fallback: RandomWalkStrategy",
        "1. flee nearest threat",
        "2. else seek nearest food",
        "3. else fallback.nextMove()",
    ], CONCRETE)
    wd = box(ax, 9.9, 1.15, 3.6, 0.9, "World", ["+ findNearest<T>(pos, Class<T>, r)"], CLIENT)
    arrow(ax, ani["right"], ms["left"], "assoc", "delegates to", label_offset=(0, 0.18))
    arrow(ax, rw["top"], (6.6, ms["bottom"][1]), "implements")
    arrow(ax, fs["top"], (8.6, ms["bottom"][1]), "implements")
    arrow(ax, fs["left"], rw["right"], "assoc", "fallback", label_offset=(0, 0.15))
    arrow(ax, fs["bottom"], wd["top"], "dep", "uses")
    note(ax, 0.3, 0.7, 4.1, 3.0,
         "Wiring (in OrganismFactory)\n"
         "Herbivore ← new FleeOrSeekStrategy(\n"
         "                 Carnivore.class, Plant.class)\n"
         "Carnivore ← new FleeOrSeekStrategy(\n"
         "                 null, Herbivore.class)\n\n"
         "Trade-off: one extra object per animal and an\n"
         "indirection, in exchange for writing the\n"
         "flee/seek/wander behaviour once instead of an\n"
         "instanceof chain or two near-identical copies.")
    legend(ax)
    pdf.savefig(fig); plt.close(fig)

    # 3. Simple Factory -------------------------------------------------
    fig, ax = new_page("3. Simple Factory — OrganismFactory",
                       "src/ecosim/OrganismFactory.java  ·  clients: SimulationFrame, SmokeTest, BatchExperiment, World, Herbivore, Carnivore")
    fac = box(ax, 6.7, 5.2, 5.0, 1.9, "OrganismFactory", [
        "+ createPlant(Position)          {static}",
        "+ createHerbivore(Position)      {static}",
        "+ createHerbivore(Position, int) {static}",
        "+ createCarnivore(Position)      {static}",
        "+ createCarnivore(Position, int) {static}",
    ], CLIENT, "final, private constructor")
    seeders = box(ax, 1.6, 6.5, 2.8, 1.0, "Seeding code", ["SimulationFrame", "SmokeTest, BatchExperiment"], NOTE)
    babies = box(ax, 1.6, 4.8, 2.8, 1.0, "Reproduction", ["Herbivore.tryReproduce", "Carnivore.tryReproduce"], NOTE)
    world = box(ax, 1.6, 3.1, 2.8, 1.0, "World", ["maybeSeedAmbientPlant", "maybeMigrate"], NOTE)
    p = box(ax, 5.0, 2.6, 1.8, 0.75, "Plant", [], CONCRETE)
    h = box(ax, 7.3, 2.6, 2.2, 0.75, "Herbivore", [], CONCRETE)
    c = box(ax, 9.8, 2.6, 2.2, 0.75, "Carnivore", [], CONCRETE)
    fsb = box(ax, 10.2, 6.5, 2.6, 0.8, "FleeOrSeekStrategy", [], CONCRETE)
    for src, ty in ((seeders, 5.7), (babies, 5.2), (world, 4.7)):
        arrow(ax, src["right"], (4.2, ty), "call")
    for t in (p, h, c):
        arrow(ax, fac["bottom"], t["top"], "dep", None)
    arrow(ax, fac["right"], fsb["left"], "dep", "wires", label_offset=(0, 0.15))
    note(ax, 3.9, 0.55, 7.8, 1.15,
         "Week-8 change: the (Position, int) overloads let a newborn carry exactly the energy its parent paid,\n"
         "fixing a bug where every birth created energy from nothing. Because construction lived in one place,\n"
         "the fix touched one class plus two call sites.")
    legend(ax)
    pdf.savefig(fig); plt.close(fig)

    # 4. Observer --------------------------------------------------------
    fig, ax = new_page("4. Observer — SimulationListener (added Weeks 8–10)",
                       "src/ecosim/SimulationListener.java  ·  subject: World  ·  observers: SimulationFrame, SmokeTest, BatchExperiment")
    w = box(ax, 2.5, 5.4, 4.2, 2.1, "World  (Subject)", [
        "- listeners: CopyOnWriteArrayList",
        "+ addListener(l)",
        "+ removeListener(l)",
        "+ tick():",
        "    ...organisms act...",
        "    notifyListeners()",
    ], CLIENT)
    sl = box(ax, 8.4, 6.2, 4.4, 1.3, "SimulationListener", [
        "+ onTickCompleted(World)",
        "+ onExtinction(World, Class) {default}",
    ], IFACE, "interface, @FunctionalInterface")
    gui = box(ax, 6.3, 3.3, 2.6, 1.2, "SimulationFrame", ["anonymous class:", "repaint + status bar"], CONCRETE)
    st = box(ax, 8.9, 3.3, 2.3, 1.2, "SmokeTest", ["lambda: print a row", "every 20 ticks"], CONCRETE)
    be = box(ax, 11.1, 3.3, 1.6, 1.2, "BatchExp.", ["per-run", "crash log"], CONCRETE)
    arrow(ax, w["right"], sl["left"], "assoc", "0..* listeners", label_offset=(0, 0.2))
    arrow(ax, gui["top"], (7.0, sl["bottom"][1]), "implements")
    arrow(ax, st["top"], (8.9, sl["bottom"][1]), "implements")
    arrow(ax, be["top"], (10.3, sl["bottom"][1]), "implements")

    # mini sequence diagram
    sx = [0.8, 2.6, 4.4]
    names = ["Timer / Step", "World", "each listener"]
    top, bot = 3.6, 0.75
    for x, n in zip(sx, names):
        ax.text(x, top + 0.18, n, ha="center", fontsize=8.5, fontweight="bold")
        ax.plot([x, x], [top, bot], color="#999999", linestyle=":", linewidth=1)
    steps = [(0, 1, "tick()"), (1, 1, "organisms act, history recorded"),
             (1, 2, "onExtinction(w, species)  [if a count just hit 0]"), (1, 2, "onTickCompleted(w)")]
    y = top - 0.45
    for a, b, label in steps:
        if a == b:
            ax.text(sx[a] + 0.1, y, "↺ " + label, fontsize=7.6, va="center")
        else:
            arrow(ax, (sx[a], y), (sx[b], y), "call")
            ax.text((sx[a] + sx[b]) / 2, y + 0.14, label, fontsize=7.4, ha="center")
        y -= 0.6
    note(ax, 5.2, 0.55, 5.4, 1.75,
         "Trade-off: the order in which observers run is not\n"
         "guaranteed by the design, and a slow observer slows\n"
         "every tick. Accepted: World no longer needs to know\n"
         "the GUI exists, so the same World runs headless in\n"
         "SmokeTest and in parallel in BatchExperiment.")
    legend(ax)
    pdf.savefig(fig); plt.close(fig)

print("wrote design/design-patterns.pdf")
