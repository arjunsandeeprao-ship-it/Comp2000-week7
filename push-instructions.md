# How to get this onto GitHub for your submission

This folder is already a git repository with a clean, meaningful commit history (13 commits — see
`git-log-sample.txt` for the exact `git log --graph --oneline --all` output, which is what question
1.1 in the worksheet is asking for).

You have two realistic situations. Pick whichever matches you:

## A) Your team already has a shared GitHub repository

The worksheet is explicit that your submitted URL must be **your own fork** of your team's repo, not
the team's URL itself.

1. On GitHub, open your team's repository and click **Fork** (top right) — this creates
   `github.com/<your-username>/<repo-name>` under your own account.
2. Clone your fork somewhere, e.g.:
   ```bash
   git clone https://github.com/<your-username>/<repo-name>.git
   cd <repo-name>
   ```
3. Copy everything from this `ecosim-project` folder into your cloned fork (or merge it in,
   if your team already has files — use your judgement on folder structure so it doesn't clash).
4. Commit and push:
   ```bash
   git add .
   git commit -m "Add ecosystem simulation scaffold for worksheet 7"
   git push origin main
   ```
   (If you'd rather preserve this project's own 13-commit history exactly as-is instead of one
   squashed commit, add it as a remote and merge histories instead — ask if you want a hand with
   that; it's a bit fiddlier with unrelated histories.)
5. Re-run `git log --graph --oneline --all` in your fork and update worksheet-7.md question 1.1 with
   the real output (it will now include your team's history too).
6. Put your fork's URL at the top of `worksheet-7.md`.

## B) You/your team haven't created a repository yet

1. Create a new **empty** repository on GitHub (don't let it auto-initialise a README — you already
   have commits locally and an empty remote avoids a merge).
2. From inside this `ecosim-project` folder:
   ```bash
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git branch -M main
   git push -u origin main
   ```
3. Because the remote was empty, this preserves the exact 13-commit history already in this repo —
   `git log --graph --oneline --all` will match `git-log-sample.txt` exactly.
4. If this is meant to become your **team's** shared repo, invite your teammates as collaborators
   (Settings → Collaborators) so they can push their own commits and open PRs into it; then, per the
   submission instructions, each of you should submit the URL of your *own fork* of it, not this URL
   directly.
5. Put the repo (or your fork's) URL at the top of `worksheet-7.md`.

## Getting the HD-level "branches / PRs" evidence

The repo now has two small real feature branches sitting off `main`, each with one genuine,
compiling, working commit:

- `feature/reset-confirmation` — adds a confirmation dialog before the Reset button wipes out a
  running simulation.
- `feature/smoketest-summary` — adds an end-of-run summary block (averages, survival, overpopulation
  count) to `SmokeTest`'s output.

These were deliberately left **unmerged** so that *you* do the actual merge, for real, on GitHub —
that gives you an authentic, visible Pull Request in your repo's history, which is much stronger
evidence for the version-control rubric's HD row ("branches, PRs, or equivalent collaborative
workflow") than a merge commit nobody can see was a PR. It only takes a couple of minutes:

1. After the initial push (`git push -u origin main`), push both branches too:
   ```bash
   git push -u origin feature/reset-confirmation
   git push -u origin feature/smoketest-summary
   ```
2. On GitHub, open your repo — it will show a banner offering to open a pull request for each
   recently-pushed branch ("Compare & pull request"). Click it for `feature/reset-confirmation`,
   review the diff, and click **Merge pull request** (then **Confirm merge**).
3. Do the same for `feature/smoketest-summary`.
4. Pull the merges back down locally so your local `main` matches:
   ```bash
   git checkout main
   git pull origin main
   ```
5. Re-run `git log --graph --oneline --all` — you'll now see two real merge commits referencing PR
   numbers, which is exactly what the rubric is asking for. Update worksheet-7.md question 1.1 with
   this final output.

If you want to genuinely practice the workflow rather than just merge immediately, you could also
read the diff, imagine you're reviewing a teammate's PR, and leave a review comment on GitHub before
approving/merging — that's a completely real way to demonstrate the collaborative review process
even solo.

## Either way, before you submit

- [ ] Replace every `[[FILL IN: ...]]` marker in `worksheet-7.md` (name, ID, repo URL, your real
      git workflow, commit percentage, and your personal reflections in sections 4 and 5).
- [ ] Back-fill `logbook/logbook-template.md` with your actual weekly entries (or your team's, if
      you've been keeping one already — don't just submit this template blank).
- [ ] Read through the code in `src/ecosim/` — the worksheet answers explain the *design*, but you
      should be able to explain it yourself too, especially for the Week 7 and Week 13 showcases.
- [ ] Submit exactly two files, not zipped: your completed `worksheet-7.md` (as required by your
      unit, likely converted to the format they want — check the assignment page) and your logbook.
