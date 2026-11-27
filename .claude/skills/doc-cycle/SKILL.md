---
name: doc-cycle
description: Tests the JeKa documentation with the naive agent, has the doc-writer agent fix what it found, then has naive check the fixes. Use to improve a page, a topic or the whole docs end to end. Argument (optional): pages or goals to test; the default scenarios of the naive agent otherwise.
---

# Doc cycle

You orchestrate the agents. They don't talk to each other: you pass each one what it needs, in its prompt, as they start
with no memory of this conversation. Follow `.claude/RULES.md` yourself for the steps you run.

The result is **uncommitted changes in the user's checkout** (`docs/`, `mkdocs.yml`, plugin READMEs, `@JkDoc`, and
`KNOWN_BUGS.md`), for the user to review and commit. Don't commit them.

## 0. Prepare

1. Run `git status` on `docs/`, `mkdocs.yml` and `KNOWN_BUGS.md`. If the user has uncommitted changes there, say so in
   the final report: doc-writer will edit the same files.
2. Check that the local build exists: `core/jeka-output/distrib/bin/jeka`. If not, stop and ask the user to bootstrap
   it (`CLAUDE.md`): don't build it yourself, it is the user's global `jeka`.
3. Create a scratch directory for this run, `<run>`.
4. **Generate the docs** as users see them, with the local build on PATH and `JEKA_USER_HOME=<run>/home`, from the repo
   root: `jeka build: enrichMkDocs`. It writes in `jeka-output/mkdocs/docs/`, which is ignored by Git. The reference
   pages then show the KBeans of this build.

## 1. Test

Split the argument into scenarios: one per page or goal given, or the default scenarios of `naive`. Launch one `naive`
agent per scenario, in the background, in parallel. Each prompt gives:
- the scenario, as given;
- its scratch directory, `<run>/naive-<n>`;
- "The generated docs are in `jeka-output/mkdocs/docs/`. End your report with the parts required by `.claude/RULES.md`."

Wait for all of them.

## 2. Sort the findings

- **Doc findings** (Blocker, Wrong, Missing, Confusing, Findability, Doc fix): merge them, and drop the duplicates found
  by several scenarios. Keep each one as its agent wrote it: doc-writer needs the details.
- A finding caused by a JeKa bug, not by the docs: it goes with the **Bug** items.
- **Bug** items: compare them with `KNOWN_BUGS.md`. Add the new ones, and complete the entries already there, in the
  user's checkout, following the rules at the top of the file.

If there is no doc finding, skip to the final report.

## 3. Fix

Launch **one** `doc-writer` agent, in the foreground: several would edit the same files. Its prompt gives:
- the doc findings, as the naive agents wrote them;
- the bugs just added to `KNOWN_BUGS.md`, so that it documents their workaround where readers need it;
- its scratch directory, `<run>/writer`;
- "End your report with the parts required by `.claude/RULES.md`."

## 4. Check

Generate the docs again (step 0.4). Launch `naive` again, on the scenarios that had findings, with new scratch
directories, and the list of the findings it should no longer meet. A real newcomer reads the fixed pages: don't tell
it what changed, only which scenario to follow.

If findings remain, run steps 3 and 4 once more, for those only. Then stop, whatever the result: two rounds at most.

## Final report

1. Per scenario: outcome before and after (completed, with workarounds, blocked).
2. **Fixed**: findings fixed and checked, with the pages changed.
3. **Remaining**: findings still there after the last round, and those doc-writer could not fix, with the reason.
4. **Code fixes and decisions** handed over by the agents, in one list.
5. The uncommitted changes the user had in these files before the cycle, if any.

End with `KNOWN_BUGS.md: <what changed>`.
