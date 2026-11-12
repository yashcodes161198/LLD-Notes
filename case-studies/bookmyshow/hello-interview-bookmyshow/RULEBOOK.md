# Rulebook: how to fill the workbooks, and how an AI evaluates them

This rulebook works for every case study made by the `lld-interview-prep` skill. Paths are relative to the case-study folder, e.g. `LLD-Notes/case-studies/bookmyshow/hello-interview-bookmyshow/`.

```
bookmyshow.py             annotated solution: study it, then close it
workbook/0N_*.py          blank exercises: never edit these directly
answer-key/0N_*.key.md    scoring keys: the student must NOT open these before submitting
attempts/YYYY-MM-DD/      your filled copies, plus the evaluator's reports
screenshots/              frames from the reference video
source/                   video, transcript, original editor file
```

---

## 1 · For the student: how to fill a workbook

1. **Copy, don't edit.** `mkdir -p attempts/$(date +%F) && cp workbook/01_requirements.py attempts/$(date +%F)/`
2. **Closed book.** Don't open `bookmyshow.py`, `answer-key/` or the video while filling in a workbook.
3. **Time box.** Each workbook header has a time limit. Set a timer. When it rings, stop and write `# TIME UP` where you stopped. Finishing after that is allowed, but the evaluator scores the part before `# TIME UP` separately (see §3.6).
4. **Replace every `TODO`.** Leave none behind. If you don't know, write `# SKIP: <what you'd ask the interviewer>`. A skip scores 0 but is not penalised as a wrong answer.
5. **Write as if you're talking.** The talk-track and "Say" fields are graded. Write what you'd actually say, in full sentences.
6. **Pseudo-code is fine.** Any consistent syntax. Don't worry about imports or types.
7. **Order.** Do 01 → 02 → 03 → 05 in one sitting if you like. Do **04 on a different day** from 03, so the hints have faded.
8. **Submit.** Ask: *"Evaluate attempts/YYYY-MM-DD/0N_*.py for bookmyshow (hello-interview-bookmyshow)"*. The evaluator follows §3.
9. **Retry.** Redo any workbook that scored under 70 after 2–3 days, in a new dated folder. Progress is tracked across folders (§4).

---

## 2 · Which workbook tests what

| # | Workbook | Framework step | Main rubric dimension |
|---|---|---|---|
| 01 | Requirements | 1 | Problem analysis, communication |
| 02 | Entities + class design | 2–3 | Class design |
| 03 | Implementation, guided (core logic and edge cases given) | 4 | Code quality |
| 04 | Implementation, blank (write your own docstrings) | 4 | Code quality, communication |
| 05 | Extensibility | 5 | Extensibility |

Each workbook gives the **agreed answer from the step before** as its input, so a mistake in 01 never carries into 02.

---

## 3 · For the AI evaluator: how to grade

**Inputs:** the submitted file `attempts/<date>/0N_*.py` and `answer-key/0N_*.key.md`. Read `bookmyshow.py` too, for context on accepted variants. Read the whole key before scoring.

### 3.1 Scoring procedure
1. Find each answer by the item ID in the workbook (e.g. `[I3-3]`, `X2`, `A3`, which maps to key item Q3) or by its section.
2. For every key item, check each **Must** criterion separately. Award its points only if the meaning is present. **Accept equivalent wording, names and syntax.** Never deduct for typos, naming or language choice.
3. Apply **Partial** rules exactly as written. Don't invent partial credit the key doesn't list, except for the general case: a criterion that is half-present gets at most half its points.
4. Apply **Red flags** as deductions. A single item's score never goes below 0.
5. **Bonus** points only where the key lists them, and never above the item's cap.
6. For code items, **run the logic mentally** on the key's trace or on a two-compartment example. If it would behave wrongly, deduct for the failed criterion even if it "looks" right.
7. **An answer better than the key** (correct, simpler, or catches something the key missed) gets full points, and the report says so in "Better than the key". Don't punish improvements.
8. A wrong statement presented as fact (e.g. "version A is idempotent") gets 0 for that criterion, even if other parts of the item are right.

### 3.2 What NOT to do
- Don't reveal the model answer for items scored ≥ 80% of their points. Show the model answer only for items the student lost points on.
- Don't rewrite the student's whole file. Quote at most 2 lines per item.
- Don't grade style beyond what the key says.

### 3.3 Level
Use the key's **Level calibration**, plus any caps it states (e.g. key 03 caps at "mid" without the I3-7 bug-spot).

### 3.4 Report format
Write to `attempts/<date>/0N_eval.md` **and** print a short summary in chat:

```markdown
# Eval 0N <workbook name>: <score>/100 · Level: <junior|mid|senior|staff>
Time-boxed score (before `# TIME UP`): <score or "n/a">

| Item | Score | Max | Verdict |
|------|------:|----:|---------|
| Q1   | 10 | 10 | ✅ |
| R3   | 3  | 5  | ⚠️ missing distinct "expired" error |
| ...  |    |    |         |

## Lost points (fix these)
- **R3 (−2):** <what was missing> → <the model answer, one or two lines>

## Strong moments
- <specific thing done well, quoted>

## Better than the key
- <if any, otherwise omit this section>

## Next drill
- <1–3 concrete actions, e.g. "re-read TRICKY #4 in bookmyshow.py, redo 04 in 3 days">
```

Verdict icons: ✅ full points · ⚠️ partial · ❌ zero · ⏭ skipped.

### 3.5 Integrity checks
- A score of 100 with no `TODO` left and answers that copy the answer key word for word: note **"possible copy"** in the report and don't assign a level.
- Leftover `TODO`s: treat as skipped.

### 3.6 Time box
If the file contains `# TIME UP`, score twice: once for everything, and once with only the content above the marker. Report both, and use the time-boxed one for the level.

---

## 4 · Tracking progress

After each evaluation, append one line to `attempts/progress.md` (create it if it's missing):

```
| date | workbook | score | time-boxed | level | top gap |
```

When 03 and 04 both have scores, report the **03 → 04 delta**. A drop of more than 15 points means the core-logic/edge-case habit isn't internalised yet. Then recommend redoing 04 only.

---

## 5 · Readiness bar for a real interview
- 01, 02, 04 each ≥ 80 time-boxed, 05 ≥ 70 (senior) or ≥ 50 (mid).
- You can say the 60-second recap at the bottom of `bookmyshow.py` from memory.
