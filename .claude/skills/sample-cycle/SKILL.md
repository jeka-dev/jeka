---
name: sample-cycle
description: Runs the JeKa sample repositories with the sample-maintainer agent, records the JeKa bugs it finds in KNOWN_BUGS.md, then has the known-bug-fixer agent fix them, without stopping in between. Use to update and check the samples end to end. Argument (optional): the samples to handle; all those of docs/examples.md otherwise.
---

# Sample cycle

You orchestrate three steps. The agents don't talk to each other: you pass each one what it needs, in its prompt, as
they start with no memory of this conversation. Follow `.claude/RULES.md` yourself for the steps you run.

Don't stop for a go between steps: the user chose to chain them. Stop only on an error that makes the next step pointless,
and report it.

Create a scratch directory for this run, `<run>`, and give each agent its own sub-directory of it.

Create a **reports directory** in the repo, `.claude/reports/<yyyy-mm-dd>-sample-cycle/` (ignored by Git; add `-2`,
`-3`... if it exists). Every agent report of the cycle is kept there, as written, so that the user can read them
afterwards: copy each one there as soon as you get it.

Don't rewrite what an agent found: rewording loses details and adds your own reading. Hand over its words, or the path of
its report.

## 1. Samples

Launch the `sample-maintainer` agent, in the foreground. Its prompt gives:
- the samples to handle: those named in the argument, or "all the samples of `docs/examples.md`";
- its scratch directory, `<run>/samples`;
- "End your report with the parts required by `.claude/RULES.md`."

Write its whole report to the reports directory as `samples.md`.

## 2. Record the bugs

From the **Bug** items of its "To hand over" part, which it writes ready to paste in `KNOWN_BUGS.md`:

1. Read `KNOWN_BUGS.md` on up-to-date `origin/master`. A bug already listed: complete its entry (evidence, other
   location, workaround) with the agent's words, instead of adding a new one. A bug the report says is already fixed on
   master: leave it out.
2. If nothing is left to add or change, skip to the final report.
3. Otherwise, in a clone `<run>/bugs/jeka`, on a branch `known-bugs/samples-<yyyy-mm-dd>`:
   - add the entries **as the agent wrote them**, changing only what the format at the top of `KNOWN_BUGS.md` requires,
     update the entries already there, and update its "updated on" line;
   - commit: `Add the known bugs found by running the samples on <JeKa version>`;
   - push, open a PR and merge it, as the "Merging" part of `.claude/RULES.md` says.

Note the titles of the entries added, and whether the PR was merged.

## 3. Fix

If entries were added and the PR was merged, launch the `known-bug-fixer` agent, in the foreground. Its prompt gives:
- "Take the entries among these, ranked by your rules, up to 3: <titles of the new entries>". Not the whole backlog: this
  cycle is about what the samples found;
- the base: up-to-date `origin/master`, which now holds these entries;
- the path of `samples.md` in the reports directory, for the reproductions in full;
- its scratch directory, `<run>/fix`;
- "End your report with the parts required by `.claude/RULES.md`."

Write its whole report to the reports directory as `fixer.md`.

If the PR of step 2 was left open, don't run the fixer: its base wouldn't hold the entries. Say so in the report.

## Final report

1. The samples table of the `sample-maintainer` report, as it is.
2. **Bugs recorded**: entries added and updated, the PR and whether it was merged.
3. **Fixes**: per bug, status, PR, merged or left open and why. Point out the samples left on an older JeKa because of a bug
   fixed now: they will move at the next release.
4. **Decisions needed** and **PRs left open**, from all the steps, in one list.
5. The other items to hand over (Doc fix, Code fix) that nobody handled.
6. Scratch directories kept, if any, and the Docker images created.
7. The path of the reports directory. Quote a report there rather than paraphrasing it.

Keep the reports directory: only the user deletes it.

End with `KNOWN_BUGS.md: <what changed>`.
