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
3. Create a scratch directory for this run, `<run>`, and a **reports directory** in the repo,
   `.claude/reports/<yyyy-mm-dd>-doc-cycle-<topic>/` (ignored by Git; add `-2`, `-3`... if it exists). Every agent report
   of the cycle is kept there, as written, so that the user can read them afterwards: copy each one there as soon as it
   is written (`naive-1.md`, `naive-2.md`... `writer-1.md`, `check-1.md`...).
4. **Generate the docs** as users see them, with the local build on PATH and `JEKA_USER_HOME=<run>/home`, from the repo
   root: `jeka build: enrichMkDocs`. It writes in `jeka-output/mkdocs/docs/`, which is ignored by Git. The reference
   pages then show the KBeans of this build.

## 1. Test

Split the argument into scenarios: one per page or goal given, or the default scenarios of `naive`.

**Don't launch `naive` as a sub-agent**: a sub-agent gets the repo's `CLAUDE.md` and the project memory in its context,
which tell it how JeKa works and hide the gaps of the docs. Run it in a separate session, outside the repo, with
`.claude/skills/doc-cycle/run-naive.sh <run>/naive-<n> "<scenario>"`. It copies the docs (generated when step 0.4 ran),
the plugin READMEs and the local build to `<run>/naive-<n>`, maps the user's JDKs in an isolated JeKa home, and writes
the report to `<run>/naive-<n>/report.md`.

Launch one per scenario, in the background (Bash `run_in_background`), in parallel, and wait for all of them. Copy
each `<run>/naive-<n>/report.md` to the reports directory as `naive-<n>.md`.

## 2. Sort the findings

Don't rewrite the findings: rewording loses details and adds your own reading. Only decide what doc-writer must skip:

- **Duplicates**: a finding met by several scenarios. Keep the most detailed one, and list the others to skip.
- **JeKa bugs**: a finding caused by JeKa, not by the docs, and the **Bug** items. Compare them with `KNOWN_BUGS.md`. Add
  the new ones, and complete the entries already there, in the user's checkout, following the rules at the top of the
  file. Then list these findings to skip.
- **Code fixes and decisions**: not for doc-writer. Keep them for the final report.

If no doc finding is left, skip to the final report.

## 3. Fix

Launch **one** `doc-writer` agent, in the foreground: several would edit the same files. Its prompt gives:
- the **paths of the naive reports** in the reports directory, to read in full: their findings, with "Suggested doc fix",
  are its input, as written;
- the findings to skip, by report and title, with the reason in a few words (duplicate of..., JeKa bug added to
  `KNOWN_BUGS.md`...);
- the bugs just added to `KNOWN_BUGS.md`, so that it documents their workaround where readers need it;
- what it can't find in the reports: the JeKa versions used, and that the user's uncommitted changes in these files, if
  any, must be kept;
- its scratch directory, `<run>/writer`;
- "End your report with the parts required by `.claude/RULES.md`."

Your own words in this prompt are limited to these points. Copy its report to the reports directory as `writer-<round>.md`.

## 4. Check

Generate the docs again (step 0.4). Run `run-naive.sh` again, on the scenarios that had findings, with new run
directories. A real newcomer reads the fixed pages: don't tell it what changed, only which scenario to follow. Copy its
report to the reports directory as `check-<round>-<n>.md`, then compare it with the findings of step 1 yourself.

If findings remain, run steps 3 and 4 once more, for those only. Then stop, whatever the result: two rounds at most.

## Final report

1. Per scenario: outcome before and after (completed, with workarounds, blocked).
2. **Fixed**: findings fixed and checked, with the pages changed.
3. **Remaining**: findings still there after the last round, and those doc-writer could not fix, with the reason.
4. **Code fixes and decisions** handed over by the agents, in one list.
5. The uncommitted changes the user had in these files before the cycle, if any.
6. The path of the reports directory. Quote a report there rather than paraphrasing it.

Delete the run directories (`<run>/naive-<n>` hold JeKa caches), unless the user may need them: then list them. Keep the
reports directory: only the user deletes it.

End with `KNOWN_BUGS.md: <what changed>`.
